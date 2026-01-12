package com.booyahx.Host;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class HostPointsHelper {

    // match -> team -> [kp, position, pp, total]
    private final List<List<int[]>> matchWise;
    private final List<String> teams;

    public HostPointsHelper(List<List<int[]>> matchWise, List<String> teams) {
        this.matchWise = matchWise;
        this.teams = teams;
    }

    // ================= POSITION POINTS =================
    private int getPositionPoints(int pos) {
        switch (pos) {
            case 1: return 12;
            case 2: return 9;
            case 3: return 8;
            case 4: return 7;
            case 5: return 6;
            case 6: return 5;
            case 7: return 4;
            case 8: return 3;
            case 9: return 2;
            case 10:
            case 11:
            case 12:
                return 1;
            default:
                return 0;
        }
    }

    // ================= FINAL TOTALS =================
    public List<TeamFinal> calculateFinalTable() {

        List<TeamFinal> finals = new ArrayList<>();

        for (int i = 0; i < teams.size(); i++) {
            finals.add(new TeamFinal(i + 1, teams.get(i)));
        }

        for (List<int[]> match : matchWise) {
            for (int t = 0; t < match.size(); t++) {
                int kp = match.get(t)[0];
                int position = match.get(t)[1];
                int pp = getPositionPoints(position);
                int tp = kp + pp;

                TeamFinal tf = finals.get(t);
                tf.totalKP += kp;
                tf.totalPP += pp;
                tf.totalTP += tp;

                if (position == 1) tf.booyahCount++;
            }
        }

        // ================= SORT LOGIC =================
        Collections.sort(finals, new Comparator<TeamFinal>() {
            @Override
            public int compare(TeamFinal a, TeamFinal b) {

                if (a.totalTP != b.totalTP)
                    return b.totalTP - a.totalTP;

                if (a.booyahCount != b.booyahCount)
                    return b.booyahCount - a.booyahCount;

                if (a.totalPP != b.totalPP)
                    return b.totalPP - a.totalPP;

                return b.totalKP - a.totalKP;
            }
        });

        // Assign SR
        for (int i = 0; i < finals.size(); i++) {
            finals.get(i).rank = i + 1;
        }

        return finals;
    }

    // ================= MATCH WISE TABLE =================
    public List<List<TeamMatchRow>> buildMatchWiseTables() {

        List<List<TeamMatchRow>> result = new ArrayList<>();

        for (int m = 0; m < matchWise.size(); m++) {
            List<TeamMatchRow> rows = new ArrayList<>();
            List<int[]> match = matchWise.get(m);

            for (int t = 0; t < match.size(); t++) {
                int kp = match.get(t)[0];
                int pos = match.get(t)[1];
                int pp = getPositionPoints(pos);

                rows.add(new TeamMatchRow(
                        teams.get(t),
                        kp,
                        pp,
                        kp + pp,
                        pos
                ));
            }

            Collections.sort(rows, (a, b) -> b.total - a.total);

            for (int i = 0; i < rows.size(); i++) {
                rows.get(i).rank = i + 1;
            }

            result.add(rows);
        }

        return result;
    }

    // ================= MODELS =================
    public static class TeamFinal {
        public int slot;
        public String team;
        public int rank;
        public int totalKP = 0;
        public int totalPP = 0;
        public int totalTP = 0;
        public int booyahCount = 0;

        public TeamFinal(int slot, String team) {
            this.slot = slot;
            this.team = team;
        }
    }

    public static class TeamMatchRow {
        public int rank;
        public String team;
        public int kp;
        public int pp;
        public int total;
        public int position;

        public TeamMatchRow(String team, int kp, int pp, int total, int position) {
            this.team = team;
            this.kp = kp;
            this.pp = pp;
            this.total = total;
            this.position = position;
        }
    }
}