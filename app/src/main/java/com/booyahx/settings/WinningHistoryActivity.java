package com.booyahx.settings;

import android.os.Bundle;
import android.view.Gravity;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.booyahx.LoaderOverlay;
import com.booyahx.R;
import com.booyahx.TokenManager;
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
    WinningHistoryAdapter adapter;
    List<WinningHistoryItem> list = new ArrayList<>();
    ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_winning_history);

        api = ApiClient.getClient(this).create(ApiService.class);

        recyclerWinningHistory = findViewById(R.id.recyclerWinningHistory);
        recyclerWinningHistory.setLayoutManager(new LinearLayoutManager(this));

        adapter = new WinningHistoryAdapter(list);
        recyclerWinningHistory.setAdapter(adapter);

        loadWinningHistory();
    }

    // --------------------------------------------------------------------
    // ⭐ LOAD WINNING HISTORY FROM API (/api/wallet/history)
    // --------------------------------------------------------------------
    private void loadWinningHistory() {

        LoaderOverlay.show(this);

        String token = TokenManager.getAccessToken(this);

        api.getWalletHistory("Bearer " + token, 50, 0).enqueue(new Callback<WalletHistoryResponse>() {
            @Override
            public void onResponse(Call<WalletHistoryResponse> call, Response<WalletHistoryResponse> response) {

                LoaderOverlay.hide(WinningHistoryActivity.this);

                if (response.isSuccessful() && response.body() != null && response.body().success) {

                    List<WalletHistoryResponse.HistoryItem> apiList = response.body().data.history;

                    list.clear();

                    for (WalletHistoryResponse.HistoryItem h : apiList) {

                        // Only show entries where "type = won" or GC added?
                        WinningHistoryItem item = new WinningHistoryItem();
                        item.amountGC = h.amountGC;
                        item.description = h.description;
                        item.date = formatDate(h.timestamp);

                        list.add(item);
                    }

                    adapter.notifyDataSetChanged();

                } else {
                    if (response.body() != null && response.body().message != null)
                        showTopRightToast(response.body().message);
                    else
                        showTopRightToast("Failed to load history!");
                }
            }

            @Override
            public void onFailure(Call<WalletHistoryResponse> call, Throwable t) {
                LoaderOverlay.hide(WinningHistoryActivity.this);
                showTopRightToast("Network error loading history");
            }
        });
    }

    // --------------------------------------------------------------------
    // FORMAT DATE
    // --------------------------------------------------------------------
    private String formatDate(String isoDate) {
        try {
            SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            Date date = iso.parse(isoDate);

            SimpleDateFormat out = new SimpleDateFormat("dd MMM yyyy • hh:mm a");
            return out.format(date);

        } catch (Exception e) {
            return isoDate;
        }
    }

    // --------------------------------------------------------------------
    // NEON TOAST
    // --------------------------------------------------------------------
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