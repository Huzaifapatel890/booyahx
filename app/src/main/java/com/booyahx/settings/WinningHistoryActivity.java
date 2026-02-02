package com.booyahx.settings;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.booyahx.LoaderOverlay;
import com.booyahx.R;
import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.WalletHistoryResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WinningHistoryActivity extends AppCompatActivity {

    RecyclerView recyclerWinningHistory;
    TextView tvEmptyState;
    WinningHistoryAdapter adapter;
    List<WinningHistoryItem> list = new ArrayList<>();
    ApiService api;

    boolean isLoading = false; // 🔒 prevent duplicate calls

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_winning_history);

        api = ApiClient.getClient(this).create(ApiService.class);

        recyclerWinningHistory = findViewById(R.id.recyclerWinningHistory);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        recyclerWinningHistory.setLayoutManager(new LinearLayoutManager(this));

        adapter = new WinningHistoryAdapter(list);
        recyclerWinningHistory.setAdapter(adapter);

        loadWinningHistory();
    }

    private void loadWinningHistory() {

        if (isLoading) return;
        isLoading = true;

        LoaderOverlay.show(this);

        api.getWalletHistory(50, 0).enqueue(new Callback<WalletHistoryResponse>() {

            @Override
            public void onResponse(Call<WalletHistoryResponse> call,
                                   Response<WalletHistoryResponse> response) {

                isLoading = false;
                LoaderOverlay.hide(WinningHistoryActivity.this);

                Log.d("WIN_HISTORY", "onResponse called");

                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().success
                        && response.body().data != null
                        && response.body().data.history != null) {

                    List<WalletHistoryResponse.HistoryItem> apiList =
                            response.body().data.history;

                    Log.d("WIN_HISTORY", "Items = " + apiList.size());

                    list.clear();

                    for (WalletHistoryResponse.HistoryItem h : apiList) {
                        WinningHistoryItem item = new WinningHistoryItem();
                        item.amountGC = h.amountGC;
                        item.description = h.description;
                        item.date = formatDate(h.timestamp);
                        item.type = h.type; // ✅ ADDED (ONLY THIS)
                        list.add(item);
                    }

                    adapter.notifyDataSetChanged();

                    // Show/hide empty state based on data
                    updateEmptyState();

                } else {
                    showTopRightToast("No history found");
                    updateEmptyState();
                }
            }

            @Override
            public void onFailure(Call<WalletHistoryResponse> call, Throwable t) {

                isLoading = false;
                LoaderOverlay.hide(WinningHistoryActivity.this);

                Log.e("WIN_HISTORY", "Request failed", t);
                showTopRightToast("Something went wrong. Try again.");
                updateEmptyState();
            }
        });
    }

    private void updateEmptyState() {
        if (list.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerWinningHistory.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerWinningHistory.setVisibility(View.VISIBLE);
        }
    }

    private String formatDate(String isoDate) {
        try {
            SimpleDateFormat iso =
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            Date date = iso.parse(isoDate);

            SimpleDateFormat out =
                    new SimpleDateFormat("dd MMM yyyy • hh:mm a");
            return out.format(date);

        } catch (Exception e) {
            return isoDate;
        }
    }

    private void showTopRightToast(String message) {

        TextView tv = new TextView(this);
        tv.setText(message);
        tv.setPadding(40, 25, 40, 25);
        tv.setTextColor(0xFFFFFFFF);
        tv.setBackgroundResource(R.drawable.toast_bg);
        tv.setTextSize(14);

        android.widget.Toast toast = new android.widget.Toast(getApplicationContext());
        toast.setView(tv);
        toast.setDuration(android.widget.Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP | Gravity.END, 40, 120);

        AlphaAnimation fade = new AlphaAnimation(0f, 1f);
        fade.setDuration(350);
        tv.startAnimation(fade);

        toast.show();
    }
}