package com.booyahx.network.models;


import java.util.List;

public class CreateTicketRequest {

    private String tournamentId;
    private String subject;
    private String issue;
    private List<String> images;

    public CreateTicketRequest(String tournamentId,
                               String subject,
                               String issue,
                               List<String> images) {
        this.tournamentId = tournamentId;
        this.subject = subject;
        this.issue = issue;
        this.images = images;
    }
}