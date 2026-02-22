package com.booyahx.tournament;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.booyahx.DashboardActivity;
import com.booyahx.ProfileCacheManager;
import com.booyahx.R;
import com.booyahx.TokenManager;
import com.booyahx.settings.EditProfileActivity;
import com.booyahx.network.ApiClient;
import com.booyahx.network.ApiService;
import com.booyahx.network.models.JoinTournamentRequest;
import com.booyahx.network.models.JoinTournamentResponse;
import com.booyahx.network.models.ProfileResponse;
import com.booyahx.network.models.Tournament;


import org.json.JSONArray;
import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class JoinTournamentDialog extends DialogFragment {

    private static final String ARG_TOURNAMENT = "arg_tournament";

    private Tournament tournament;
    private ApiService api;

    private EditText etTeamName, etPlayer1;
    private TextView btnJoin;
    private ImageView btnClose;

    private boolean isJoining = false; // 🔥 STATE FLAG

    public static JoinTournamentDialog newInstance(Tournament t) {
        JoinTournamentDialog d = new JoinTournamentDialog();
        Bundle b = new Bundle();
        b.putParcelable(ARG_TOURNAMENT, t);
        d.setArguments(b);
        return d;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View v = inflater.inflate(R.layout.dialog_join_tournament, container, false);

        tournament = getArguments() != null
                ? getArguments().getParcelable(ARG_TOURNAMENT)
                : null;

        api = ApiClient.getClient(requireContext()).create(ApiService.class);

        etTeamName = v.findViewById(R.id.etTeamName);
        etPlayer1 = v.findViewById(R.id.etPlayer1);
        btnJoin = v.findViewById(R.id.btnJoin);
        btnClose = v.findViewById(R.id.btnClose);

        // 🔒 single-line + lock IGN
        etTeamName.setSingleLine(true);
        etTeamName.setMaxLines(1);

        etPlayer1.setSingleLine(true);
        etPlayer1.setMaxLines(1);
        etPlayer1.setEnabled(false);

        btnClose.setOnClickListener(v1 -> {
            if (!isJoining) dismiss();
        });

        btnJoin.setOnClickListener(v1 -> {
            if (!isJoining) {
                // ✅ CHECK IGN BEFORE SUBMITTING
                String ign = etPlayer1.getText().toString().trim();
                if (TextUtils.isEmpty(ign)) {
                    // ✅ REDIRECT TO UPDATE PROFILE
                    dismiss();
                    Intent intent = new Intent(requireContext(), EditProfileActivity.class);
                    startActivity(intent);
                } else {
                    submitJoin();
                }
            }
        });

        loadProfileAndPrefill();

        return v;
    }

    // --------------------------------------------------
    // LOAD PROFILE → PREFILL IGN + TEAM NAME (FROM CACHE)
    // --------------------------------------------------
    private void loadProfileAndPrefill() {
        // ✅ LOAD FROM CACHE INSTEAD OF API
        String ign = ProfileCacheManager.getIgn(requireContext());

        if (!TextUtils.isEmpty(ign)) {
            etPlayer1.setText(ign);
            etTeamName.setText(ign + "'s team");
            etTeamName.setSelection(etTeamName.getText().length());
        }
        // No need for error handling since cache is always available locally
    }

    // --------------------------------------------------
    // JOIN TOURNAMENT (CSRF APPLIED — NO LOGIC REMOVED)
    // --------------------------------------------------
    private void submitJoin() {

        String teamName = etTeamName.getText().toString().trim();

        if (TextUtils.isEmpty(teamName) || tournament == null) return;

        setUiEnabled(false); // 🔥 DISABLE EVERYTHING

        JoinTournamentRequest req =
                new JoinTournamentRequest(
                        tournament.getId(),
                        teamName
                );

        // ✅ DIRECT API CALL
        api.joinTournament(req)
                .enqueue(new Callback<JoinTournamentResponse>() {

                    @Override
                    public void onResponse(
                            Call<JoinTournamentResponse> call,
                            Response<JoinTournamentResponse> response
                    ) {

                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().success) {

                            showToast(response.body().message);

                            Bundle result = new Bundle();
                            result.putString("tournament_id", tournament.getId());
                            getParentFragmentManager().setFragmentResult(
                                    "join_success",
                                    result
                            );

                            // ✅ Delay dismiss so the loader finishes its hide animation
                            // before the dialog closes — makes the transition feel smooth.
                            // The GlobalLoadingInterceptor hides the loader on the same
                            // onResponse chain; 350ms gives it time to fully complete.
                            if (getView() != null) {
                                getView().postDelayed(() -> {
                                    if (isAdded()) dismiss();
                                }, 350);
                            } else {
                                dismiss();
                            }

                        } else {
                            showToast(parseErrorMessage(response));
                            setUiEnabled(true);
                        }
                    }

                    @Override
                    public void onFailure(Call<JoinTournamentResponse> call, Throwable t) {
                        showToast("Network error");
                        setUiEnabled(true);
                    }
                });
    }

    // --------------------------------------------------
    // ENABLE / DISABLE UI
    // --------------------------------------------------
    private void setUiEnabled(boolean enabled) {
        isJoining = !enabled;

        btnJoin.setEnabled(enabled);
        btnClose.setEnabled(enabled);
        etTeamName.setEnabled(enabled);

        btnJoin.setAlpha(enabled ? 1f : 0.6f);
    }

    // --------------------------------------------------
    // PARSE BACKEND ERROR MESSAGE
    // --------------------------------------------------
    private String parseErrorMessage(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String error = response.errorBody().string();
                JSONObject obj = new JSONObject(error);

                if (obj.has("errors")) {
                    JSONArray arr = obj.getJSONArray("errors");
                    if (arr.length() > 0) {
                        return arr.getJSONObject(0).getString("message");
                    }
                }

                if (obj.has("message")) {
                    return obj.getString("message");
                }
            }
        } catch (Exception ignored) {}

        return "Request failed";
    }

    // --------------------------------------------------
    // DASHBOARD TOAST
    // --------------------------------------------------
    private void showToast(String msg) {
        if (getActivity() instanceof DashboardActivity) {
            ((DashboardActivity) getActivity()).showTopRightToast(msg);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            getDialog().getWindow()
                    .setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
}