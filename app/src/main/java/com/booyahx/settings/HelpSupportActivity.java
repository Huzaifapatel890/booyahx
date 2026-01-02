package com.booyahx.settings;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.booyahx.R;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.booyahx.helpandsupport.TicketAdapter;
import com.booyahx.helpandsupport.FAQ;
import com.booyahx.helpandsupport.CreateTicketDialog;
import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.Ticket;
import com.booyahx.network.models.TicketResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HelpSupportActivity extends AppCompatActivity {

    private LinearLayout faqContainer;
    private RecyclerView ticketsRecyclerView;
    private TicketAdapter ticketAdapter;

    private List<Ticket> allTickets = new ArrayList<>();
    private List<Ticket> openTickets = new ArrayList<>();
    private List<Ticket> closedTickets = new ArrayList<>();

    private TextView tabAllTickets, tabOpen, tabClosed;
    private CardView btnCreateTicket;

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_support);

        apiService = ApiClient.getClient(this).create(ApiService.class);

        initViews();
        setupFAQs();
        setupTickets();
        setupListeners();

        loadAllTickets(); // default
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

    /* ---------------- FAQ LOGIC (UNCHANGED) ---------------- */

    private void setupFAQs() {
        List<FAQ> roomMatchFAQs = new ArrayList<>();
        roomMatchFAQs.add(new FAQ("How do I join a scrim room?",
                "Go to the 'Rooms' tab, select your desired scrim match, and click 'Join Room'. Make sure you have sufficient balance for entry fee."));
        roomMatchFAQs.add(new FAQ("What happens if I can't join the room on time?",
                "If you miss the room entry time, your entry fee will be automatically refunded within 24 hours."));
        roomMatchFAQs.add(new FAQ("How are winners determined?",
                "Winners are determined based on final placement and kills. Results are updated automatically after match completion."));
        addFAQCategory("🎮 Room & Match", roomMatchFAQs);

        List<FAQ> walletFAQs = new ArrayList<>();
        walletFAQs.add(new FAQ("How do I add money to my wallet?",
                "Click on 'Wallet' → 'Add Money', enter the amount and select your preferred payment method. We support UPI, Cards, and Net Banking."));
        walletFAQs.add(new FAQ("How long does withdrawal take?",
                "Withdrawals are processed within 24-48 hours. Amount will be credited to your registered bank account or UPI."));
        walletFAQs.add(new FAQ("Is there a minimum withdrawal amount?",
                "Yes, the minimum withdrawal amount is ₹100. Maximum per transaction is ₹50,000."));
        addFAQCategory("💰 Wallet & Payment", walletFAQs);

        List<FAQ> prizeFAQs = new ArrayList<>();
        prizeFAQs.add(new FAQ("When will I receive my winnings?",
                "Winnings are credited to your wallet within 1 hour of match result verification."));
        prizeFAQs.add(new FAQ("How can I view my win history?",
                "Go to 'Profile' → 'Match History' to see all your past matches, winnings, and statistics."));
        addFAQCategory("🏆 Prize & Winnings", prizeFAQs);

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

        for (FAQ faq : faqs) {
            View faqView = getLayoutInflater().inflate(R.layout.item_faq_question, faqItemsContainer, false);
            TextView tvQuestion = faqView.findViewById(R.id.tvQuestion);
            TextView tvAnswer = faqView.findViewById(R.id.tvAnswer);
            CardView cardFaq = faqView.findViewById(R.id.cardFaq);

            tvQuestion.setText(faq.getQuestion());
            tvAnswer.setText(faq.getAnswer());

            cardFaq.setOnClickListener(v ->
                    tvAnswer.setVisibility(
                            tvAnswer.getVisibility() == View.GONE ? View.VISIBLE : View.GONE
                    )
            );

            faqItemsContainer.addView(faqView);
        }

        categoryHeader.setOnClickListener(v -> {
            boolean expand = faqItemsContainer.getVisibility() == View.GONE;
            faqItemsContainer.setVisibility(expand ? View.VISIBLE : View.GONE);
            tvCategoryIcon.setRotation(expand ? 180 : 0);
        });

        faqContainer.addView(categoryView);
    }

    /* ---------------- TICKETS (REAL API) ---------------- */

    private void setupTickets() {
        ticketAdapter = new TicketAdapter(allTickets);
        ticketsRecyclerView.setAdapter(ticketAdapter);
    }

    private void setupListeners() {
        tabAllTickets.setOnClickListener(v -> {
            switchTab(tabAllTickets);
            loadAllTickets();
        });

        tabOpen.setOnClickListener(v -> {
            switchTab(tabOpen);
            loadOpenTickets();
        });

        tabClosed.setOnClickListener(v -> {
            switchTab(tabClosed);
            loadClosedTickets();
        });

        btnCreateTicket.setOnClickListener(v ->
                new CreateTicketDialog(this).show()
        );
    }

    private void switchTab(TextView selectedTab) {
        tabAllTickets.setBackgroundResource(R.drawable.tab_unselected);
        tabOpen.setBackgroundResource(R.drawable.tab_unselected);
        tabClosed.setBackgroundResource(R.drawable.tab_unselected);

        tabAllTickets.setTextColor(Color.GRAY);
        tabOpen.setTextColor(Color.GRAY);
        tabClosed.setTextColor(Color.GRAY);

        selectedTab.setBackgroundResource(R.drawable.tab_selected);
        selectedTab.setTextColor(Color.WHITE);
    }

    private void loadAllTickets() {
        apiService.getTickets(1, 20, null).enqueue(ticketCallback(allTickets));
    }

    private void loadOpenTickets() {
        apiService.getTickets(1, 20, "open").enqueue(ticketCallback(openTickets));
    }

    private void loadClosedTickets() {
        apiService.getTickets(1, 20, "closed").enqueue(ticketCallback(closedTickets));
    }

    private Callback<TicketResponse> ticketCallback(List<Ticket> targetList) {
        return new Callback<TicketResponse>() {
            @Override
            public void onResponse(Call<TicketResponse> call, Response<TicketResponse> response) {
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().isSuccess()) {

                    targetList.clear();
                    targetList.addAll(response.body().getData().getTickets());
                    ticketAdapter.updateTickets(targetList);
                }
            }

            @Override
            public void onFailure(Call<TicketResponse> call, Throwable t) {
                t.printStackTrace();
            }
        };
    }
}