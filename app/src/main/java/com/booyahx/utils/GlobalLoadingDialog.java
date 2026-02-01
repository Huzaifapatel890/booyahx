package com.booyahx.utils;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.booyahx.R;

public class GlobalLoadingDialog extends DialogFragment {

    private static final String TAG = "GlobalLoadingDialog";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_global_loading, container, false);

        View blurLayer = view.findViewById(R.id.blurLayer);
        ImageView ring = view.findViewById(R.id.loaderRing);
        ImageView glow = view.findViewById(R.id.loaderGlow);

        // Apply blur effect (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            blurLayer.setRenderEffect(
                    android.graphics.RenderEffect.createBlurEffect(
                            28f, 28f,
                            android.graphics.Shader.TileMode.CLAMP
                    )
            );
        } else {
            blurLayer.setAlpha(0.5f);
        }

        // ROTATE RING
        RotateAnimation rotate = new RotateAnimation(
                0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        rotate.setDuration(900);
        rotate.setRepeatCount(Animation.INFINITE);
        rotate.setInterpolator(new LinearInterpolator());
        ring.startAnimation(rotate);

        // GLOW PULSE
        ScaleAnimation pulse = new ScaleAnimation(
                1f, 1.25f,
                1f, 1.25f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        pulse.setDuration(850);
        pulse.setRepeatCount(Animation.INFINITE);
        pulse.setRepeatMode(Animation.REVERSE);
        glow.startAnimation(pulse);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    /**
     * Shows the loading dialog. This method handles all edge cases and prevents crashes.
     * @param fragmentManager The FragmentManager to use
     */
    public static void show(FragmentManager fragmentManager) {
        if (fragmentManager == null) {
            return;
        }

        try {
            // Remove any existing instance first
            DialogFragment existingDialog = (DialogFragment) fragmentManager.findFragmentByTag(TAG);
            if (existingDialog != null) {
                existingDialog.dismissAllowingStateLoss();
            }

            // Execute pending transactions to ensure old dialog is fully removed
            fragmentManager.executePendingTransactions();

            // Create and show new instance
            GlobalLoadingDialog dialog = new GlobalLoadingDialog();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.add(dialog, TAG);
            transaction.commitAllowingStateLoss();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Hides the loading dialog
     * @param fragmentManager The FragmentManager to use
     */
    public static void hide(FragmentManager fragmentManager) {
        if (fragmentManager == null) {
            return;
        }

        try {
            DialogFragment dialog = (DialogFragment) fragmentManager.findFragmentByTag(TAG);
            if (dialog != null) {
                dialog.dismissAllowingStateLoss();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}