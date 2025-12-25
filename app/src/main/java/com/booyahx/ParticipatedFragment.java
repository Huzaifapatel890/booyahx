package com.booyahx;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.JoinedTournamentResponse;
import com.booyahx.tournament.JoinedTournamentAdapter;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ParticipatedFragment extends Fragment {

    private static final String TAG = "JoinedTrace";

    RecyclerView rvTournaments;
    JoinedTournamentAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        Log.d(TAG, "onCreateView()");
        return inflater.inflate(R.layout.fragment_joined_tournaments, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        Log.d(TAG, "onViewCreated()");

        rvTournaments = view.findViewById(R.id.rvTournaments);
        Log.d(TAG, "RecyclerView found = " + (rvTournaments != null));

        rvTournaments.setLayoutManager(new LinearLayoutManager(requireContext()));
        Log.d(TAG, "LayoutManager set");

        adapter = new JoinedTournamentAdapter(null);
        rvTournaments.setAdapter(adapter);
        Log.d(TAG, "Adapter set");

        rvTournaments.post(() ->
                Log.d(TAG, "RecyclerView height = " + rvTournaments.getHeight())
        );

        getParentFragmentManager().setFragmentResultListener(
                "joined_refresh",
                this,
                (key, bundle) -> {
                    Log.d(TAG, "🔥 FragmentResult received → joined_refresh");
                    fetchJoinedTournaments();
                }
        );

        fetchJoinedTournaments();
    }

    private void fetchJoinedTournaments() {

        Log.d(TAG, "fetchJoinedTournaments() called");

        ApiService api = ApiClient.getClient(requireContext())
                .create(ApiService.class);

        api.getJoinedTournaments().enqueue(new Callback<JoinedTournamentResponse>() {
            @Override
            public void onResponse(
                    @NonNull Call<JoinedTournamentResponse> call,
                    @NonNull Response<JoinedTournamentResponse> response
            ) {
                Log.d(TAG, "API onResponse()");
                Log.d(TAG, "HTTP code = " + response.code());

                if (response.body() == null) {
                    Log.e(TAG, "Response body is NULL");
                    return;
                }

                if (response.body().getData() == null) {
                    Log.e(TAG, "Response data is NULL");
                    return;
                }

                if (response.body().getData().getTournaments() == null) {
                    Log.e(TAG, "Tournament list is NULL");
                    return;
                }

                Log.d(TAG,
                        "Tournament list size = "
                                + response.body().getData().getTournaments().size()
                );

                adapter.updateData(
                        response.body().getData().getTournaments()
                );
            }

            @Override
            public void onFailure(
                    @NonNull Call<JoinedTournamentResponse> call,
                    @NonNull Throwable t
            ) {
                Log.e(TAG, "API FAILED", t);
            }
        });
    }
}