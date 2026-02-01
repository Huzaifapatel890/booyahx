package com.booyahx;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.WalletBalanceResponse;
import com.booyahx.network.models.TopUpHistoryResponse;
import com.booyahx.payment.PaymentTopUpDialog;
import com.booyahx.network.models.Transaction;
import com.booyahx.payment.TransactionAdapter;
import com.booyahx.payment.WithdrawDialog;
import com.booyahx.TokenManager;
import com.booyahx.WalletCacheManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WalletFragment extends Fragment {

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

        btnTopUp.setOnClickListener(v -> {
            PaymentTopUpDialog dialog = new PaymentTopUpDialog(requireContext(), WalletFragment.this);
            dialog.show();
        });

        // 🔥 OPEN WITHDRAW DIALOG
        btnWithdraw.setOnClickListener(v -> {
            WithdrawDialog dialog = new WithdrawDialog(requireContext());
            dialog.show();
        });

        // 🔥 SWIPE TO REFRESH (Optional)

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

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 🔥 REFRESH FROM CACHE WHEN RETURNING
        loadBalanceFromCache();
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