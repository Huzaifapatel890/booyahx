package com.booyahx.network.models;

public class HostApplyRequest {

    public ApplicationDetails applicationDetails;

    public HostApplyRequest(
            String experience,
            String reason,
            String additionalInfo
    ) {
        this.applicationDetails = new ApplicationDetails(
                experience,
                reason,
                additionalInfo
        );
    }

    public static class ApplicationDetails {
        public String experience;
        public String reason;
        public String additionalInfo;

        public ApplicationDetails(
                String experience,
                String reason,
                String additionalInfo
        ) {
            this.experience = experience;
            this.reason = reason;
            this.additionalInfo = additionalInfo;
        }
    }
}