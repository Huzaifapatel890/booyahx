package com.booyahx.utils;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
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

import com.booyahx.R;

public class GlobalLoadingDialog extends DialogFragment {

    private static final String TAG = "LoadingDialog_DEBUG";
    private static GlobalLoadingDialog currentInstance = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "🟢 onCreate() called");
        setCancelable(false);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "🟢 onCreateDialog() called");
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
        Log.d(TAG, "🟢 onCreateView() called");
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

        Log.d(TAG, "✅ View created with animations");
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "🟢 onStart() called");
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        Log.d(TAG, "✅ Dialog started and visible");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "🔴 onDestroyView() called");
        if (currentInstance == this) {
            Log.d(TAG, "   Clearing currentInstance");
            currentInstance = null;
        }
    }

    /**
     * Shows the loading dialog with extensive logging
     */
    public static void show(FragmentManager fragmentManager) {
        Log.d(TAG, "========================================");
        Log.d(TAG, "🔵 show() called");
        Log.d(TAG, "   FragmentManager: " + (fragmentManager != null ? "EXISTS" : "NULL"));

        if (fragmentManager == null) {
            Log.e(TAG, "   ❌ FragmentManager is NULL - CANNOT SHOW");
            return;
        }

        Log.d(TAG, "   FragmentManager.isDestroyed(): " + fragmentManager.isDestroyed());

        if (fragmentManager.isDestroyed()) {
            Log.e(TAG, "   ❌ FragmentManager is DESTROYED - CANNOT SHOW");
            return;
        }

        try {
            // Check if already showing
            if (currentInstance != null) {
                Log.d(TAG, "   currentInstance exists");
                Log.d(TAG, "      isAdded: " + currentInstance.isAdded());
                Log.d(TAG, "      isVisible: " + currentInstance.isVisible());
                Log.d(TAG, "      isResumed: " + currentInstance.isResumed());

                if (currentInstance.isAdded()) {
                    Log.d(TAG, "   ℹ️ Dialog already showing - SKIPPING");
                    return;
                }
            }

            // Remove any existing dialog by tag
            DialogFragment existingDialog = (DialogFragment) fragmentManager.findFragmentByTag(TAG);
            if (existingDialog != null) {
                Log.d(TAG, "   Found existing dialog by tag - dismissing");
                existingDialog.dismissAllowingStateLoss();
            }

            // Clear reference
            currentInstance = null;

            // Execute pending transactions
            Log.d(TAG, "   Executing pending transactions...");
            fragmentManager.executePendingTransactions();
            Log.d(TAG, "   ✅ Pending transactions executed");

            // Create and show
            Log.d(TAG, "   Creating new dialog instance...");
            currentInstance = new GlobalLoadingDialog();
            Log.d(TAG, "   Calling show()...");
            currentInstance.show(fragmentManager, TAG);
            Log.d(TAG, "   ✅✅✅ DIALOG SHOW CALLED SUCCESSFULLY");

        } catch (IllegalStateException e) {
            Log.e(TAG, "   ❌ IllegalStateException: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            Log.e(TAG, "   ❌ Exception: " + e.getMessage());
            e.printStackTrace();
        }
        Log.d(TAG, "========================================");
    }

    /**
     * Hides the loading dialog with extensive logging
     */
    public static void hide(FragmentManager fragmentManager) {
        Log.d(TAG, "========================================");
        Log.d(TAG, "🔴 hide() called");
        Log.d(TAG, "   FragmentManager: " + (fragmentManager != null ? "EXISTS" : "NULL"));

        if (fragmentManager == null) {
            Log.e(TAG, "   ⚠️ FragmentManager is NULL");
            return;
        }

        Log.d(TAG, "   FragmentManager.isDestroyed(): " + fragmentManager.isDestroyed());

        if (fragmentManager.isDestroyed()) {
            Log.e(TAG, "   ⚠️ FragmentManager is DESTROYED");
            return;
        }

        try {
            // Dismiss current instance
            if (currentInstance != null) {
                Log.d(TAG, "   Dismissing currentInstance");
                Log.d(TAG, "      isAdded: " + currentInstance.isAdded());
                Log.d(TAG, "      isVisible: " + currentInstance.isVisible());
                currentInstance.dismissAllowingStateLoss();
                currentInstance = null;
                Log.d(TAG, "   ✅ currentInstance dismissed");
            } else {
                Log.d(TAG, "   ℹ️ No currentInstance to dismiss");
            }

            // Also remove by tag as fallback
            DialogFragment dialog = (DialogFragment) fragmentManager.findFragmentByTag(TAG);
            if (dialog != null) {
                Log.d(TAG, "   Found dialog by tag - dismissing");
                dialog.dismissAllowingStateLoss();
                Log.d(TAG, "   ✅ Dialog dismissed by tag");
            } else {
                Log.d(TAG, "   ℹ️ No dialog found by tag");
            }

            Log.d(TAG, "   ✅✅✅ HIDE COMPLETED");

        } catch (Exception e) {
            Log.e(TAG, "   ❌ Exception during hide: " + e.getMessage());
            e.printStackTrace();
        }
        Log.d(TAG, "========================================");
    }
}