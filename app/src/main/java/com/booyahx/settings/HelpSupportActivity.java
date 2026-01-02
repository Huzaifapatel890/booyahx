package com.booyahx.settings;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.booyahx.R;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.booyahx.helpandsupport.TicketAdapter;
import com.booyahx.helpandsupport.FAQ;
import com.booyahx.helpandsupport.Ticket;
import java.util.ArrayList;
import java.util.List;

public class HelpSupportActivity extends AppCompatActivity {

    private LinearLayout faqContainer;
    private RecyclerView ticketsRecyclerView;
    private TicketAdapter ticketAdapter;
    private List<Ticket> allTickets, openTickets, closedTickets;
    private TextView tabAllTickets, tabOpen, tabClosed;
    private CardView btnCreateTicket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_support);

        initViews();
        setupFAQs();
        setupTickets();
        setupListeners();
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> onBackPressed());

        faqContainer = findViewById(R.id.faqContainer);
        ticketsRecyclerView = findViewById(R.id.ticketsRecyclerView);
        tabAllTickets = findViewById(R.id.tabAllTickets);
        tabOpen = findViewById(R.id.tabOpen);
        tabClosed = findViewById(R.id.tabClosed);
        btnCreateTicket = findViewById(R.id.btnCreateTicket);

        ticketsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupFAQs() {
        // Room & Match Category
        List<FAQ> roomMatchFAQs = new ArrayList<>();
        roomMatchFAQs.add(new FAQ("How do I join a scrim room?",
                "Go to the 'Rooms' tab, select your desired scrim match, and click 'Join Room'. Make sure you have sufficient balance for entry fee."));
        roomMatchFAQs.add(new FAQ("What happens if I can't join the room on time?",
                "If you miss the room entry time, your entry fee will be automatically refunded within 24 hours."));
        roomMatchFAQs.add(new FAQ("How are winners determined?",
                "Winners are determined based on final placement and kills. Results are updated automatically after match completion."));
        addFAQCategory("🎮 Room & Match", roomMatchFAQs);

        // Wallet & Payment Category
        List<FAQ> walletFAQs = new ArrayList<>();
        walletFAQs.add(new FAQ("How do I add money to my wallet?",
                "Click on 'Wallet' → 'Add Money', enter the amount and select your preferred payment method. We support UPI, Cards, and Net Banking."));
        walletFAQs.add(new FAQ("How long does withdrawal take?",
                "Withdrawals are processed within 24-48 hours. Amount will be credited to your registered bank account or UPI."));
        walletFAQs.add(new FAQ("Is there a minimum withdrawal amount?",
                "Yes, the minimum withdrawal amount is ₹100. Maximum per transaction is ₹50,000."));
        addFAQCategory("💰 Wallet & Payment", walletFAQs);

        // Prize & Winnings Category
        List<FAQ> prizeFAQs = new ArrayList<>();
        prizeFAQs.add(new FAQ("When will I receive my winnings?",
                "Winnings are credited to your wallet within 1 hour of match result verification."));
        prizeFAQs.add(new FAQ("How can I view my win history?",
                "Go to 'Profile' → 'Match History' to see all your past matches, winnings, and statistics."));
        addFAQCategory("🏆 Prize & Winnings", prizeFAQs);

        // Account & Profile Category
        List<FAQ> accountFAQs = new ArrayList<>();
        accountFAQs.add(new FAQ("How do I link my Free Fire ID?",
                "Go to 'Profile' → 'Game Settings' → 'Link Free Fire ID' and enter your 10-digit ID number."));
        accountFAQs.add(new FAQ("Can I change my registered mobile number?",
                "Yes, you can change it from 'Profile' → 'Account Settings'. You'll need to verify the new number via OTP."));
        addFAQCategory("👤 Account & Profile", accountFAQs);
    }

    private void addFAQCategory(String categoryName, List<FAQ> faqs) {
        View categoryView = getLayoutInflater().inflate(R.layout.item_faq_category, faqContainer, false);

        TextView tvCategoryName = categoryView.findViewById(R.id.tvCategoryName);
        TextView tvCategoryIcon = categoryView.findViewById(R.id.tvCategoryIcon);
        LinearLayout categoryHeader = categoryView.findViewById(R.id.categoryHeader);
        LinearLayout faqItemsContainer = categoryView.findViewById(R.id.faqItemsContainer);

        tvCategoryName.setText(categoryName);

        // Add FAQ items
        for (FAQ faq : faqs) {
            View faqView = getLayoutInflater().inflate(R.layout.item_faq_question, faqItemsContainer, false);
            TextView tvQuestion = faqView.findViewById(R.id.tvQuestion);
            TextView tvAnswer = faqView.findViewById(R.id.tvAnswer);
            CardView cardFaq = faqView.findViewById(R.id.cardFaq);

            tvQuestion.setText(faq.getQuestion());
            tvAnswer.setText(faq.getAnswer());

            cardFaq.setOnClickListener(v -> {
                if (tvAnswer.getVisibility() == View.GONE) {
                    tvAnswer.setVisibility(View.VISIBLE);
                } else {
                    tvAnswer.setVisibility(View.GONE);
                }
            });

            faqItemsContainer.addView(faqView);
        }

        // Category expand/collapse
        categoryHeader.setOnClickListener(v -> {
            if (faqItemsContainer.getVisibility() == View.GONE) {
                faqItemsContainer.setVisibility(View.VISIBLE);
                tvCategoryIcon.setRotation(180);
            } else {
                faqItemsContainer.setVisibility(View.GONE);
                tvCategoryIcon.setRotation(0);
            }
        });

        faqContainer.addView(categoryView);
    }

    private void setupTickets() {
        // Sample ticket data
        allTickets = new ArrayList<>();
        allTickets.add(new Ticket("TKT-1247", "Payment not received", "28 Dec 2025, 3:45 PM", true));
        allTickets.add(new Ticket("TKT-1182", "Room ID not working", "25 Dec 2025, 11:20 AM", false));

        openTickets = new ArrayList<>();
        openTickets.add(new Ticket("TKT-1247", "Payment not received", "28 Dec 2025, 3:45 PM", true));

        closedTickets = new ArrayList<>();
        closedTickets.add(new Ticket("TKT-1182", "Room ID not working", "25 Dec 2025, 11:20 AM", false));

        ticketAdapter = new TicketAdapter(allTickets);
        ticketsRecyclerView.setAdapter(ticketAdapter);
    }

    private void setupListeners() {
        tabAllTickets.setOnClickListener(v -> switchTab(tabAllTickets, allTickets));
        tabOpen.setOnClickListener(v -> switchTab(tabOpen, openTickets));
        tabClosed.setOnClickListener(v -> switchTab(tabClosed, closedTickets));
        btnCreateTicket.setOnClickListener(v -> showCreateTicketDialog());
    }

    private void switchTab(TextView selectedTab, List<Ticket> tickets) {
        // Reset all tabs
        tabAllTickets.setBackgroundResource(R.drawable.tab_unselected);
        tabAllTickets.setTextColor(Color.GRAY);
        tabOpen.setBackgroundResource(R.drawable.tab_unselected);
        tabOpen.setTextColor(Color.GRAY);
        tabClosed.setBackgroundResource(R.drawable.tab_unselected);
        tabClosed.setTextColor(Color.GRAY);

        // Set selected tab
        selectedTab.setBackgroundResource(R.drawable.tab_selected);
        selectedTab.setTextColor(Color.WHITE);

        // Update RecyclerView
        ticketAdapter.updateTickets(tickets);
    }

    private void showCreateTicketDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_create_ticket);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        ImageView btnClose = dialog.findViewById(R.id.btnCloseDialog);
        Spinner spinnerSubject = dialog.findViewById(R.id.spinnerSubject);
        EditText etDescription = dialog.findViewById(R.id.etDescription);
        EditText etImageUrl = dialog.findViewById(R.id.etImageUrl);
        CardView btnSubmit = dialog.findViewById(R.id.btnSubmitTicket);

        // Setup Spinner
        String[] subjects = {"Select Subject", "Dispute", "Refund", "Top-up Limit", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, subjects) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setTextColor(Color.WHITE);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
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
                Toast.makeText(this, "Please select a subject", Toast.LENGTH_SHORT).show();
                return;
            }

            if (description.isEmpty()) {
                Toast.makeText(this, "Please describe your issue", Toast.LENGTH_SHORT).show();
                return;
            }

            // Handle ticket creation
            // TODO: Send data to server
            Toast.makeText(this, "Ticket created successfully! Our support team will get back to you soon.", Toast.LENGTH_LONG).show();
            dialog.dismiss();
        });

        dialog.show();
    }
}


