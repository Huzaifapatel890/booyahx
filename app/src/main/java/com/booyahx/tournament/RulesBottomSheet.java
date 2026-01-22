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

import com.booyahx.ProfileCacheManager;
import com.booyahx.R;
import com.booyahx.network.models.Tournament;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RulesBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_TOURNAMENT = "arg_tournament";

    private LinearLayout lobbyInfoContainer;
    private LinearLayout generalRulesContainer;
    private LinearLayout killPointsRow;
    private RecyclerView pointsGrid;
    private LinearLayout additionalRulesContainer;

    private TextView btnGotIt, btnJoinNow;
    private ImageView btnCloseSheet;

    private Tournament tournament;

    public static RulesBottomSheet newInstance(Tournament tournament) {
        RulesBottomSheet sheet = new RulesBottomSheet();
        Bundle b = new Bundle();
        b.putParcelable(ARG_TOURNAMENT, tournament);
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
        View view = inflater.inflate(R.layout.bottom_sheet_rules, container, false);

        tournament = getArguments() != null
                ? getArguments().getParcelable(ARG_TOURNAMENT)
                : null;

        initViews(view);
        setupButtonsBasedOnRole();
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
        btnJoinNow = v.findViewById(R.id.btnJoinNow);
        btnCloseSheet = v.findViewById(R.id.btnCloseSheet);
    }

    private void setupButtonsBasedOnRole() {
        String role = ProfileCacheManager.getRole(requireContext());

        if ("host".equalsIgnoreCase(role)) {
            // 🔥 HOST: HIDE JOIN BUTTON, CENTER GOT IT BUTTON
            btnJoinNow.setVisibility(View.GONE);
            btnGotIt.setVisibility(View.VISIBLE);

            // Center the Got It button
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) btnGotIt.getLayoutParams();
            params.width = LinearLayout.LayoutParams.MATCH_PARENT;
            btnGotIt.setLayoutParams(params);

        } else {
            // 🔥 USER: SHOW BOTH BUTTONS SIDE BY SIDE
            btnGotIt.setVisibility(View.VISIBLE);
            btnJoinNow.setVisibility(View.VISIBLE);

            // Reset to default side-by-side layout
            LinearLayout.LayoutParams paramsGotIt = (LinearLayout.LayoutParams) btnGotIt.getLayoutParams();
            paramsGotIt.width = 0;
            paramsGotIt.weight = 1;
            btnGotIt.setLayoutParams(paramsGotIt);

            LinearLayout.LayoutParams paramsJoin = (LinearLayout.LayoutParams) btnJoinNow.getLayoutParams();
            paramsJoin.width = 0;
            paramsJoin.weight = 1;
            btnJoinNow.setLayoutParams(paramsJoin);
        }
    }

    private void setupListeners() {
        btnCloseSheet.setOnClickListener(v -> dismiss());
        btnGotIt.setOnClickListener(v -> dismiss());

        btnJoinNow.setOnClickListener(v -> {
            if (tournament == null || getParentFragmentManager() == null) return;

            // 🔥 OPEN JOIN DIALOG (NO API HERE)
            JoinTournamentDialog dialog =
                    JoinTournamentDialog.newInstance(tournament);

            dialog.show(getParentFragmentManager(), "JoinTournamentDialog");

            dismiss(); // close rules sheet
        });
    }

    private void populateUI() {
        if (tournament == null || tournament.getRules() == null) return;

        Tournament.TournamentRules r = tournament.getRules();

        addInfoRow(lobbyInfoContainer, "Mode", tournament.getDisplayMode());
        addInfoRow(lobbyInfoContainer, "Matches", r.numberOfMatches + " matches");
        addInfoRow(lobbyInfoContainer, "Max Players", r.maxPlayers + " players");
        addInfoRow(lobbyInfoContainer, "Min Teams to Start", r.minTeamsToStart + " teams");

        if (r.rules != null) {
            for (String rule : r.rules) {
                String lower = rule.toLowerCase();
                if (lower.contains("kill point")
                        || lower.contains("kill points")
                        || lower.contains("position point")
                        || lower.contains("position points")) {
                    continue;
                }
                addRuleItem(generalRulesContainer, rule);
            }
        }

        addInfoRow(killPointsRow, "Kill Points", "1 point per kill per team");

        List<PositionPoint> list = new ArrayList<>();
        if (r.positionPoints != null) {
            for (Map.Entry<String, Integer> e : r.positionPoints.entrySet()) {
                list.add(new PositionPoint(e.getKey(), e.getValue() + " points"));
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