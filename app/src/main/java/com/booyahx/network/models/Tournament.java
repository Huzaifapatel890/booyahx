package com.booyahx.network.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Tournament implements Parcelable {

    @SerializedName("_id")
    private String id;

    @SerializedName("game")
    private String game;

    @SerializedName("mode")
    private String mode;

    @SerializedName("subMode")
    private String subMode;

    @SerializedName("entryFee")
    private int entryFee;

    @SerializedName("maxPlayers")
    private int maxPlayers;

    // ✅ FIXED: Use correct API field names
    @SerializedName("joinedTeams")
    private int joinedTeams;

    @SerializedName("maxTeams")
    private int maxTeams;

    @SerializedName("playersPerTeam")
    private int playersPerTeam;

    @SerializedName("prizePool")
    private int prizePool;

    // ✅ NEW: Max prize pool after all platform/host/caster fees are deducted
    @SerializedName("potentialPrizePool")
    private PotentialPrizePool potentialPrizePool;

    public static class PotentialPrizePool {
        @SerializedName("totalPrizePool")
        public int totalPrizePool;

        @SerializedName("platformFee")
        public int platformFee;

        @SerializedName("hostFee")
        public int hostFee;

        @SerializedName("casterFee")
        public int casterFee;

        @SerializedName("totalFees")
        public int totalFees;

        // ✅ Prize pool after all charges — this is what players can win
        @SerializedName("winnerPrizePool")
        public int winnerPrizePool;
    }

    @SerializedName("date")
    private String date;

    @SerializedName("startTime")
    private String startTime;

    @SerializedName("lobbyName")
    private String lobbyName;

    @SerializedName("rules")
    private TournamentRules rules;

    /* ================= STATUS ================= */

    @SerializedName("status")
    private String status;

    public String getStatus() {
        return status;
    }

    /* ================= HOST APPLICATION FIELDS (NEW) ================= */

    @SerializedName("hasApplied")
    public Boolean hasApplied;

    @SerializedName("applicationStatus")
    public String applicationStatus;

    @SerializedName("hostApplication")
    public HostApplication hostApplication;

    public static class HostApplication implements Parcelable {
        @SerializedName("applicationId")
        public String applicationId;

        @SerializedName("status")
        public String status;

        @SerializedName("appliedAt")
        public String appliedAt;

        protected HostApplication(Parcel in) {
            applicationId = in.readString();
            status = in.readString();
            appliedAt = in.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(applicationId);
            dest.writeString(status);
            dest.writeString(appliedAt);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<HostApplication> CREATOR = new Creator<HostApplication>() {
            @Override
            public HostApplication createFromParcel(Parcel in) {
                return new HostApplication(in);
            }

            @Override
            public HostApplication[] newArray(int size) {
                return new HostApplication[size];
            }
        };
    }

    /* ================= TEAMS (DERIVED JOIN LOGIC) ================= */

    @SerializedName("teams")
    private List<Team> teams;

    @SerializedName("hostId")
    private Host hostId;

    public Host getHostId() {
        return hostId;
    }

    public static class Host implements Parcelable {
        @SerializedName("_id")
        public String id;

        public String name;
        public String ign;

        protected Host(Parcel in) {
            id = in.readString();
            name = in.readString();
            ign = in.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(id);
            dest.writeString(name);
            dest.writeString(ign);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<Host> CREATOR = new Creator<Host>() {
            @Override
            public Host createFromParcel(Parcel in) {
                return new Host(in);
            }

            @Override
            public Host[] newArray(int size) {
                return new Host[size];
            }
        };
    }

    public boolean isJoinedDerived(String userId) {
        if (teams == null || userId == null) return false;
        for (Team team : teams) {
            if (userId.equals(team.leaderUserId)) {
                return true;
            }
        }
        return false;
    }

    public static class Team {
        @SerializedName("leaderUserId")
        public String leaderUserId;
    }

    public String getDate() {
        return date;
    }

    public String getStartTime() {
        return startTime;
    }
    /* ================= REQUIRED FOR JOIN ================= */

    public String getId() {
        return id;
    }

    /* ================= GETTERS ================= */

    public TournamentRules getRules() {
        return rules;
    }

    public int getEntryFee() {
        return entryFee;
    }

    // ✅ NEW: Expose subMode for client-side filtering
    public String getSubMode() {
        return subMode;
    }

    /* ================= DISPLAY HELPERS ================= */

    public String getTitle() {
        return lobbyName != null && !lobbyName.isEmpty()
                ? lobbyName
                : game + " " + subMode;
    }

    // ✅ UPDATED: Returns max prize pool after all charges are deducted.
    // Prefers potentialPrizePool.winnerPrizePool (explicit post-fee value from API).
    // Falls back to prizePool if potentialPrizePool is not present.
    public int getExpectedPP() {
        if (potentialPrizePool != null && potentialPrizePool.winnerPrizePool > 0) {
            return potentialPrizePool.winnerPrizePool;
        }
        return prizePool;
    }

    // ✅ FIXED: Calculate current prize pool from joined teams
    public int getCurrentPP() {
        return joinedTeams * entryFee;
    }

    /* ================= SLOT LOGIC - FIXED ================= */

    // ✅ FIXED: Return total number of team slots directly from API
    public int getTotalSlots() {
        return maxTeams;  // Use maxTeams from API instead of calculating
    }

    // ✅ FIXED: Return joined teams directly from API
    public int getUsedSlots() {
        return joinedTeams;  // Use joinedTeams from API instead of calculating
    }

    public String getDisplayMode() {
        return mode + " " + subMode;
    }

    public String getFormattedDateTime() {
        try {
            SimpleDateFormat api =
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            SimpleDateFormat ui =
                    new SimpleDateFormat("dd MMM yyyy", Locale.US);

            Date d = api.parse(date);
            return ui.format(d) + " • " + startTime;
        } catch (Exception e) {
            return startTime;
        }
    }

    /* ================= MAP ROTATION ================= */

    public String getMapRotationShort() {
        if (rules == null || rules.mapRotation == null || rules.mapRotation.isEmpty()) {
            return null;
        }

        StringBuilder rotation = new StringBuilder();
        for (int i = 0; i < rules.mapRotation.size(); i++) {
            String map = rules.mapRotation.get(i);
            rotation.append(map.substring(0, 1).toUpperCase());
            if (i < rules.mapRotation.size() - 1) rotation.append("/");
        }
        return rotation.toString();
    }

    /* ================= PARCELABLE ================= */

    protected Tournament(Parcel in) {
        id = in.readString();
        game = in.readString();
        mode = in.readString();
        subMode = in.readString();
        entryFee = in.readInt();
        maxPlayers = in.readInt();
        joinedTeams = in.readInt();  // ✅ FIXED
        maxTeams = in.readInt();      // ✅ FIXED
        playersPerTeam = in.readInt(); // ✅ FIXED
        prizePool = in.readInt();
        // ✅ NEW: Restore winnerPrizePool from parcel (-1 means not set)
        int winnerPrizePool = in.readInt();
        if (winnerPrizePool >= 0) {
            potentialPrizePool = new PotentialPrizePool();
            potentialPrizePool.winnerPrizePool = winnerPrizePool;
        }
        date = in.readString();
        startTime = in.readString();
        lobbyName = in.readString();
        rules = in.readParcelable(TournamentRules.class.getClassLoader());
        status = in.readString();
        hasApplied = (Boolean) in.readValue(Boolean.class.getClassLoader());
        applicationStatus = in.readString();
        hostApplication = in.readParcelable(HostApplication.class.getClassLoader());
        hostId = in.readParcelable(Host.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(game);
        dest.writeString(mode);
        dest.writeString(subMode);
        dest.writeInt(entryFee);
        dest.writeInt(maxPlayers);
        dest.writeInt(joinedTeams);    // ✅ FIXED
        dest.writeInt(maxTeams);        // ✅ FIXED
        dest.writeInt(playersPerTeam);  // ✅ FIXED
        dest.writeInt(prizePool);
        // ✅ NEW: Persist winnerPrizePool (-1 signals not available)
        dest.writeInt(potentialPrizePool != null ? potentialPrizePool.winnerPrizePool : -1);
        dest.writeString(date);
        dest.writeString(startTime);
        dest.writeString(lobbyName);
        dest.writeParcelable(rules, flags);
        dest.writeString(status);
        dest.writeValue(hasApplied);
        dest.writeString(applicationStatus);
        dest.writeParcelable(hostApplication, flags);
        dest.writeParcelable(hostId, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Tournament> CREATOR = new Creator<Tournament>() {
        @Override
        public Tournament createFromParcel(Parcel in) {
            return new Tournament(in);
        }

        @Override
        public Tournament[] newArray(int size) {
            return new Tournament[size];
        }
    };

    /* ================= INNER RULES ================= */

    public static class TournamentRules implements Parcelable {

        @SerializedName("mapRotation")
        public List<String> mapRotation;

        @SerializedName("rules")
        public List<String> rules;

        @SerializedName("generalRules")
        public List<String> generalRules;

        @SerializedName("positionPoints")
        public Map<String, Integer> positionPoints;

        @SerializedName("numberOfMatches")
        public int numberOfMatches;

        @SerializedName("maxPlayers")
        public int maxPlayers;

        @SerializedName("minTeamsToStart")
        public int minTeamsToStart;

        protected TournamentRules(Parcel in) {
            mapRotation = in.createStringArrayList();
            rules = in.createStringArrayList();
            generalRules = in.createStringArrayList();
            numberOfMatches = in.readInt();
            maxPlayers = in.readInt();
            minTeamsToStart = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeStringList(mapRotation);
            dest.writeStringList(rules);
            dest.writeStringList(generalRules);
            dest.writeInt(numberOfMatches);
            dest.writeInt(maxPlayers);
            dest.writeInt(minTeamsToStart);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<TournamentRules> CREATOR =
                new Creator<TournamentRules>() {
                    @Override
                    public TournamentRules createFromParcel(Parcel in) {
                        return new TournamentRules(in);
                    }

                    @Override
                    public TournamentRules[] newArray(int size) {
                        return new TournamentRules[size];
                    }
                };
    }
}