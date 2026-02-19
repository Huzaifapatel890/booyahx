package com.booyahx;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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

    // ── Filter chips ──────────────────────────────────────────────────────────
    private TextView chipAll, chipTopUp, chipWithdraw, chipSuccess, chipPending, chipCancelled;
    private String currentFilter = "all";
    // filteredList is what the adapter actually displays; transactionList is the master list
    private List<Transaction> filteredList;
    // ─────────────────────────────────────────────────────────────────────────

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

        // ── Initialize filter chips ──────────────────────────────────────────
        chipAll        = view.findViewById(R.id.chipAll);
        chipTopUp      = view.findViewById(R.id.chipTopUp);
        chipWithdraw   = view.findViewById(R.id.chipWithdraw);
        chipSuccess    = view.findViewById(R.id.chipSuccess);
        chipPending    = view.findViewById(R.id.chipPending);
        chipCancelled  = view.findViewById(R.id.chipCancelled);

        // Apply default chip styles then activate "All"
        styleAllChipsDefault();
        setActiveChip(chipAll);

        // Chip click listeners
        chipAll.setOnClickListener(v -> {
            currentFilter = "all";
            setActiveChip(chipAll);
            applyFilter();
        });

        chipTopUp.setOnClickListener(v -> {
            currentFilter = "topup";
            setActiveChip(chipTopUp);
            applyFilter();
        });

        chipWithdraw.setOnClickListener(v -> {
            currentFilter = "withdraw";
            setActiveChip(chipWithdraw);
            applyFilter();
        });

        chipSuccess.setOnClickListener(v -> {
            currentFilter = "success";
            setActiveChip(chipSuccess);
            applyFilter();
        });

        chipPending.setOnClickListener(v -> {
            currentFilter = "pending";
            setActiveChip(chipPending);
            applyFilter();
        });

        // TODO: Wire chipCancelled click listener when cancel transaction API is ready.
        // chipCancelled is intentionally NOT clickable (set in XML: android:clickable="false")
        // until the cancel transaction API is integrated.
        // ─────────────────────────────────────────────────────────────────────

        // Initialize API service
        api = ApiClient.getClient(requireContext()).create(ApiService.class);

        // Setup RecyclerView with filteredList (adapter shows filtered view)
        rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionList = new ArrayList<>();
        filteredList = new ArrayList<>();
        transactionAdapter = new TransactionAdapter(getContext(), filteredList);
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
                                data.getMaxWithdrawableGC().intValue(),
                                (int) balance,
                                data.getTotalDepositsGC().intValue(),
                                data.getTotalWithdrawnGC().intValue()
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

                        // ── Apply current chip filter after loading ──────────
                        applyFilter();
                        // ─────────────────────────────────────────────────────

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
        // ── Re-apply current filter so the pending item respects active chip ──
        applyFilter();
        // ─────────────────────────────────────────────────────────────────────
        rvTransactions.scrollToPosition(0);
        Log.d(TAG, "Added pending top-up: " + amount + " GC");
    }

    // ✅ Refresh balance - called from dialogs
    public void refreshBalance() {
        loadBalanceFromAPI();
        refreshData();
        Log.d(TAG, "Balance refresh requested");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ── FILTER CHIP HELPERS ──────────────────────────────────────────────────
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Rebuilds filteredList from the master transactionList based on currentFilter,
     * then notifies the adapter. Mirrors the HTML filterTx() logic exactly:
     *   - "all"      → show everything
     *   - anything else → match transaction type OR status (case-insensitive)
     */
    private void applyFilter() {
        filteredList.clear();

        if ("all".equals(currentFilter)) {
            filteredList.addAll(transactionList);
        } else {
            for (Transaction tx : transactionList) {
                String type   = tx.getType()   != null ? tx.getType().toLowerCase()   : "";
                String status = tx.getStatus() != null ? tx.getStatus().toLowerCase() : "";

                Log.d(TAG, "  checking tx → type='" + type + "' status='" + status + "' filter='" + currentFilter + "'");

                boolean typeMatch;
                if ("withdraw".equals(currentFilter)) {
                    // API may return type as "withdraw" OR "withdrawal" — match both
                    typeMatch = type.equals("withdraw") || type.equals("withdrawal");
                } else {
                    typeMatch = type.equals(currentFilter);
                }

                boolean statusMatch = status.equals(currentFilter);

                if (typeMatch || statusMatch) {
                    filteredList.add(tx);
                }
            }
        }

        if (isAdded()) {
            transactionAdapter.notifyDataSetChanged();
        }

        Log.d(TAG, "Filter applied: " + currentFilter + " → " + filteredList.size() + " items");
    }

    /**
     * Resets every chip to its default (unselected) visual style, then
     * highlights the selected chip with the cyan active style.
     */
    private void setActiveChip(TextView selectedChip) {
        float dp = getResources().getDisplayMetrics().density;
        int cornerRadius = (int) (20 * dp);   // pill shape
        int strokeWidth  = (int) (1.5f * dp);

        // Default style colours
        int defaultStroke = Color.parseColor("#2A2A2A");
        int defaultFill   = Color.parseColor("#060D12");
        int defaultText   = Color.parseColor("#6B7F8A");

        // Active style colours  (cyan — matches HTML var(--cyan) #00D4FF)
        int activeStroke = Color.parseColor("#00D4FF");
        int activeFill   = Color.argb(34, 0, 212, 255);  // #00D4FF at ~13% opacity
        int activeText   = Color.parseColor("#00D4FF");

        // All chips to iterate (cancelled included so it stays styled when reset)
        TextView[] allChips = {chipAll, chipTopUp, chipWithdraw, chipSuccess, chipPending, chipCancelled};

        for (TextView chip : allChips) {
            if (chip == null) continue;
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(cornerRadius);
            bg.setStroke(strokeWidth, defaultStroke);
            bg.setColor(defaultFill);
            chip.setBackground(bg);
            chip.setTextColor(defaultText);
        }

        // Highlight the selected chip
        GradientDrawable activeBg = new GradientDrawable();
        activeBg.setShape(GradientDrawable.RECTANGLE);
        activeBg.setCornerRadius(cornerRadius);
        activeBg.setStroke(strokeWidth, activeStroke);
        activeBg.setColor(activeFill);
        selectedChip.setBackground(activeBg);
        selectedChip.setTextColor(activeText);
    }

    /** Applies the default style to every chip on first load. */
    private void styleAllChipsDefault() {
        float dp = getResources().getDisplayMetrics().density;
        int cornerRadius = (int) (20 * dp);
        int strokeWidth  = (int) (1.5f * dp);

        TextView[] allChips = {chipAll, chipTopUp, chipWithdraw, chipSuccess, chipPending, chipCancelled};
        for (TextView chip : allChips) {
            if (chip == null) continue;
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(cornerRadius);
            bg.setStroke(strokeWidth, Color.parseColor("#2A2A2A"));
            bg.setColor(Color.parseColor("#060D12"));
            chip.setBackground(bg);
            chip.setTextColor(Color.parseColor("#6B7F8A"));
        }
    }
    // ─────────────────────────────────────────────────────────────────────────
}