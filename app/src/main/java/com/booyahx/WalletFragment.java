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
import com.booyahx.payment.PaymentTopUpDialog;
import com.booyahx.payment.Transaction;
import com.booyahx.payment.TransactionAdapter;

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

        loadFakeTransactions(); // remove after real API

        btnTopUp.setOnClickListener(v -> {
            PaymentTopUpDialog dialog = new PaymentTopUpDialog(requireContext(), WalletFragment.this);
            dialog.show();
        });

        btnWithdraw.setOnClickListener(v ->
                Toast.makeText(getContext(), "Withdraw coming soon", Toast.LENGTH_SHORT).show()
        );

        // 🔥 LISTEN FOR BALANCE UPDATES FROM OTHER FRAGMENTS
        getParentFragmentManager().setFragmentResultListener(
                "balance_updated",
                this,
                (requestKey, bundle) -> {
                    if (isAdded()) {
                        loadBalanceFromCache();
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
                Toast.makeText(getContext(), "Failed to load balance", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadFakeTransactions() {
        transactionList.add(new Transaction("Dec 23", "Top Up", "+500 GC", "Completed", "Card", true));
        transactionList.add(new Transaction("Dec 22", "Withdraw", "-250 GC", "Completed", "Bank", false));
        transactionAdapter.notifyDataSetChanged();
    }

    public void addPendingTopup(int amount) {
        Transaction t = new Transaction(
                "Just Now",
                "Top Up",
                "+" + amount + " GC",
                "Pending",
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
    }
}