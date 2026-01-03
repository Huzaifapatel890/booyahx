package com.booyahx.helpandsupport;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import android.os.Bundle;
import android.view.Gravity;
import android.view.animation.AlphaAnimation;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.booyahx.R;
import com.booyahx.TokenManager;
import com.booyahx.LoaderOverlay;
import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.ProfileResponse;
import com.booyahx.network.models.UpdateProfileRequest;
import com.booyahx.network.models.SimpleResponse;
import com.booyahx.utils.CSRFHelper;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import androidx.cardview.widget.CardView;

import com.booyahx.R;
import com.booyahx.TokenManager;
import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.CreateTicketRequest;
import com.booyahx.network.models.TicketResponse;
import com.booyahx.utils.CSRFHelper;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateTicketDialog {

    private final Context context;
    private final ApiService api;

    private boolean isSubmitting = false;

    public CreateTicketDialog(Context context) {
        this.context = context;
        this.api = ApiClient.getClient(context).create(ApiService.class);
    }

    public void show() {

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_create_ticket);
        dialog.getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        ImageView btnClose = dialog.findViewById(R.id.btnCloseDialog);
        Spinner spinnerSubject = dialog.findViewById(R.id.spinnerSubject);
        EditText etDescription = dialog.findViewById(R.id.etDescription);
        EditText etImageUrl = dialog.findViewById(R.id.etImageUrl);
        CardView btnSubmit = dialog.findViewById(R.id.btnSubmitTicket);

        // ✅ DEFAULT SAFE SPINNER (same as EditProfileActivity)
        String[] subjects = {
                "Select Subject",
                "Dispute",
                "Refund",
                "Top-up Limit",
                "Other"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_item,
                subjects
        );
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );
        spinnerSubject.setAdapter(adapter);

        btnClose.setOnClickListener(v -> {
            if (!isSubmitting) dialog.dismiss();
        });

        btnSubmit.setOnClickListener(v -> {
            if (!isSubmitting) submitTicket(
                    spinnerSubject,
                    etDescription,
                    etImageUrl,
                    btnSubmit,
                    dialog
            );
        });

        dialog.show();
    }

    // --------------------------------------------------
    // CREATE TICKET (SAME PATTERN AS EDIT PROFILE)
    // --------------------------------------------------
    private void submitTicket(
            Spinner spinnerSubject,
            EditText etDescription,
            EditText etImageUrl,
            CardView btnSubmit,
            Dialog dialog
    ) {

        String subject = spinnerSubject.getSelectedItem().toString();
        String issue = etDescription.getText().toString().trim();
        String imageUrl = etImageUrl.getText().toString().trim();

        if ("Select Subject".equals(subject)) {
            Toast.makeText(context, "Please select a subject", Toast.LENGTH_SHORT).show();
            return;
        }

        if (issue.isEmpty()) {
            Toast.makeText(context, "Please describe your issue", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> images = new ArrayList<>();
        if (!imageUrl.isEmpty()) {
            images.add(imageUrl);
        }

        CreateTicketRequest req = new CreateTicketRequest(
                null,
                subject,
                issue,
                images
        );

        setUiEnabled(false, btnSubmit);

        String token = TokenManager.getAccessToken(context);

        // ⭐ FETCH CSRF FIRST (IDENTICAL FLOW)
        CSRFHelper.fetchToken(context, new CSRFHelper.CSRFCallback() {

            @Override
            public void onSuccess(String csrf) {

                api.createTicket(
                        "Bearer " + token,
                        csrf,
                        req
                ).enqueue(new Callback<TicketResponse>() {

                    @Override
                    public void onResponse(
                            Call<TicketResponse> call,
                            Response<TicketResponse> response
                    ) {

                        setUiEnabled(true, btnSubmit);

                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()) {

                            Toast.makeText(
                                    context,
                                    "Ticket created successfully!",
                                    Toast.LENGTH_LONG
                            ).show();

                            dialog.dismiss();

                        } else {
                            Toast.makeText(
                                    context,
                                    "Failed to create ticket",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<TicketResponse> call, Throwable t) {
                        setUiEnabled(true, btnSubmit);
                        Toast.makeText(
                                context,
                                "Network error. Please try again",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                setUiEnabled(true, btnSubmit);
                Toast.makeText(
                        context,
                        error != null ? error : "Security error! Try again.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    // --------------------------------------------------
    // UI STATE
    // --------------------------------------------------
    private void setUiEnabled(boolean enabled, CardView btnSubmit) {
        isSubmitting = !enabled;
        btnSubmit.setEnabled(enabled);
        btnSubmit.setAlpha(enabled ? 1f : 0.6f);
    }
}