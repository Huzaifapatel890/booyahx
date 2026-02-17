package com.booyahx.settings;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.booyahx.Host.EnhancedFinalResultDialog;
import com.booyahx.Host.FinalRow; // 🔥 ADDED: needed to pass standings to dialog
import com.booyahx.R;
import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.TournamentHistoryResponse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResultHistoryActivity extends AppCompatActivity {

    private static final String TAG = "ResultHistoryActivity";

    // Views
    private ProgressBar progressBarMain;
    private RecyclerView recyclerHistory;
    private LinearLayout emptyState;
    private LinearLayout errorState;
    private TextView txtError;
    private TextView btnRetry;

    // Adapter
    private ResultHistoryAdapter adapter;
    private List<TournamentHistoryResponse.TournamentHistory> historyList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_history);

        initViews();
        setupRecycler();
        loadHistory();
    }

    // ============================
    // INIT VIEWS
    // ============================
    private void initViews() {
        progressBarMain = findViewById(R.id.progressBarMain);
        recyclerHistory  = findViewById(R.id.recyclerHistory);
        emptyState       = findViewById(R.id.emptyState);
        errorState       = findViewById(R.id.errorState);
        txtError         = findViewById(R.id.txtError);
        btnRetry         = findViewById(R.id.btnRetry);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Retry button
        btnRetry.setOnClickListener(v -> loadHistory());
    }

    // ============================
    // SETUP RECYCLER
    // ============================
    private void setupRecycler() {
        adapter = new ResultHistoryAdapter(historyList, this::onCardClicked);
        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        recyclerHistory.setAdapter(adapter);

        // Scroll-to-load-more (pagination — loads next 50 when near bottom)
        recyclerHistory.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm == null) return;
                int lastVisible = lm.findLastCompletelyVisibleItemPosition();
                if (lastVisible == historyList.size() - 1 && !adapter.isLoading()) {
                    // 🔥 FIX: post to next frame — notifyItem* cannot be called during layout pass
                    rv.post(() -> {
                        adapter.setLoading(true);
                        loadMoreHistory();
                    });
                }
            }
        });
    }

    // ============================
    // CARD CLICK → open EnhancedFinalResultDialog
    // 🔥 FIXED: was passing null → dialog got "No results to display"
    //    Now converts "standings" array (already in history response) to FinalRow list
    // ============================
    private void onCardClicked(TournamentHistoryResponse.TournamentHistory tournament) {
        if (tournament == null) return;

        Log.d(TAG, "Card clicked: " + tournament.getId() + " status=" + tournament.getStatus());

        // 🔥 Convert standings from history response → List<FinalRow>
        // API response confirmed: standings[].{teamName, totalPoint, kills, booyah, totalPositionPoints, position}
        // FinalRow constructor: FinalRow(teamName, kp=kills, pp=totalPositionPoints, total=totalPoint, booyah)
        List<FinalRow> results = buildFinalRows(tournament);

        EnhancedFinalResultDialog dialog = new EnhancedFinalResultDialog(
                this,
                results,               // 🔥 full standings — never null if API returned standings
                tournament.getId(),
                tournament.getStatus()
        );

        dialog.show();
    }

    // 🔥 ADDED: Converts standings[] from API response into FinalRow list for the dialog
    private List<FinalRow> buildFinalRows(TournamentHistoryResponse.TournamentHistory t) {
        List<TournamentHistoryResponse.Standing> standings = t.getStandings();

        if (standings == null || standings.isEmpty()) {
            // Standings not present — pass null so dialog falls back to fetchLiveResults()
            Log.w(TAG, "standings[] is empty in history response — dialog will fetch /liveResult");
            return null;
        }

        // Sort by totalPoint descending — same order as EnhancedFinalResultDialog.fetchLiveResults()
        standings.sort((a, b) -> Integer.compare(b.getTotalPoint(), a.getTotalPoint()));

        List<FinalRow> rows = new ArrayList<>();
        for (TournamentHistoryResponse.Standing s : standings) {
            rows.add(new FinalRow(
                    s.getTeamName(),            // teamName
                    s.getKills(),               // kp  = kills
                    s.getTotalPositionPoints(), // pp  = totalPositionPoints
                    s.getTotalPoint(),          // total
                    s.getBooyah()               // booyah
            ));
        }

        Log.d(TAG, "Built " + rows.size() + " FinalRows from standings");
        return rows;
    }

    // ============================
    // LOAD HISTORY — initial fetch
    // ============================
    private int currentOffset = 0;
    private static final int PAGE_LIMIT = 50;
    private boolean hasMore = true;

    private void loadHistory() {
        showLoading();
        currentOffset = 0;
        hasMore = true;
        historyList.clear();

        callHistoryApi(0);
    }

    // ============================
    // LOAD MORE — pagination
    // ============================
    private void loadMoreHistory() {
        if (!hasMore) {
            adapter.setLoading(false);
            return;
        }
        callHistoryApi(currentOffset);
    }

    // ============================
    // API CALL
    // ============================
    private void callHistoryApi(int offset) {
        Log.d(TAG, "Fetching history offset=" + offset);

        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);

        // 🔥 Calls GET /api/tournament/userHistory?limit=50&offset=N
        Call<TournamentHistoryResponse> call = apiService.getUserTournamentHistory(
                PAGE_LIMIT,
                offset
        );

        call.enqueue(new Callback<TournamentHistoryResponse>() {
            @Override
            public void onResponse(@NonNull Call<TournamentHistoryResponse> call,
                                   @NonNull Response<TournamentHistoryResponse> response) {

                progressBarMain.setVisibility(View.GONE);
                adapter.setLoading(false);

                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "API error: " + response.code());
                    if (historyList.isEmpty()) showError("Server error " + response.code());
                    return;
                }

                TournamentHistoryResponse body = response.body();

                if (!body.isSuccess() || body.getData() == null) {
                    Log.e(TAG, "Response unsuccessful: " + body.getMessage());
                    if (historyList.isEmpty()) showError(body.getMessage());
                    return;
                }

                List<TournamentHistoryResponse.TournamentHistory> fetched =
                        body.getData().getHistory();

                if (fetched == null || fetched.isEmpty()) {
                    hasMore = false;
                    if (historyList.isEmpty()) {
                        showEmpty();
                    }
                    return;
                }

                // Update pagination state
                currentOffset = offset + fetched.size();
                int total = body.getData().getTotal();
                hasMore = currentOffset < total;

                // Append to list and notify
                int insertStart = historyList.size();
                historyList.addAll(fetched);
                adapter.notifyItemRangeInserted(insertStart, fetched.size());

                showList();
                Log.d(TAG, "Loaded " + fetched.size() + " items. Total=" + total + " hasMore=" + hasMore);
            }

            @Override
            public void onFailure(@NonNull Call<TournamentHistoryResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Network error: " + t.getMessage());
                progressBarMain.setVisibility(View.GONE);
                adapter.setLoading(false);
                if (historyList.isEmpty()) showError("Network error. Check your connection.");
            }
        });
    }

    // ============================
    // UI STATE HELPERS
    // ============================
    private void showLoading() {
        progressBarMain.setVisibility(View.VISIBLE);
        recyclerHistory.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
        errorState.setVisibility(View.GONE);
    }

    private void showList() {
        progressBarMain.setVisibility(View.GONE);
        recyclerHistory.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        errorState.setVisibility(View.GONE);
    }

    private void showEmpty() {
        progressBarMain.setVisibility(View.GONE);
        recyclerHistory.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
        errorState.setVisibility(View.GONE);
    }

    private void showError(String msg) {
        progressBarMain.setVisibility(View.GONE);
        recyclerHistory.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
        errorState.setVisibility(View.VISIBLE);
        if (msg != null) txtError.setText(msg);
    }


    // ====================================================================
    //  INNER ADAPTER
    // ====================================================================
    public static class ResultHistoryAdapter
            extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int TYPE_ITEM    = 0;
        private static final int TYPE_LOADING = 1;

        public interface OnCardClickListener {
            void onClick(TournamentHistoryResponse.TournamentHistory tournament);
        }

        private final List<TournamentHistoryResponse.TournamentHistory> items;
        private final OnCardClickListener listener;
        private boolean loading = false;

        public ResultHistoryAdapter(List<TournamentHistoryResponse.TournamentHistory> items,
                                    OnCardClickListener listener) {
            this.items    = items;
            this.listener = listener;
        }

        public boolean isLoading() { return loading; }

        public void setLoading(boolean loading) {
            boolean was = this.loading;
            this.loading = loading;
            if (was && !loading) notifyItemRemoved(items.size());
            else if (!was && loading) notifyItemInserted(items.size());
        }

        @Override
        public int getItemViewType(int position) {
            return (loading && position == items.size()) ? TYPE_LOADING : TYPE_ITEM;
        }

        @Override
        public int getItemCount() {
            return items.size() + (loading ? 1 : 0);
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent,
                                                          int viewType) {
            android.view.LayoutInflater inflater = android.view.LayoutInflater.from(parent.getContext());

            if (viewType == TYPE_LOADING) {
                View v = inflater.inflate(R.layout.item_loading_footer, parent, false);
                return new LoadingVH(v);
            }

            View v = inflater.inflate(R.layout.item_result_history, parent, false);
            return new ItemVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (getItemViewType(position) == TYPE_LOADING) return;

            ItemVH vh = (ItemVH) holder;
            TournamentHistoryResponse.TournamentHistory t = items.get(position);

            // ── Tournament name ──
            vh.txtTournamentName.setText(
                    t.getLobbyName() != null && !t.getLobbyName().isEmpty()
                            ? t.getLobbyName().toUpperCase()
                            : "TOURNAMENT");

            // ── Mode badge ──
            String modeLabel = buildModeLabel(t.getMode(), t.getSubMode());
            vh.txtGameMode.setText("🎮 " + modeLabel);

            // ── Entry fee ──
            vh.txtEntryFee.setText(t.getEntryFee() > 0
                    ? t.getEntryFee() + " GC"
                    : "FREE");

            // ── Date ──
            vh.txtDate.setText(formatDate(t.getDate()));

            // ── Time ──
            vh.txtTime.setText(t.getStartTime() != null ? t.getStartTime() : "—");

            // ── Position ──
            int pos = t.getMyResult() != null ? t.getMyResult().getPosition() : 0;
            vh.txtPosition.setText(pos > 0 ? "#" + pos : "—");
            applyPositionColor(vh.txtPosition, pos);

            // ── Kills ──
            int kills = t.getMyResult() != null ? t.getMyResult().getKills() : 0;
            vh.txtKills.setText(String.valueOf(kills));

            // ── Reward ──
            int reward = t.getMyResult() != null ? t.getMyResult().getRewardGC() : 0;
            vh.txtReward.setText(reward + " GC");

            // ── Claimed ──
            boolean claimed = t.getMyResult() != null && t.getMyResult().isClaimed();
            vh.txtClaimed.setText(claimed ? "✅ Claimed" : "⏳ Pending");

            // ── Click listener ──
            vh.itemView.setOnClickListener(v -> listener.onClick(t));
        }

        // ── Helpers ──

        private String buildModeLabel(String mode, String subMode) {
            String m = mode != null ? mode : "—";
            String s = subMode != null
                    ? (subMode.substring(0, 1).toUpperCase() + subMode.substring(1))
                    : "";
            return s.isEmpty() ? m.toUpperCase() : (m + " " + s).toUpperCase();
        }

        private String formatDate(String iso) {
            if (iso == null || iso.isEmpty()) return "—";
            try {
                SimpleDateFormat input  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                SimpleDateFormat output = new SimpleDateFormat("dd MMM yyyy", Locale.US);
                Date d = input.parse(iso);
                return d != null ? output.format(d) : iso;
            } catch (ParseException e) {
                // Try without milliseconds
                try {
                    SimpleDateFormat input2  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                    SimpleDateFormat output2 = new SimpleDateFormat("dd MMM yyyy", Locale.US);
                    Date d = input2.parse(iso);
                    return d != null ? output2.format(d) : iso.substring(0, 10);
                } catch (ParseException ex) {
                    return iso.length() >= 10 ? iso.substring(0, 10) : iso;
                }
            }
        }

        private void applyPositionColor(TextView tv, int pos) {
            android.content.Context ctx = tv.getContext();
            if (pos == 1) {
                tv.setTextColor(android.graphics.Color.parseColor("#FFD700")); // gold
            } else if (pos == 2) {
                tv.setTextColor(android.graphics.Color.parseColor("#C0C0C0")); // silver
            } else if (pos == 3) {
                tv.setTextColor(android.graphics.Color.parseColor("#CD7F32")); // bronze
            } else {
                tv.setTextColor(android.graphics.Color.parseColor("#00D9FF")); // neon blue
            }
        }

        // ── ViewHolders ──

        static class ItemVH extends RecyclerView.ViewHolder {
            TextView txtTournamentName, txtGameMode;
            TextView txtEntryFee, txtDate, txtTime;
            TextView txtPosition, txtKills, txtReward, txtClaimed;

            ItemVH(@NonNull View v) {
                super(v);
                txtTournamentName = v.findViewById(R.id.txtTournamentName);
                txtGameMode       = v.findViewById(R.id.txtGameMode);
                txtEntryFee       = v.findViewById(R.id.txtEntryFee);
                txtDate           = v.findViewById(R.id.txtDate);
                txtTime           = v.findViewById(R.id.txtTime);
                txtPosition       = v.findViewById(R.id.txtPosition);
                txtKills          = v.findViewById(R.id.txtKills);
                txtReward         = v.findViewById(R.id.txtReward);
                txtClaimed        = v.findViewById(R.id.txtClaimed);
            }
        }

        static class LoadingVH extends RecyclerView.ViewHolder {
            LoadingVH(@NonNull View v) { super(v); }
        }
    }
}