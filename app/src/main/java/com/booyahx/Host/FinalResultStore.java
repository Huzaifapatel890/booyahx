package com.booyahx.Host;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public class FinalResultStore {

    private static final String PREF = "final_tournament_results";
    private static final long EXPIRY_MS = 60L * 24 * 60 * 60 * 1000;

    public static void save(Context ctx, String tournamentId, List<FinalRow> rows) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();

        long now = System.currentTimeMillis();
        ed.putLong(tournamentId + "_ts", now);
        ed.putInt(tournamentId + "_size", rows.size());

        for (int i = 0; i < rows.size(); i++) {
            FinalRow r = rows.get(i);
            String value =
                    r.teamName + "|" +
                            r.kp + "|" +
                            r.pp + "|" +
                            r.total + "|" +
                            r.booyah;

            ed.putString(tournamentId + "row" + i, value);
        }

        ed.apply();
    }

    public static List<FinalRow> load(Context ctx, String tournamentId) {
        SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);

        long ts = sp.getLong(tournamentId + "_ts", 0);
        if (System.currentTimeMillis() - ts > EXPIRY_MS) return null;

        int size = sp.getInt(tournamentId + "_size", 0);
        if (size == 0) return null;

        List<FinalRow> rows = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String raw = sp.getString(tournamentId + "row" + i, null);
            if (raw == null) continue;

            String[] p = raw.split("\\|");
            rows.add(new FinalRow(
                    p[0],
                    Integer.parseInt(p[1]),
                    Integer.parseInt(p[2]),
                    Integer.parseInt(p[3]),
                    Integer.parseInt(p[4])
            ));
        }
        return rows;
    }
}