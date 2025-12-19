package com.booyahx;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.booyahx.R;
import com.booyahx.network.models.Tournament;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private LinearLayout tournamentsContainer;
    private LinearLayout btnBermuda, btnClashSquad, btnSpecial;
    private String currentMode = "BERMUDA";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize views
        tournamentsContainer = view.findViewById(R.id.tournamentsContainer);
        btnBermuda = view.findViewById(R.id.btnBermuda);
        btnClashSquad = view.findViewById(R.id.btnClashSquad);
        btnSpecial = view.findViewById(R.id.btnSpecial);

        // Set click listeners for mode buttons
        btnBermuda.setOnClickListener(v -> switchMode("BERMUDA"));
        btnClashSquad.setOnClickListener(v -> switchMode("CLASH_SQUAD"));
        btnSpecial.setOnClickListener(v -> switchMode("SPECIAL"));

        // Load initial data
        loadTournamentsForMode(currentMode);

        return view;
    }

    private void switchMode(String mode) {
        currentMode = mode;

        // Update button states (visual feedback)
        updateButtonStates(mode);

        // Load tournaments for selected mode
        loadTournamentsForMode(mode);
    }

    private void updateButtonStates(String mode) {
        // Reset all buttons
        btnBermuda.setAlpha(0.5f);
        btnClashSquad.setAlpha(0.5f);
        btnSpecial.setAlpha(0.5f);

        // Highlight selected button
        switch (mode) {
            case "BERMUDA":
                btnBermuda.setAlpha(1.0f);
                break;
            case "CLASH_SQUAD":
                btnClashSquad.setAlpha(1.0f);
                break;
            case "SPECIAL":
                btnSpecial.setAlpha(1.0f);
                break;
        }
    }

    private void loadTournamentsForMode(String mode) {
        // Clear existing tournament cards
        tournamentsContainer.removeAllViews();

        // Get tournaments for this mode (from API/Database)
        List<Tournament> tournaments = getTournamentsForMode(mode);

        // Add tournament cards dynamically
        for (Tournament tournament : tournaments) {
            View tournamentCard = createTournamentCard(tournament);
            tournamentsContainer.addView(tournamentCard);
        }
    }

    private View createTournamentCard(Tournament tournament) {
        View card = LayoutInflater.from(getContext())
                .inflate(R.layout.item_tournament_card, tournamentsContainer, false);

        // Populate card with tournament data
        TextView txtTitle = card.findViewById(R.id.txtT1Title);
        TextView txtExpectedPP = card.findViewById(R.id.txtExpectedPP);
        TextView txtCurrentPP = card.findViewById(R.id.txtCurrentPP);
        TextView txtSub = card.findViewById(R.id.txtT1Sub);
        TextView txtTime = card.findViewById(R.id.txtT1Time);
        TextView txtMode = card.findViewById(R.id.txtT1Mode);
        TextView btnJoin = card.findViewById(R.id.btnT1Join);

        txtTitle.setText(tournament.getTitle());
        txtExpectedPP.setText(tournament.getExpectedPP() + " GC");
        txtCurrentPP.setText("(" + tournament.getCurrentPP() + "/" +
                tournament.getExpectedPP() + ") GC");
        txtSub.setText("Entry ₹" + tournament.getEntryFee() + " • Slots " +
                tournament.getCurrentSlots() + " / " + tournament.getTotalSlots());
        txtTime.setText(tournament.getDateTime());
        txtMode.setText("Mode: " + tournament.getMode());

        btnJoin.setOnClickListener(v -> {
            // Handle join tournament
            joinTournament(tournament);
        });

        return card;
    }

    private List<Tournament> getTournamentsForMode(String mode) {
        // TODO: Replace with actual API call or database query
        List<Tournament> tournaments = new ArrayList<>();

        // Sample data based on mode
        switch (mode) {
            case "BERMUDA":
                tournaments.add(new Tournament(
                        "Free Fire Bermuda Solo", 2000, 800, 30, 32, 100,
                        "Today • 7:30 PM", "SOLO"
                ));
                tournaments.add(new Tournament(
                        "Bermuda Squad Rush", 5000, 3200, 50, 45, 100,
                        "Tomorrow • 8:00 PM", "SQUAD"
                ));
                break;
            case "CLASH_SQUAD":
                tournaments.add(new Tournament(
                        "Clash Squad Pro", 3000, 1500, 40, 28, 80,
                        "Today • 9:00 PM", "CLASH SQUAD"
                ));
                tournaments.add(new Tournament(
                        "CS Championship", 10000, 7500, 100, 60, 120,
                        "Saturday • 6:00 PM", "CLASH SQUAD"
                ));
                break;
            case "SPECIAL":
                tournaments.add(new Tournament(
                        "Special Event Royale", 15000, 12000, 75, 50, 100,
                        "Sunday • 5:00 PM", "SPECIAL"
                ));
                break;
        }

        return tournaments;
    }

    private void joinTournament(Tournament tournament) {
        // TODO: Implement join tournament logic
        // Show confirmation dialog, process payment, etc.
    }
}