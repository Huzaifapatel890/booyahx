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

import com.booyahx.settings.Transaction;
import com.booyahx.settings.TransactionAdapter;
import com.booyahx.R;

import java.util.ArrayList;
import java.util.List;

public class WalletFragment extends Fragment {

    private TextView tvBalance;
    private TextView btnTopUp, btnWithdraw;   // ✅ FIXED
    private RecyclerView rvTransactions;
    private TransactionAdapter transactionAdapter;
    private List<Transaction> transactionList;

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

        rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));

        loadTransactions();

        transactionAdapter = new TransactionAdapter(getContext(), transactionList);
        rvTransactions.setAdapter(transactionAdapter);

        btnTopUp.setOnClickListener(v ->
                Toast.makeText(getContext(), "Top Up clicked", Toast.LENGTH_SHORT).show()
        );

        btnWithdraw.setOnClickListener(v ->
                Toast.makeText(getContext(), "Withdraw clicked", Toast.LENGTH_SHORT).show()
        );

        return view;
    }

    private void loadTransactions() {
        transactionList = new ArrayList<>();

        transactionList.add(new Transaction(
                "Dec 23, 2025 • 14:32",
                "Top Up",
                "+5,000 GC",
                "Completed",
                "Payment via Credit Card",
                true
        ));

        transactionList.add(new Transaction(
                "Dec 22, 2025 • 09:15",
                "Withdraw",
                "-2,500 GC",
                "Completed",
                "Bank Transfer to *****1234",
                false
        ));

        transactionList.add(new Transaction(
                "Dec 21, 2025 • 18:45",
                "Top Up",
                "+10,000 GC",
                "Pending",
                "Payment Processing...",
                true
        ));

        transactionList.add(new Transaction(
                "Dec 20, 2025 • 11:20",
                "Purchase",
                "-50 GC",
                "Completed",
                "Game Item Purchase",
                false
        ));
    }

    public void updateBalance(String newBalance) {
        if (tvBalance != null) {
            tvBalance.setText(newBalance);
        }
    }

    public void addTransaction(Transaction transaction) {
        transactionList.add(0, transaction);
        transactionAdapter.notifyItemInserted(0);
        rvTransactions.smoothScrollToPosition(0);
    }
}