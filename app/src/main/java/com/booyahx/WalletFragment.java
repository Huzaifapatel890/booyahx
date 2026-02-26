package com.booyahx;

import android.app.DatePickerDialog;
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
import com.booyahx.network.models.CancelWithdrawalResponse;
import com.booyahx.network.models.TopUpHistoryResponse;
import com.booyahx.network.models.Transaction;
import com.booyahx.network.models.WalletBalanceResponse;
import com.booyahx.network.models.WithdrawalLimitResponse;

import java.util.ArrayList;
import java.util.Calendar;
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

    // ── 2 filter chips ───────────────────────────────────────────────────────
    private TextView chipTopUp, chipWithdraw;
    private String currentFilter = "topup";   // default: Top-Up tab
    private List<Transaction> filteredList;
    // ─────────────────────────────────────────────────────────────────────────

    // ── Date filter views ─────────────────────────────────────────────────────
    private TextView tvFromDate, tvToDate, btnClearDates;
    // Stored as "YYYY-MM-DD" strings; null = not set
    private String selectedFromDate = null;
    private String selectedToDate   = null;
    // ─────────────────────────────────────────────────────────────────────────

    private ApiService api;

    // ✅ Silent client — no loader shown. Used ONLY for broadcast/websocket-driven calls.
    private ApiService silentApi;

    // API now uses page-based pagination (not skip)
    private int currentPage   = 1;
    private int limitPerPage  = 50;
    private boolean isLoading = false;
    private boolean hasMore   = true;

    // ✅ ONLY BroadcastReceiver — socket events handled by DashboardActivity
    private BroadcastReceiver walletUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isAdded()) {
                loadBalanceFromCache();
                silentLoadBalanceFromAPI(); // ✅ SILENT: no loader for broadcast-triggered refresh
                silentRefreshData();        // ✅ SILENT: no loader for broadcast-triggered refresh
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

        // ── Core views ───────────────────────────────────────────────────────
        tvBalance      = view.findViewById(R.id.tvBalance);
        btnTopUp       = view.findViewById(R.id.btnTopUp);
        btnWithdraw    = view.findViewById(R.id.btnWithdraw);
        rvTransactions = view.findViewById(R.id.rvTransactions);

        // ── Filter chips ─────────────────────────────────────────────────────
        chipTopUp    = view.findViewById(R.id.chipTopUp);
        chipWithdraw = view.findViewById(R.id.chipWithdraw);

        styleAllChipsDefault();
        setActiveChip(chipTopUp);

        chipTopUp.setOnClickListener(v -> {
            currentFilter = "topup";
            setActiveChip(chipTopUp);
            refreshData();
        });
        chipWithdraw.setOnClickListener(v -> {
            currentFilter = "withdraw";
            setActiveChip(chipWithdraw);
            refreshData();
        });

        // ── Date filter views ────────────────────────────────────────────────
        tvFromDate    = view.findViewById(R.id.tvFromDate);
        tvToDate      = view.findViewById(R.id.tvToDate);
        btnClearDates = view.findViewById(R.id.btnClearDates);

        tvFromDate.setOnClickListener(v -> showDatePicker(true));
        tvToDate.setOnClickListener(v -> showDatePicker(false));

        btnClearDates.setOnClickListener(v -> {
            selectedFromDate = null;
            selectedToDate   = null;
            tvFromDate.setText("📅 Start date");
            tvFromDate.setTextColor(Color.parseColor("#8A9BB0"));
            tvToDate.setText("📅 End date");
            tvToDate.setTextColor(Color.parseColor("#8A9BB0"));
            btnClearDates.setVisibility(View.GONE);
            refreshData();
        });
        // ─────────────────────────────────────────────────────────────────────

        // ── API + RecyclerView ───────────────────────────────────────────────
        api       = ApiClient.getClient(requireContext()).create(ApiService.class);
        // ✅ Silent client: X-Silent-Request header skips loader in GlobalLoadingInterceptor
        silentApi = ApiClient.getSilentClient(requireContext()).create(ApiService.class);

        rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionList = new ArrayList<>();
        filteredList    = new ArrayList<>();
        transactionAdapter = new TransactionAdapter(getContext(), filteredList, this);
        rvTransactions.setAdapter(transactionAdapter);

        // Hide top-up for hosts
        String role = TokenManager.getRole(requireContext());
        if ("host".equalsIgnoreCase(role)) {
            btnTopUp.setVisibility(View.GONE);
            ViewGroup.LayoutParams params = btnWithdraw.getLayoutParams();
            if (params instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) params).setMarginStart(0);
                btnWithdraw.setLayoutParams(params);
            }
        }

        // Initial load
        loadBalanceFromCache();
        loadBalanceFromAPI();
        loadTransactionsFromAPI();

        // Top-Up / Withdraw action buttons
        btnTopUp.setOnClickListener(v -> {
            PaymentTopUpDialog dialog = new PaymentTopUpDialog(requireContext(), WalletFragment.this);
            dialog.show();
        });
        btnWithdraw.setOnClickListener(v -> {
            WithdrawDialog dialog = new WithdrawDialog(requireContext());
            dialog.show();
        });

        // Pagination on scroll
        rvTransactions.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm != null && !isLoading && hasMore) {
                    int visible = lm.getChildCount();
                    int total   = lm.getItemCount();
                    int first   = lm.findFirstVisibleItemPosition();
                    if ((visible + first) >= total - 5 && first >= 0) {
                        loadMoreTransactions();
                    }
                }
            }
        });

        // Fragment result listener
        getParentFragmentManager().setFragmentResultListener(
                "balance_updated", this,
                (requestKey, bundle) -> {
                    if (isAdded()) {
                        loadBalanceFromCache();
                        silentRefreshData(); // ✅ SILENT: socket-driven, no loader
                    }
                }
        );

        // LocalBroadcast listener
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

    // ─────────────────────────────────────────────────────────────────────────
    // DATE PICKER
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Opens a DatePickerDialog.
     * @param isFrom  true = picking "from" date; false = picking "to" date.
     */
    private void showDatePicker(boolean isFrom) {
        Calendar cal = Calendar.getInstance();

        // Pre-fill with existing selection if available
        if (isFrom && selectedFromDate != null) {
            cal = parseDateToCalendar(selectedFromDate);
        } else if (!isFrom && selectedToDate != null) {
            cal = parseDateToCalendar(selectedToDate);
        }

        final Calendar startCal = cal;

        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                android.R.style.Theme_DeviceDefault_Dialog_MinWidth,
                (datePicker, year, month, day) -> {
                    // month is 0-indexed
                    String dateStr = String.format("%04d-%02d-%02d", year, month + 1, day);
                    String label   = String.format("%02d %s %04d", day, getMonthShort(month + 1), year);

                    if (isFrom) {
                        selectedFromDate = dateStr;
                        tvFromDate.setText("📅 " + label);
                        tvFromDate.setTextColor(Color.parseColor("#00D4FF"));
                    } else {
                        selectedToDate = dateStr;
                        tvToDate.setText("📅 " + label);
                        tvToDate.setTextColor(Color.parseColor("#00D4FF"));
                    }

                    // Show clear button as soon as any date is set
                    btnClearDates.setVisibility(View.VISIBLE);

                    // Reload with new date range
                    refreshData();
                },
                startCal.get(Calendar.YEAR),
                startCal.get(Calendar.MONTH),
                startCal.get(Calendar.DAY_OF_MONTH)
        );

        // Constrain toDate picker to not go before fromDate
        if (!isFrom && selectedFromDate != null) {
            Calendar fromCal = parseDateToCalendar(selectedFromDate);
            dialog.getDatePicker().setMinDate(fromCal.getTimeInMillis());
        }

        // Don't allow future dates
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());

        dialog.show();
    }

    private Calendar parseDateToCalendar(String dateStr) {
        // dateStr = "YYYY-MM-DD"
        Calendar cal = Calendar.getInstance();
        try {
            String[] parts = dateStr.split("-");
            cal.set(Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]) - 1,   // month 0-indexed
                    Integer.parseInt(parts[2]));
        } catch (Exception ignored) { }
        return cal;
    }

    private String getMonthShort(int month) {
        String[] months = {"Jan","Feb","Mar","Apr","May","Jun",
                "Jul","Aug","Sep","Oct","Nov","Dec"};
        return (month >= 1 && month <= 12) ? months[month - 1] : "";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BALANCE
    // ─────────────────────────────────────────────────────────────────────────

    private void loadBalanceFromCache() {
        if (WalletCacheManager.hasBalance(requireContext())) {
            int balance = WalletCacheManager.getBalanceAsInt(requireContext());
            tvBalance.setText(String.valueOf(balance));
            Log.d(TAG, "Balance loaded from cache: " + balance);
        }
    }

    private void loadBalanceFromAPI() {
        api.getWalletBalance().enqueue(new Callback<WalletBalanceResponse>() {
            @Override
            public void onResponse(@NonNull Call<WalletBalanceResponse> call,
                                   @NonNull Response<WalletBalanceResponse> response) {
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().data != null) {

                    double balance = response.body().data.balanceGC;
                    WalletCacheManager.saveBalance(requireContext(), balance);

                    if (isAdded()) {
                        tvBalance.setText(String.valueOf((int) Math.round(balance)));
                        Log.d(TAG, "Balance updated from API: " + balance);
                    }

                    WalletBalanceResponse.Data data = response.body().data;
                    if (data.getMaxWithdrawableGC() != null
                            && data.getTotalDepositsGC() != null
                            && data.getTotalWithdrawnGC() != null) {
                        WalletLimitCache.saveLimit(
                                requireContext(),
                                data.getMaxWithdrawableGC().intValue(),
                                (int) balance,
                                data.getTotalDepositsGC().intValue(),
                                data.getTotalWithdrawnGC().intValue()
                        );
                    } else {
                        WalletLimitCache.markUnavailable(requireContext());
                        Log.w(TAG, "⚠️ Withdrawal limit data missing");
                    }
                } else {
                    WalletLimitCache.markUnavailable(requireContext());
                    Log.e(TAG, "Failed to load balance: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<WalletBalanceResponse> call, @NonNull Throwable t) {
                WalletLimitCache.markUnavailable(requireContext());
                Log.e(TAG, "Balance API call failed", t);
                if (isAdded())
                    Toast.makeText(requireContext(), "Failed to load balance", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TRANSACTIONS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Converts currentFilter chip to the API "type" param.
     * "topup" → "topup"  |  "withdraw" → "withdrawal"  |  null = both (not used here since we always filter)
     */
    private String apiTypeParam() {
        if ("topup".equals(currentFilter))    return "topup";
        if ("withdraw".equals(currentFilter)) return "withdrawal";
        return null;
    }

    private void loadTransactionsFromAPI() {
        if (isLoading) return;
        isLoading = true;

        api.getTopUpHistory(
                currentPage,
                limitPerPage,
                apiTypeParam(),    // "topup" | "withdrawal"
                selectedFromDate,  // "YYYY-MM-DD" or null
                selectedToDate     // "YYYY-MM-DD" or null
        ).enqueue(new Callback<TopUpHistoryResponse>() {
            @Override
            public void onResponse(@NonNull Call<TopUpHistoryResponse> call,
                                   @NonNull Response<TopUpHistoryResponse> response) {
                isLoading = false;

                if (response.isSuccessful() && response.body() != null) {
                    TopUpHistoryResponse.TopUpHistoryData data = response.body().data;

                    if (data != null && data.history != null) {
                        if (currentPage == 1) {
                            transactionList.clear();
                            filteredList.clear();
                        }

                        transactionList.addAll(data.history);
                        // Since the API already filters by type + date,
                        // everything in transactionList goes straight into filteredList
                        filteredList.addAll(data.history);

                        if (isAdded()) transactionAdapter.notifyDataSetChanged();

                        // Check pagination
                        if (data.pagination != null) {
                            hasMore = data.pagination.hasNextPage;
                        } else {
                            hasMore = data.history.size() >= limitPerPage;
                        }

                        Log.d(TAG, "Loaded " + data.history.size()
                                + " transactions (page " + currentPage + "), hasMore: " + hasMore);
                    }
                } else {
                    if (isAdded())
                        Toast.makeText(requireContext(), "Failed to load transactions", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Transaction API error: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<TopUpHistoryResponse> call, @NonNull Throwable t) {
                isLoading = false;
                if (isAdded())
                    Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Transaction API failure", t);
            }
        });
    }

    private void loadMoreTransactions() {
        currentPage++;
        loadTransactionsFromAPI();
        Log.d(TAG, "Loading page " + currentPage);
    }

    private void refreshData() {
        currentPage = 1;
        hasMore     = true;
        transactionList.clear();
        filteredList.clear();
        if (isAdded()) transactionAdapter.notifyDataSetChanged();
        loadTransactionsFromAPI();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CANCEL WITHDRAWAL — called by TransactionAdapter
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Cancel a pending withdrawal and refresh on success.
     */
    public void cancelWithdrawal(String transactionId, int adapterPosition) {
        Log.d(TAG, "Cancelling withdrawal: " + transactionId);

        api.cancelWithdrawal(transactionId).enqueue(new Callback<CancelWithdrawalResponse>() {
            @Override
            public void onResponse(@NonNull Call<CancelWithdrawalResponse> call,
                                   @NonNull Response<CancelWithdrawalResponse> response) {
                if (!isAdded()) return;

                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().success) {

                    Toast.makeText(requireContext(),
                            "Withdrawal cancelled. Amount refunded to wallet.",
                            Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "✅ Withdrawal cancelled: " + transactionId);

                    // ✅ SILENT: loader already dismissed after cancel call — no second spinner
                    silentLoadBalanceFromAPI();
                    silentRefreshData();

                } else {
                    String msg = "Failed to cancel withdrawal";
                    if (response.body() != null && response.body().message != null)
                        msg = response.body().message;
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Cancel failed: " + response.code() + " — " + msg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<CancelWithdrawalResponse> call, @NonNull Throwable t) {
                if (isAdded())
                    Toast.makeText(requireContext(), "Network error. Try again.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Cancel withdrawal network error", t);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC HELPERS — called by dialogs
    // ─────────────────────────────────────────────────────────────────────────

    public void addPendingTopup(int amount) {
        Transaction t = new Transaction(
                "Just Now", "topup", "+" + amount + " GC", "pending", "Processing...", true
        );
        transactionList.add(0, t);
        filteredList.add(0, t);
        if (isAdded()) transactionAdapter.notifyDataSetChanged();
        rvTransactions.scrollToPosition(0);
        Log.d(TAG, "Added pending top-up: " + amount + " GC");
    }

    public void refreshBalance() {
        loadBalanceFromAPI();
        refreshData();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SILENT API METHODS — no loader shown (broadcast / websocket paths only)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * ✅ Silent version of loadBalanceFromAPI().
     * Uses silentApi so GlobalLoadingInterceptor skips the loader entirely.
     * Call ONLY from broadcast/websocket-triggered code paths.
     */
    private void silentLoadBalanceFromAPI() {
        silentApi.getWalletBalance().enqueue(new Callback<WalletBalanceResponse>() {
            @Override
            public void onResponse(@NonNull Call<WalletBalanceResponse> call,
                                   @NonNull Response<WalletBalanceResponse> response) {
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().data != null) {

                    double balance = response.body().data.balanceGC;
                    WalletCacheManager.saveBalance(requireContext(), balance);

                    if (isAdded()) {
                        tvBalance.setText(String.valueOf((int) Math.round(balance)));
                        Log.d(TAG, "Balance updated silently from API: " + balance);
                    }

                    WalletBalanceResponse.Data data = response.body().data;
                    if (data.getMaxWithdrawableGC() != null
                            && data.getTotalDepositsGC() != null
                            && data.getTotalWithdrawnGC() != null) {
                        WalletLimitCache.saveLimit(
                                requireContext(),
                                data.getMaxWithdrawableGC().intValue(),
                                (int) balance,
                                data.getTotalDepositsGC().intValue(),
                                data.getTotalWithdrawnGC().intValue()
                        );
                    } else {
                        WalletLimitCache.markUnavailable(requireContext());
                        Log.w(TAG, "⚠️ Withdrawal limit data missing (silent)");
                    }
                } else {
                    WalletLimitCache.markUnavailable(requireContext());
                    Log.e(TAG, "Silent balance load failed: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<WalletBalanceResponse> call, @NonNull Throwable t) {
                WalletLimitCache.markUnavailable(requireContext());
                Log.e(TAG, "Silent balance API call failed", t);
            }
        });
    }

    /**
     * ✅ Silent version of loadTransactionsFromAPI().
     * Uses silentApi — no loader shown.
     * Call ONLY from broadcast/websocket-triggered code paths.
     */
    private void silentLoadTransactionsFromAPI() {
        if (isLoading) return;
        isLoading = true;

        silentApi.getTopUpHistory(
                currentPage,
                limitPerPage,
                apiTypeParam(),
                selectedFromDate,
                selectedToDate
        ).enqueue(new Callback<TopUpHistoryResponse>() {
            @Override
            public void onResponse(@NonNull Call<TopUpHistoryResponse> call,
                                   @NonNull Response<TopUpHistoryResponse> response) {
                isLoading = false;

                if (response.isSuccessful() && response.body() != null) {
                    TopUpHistoryResponse.TopUpHistoryData data = response.body().data;

                    if (data != null && data.history != null) {
                        if (currentPage == 1) {
                            transactionList.clear();
                            filteredList.clear();
                        }

                        transactionList.addAll(data.history);
                        filteredList.addAll(data.history);

                        if (isAdded()) transactionAdapter.notifyDataSetChanged();

                        if (data.pagination != null) {
                            hasMore = data.pagination.hasNextPage;
                        } else {
                            hasMore = data.history.size() >= limitPerPage;
                        }

                        Log.d(TAG, "Silent load: " + data.history.size()
                                + " transactions (page " + currentPage + ")");
                    }
                } else {
                    Log.e(TAG, "Silent transaction load error: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<TopUpHistoryResponse> call, @NonNull Throwable t) {
                isLoading = false;
                Log.e(TAG, "Silent transaction API failure", t);
            }
        });
    }

    /**
     * ✅ Silent version of refreshData().
     * Resets to page 1 and calls silentLoadTransactionsFromAPI() — no loader shown.
     * Used by walletUpdateReceiver and "balance_updated" fragment result listener.
     */
    private void silentRefreshData() {
        currentPage = 1;
        hasMore     = true;
        transactionList.clear();
        filteredList.clear();
        if (isAdded()) transactionAdapter.notifyDataSetChanged();
        silentLoadTransactionsFromAPI();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHIP HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private void setActiveChip(TextView selected) {
        styleAllChipsDefault();   // reset all first

        float dp = getResources().getDisplayMetrics().density;
        GradientDrawable activeBg = new GradientDrawable();
        activeBg.setShape(GradientDrawable.RECTANGLE);
        activeBg.setCornerRadius((int) (20 * dp));
        activeBg.setStroke((int) (1.5f * dp), Color.parseColor("#00D4FF"));
        activeBg.setColor(Color.argb(34, 0, 212, 255));
        selected.setBackground(activeBg);
        selected.setTextColor(Color.parseColor("#00D4FF"));
    }

    private void styleAllChipsDefault() {
        float dp = getResources().getDisplayMetrics().density;
        int corner = (int) (20 * dp);
        int stroke  = (int) (1.5f * dp);

        for (TextView chip : new TextView[]{chipTopUp, chipWithdraw}) {
            if (chip == null) continue;
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(corner);
            bg.setStroke(stroke, Color.parseColor("#2A2A2A"));
            bg.setColor(Color.parseColor("#060D12"));
            chip.setBackground(bg);
            chip.setTextColor(Color.parseColor("#6B7F8A"));
        }
    }
}