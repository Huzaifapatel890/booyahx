package com.booyahx.tournament;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.booyahx.R;
import com.booyahx.network.models.JoinedTournament;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SlotsBottomSheetDialog extends BottomSheetDialogFragment {

    private static final String ARG_TOURNAMENT = "tournament";
    private static final String ARG_USER_ID    = "user_id";

    private JoinedTournament tournament;
    private String           userId;

    public static SlotsBottomSheetDialog newInstance(JoinedTournament tournament, String userId) {
        SlotsBottomSheetDialog dialog = new SlotsBottomSheetDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TOURNAMENT, tournament);
        args.putString(ARG_USER_ID, userId);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tournament = (JoinedTournament) getArguments().getSerializable(ARG_TOURNAMENT);
            userId     = getArguments().getString(ARG_USER_ID, "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_slots_bottom_sheet, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Make sheet background transparent so our custom rounded bg_slots_sheet shows
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        // Expand fully on open
        View bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (tournament == null) {
            dismiss();
            return;
        }

        TextView tvTitle  = view.findViewById(R.id.tvSlotsTitle);
        TextView tvMode   = view.findViewById(R.id.tvSlotsMode);
        TextView tvBadge  = view.findViewById(R.id.tvSlotsBadge);
        TextView tvFilled = view.findViewById(R.id.tvSlotsFilled);
        TextView tvOpen   = view.findViewById(R.id.tvSlotsOpen);
        TextView tvTotal  = view.findViewById(R.id.tvSlotsTotal);
        RecyclerView rv   = view.findViewById(R.id.rvSlots);

        // ── Header ──
        String lobbyName = tournament.getLobbyName();
        tvTitle.setText(lobbyName != null && !lobbyName.isEmpty() ? lobbyName : tournament.getGame());

        String modeText = (tournament.getMode()    != null ? tournament.getMode()    : "") +
                " · " +
                (tournament.getSubMode() != null ? tournament.getSubMode() : "") +
                " · " + tournament.getMaxTeams() + " Slots";
        tvMode.setText(modeText);

        // ── Status badge ──
        String status = tournament.getStatus() != null
                ? tournament.getStatus().toLowerCase(Locale.getDefault()) : "";

        if (status.contains("live") || status.contains("running")) {
            tvBadge.setText("LIVE");
            tvBadge.setBackgroundResource(R.drawable.bg_badge_live);
            tvBadge.setTextColor(0xFF00E676);
        } else if (status.contains("cancel")) {
            tvBadge.setText("CANCELLED");
            tvBadge.setBackgroundResource(R.drawable.bg_badge_cancelled);
            tvBadge.setTextColor(0xFFFF5252);
        } else if (status.contains("pending") || status.contains("result")) {
            tvBadge.setText("PENDING");
            tvBadge.setBackgroundResource(R.drawable.bg_badge_pending);
            tvBadge.setTextColor(0xFFFFC94D);
        } else if (status.contains("finished") || status.contains("completed")) {
            tvBadge.setText("FINISHED");
            tvBadge.setBackgroundResource(R.drawable.bg_badge_cancelled);
            tvBadge.setTextColor(0xFFFF5252);
        } else {
            tvBadge.setText("UPCOMING");
            tvBadge.setBackgroundResource(R.drawable.bg_badge_upcoming);
            tvBadge.setTextColor(0xFF00E5FF);
        }

        // ── Summary chips ──
        int filled = tournament.getJoinedTeams();
        int total  = tournament.getMaxTeams();
        int open   = Math.max(0, total - filled);
        tvFilled.setText(String.valueOf(filled));
        tvOpen.setText(String.valueOf(open));
        tvTotal.setText(String.valueOf(total));

        // ── Build slot item list ──
        List<SlotAdapter.SlotItem> slots = new ArrayList<>();
        List<JoinedTournament.JoinedTeamInfo> teamsList = tournament.getJoinedTeamsList();

        for (int i = 1; i <= total; i++) {
            JoinedTournament.JoinedTeamInfo teamInfo =
                    (teamsList != null && i <= teamsList.size()) ? teamsList.get(i - 1) : null;

            boolean isFilled = teamInfo != null;
            boolean isYou    = isFilled && userId != null && userId.equals(teamInfo.leaderUserId);
            String  teamName = isFilled ? (teamInfo.teamName != null ? teamInfo.teamName : "") : "";

            slots.add(new SlotAdapter.SlotItem(i, teamName, isFilled, isYou));
        }

        rv.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rv.setAdapter(new SlotAdapter(slots));
    }
}