package com.booyahx.tournament;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.booyahx.R;
import com.booyahx.network.models.HostTournament;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HostRulesBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_TOURNAMENT_JSON = "arg_tournament_json";

    private LinearLayout lobbyInfoContainer;
    private LinearLayout generalRulesContainer;
    private LinearLayout killPointsRow;
    private RecyclerView pointsGrid;
    private LinearLayout additionalRulesContainer;

    private TextView btnGotIt;
    private ImageView btnCloseSheet;

    private HostTournament tournament;

    public static HostRulesBottomSheet newInstance(HostTournament tournament) {
        HostRulesBottomSheet sheet = new HostRulesBottomSheet();
        Bundle b = new Bundle();
        // Convert tournament to JSON string to pass safely
        b.putString(ARG_TOURNAMENT_JSON, new Gson().toJson(tournament));
        sheet.setArguments(b);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.bottom_sheet_host_rules, container, false);

        // Parse tournament from JSON
        if (getArguments() != null) {
            String json = getArguments().getString(ARG_TOURNAMENT_JSON);
            tournament = new Gson().fromJson(json, HostTournament.class);
        }

        initViews(view);
        setupListeners();
        populateUI();

        return view;
    }

    private void initViews(View v) {
        lobbyInfoContainer = v.findViewById(R.id.lobbyInfoContainer);
        generalRulesContainer = v.findViewById(R.id.generalRulesContainer);
        killPointsRow = v.findViewById(R.id.killPointsRow);
        pointsGrid = v.findViewById(R.id.pointsGrid);
        additionalRulesContainer = v.findViewById(R.id.additionalRulesContainer);

        btnGotIt = v.findViewById(R.id.btnGotIt);
        btnCloseSheet = v.findViewById(R.id.btnCloseSheet);
    }

    private void setupListeners() {
        btnCloseSheet.setOnClickListener(v -> dismiss());
        btnGotIt.setOnClickListener(v -> dismiss());
    }

    private void populateUI() {
        if (tournament == null) return;

        // Get rules from tournament using getRules() method
        JsonObject rules = tournament.getRules();
        if (rules == null) return;

        // Display mode
        String modeDisplay = tournament.getModeDisplay();
        addInfoRow(lobbyInfoContainer, "Mode", modeDisplay);

        // Get nested rules data
        int numberOfMatches = rules.has("numberOfMatches") ? rules.get("numberOfMatches").getAsInt() : 6;
        int maxPlayers = rules.has("maxPlayers") ? rules.get("maxPlayers").getAsInt() : 48;
        int minTeamsToStart = rules.has("minTeamsToStart") ? rules.get("minTeamsToStart").getAsInt() : 10;

        addInfoRow(lobbyInfoContainer, "Matches", numberOfMatches + " matches");
        addInfoRow(lobbyInfoContainer, "Max Players", maxPlayers + " players");
        addInfoRow(lobbyInfoContainer, "Min Teams to Start", minTeamsToStart + " teams");

        // Add general rules
        if (rules.has("rules") && rules.get("rules").isJsonArray()) {
            rules.get("rules").getAsJsonArray().forEach(element -> {
                String rule = element.getAsString();
                String lower = rule.toLowerCase();
                // Skip kill and position point rules as they're shown separately
                if (!lower.contains("kill point") && !lower.contains("position point")) {
                    addRuleItem(generalRulesContainer, rule);
                }
            });
        }

        // Kill points
        addInfoRow(killPointsRow, "Kill Points", "1 point per kill per team");

        // Position points
        List<PositionPoint> pointsList = new ArrayList<>();
        if (rules.has("positionPoints") && rules.get("positionPoints").isJsonObject()) {
            JsonObject positionPoints = rules.get("positionPoints").getAsJsonObject();
            for (Map.Entry<String, com.google.gson.JsonElement> entry : positionPoints.entrySet()) {
                pointsList.add(new PositionPoint(
                        entry.getKey(),
                        entry.getValue().getAsInt() + " points"
                ));
            }
        }

        pointsGrid.setLayoutManager(new GridLayoutManager(getContext(), 2));
        pointsGrid.setAdapter(new PointsAdapter(pointsList));

        // Additional rules
        if (rules.has("generalRules") && rules.get("generalRules").isJsonArray()) {
            rules.get("generalRules").getAsJsonArray().forEach(element -> {
                addRuleItem(additionalRulesContainer, element.getAsString());
            });
        }
    }

    private void addInfoRow(LinearLayout container, String label, String value) {
        View v = LayoutInflater.from(getContext())
                .inflate(R.layout.item_info_row, container, false);
        ((TextView) v.findViewById(R.id.tvLabel)).setText(label);
        ((TextView) v.findViewById(R.id.tvValue)).setText(value);
        container.addView(v);
    }

    private void addRuleItem(LinearLayout container, String text) {
        View v = LayoutInflater.from(getContext())
                .inflate(R.layout.item_rule, container, false);
        ((TextView) v.findViewById(R.id.tvRuleText)).setText(text);
        container.addView(v);
    }

    @Override
    public int getTheme() {
        return R.style.BottomSheetDialogTheme;
    }

    /* ================= POINTS ================= */

    private static class PositionPoint {
        String pos, pts;
        PositionPoint(String p, String pts) {
            this.pos = p;
            this.pts = pts;
        }
    }

    private static class PointsAdapter extends RecyclerView.Adapter<PointsAdapter.VH> {

        private final List<PositionPoint> list;

        PointsAdapter(List<PositionPoint> l) {
            list = l;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int v) {
            return new VH(LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_point_card, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int i) {
            h.pos.setText(list.get(i).pos);
            h.pts.setText(list.get(i).pts);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView pos, pts;
            VH(View v) {
                super(v);
                pos = v.findViewById(R.id.tvPosition);
                pts = v.findViewById(R.id.tvPoints);
            }
        }
    }
}
