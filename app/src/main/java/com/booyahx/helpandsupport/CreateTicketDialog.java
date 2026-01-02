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
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.booyahx.R;

public class CreateTicketDialog {

    private final Context context;

    public CreateTicketDialog(Context context) {
        this.context = context;
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

        // Spinner setup
        String[] subjects = {
                "Select Subject",
                "Dispute",
                "Refund",
                "Top-up Limit",
                "Other"
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, subjects) {

                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View view = super.getView(position, convertView, parent);
                        TextView text = view.findViewById(android.R.id.text1);
                        text.setTextColor(Color.WHITE);
                        return view;
                    }

                    @Override
                    public View getDropDownView(int position, View convertView, ViewGroup parent) {
                        View view = super.getDropDownView(position, convertView, parent);
                        TextView text = view.findViewById(android.R.id.text1);
                        text.setTextColor(Color.WHITE);
                        text.setBackgroundColor(Color.parseColor("#1A1A1A"));
                        return view;
                    }
                };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubject.setAdapter(adapter);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnSubmit.setOnClickListener(v -> {

            String subject = spinnerSubject.getSelectedItem().toString();
            String description = etDescription.getText().toString().trim();
            String imageUrl = etImageUrl.getText().toString().trim();

            if (subject.equals("Select Subject")) {
                Toast.makeText(context, "Please select a subject", Toast.LENGTH_SHORT).show();
                return;
            }

            if (description.isEmpty()) {
                Toast.makeText(context, "Please describe your issue", Toast.LENGTH_SHORT).show();
                return;
            }

            // 🔥 API CALL WILL GO HERE LATER

            Toast.makeText(
                    context,
                    "Ticket created successfully! Our support team will get back to you soon.",
                    Toast.LENGTH_LONG
            ).show();

            dialog.dismiss();
        });

        dialog.show();
    }
}