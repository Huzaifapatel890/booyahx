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

        loadWalletBalance();
        loadFakeTransactions(); // remove after real API

        btnTopUp.setOnClickListener(v -> {
            PaymentTopUpDialog dialog = new PaymentTopUpDialog(requireContext(), WalletFragment.this);
            dialog.show();
        });

        btnWithdraw.setOnClickListener(v ->
                Toast.makeText(getContext(), "Withdraw coming soon", Toast.LENGTH_SHORT).show()
        );

        return view;
    }

    private void loadWalletBalance() {
        api.getWalletBalance().enqueue(new Callback<WalletBalanceResponse>() {
            @Override
            public void onResponse(Call<WalletBalanceResponse> call, Response<WalletBalanceResponse> res) {
                if (res.isSuccessful() && res.body() != null && res.body().data != null) {

                    double balance = res.body().data.balanceGC;
                    int rupees = (int) Math.round(balance);   // remove paise

                    tvBalance.setText(String.valueOf(rupees)); // no GC, no .00
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

    public void refreshBalance() {
        loadWalletBalance();
    }
}