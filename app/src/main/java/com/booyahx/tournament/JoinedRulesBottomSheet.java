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
import com.booyahx.network.models.JoinedTournament;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JoinedRulesBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_JOINED = "arg_joined";

    private JoinedTournament tournament;

    private LinearLayout lobbyInfoContainer;
    private LinearLayout generalRulesContainer;
    private LinearLayout killPointsRow;
    private RecyclerView pointsGrid;
    private LinearLayout additionalRulesContainer;

    private TextView btnGotIt;
    private ImageView btnCloseSheet;

    public static JoinedRulesBottomSheet newInstance(JoinedTournament t) {
        JoinedRulesBottomSheet sheet = new JoinedRulesBottomSheet();
        Bundle b = new Bundle();
        b.putSerializable(ARG_JOINED, t);
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
        View v = inflater.inflate(R.layout.bottom_sheet_joined_rules, container, false);

        tournament = getArguments() != null
                ? (JoinedTournament) getArguments().getSerializable(ARG_JOINED)
                : null;

        initViews(v);
        setupListeners();
        populateUI();

        return v;
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

        if (tournament == null || tournament.getRules() == null) return;

        JoinedTournament.JoinedRules r = tournament.getRules();

        addInfoRow(lobbyInfoContainer, "Mode",
                tournament.getMode() + " - " + tournament.getSubMode());

        addInfoRow(lobbyInfoContainer, "Matches",
                r.numberOfMatches + " matches");

        addInfoRow(lobbyInfoContainer, "Max Players",
                r.maxPlayers + " players");

        addInfoRow(lobbyInfoContainer, "Min Teams to Start",
                r.minTeamsToStart + " teams");

        if (r.rules != null) {
            for (String rule : r.rules) {
                addRuleItem(generalRulesContainer, rule);
            }
        }

        addInfoRow(killPointsRow, "Kill Points", "1 point per kill per team");

        List<PositionPoint> list = new ArrayList<>();
        if (r.positionPoints != null) {
            for (Map.Entry<String, Integer> e : r.positionPoints.entrySet()) {
                list.add(new PositionPoint(e.getKey(), e.getValue() + " pts"));
            }
        }

        pointsGrid.setLayoutManager(new GridLayoutManager(getContext(), 2));
        pointsGrid.setAdapter(new PointsAdapter(list));

        if (r.generalRules != null) {
            for (String rule : r.generalRules) {
                addRuleItem(additionalRulesContainer, rule);
            }
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

    private static class PointsAdapter
            extends RecyclerView.Adapter<PointsAdapter.VH> {

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