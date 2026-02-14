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

import java.util.ArrayList;
import java.util.List;

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

    private int currentSkip = 0;
    private int limitPerPage = 50;
    private boolean isLoading = false;
    private boolean hasMore = true;

    // ✅ ONLY BroadcastReceiver - NO socket instance here
    // Socket events are handled by DashboardActivity and sent via broadcasts
    private BroadcastReceiver walletUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isAdded()) {
                loadBalanceFromCache();
                loadBalanceFromAPI();
                refreshData();
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

        // Initialize views
        tvBalance = view.findViewById(R.id.tvBalance);
        btnTopUp = view.findViewById(R.id.btnTopUp);
        btnWithdraw = view.findViewById(R.id.btnWithdraw);
        rvTransactions = view.findViewById(R.id.rvTransactions);

        // Initialize API service
        api = ApiClient.getClient(requireContext()).create(ApiService.class);

        // Setup RecyclerView
        rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionList = new ArrayList<>();
        transactionAdapter = new TransactionAdapter(getContext(), transactionList);
        rvTransactions.setAdapter(transactionAdapter);

        // Hide top-up button for hosts and adjust margins
        String role = TokenManager.getRole(requireContext());
        if ("host".equalsIgnoreCase(role)) {
            btnTopUp.setVisibility(View.GONE);

            ViewGroup.LayoutParams params = btnWithdraw.getLayoutParams();
            if (params instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) params;
                marginParams.setMarginStart(0);
                btnWithdraw.setLayoutParams(marginParams);
            }
        }

        // Load initial data
        loadBalanceFromCache();
        loadBalanceFromAPI();
        loadTransactionsFromAPI();

        // Setup button click listeners
        btnTopUp.setOnClickListener(v -> {
            PaymentTopUpDialog dialog = new PaymentTopUpDialog(requireContext(), WalletFragment.this);
            dialog.show();
        });

        btnWithdraw.setOnClickListener(v -> {
            WithdrawDialog dialog = new WithdrawDialog(requireContext());
            dialog.show();
        });

        // Setup RecyclerView scroll listener for pagination
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

        // ✅ Listen to fragment results from DashboardActivity
        getParentFragmentManager().setFragmentResultListener(
                "balance_updated",
                this,
                (requestKey, bundle) -> {
                    if (isAdded()) {
                        loadBalanceFromCache();
                        refreshData();
                    }
                }
        );

        // ✅ ONLY LocalBroadcast listener - socket events come from DashboardActivity
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
                walletUpdateReceiver,
                new IntentFilter("WALLET_UPDATED")
        );

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadBalanceFromCache();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(walletUpdateReceiver);
    }

    // ✅ Load balance from cache (using proper method from 460-line version)
    private void loadBalanceFromCache() {
        if (WalletCacheManager.hasBalance(requireContext())) {
            int balance = WalletCacheManager.getBalanceAsInt(requireContext());
            tvBalance.setText(String.valueOf(balance));
            Log.d(TAG, "Balance loaded from cache: " + balance);
        }
    }

    // ✅ Load balance from API (using proper method from 460-line version)
    private void loadBalanceFromAPI() {
        api.getWalletBalance().enqueue(new Callback<WalletBalanceResponse>() {
            @Override
            public void onResponse(@NonNull Call<WalletBalanceResponse> call, @NonNull Response<WalletBalanceResponse> response) {
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().data != null) {

                    double balance = response.body().data.balanceGC;
                    WalletCacheManager.saveBalance(requireContext(), balance);

                    if (isAdded()) {
                        int rupees = (int) Math.round(balance);
                        tvBalance.setText(String.valueOf(rupees));
                        Log.d(TAG, "Balance updated from API: " + balance);
                    }

                    // 🔥 NEW: Save withdrawal limit data from merged response
                    WalletBalanceResponse.Data data = response.body().data;
                    if (data.getMaxWithdrawableGC() != null &&
                            data.getTotalDepositsGC() != null &&
                            data.getTotalWithdrawnGC() != null) {

                        WalletLimitCache.saveLimit(
                                requireContext(),
                                data.getMaxWithdrawableGC(),
                                (int) balance,
                                data.getTotalDepositsGC(),
                                data.getTotalWithdrawnGC()
                        );

                        Log.d(TAG, "✅ Balance & Limits saved - Balance: " + balance +
                                ", MaxWithdrawable: " + data.getMaxWithdrawableGC());
                    } else {
                        // 🔥 NEW: If limit data is missing from response, mark unavailable
                        WalletLimitCache.markUnavailable(requireContext());
                        Log.w(TAG, "⚠️ Withdrawal limit data missing from API response");
                    }
                } else {
                    // 🔥 NEW: API call failed, mark limits as unavailable
                    WalletLimitCache.markUnavailable(requireContext());
                    Log.e(TAG, "Failed to load balance: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<WalletBalanceResponse> call, @NonNull Throwable t) {
                // 🔥 NEW: Network error, mark limits as unavailable
                WalletLimitCache.markUnavailable(requireContext());
                Log.e(TAG, "API call failed", t);
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Failed to load balance", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // ✅ Load transactions from API (using proper method from 460-line version)
    private void loadTransactionsFromAPI() {
        if (isLoading) return;

        isLoading = true;

        api.getTopUpHistory(limitPerPage, currentSkip).enqueue(new Callback<TopUpHistoryResponse>() {
            @Override
            public void onResponse(@NonNull Call<TopUpHistoryResponse> call, @NonNull Response<TopUpHistoryResponse> response) {
                isLoading = false;

                if (response.isSuccessful() && response.body() != null) {
                    TopUpHistoryResponse.TopUpHistoryData data = response.body().data;

                    if (data != null && data.history != null) {
                        if (currentSkip == 0) {
                            transactionList.clear();
                        }

                        transactionList.addAll(data.history);
                        transactionAdapter.notifyDataSetChanged();

                        hasMore = data.history.size() >= limitPerPage;
                        Log.d(TAG, "Loaded " + data.history.size() + " transactions, hasMore: " + hasMore);
                    }
                } else {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Failed to load transactions", Toast.LENGTH_SHORT).show();
                    }
                    Log.e(TAG, "Failed to load transactions: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<TopUpHistoryResponse> call, @NonNull Throwable t) {
                isLoading = false;

                if (isAdded()) {
                    Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
                Log.e(TAG, "Error loading transactions", t);
            }
        });
    }

    // ✅ Load more transactions for pagination
    private void loadMoreTransactions() {
        currentSkip += limitPerPage;
        loadTransactionsFromAPI();
        Log.d(TAG, "Loading more transactions, skip: " + currentSkip);
    }

    // ✅ Refresh all data
    private void refreshData() {
        currentSkip = 0;
        hasMore = true;
        loadBalanceFromAPI();
        loadTransactionsFromAPI();
        Log.d(TAG, "Refreshing all wallet data");
    }

    // ✅ Add pending top-up transaction to list
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
        Log.d(TAG, "Added pending top-up: " + amount + " GC");
    }

    // ✅ Refresh balance - called from dialogs
    public void refreshBalance() {
        loadBalanceFromAPI();
        refreshData();
        Log.d(TAG, "Balance refresh requested");
    }
}