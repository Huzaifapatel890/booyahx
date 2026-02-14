package com.booyahx.Host;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.booyahx.R;
import com.booyahx.ProfileCacheManager;
import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.LiveResultResponse;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EnhancedFinalResultDialog extends Dialog {

    private static final String TAG = "EnhancedFinalDialog";

    private final Context context;
    private List<FinalRow> results;
    private final String tournamentId;
    private final String tournamentStatus;

    private ImageView resultPreview;
    private LinearLayout downloadBtn, shareBtn;
    private TextView closeBtn, headerTitle, subHeaderText;
    private ProgressBar progressBar;

    private ProfessionalResultGenerator generator;

    // ✅ File exists ONLY after user presses download
    private File savedImageFile;

    // ✅ SINGLE SOURCE OF TRUTH FOR THEME
    private int selectedThemeRes;

    private volatile boolean isDestroyed = false;

    // ✅ Role-based flags
    private boolean isHost = false;
    private boolean isUser = false;

    // ✅ NEW: Callback for host navigation
    private OnHostCloseListener hostCloseListener;

    /**
     * ✅ NEW: Callback interface for host navigation
     */
    public interface OnHostCloseListener {
        void onHostClose();
    }

    /**
     * ✅ NEW: Setter for host close listener
     */
    public void setOnHostCloseListener(OnHostCloseListener listener) {
        this.hostCloseListener = listener;
    }

    /**
     * Constructor for role-based dialog
     * @param context Context
     * @param results Initial results (used for HOST only)
     * @param tournamentId Tournament ID for fetching live results
     * @param tournamentStatus Tournament status ("running" or "finished")
     */
    public EnhancedFinalResultDialog(
            @NonNull Context context,
            List<FinalRow> results,
            String tournamentId,
            String tournamentStatus
    ) {
        super(context);
        this.context = context;
        this.results = results;
        this.tournamentId = tournamentId;
        this.tournamentStatus = tournamentStatus;
        this.generator = new ProfessionalResultGenerator(context);

        // ✅ Get user role from ProfileCacheManager (the ACTUAL source of truth)
        String role = ProfileCacheManager.getRole(context);

        // Set flags based on role string
        this.isHost = "host".equalsIgnoreCase(role);
        this.isUser = "user".equalsIgnoreCase(role);

        Log.d(TAG, "Dialog initialized - role: " + role + ", isHost: " + isHost + ", isUser: " + isUser + ", status: " + tournamentStatus);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_enhanced_result);

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        initViews();

        // ✅ CONDITIONAL LOGIC BASED ON ROLE AND STATUS
        if (isHost) {
            // HOST: Check if results are provided or need to fetch
            if (results != null && !results.isEmpty()) {
                // Host provided results directly (old behavior)
                Log.d(TAG, "HOST view - showing provided results");
                headerTitle.setText("Overall Standings 🏆");
                subHeaderText.setText("Contact host via Raising Dispute ticket if you find any Error");
                resultPreview.post(this::generateBasedOnTime);
            } else {
                // Host needs to fetch results (match 6 scenario)
                Log.d(TAG, "HOST view - fetching results from API");
                headerTitle.setText("Overall Standings 🏆");
                subHeaderText.setText("Contact host via Raising Dispute ticket if you find any Error");
                fetchLiveResults();
            }
        } else if (isUser) {
            // USER: Dynamic behavior based on status
            if ("running".equalsIgnoreCase(tournamentStatus)) {
                Log.d(TAG, "USER view - tournament RUNNING - showing Live Results");
                headerTitle.setText("Live Results 🔥");
                subHeaderText.setText("Results are being updated live. Refresh to see latest standings.");
                fetchLiveResults();
            } else if ("finished".equalsIgnoreCase(tournamentStatus)) {
                Log.d(TAG, "USER view - tournament FINISHED - showing Overall Standings");
                headerTitle.setText("Overall Standings 🏆");
                subHeaderText.setText("Tournament has concluded. Final results are shown below.");
                fetchLiveResults(); // Fetch final standings
            } else {
                // Fallback for unknown status
                Log.w(TAG, "Unknown status: " + tournamentStatus + " - showing static view");
                headerTitle.setText("Tournament Results");
                subHeaderText.setText("Contact host via Raising Dispute ticket if you find any Error");
                resultPreview.post(this::generateBasedOnTime);
            }
        } else {
            // Fallback if role is not set
            Log.w(TAG, "No role detected - showing static view");
            headerTitle.setText("Overall Standings 🏆");
            subHeaderText.setText("Contact host via Raising Dispute ticket if you find any Error");
            resultPreview.post(this::generateBasedOnTime);
        }

        setupButtons();
    }

    @Override
    public void dismiss() {
        isDestroyed = true;
        super.dismiss();
    }

    private void initViews() {
        resultPreview = findViewById(R.id.resultPreview);
        downloadBtn = findViewById(R.id.downloadBtn);
        shareBtn = findViewById(R.id.shareBtn);
        closeBtn = findViewById(R.id.closeBtn);
        progressBar = findViewById(R.id.progressBar);

        // ✅ Get header views from layout
        headerTitle = findViewById(R.id.headerTitle);
        subHeaderText = findViewById(R.id.subHeaderText);

        resultPreview.setAdjustViewBounds(true);
        resultPreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
    }

    // ================= FETCH LIVE RESULTS (USER ONLY) =================
    private void fetchLiveResults() {
        if (tournamentId == null || tournamentId.isEmpty()) {
            Log.e(TAG, "Tournament ID is null or empty - cannot fetch live results");
            Toast.makeText(context, "Invalid tournament ID", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(ProgressBar.VISIBLE);
        resultPreview.setImageDrawable(null);

        ApiService apiService = ApiClient.getClient(context).create(ApiService.class);

        Log.d(TAG, "Fetching live results for tournament: " + tournamentId);

        apiService.getLiveResults(tournamentId).enqueue(new Callback<LiveResultResponse>() {
            @Override
            public void onResponse(Call<LiveResultResponse> call, Response<LiveResultResponse> response) {
                if (isDestroyed) return;

                progressBar.setVisibility(ProgressBar.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    LiveResultResponse liveResponse = response.body();

                    if (liveResponse.isSuccess() && liveResponse.getData() != null) {
                        Log.d(TAG, "Live results fetched successfully");

                        // ✅ Convert API standings to FinalRow format
                        List<LiveResultResponse.Standing> standings = liveResponse.getData().getStandings();

                        if (standings != null && !standings.isEmpty()) {
                            // ✅ Sort by totalPoint descending (highest first)
                            Collections.sort(standings, new Comparator<LiveResultResponse.Standing>() {
                                @Override
                                public int compare(LiveResultResponse.Standing s1, LiveResultResponse.Standing s2) {
                                    return Integer.compare(s2.getTotalPoint(), s1.getTotalPoint());
                                }
                            });

                            results = convertStandingsToFinalRows(standings);

                            Log.d(TAG, "Converted " + results.size() + " standings to FinalRow");

                            // Generate preview with fetched data
                            resultPreview.post(() -> generateBasedOnTime());
                        } else {
                            Log.w(TAG, "No standings data received from API");
                            Toast.makeText(context, "No results available yet", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "API response unsuccessful: " + liveResponse.getMessage());
                        Toast.makeText(context, liveResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "API call failed with code: " + response.code());
                    Toast.makeText(context, "Failed to load results: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LiveResultResponse> call, Throwable t) {
                if (isDestroyed) return;

                progressBar.setVisibility(ProgressBar.GONE);
                Log.e(TAG, "API call failed", t);
                Toast.makeText(context, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * ✅ FIXED: Convert API standings to FinalRow objects
     * FinalRow constructor: FinalRow(teamName, kills, positionPoints, totalPoints, booyah)
     */
    private List<FinalRow> convertStandingsToFinalRows(List<LiveResultResponse.Standing> standings) {
        List<FinalRow> finalRows = new ArrayList<>();

        for (LiveResultResponse.Standing standing : standings) {
            FinalRow row = new FinalRow(
                    standing.getTeamName() != null ? standing.getTeamName() : "Unknown",
                    standing.getKills(),                    // kills
                    standing.getTotalPositionPoints(),      // ✅ FIXED: Use getTotalPositionPoints() not getPosition()
                    standing.getTotalPoint(),               // totalPoints
                    standing.getBooyah()                    // booyah
            );
            finalRows.add(row);
        }

        return finalRows;
    }
    // ==================================================================

    // ================= PREVIEW (NO SAVE) =================
    private void generateBasedOnTime() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if (hour >= 6 && hour < 12) {
            selectedThemeRes = R.drawable.bloom_theme;
        } else if (hour >= 12 && hour < 17) {
            selectedThemeRes = R.drawable.anim_theme;
        } else if (hour >= 17 && hour < 21) {
            selectedThemeRes = R.drawable.nostalagic_theme;
        } else if (hour >= 21 && hour < 24) {
            selectedThemeRes = R.drawable.mountain_theme;
        } else {
            selectedThemeRes = R.drawable.galexy_theme;
        }

        generatePreview();
    }

    private void generatePreview() {
        if (results == null || results.isEmpty()) {
            Log.w(TAG, "No results available to generate preview");
            Toast.makeText(context, "No results to display", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(ProgressBar.VISIBLE);
        resultPreview.setImageDrawable(null);

        new Thread(() -> {
            Bitmap preview = generator.generatePreviewBitmap(results, selectedThemeRes);

            resultPreview.post(() -> {
                if (isDestroyed) return;
                resultPreview.setImageBitmap(preview);
                progressBar.setVisibility(ProgressBar.GONE);
            });
        }).start();
    }
    // ====================================================

    private void setupButtons() {

        // ================= DOWNLOAD =================
        downloadBtn.setOnClickListener(v -> {
            if (results == null || results.isEmpty()) {
                Toast.makeText(context, "No results to download", Toast.LENGTH_SHORT).show();
                return;
            }

            if (savedImageFile != null && savedImageFile.exists()) {
                Toast.makeText(context,
                        "Result already saved",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(ProgressBar.VISIBLE);

            new Thread(() -> {
                File file = generator.generateWithBackground(
                        results,
                        "Final_Result",
                        selectedThemeRes // ✅ SAME THEME AS PREVIEW
                );

                savedImageFile = file;

                resultPreview.post(() -> {
                    progressBar.setVisibility(ProgressBar.GONE);
                    Toast.makeText(context,
                            "Result saved to Pictures/BooyahX/",
                            Toast.LENGTH_LONG).show();
                });
            }).start();
        });

        // ================= SHARE =================
        shareBtn.setOnClickListener(v -> {
            if (savedImageFile == null || !savedImageFile.exists()) {
                Toast.makeText(context,
                        "Please download first",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            shareImage();
        });

        // ✅ FIXED: Close button behavior based on role
        closeBtn.setOnClickListener(v -> {
            if (isHost && hostCloseListener != null) {
                // ✅ Host: Trigger callback to reopen HostSubmitResultDialog
                Log.d(TAG, "Host clicked close - triggering callback");
                dismiss();
                hostCloseListener.onHostClose();
            } else {
                // ✅ User or no callback: Just dismiss
                Log.d(TAG, "User clicked close - just dismissing");
                dismiss();
            }
        });
    }

    private void shareImage() {
        if (savedImageFile == null || !savedImageFile.exists()) {
            Toast.makeText(context, "Image not found", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri imageUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    savedImageFile
            );

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_STREAM, imageUri);
            intent.putExtra(Intent.EXTRA_TEXT,
                    "🏆 Tournament Results - Shared from BooyahX");

            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // 🔥 REQUIRED FOR DIALOG

            context.startActivity(Intent.createChooser(intent, "Share Result"));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context,
                    "Failed to share image",
                    Toast.LENGTH_SHORT).show();
        }
    }
}