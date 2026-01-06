package com.booyahx.network.models;
import java.util.List;
import com.google.gson.annotations.SerializedName;
public class HostApplicationsResponse {
    public Data data;

    public static class Data {
        public List<Application> applications;
    }

    public static class Application {
        public String status;
        public TournamentRef tournamentId;
    }

    public static class TournamentRef {
        @SerializedName("_id")
        public String id;
    }
}