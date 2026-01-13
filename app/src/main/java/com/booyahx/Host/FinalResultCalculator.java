package com.booyahx.Host;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FinalResultCalculator {

    public static List<FinalRow> calculate(
            List<List<int[]>> matchScores,
            List<String> teams
    ) {

        int teamCount = teams.size();
        int[] kp = new int[teamCount];
        int[] pp = new int[teamCount];
        int[] total = new int[teamCount];
        int[] booyah = new int[teamCount];

        for (List<int[]> match : matchScores) {
            for (int t = 0; t < teamCount; t++) {
                int[] row = match.get(t);
                kp[t] += row[0];
                pp[t] += row[2];
                total[t] += row[3];
                if (row[1] == 1) booyah[t]++; // 🔥 BOOYAH LOGIC
            }
        }

        List<FinalRow> result = new ArrayList<>();
        for (int i = 0; i < teamCount; i++) {
            result.add(new FinalRow(
                    teams.get(i),
                    kp[i],
                    pp[i],
                    total[i],
                    booyah[i]
            ));
        }

        Collections.sort(result, (a, b) -> {
            if (b.total != a.total) return b.total - a.total;
            if (b.booyah != a.booyah) return b.booyah - a.booyah;
            return b.kp - a.kp;
        });

        return result;
    }
}