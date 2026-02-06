package com.booyahx;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.booyahx.payment.TransactionAdapter;
import com.booyahx.payment.PaymentTopUpDialog;
import com.booyahx.payment.WithdrawDialog;
import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.TopUpHistoryResponse;
import com.booyahx.network.models.Transaction;
import com.booyahx.network.models.WalletBalanceResponse;
import com.booyahx.network.models.WithdrawalLimitResponse;
import com.booyahx.socket.SocketManager;
import com.booyahx.WalletLimitCache;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.socket.client.Socket;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WalletFragment extends Fragment {

    private static final String TAG = "WalletFragment";

    private TextView tvBalance;
    private TextView btnTopUp, btnWithdraw;
    private RecyclerView rvTransactions;
    private TransactionAdapter transactionAdapter;
    private List<Transaction> transactionList;
    private ApiService api;

    // Pagination
    private int currentSkip = 0;
    private int limitPerPage = 50;
    private boolean isLoading = false;
    private boolean hasMore = true;

    // 🔥 SOCKET FOR REAL-TIME WALLET UPDATES
    private Socket socket;

    // FIX 1: BroadcastReceiver for wallet updates
    private BroadcastReceiver walletUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isAdded()) {
                loadBalanceFromCache();
                loadBalanceFromAPI();
                fetchWithdrawLimitSilently();
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.activity_wallet, container, false);

        tvBalance = view.findViewById(R.id.tvBalance);
        btnTopUp = view.findViewById(R.id.btnTopUp);
        btnWithdraw = view.findViewById(R.id.btnWithdraw);
        rvTransactions = view.findViewById(R.id.rvTransactions);

        api = ApiClient.getClient(requireContext()).create(ApiService.class);

        rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionList = new ArrayList<>();
        transactionAdapter = new TransactionAdapter(getContext(), transactionList);
        rvTransactions.setAdapter(transactionAdapter);

        // 🔥 CHECK ROLE AND HIDE TOP-UP FOR HOSTS
        String role = TokenManager.getRole(requireContext());
        if ("host".equalsIgnoreCase(role)) {
            btnTopUp.setVisibility(View.GONE);

            // Center the withdraw button by removing layout weight/gravity constraints
            ViewGroup.LayoutParams params = btnWithdraw.getLayoutParams();
            if (params instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) params;
                // Remove any start margin to center it
                marginParams.setMarginStart(0);
                btnWithdraw.setLayoutParams(marginParams);
            }
        }

        // 🔥 LOAD FROM CACHE FIRST (INSTANT), THEN API (BACKGROUND)
        loadBalanceFromCache();
        loadBalanceFromAPI();

        // 🔥 LOAD REAL TRANSACTIONS FROM API
        loadTransactionsFromAPI();

        // 🔥 FETCH WITHDRAWAL LIMIT SILENTLY IN BACKGROUND
        fetchWithdrawLimitSilently();

        btnTopUp.setOnClickListener(v -> {
            PaymentTopUpDialog dialog = new PaymentTopUpDialog(requireContext(), WalletFragment.this);
            dialog.show();
        });

        // 🔥 OPEN WITHDRAW DIALOG
        btnWithdraw.setOnClickListener(v -> {
            WithdrawDialog dialog = new WithdrawDialog(requireContext());
            dialog.show();
        });

        // 🔥 PAGINATION - Load more when scrolling to bottom
        rvTransactions.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && !isLoading && hasMore) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5
                            && firstVisibleItemPosition >= 0) {
                        loadMoreTransactions();
                    }
                }
            }
        });

        // 🔥 LISTEN FOR BALANCE UPDATES FROM OTHER FRAGMENTS
        getParentFragmentManager().setFragmentResultListener(
                "balance_updated",
                this,
                (requestKey, bundle) -> {
                    if (isAdded()) {
                        loadBalanceFromCache();
                        refreshData(); // Also refresh transactions
                    }
                }
        );

        // FIX 1: Register broadcast receiver
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
                walletUpdateReceiver,
                new IntentFilter("WALLET_UPDATED")
        );

        // 🔥 SETUP SOCKET LISTENERS FOR WALLET
        setupSocketListeners();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 🔥 ENSURE SOCKET CONNECTED ON RESUME
        if (socket != null && !socket.connected()) {
            SocketManager.connect();
        }
        // 🔥 REFRESH FROM CACHE WHEN RETURNING
        loadBalanceFromCache();
        // 🔥 REFRESH WITHDRAWAL LIMIT SILENTLY
        fetchWithdrawLimitSilently();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // FIX 1: Unregister broadcast receiver
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(walletUpdateReceiver);

        // 🔥 REMOVE SOCKET LISTENERS
        removeSocketListeners();
    }

    // 🔥 SETUP SOCKET LISTENERS FOR WALLET UPDATES
    private void setupSocketListeners() {
        String token = TokenManager.getAccessToken(requireContext());
        socket = SocketManager.getSocket(token);

        if (socket == null) {
            Log.e(TAG, "Socket is null, cannot setup listeners");
            return;
        }

        // 🔥 ENSURE SOCKET CONNECTED
        if (!socket.connected()) {
            SocketManager.connect();
        }

        String userId = TokenManager.getUserId(requireContext());

        // 🔥 SUBSCRIBE TO WALLET UPDATES
        if (socket.connected() && userId != null) {
            socket.emit("subscribe:wallet", userId);
            Log.d(TAG, "✅ Subscribed to wallet updates for user: " + userId);
        }

        // 🔥 RE-SUBSCRIBE ON RECONNECT
        socket.on(Socket.EVENT_CONNECT, args -> {
            String uid = TokenManager.getUserId(requireContext());
            if (uid != null) {
                socket.emit("subscribe:wallet", uid);
                Log.d(TAG, "✅ Re-subscribed to wallet on reconnect");
            }
            // 🔥 REFRESH DATA ON RECONNECT
            if (isAdded()) {
                android.app.Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        loadBalanceFromAPI();
                        refreshData();
                    });
                }
            }
        });

        // 🔥 LISTEN FOR BALANCE UPDATES
        socket.on("wallet:balance-updated", args -> {
            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    double newBalance = data.optDouble("balanceGC", 0);

                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            // Update cache
                            WalletCacheManager.saveBalance(requireContext(), newBalance);

                            // Update UI
                            int rupees = (int) Math.round(newBalance);
                            tvBalance.setText(String.valueOf(rupees));

                            Log.d(TAG, "✅ Balance updated via socket: " + newBalance);
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing balance update", e);
                }
            }
        });

        // 🔥 LISTEN FOR TRANSACTION UPDATES
        socket.on("wallet:transaction-updated", args -> {
            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String action = data.optString("action", "");

                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            // Refresh transactions list
                            refreshData();

                            Log.d(TAG, "✅ Transaction updated via socket: " + action);
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing transaction update", e);
                }
            }
        });

        // 🔥 LISTEN FOR WALLET HISTORY UPDATES
        socket.on("wallet:history-updated", args -> {
            if (args.length > 0) {
                try {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            // Refresh transactions list
                            refreshData();

                            Log.d(TAG, "✅ Wallet history updated via socket");
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing wallet history update", e);
                }
            }
        });

        // 🔥 LISTEN FOR QR PAYMENT STATUS UPDATES
        socket.on("payment:qr-status-updated", args -> {
            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String status = data.optString("status", "");
                    String action = data.optString("action", "");

                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            if ("success".equalsIgnoreCase(status)) {
                                Toast.makeText(requireContext(),
                                        "Payment successful!",
                                        Toast.LENGTH_SHORT).show();

                                // Refresh balance and transactions
                                loadBalanceFromAPI();
                                refreshData();
                            }

                            Log.d(TAG, "✅ QR Payment status updated: " + status + " - " + action);
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing QR payment status", e);
                }
            }
        });

        Log.d(TAG, "✅ Socket listeners setup complete");
    }

    // 🔥 REMOVE SOCKET LISTENERS ON DESTROY
    private void removeSocketListeners() {
        if (socket != null) {
            String userId = TokenManager.getUserId(requireContext());
            if (userId != null) {
                socket.emit("unsubscribe:wallet", userId);
            }

            socket.off("wallet:balance-updated");
            socket.off("wallet:transaction-updated");
            socket.off("wallet:history-updated");
            socket.off("payment:qr-status-updated");

            Log.d(TAG, "✅ Socket listeners removed");
        }
    }

    // 🔥 LOAD FROM CACHE (INSTANT)
    private void loadBalanceFromCache() {
        if (WalletCacheManager.hasBalance(requireContext())) {
            int balance = WalletCacheManager.getBalanceAsInt(requireContext());
            tvBalance.setText(String.valueOf(balance));
        }
    }

    // 🔥 LOAD FROM API (BACKGROUND - ENSURES FRESH DATA)
    private void loadBalanceFromAPI() {
        api.getWalletBalance().enqueue(new Callback<WalletBalanceResponse>() {
            @Override
            public void onResponse(Call<WalletBalanceResponse> call, Response<WalletBalanceResponse> res) {
                if (res.isSuccessful()
                        && res.body() != null
                        && res.body().data != null) {

                    double balance = res.body().data.balanceGC;

                    // 🔥 UPDATE CACHE
                    WalletCacheManager.saveBalance(requireContext(), balance);

                    // Update UI
                    int rupees = (int) Math.round(balance);
                    tvBalance.setText(String.valueOf(rupees));
                }
            }

            @Override
            public void onFailure(Call<WalletBalanceResponse> call, Throwable t) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to load balance", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Silently fetch withdrawal limit in background without showing loader
     */
    private void fetchWithdrawLimitSilently() {
        ApiService apiService = ApiClient.getClient(getContext()).create(ApiService.class);

        apiService.getWithdrawLimit().enqueue(new Callback<WithdrawalLimitResponse>() {
            @Override
            public void onResponse(Call<WithdrawalLimitResponse> call, Response<WithdrawalLimitResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WithdrawalLimitResponse limitResponse = response.body();

                    if (limitResponse.isSuccess() && limitResponse.getData() != null) {
                        // Save to cache silently
                        WalletLimitCache.saveLimit(
                                getContext(),
                                limitResponse.getData().getMaxWithdrawableGC(),
                                limitResponse.getData().getBalanceGC(),
                                limitResponse.getData().getTotalDepositedGC(),
                                limitResponse.getData().getWithdrawnGC()
                        );

                        Log.d(TAG, "Withdrawal limit cached silently: " +
                                limitResponse.getData().getMaxWithdrawableGC() + " GC");
                    } else {
                        // Mark as unavailable if API returns error
                        WalletLimitCache.markUnavailable(getContext());
                        Log.e(TAG, "Failed to fetch limit: " + limitResponse.getMessage());
                    }
                } else {
                    // Mark as unavailable on response error
                    WalletLimitCache.markUnavailable(getContext());
                    Log.e(TAG, "Response unsuccessful");
                }
            }

            @Override
            public void onFailure(Call<WithdrawalLimitResponse> call, Throwable t) {
                // Mark as unavailable on network failure
                WalletLimitCache.markUnavailable(getContext());
                Log.e(TAG, "Network error fetching limit: " + t.getMessage());
            }
        });
    }

    // 🔥 LOAD TRANSACTIONS FROM API
    private void loadTransactionsFromAPI() {
        if (isLoading) return;

        isLoading = true;

        api.getTopUpHistory(limitPerPage, currentSkip).enqueue(new Callback<TopUpHistoryResponse>() {
            @Override
            public void onResponse(Call<TopUpHistoryResponse> call, Response<TopUpHistoryResponse> response) {
                isLoading = false;

                if (response.isSuccessful() && response.body() != null) {
                    TopUpHistoryResponse.TopUpHistoryData data = response.body().data;

                    if (data != null && data.history != null) {
                        if (currentSkip == 0) {
                            // First load - replace all
                            transactionList.clear();
                        }

                        transactionList.addAll(data.history);
                        transactionAdapter.notifyDataSetChanged();

                        // Check if there's more data
                        hasMore = data.history.size() >= limitPerPage;
                    }
                } else {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed to load transactions", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<TopUpHistoryResponse> call, Throwable t) {
                isLoading = false;

                if (isAdded()) {
                    Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // 🔥 LOAD MORE TRANSACTIONS (PAGINATION)
    private void loadMoreTransactions() {
        currentSkip += limitPerPage;
        loadTransactionsFromAPI();
    }

    // 🔥 REFRESH ALL DATA
    private void refreshData() {
        currentSkip = 0;
        hasMore = true;
        loadBalanceFromAPI();
        loadTransactionsFromAPI();
    }

    // 🔥 CALLED WHEN USER ADDS A PENDING TOP-UP (OPTIMISTIC UI UPDATE)
    public void addPendingTopup(int amount) {
        Transaction t = new Transaction(
                "Just Now",
                "topup",
                "+" + amount + " GC",
                "pending",
                "Processing...",
                true
        );
        transactionList.add(0, t);
        transactionAdapter.notifyItemInserted(0);
        rvTransactions.scrollToPosition(0);
    }

    // 🔥 CALLED AFTER SUCCESSFUL TOP-UP
    public void refreshBalance() {
        loadBalanceFromAPI(); // Force refresh from API
        refreshData(); // Refresh transactions too
    }
}