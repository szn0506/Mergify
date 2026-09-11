package com.szn.merger.Utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ReplacementSpan;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import java.util.function.Consumer;

public class Utils {

    /**
     * ALL-IN-ONE METHOD:
     * Handles TextWatcher, Debounce Handler, Layout Transition Animations,
     * and independently triggers premium typing text effects.
     * * Uses java.util.function.Consumer<String> as a callback.
     */
    public static void animateTextChange(
            EditText editText,
            ViewGroup transitionContainer,
            long delayMillis,
            Consumer<String> listener
    ) {
        if (editText == null) return;

        // Local Handler & Runnable, completely isolated within the method
        final Handler searchHandler = new Handler(Looper.getMainLooper());
        final Runnable[] searchRunnable = new Runnable[1];

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 1. Debounce Logic
                if (searchRunnable[0] != null) {
                    searchHandler.removeCallbacks(searchRunnable[0]);
                }

                if (listener != null) {
                    searchRunnable[0] = () -> {
                        if (transitionContainer != null) {
                            applyLayoutTransition(transitionContainer);
                        }
                        listener.accept(s.toString().toLowerCase().trim());
                    };
                    searchHandler.postDelayed(searchRunnable[0], delayMillis);
                }

                // 2. Text Animation Logic
                if (count > before) {
                    Editable editable = editText.getText();

                    class LocalAnimatedTypingSpan extends ReplacementSpan {
                        private float progress = 0f;

                        public void setProgress(float progress) {
                            this.progress = progress;
                        }

                        @Override
                        public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
                            return (int) paint.measureText(text, start, end);
                        }

                        @Override
                        public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
                            Paint animatedPaint = new Paint(paint);
                            animatedPaint.setAlpha((int) (70 + (185 * progress)));

                            float scale = 0.75f + (0.25f * (progress * progress));
                            float blur = 30f * (1f - progress);

                            animatedPaint.setShadowLayer(blur, 0f, 0f, animatedPaint.getColor());
                            float width = animatedPaint.measureText(text, start, end);

                            canvas.save();
                            canvas.translate(x + (width / 2f), y);
                            canvas.scale(scale, scale);
                            canvas.drawText(text, start, end, -width / 2f, 0, animatedPaint);
                            canvas.restore();
                        }
                    }

                    if (editable == null || editable.length() == 0) {
                        LocalAnimatedTypingSpan[] oldSpans = editable.getSpans(
                                0, editable.length(), LocalAnimatedTypingSpan.class
                        );
                        for (LocalAnimatedTypingSpan span : oldSpans) {
                            editable.removeSpan(span);
                        }
                        return;
                    }

                    final int index = editable.length() - 1;
                    if (index < 0 || index >= editable.length()) return;

                    editText.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

                    Object oldAnimator = editText.getTag();
                    if (oldAnimator instanceof ValueAnimator) {
                        ((ValueAnimator) oldAnimator).cancel();
                    }

                    LocalAnimatedTypingSpan[] oldSpans = editable.getSpans(
                            0, editable.length(), LocalAnimatedTypingSpan.class
                    );
                    for (LocalAnimatedTypingSpan span : oldSpans) {
                        editable.removeSpan(span);
                    }

                    LocalAnimatedTypingSpan span = new LocalAnimatedTypingSpan();
                    editable.setSpan(span, index, index + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
                    animator.setDuration(150);
                    animator.setInterpolator(new PathInterpolator(0.16f, 1f, 0.3f, 1f));

                    animator.addUpdateListener(animation -> {
                        span.setProgress((float) animation.getAnimatedValue());
                        editText.invalidate();
                    });

                    animator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            Editable current = editText.getText();
                            if (current == null) return;
                            current.removeSpan(span);
                            editText.invalidate();
                        }
                    });

                    editText.setTag(animator);
                    animator.start();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        editText.setTag(watcher);
        editText.addTextChangedListener(watcher);
    }

    public static void applyLayoutTransition(View view) {
        if (view != null && view.getParent() instanceof ViewGroup) {
            applyLayoutTransition((ViewGroup) view.getParent());
        }
    }

    public static void applyLayoutTransition(ViewGroup viewGroup) {
        if (viewGroup != null) {
            AutoTransition transition = new AutoTransition();
            transition.setDuration(220);
            transition.setInterpolator(new PathInterpolator(0.4f, 0f, 0.2f, 1f));
            TransitionManager.beginDelayedTransition(viewGroup, transition);
        }
    }

    public static void animateButtonState(MaterialButton button, boolean enable) {
        if (button == null || button.isEnabled() == enable) return;

        TypedValue colorSurface = new TypedValue();
        TypedValue colorPrimary = new TypedValue();

        button.getContext().getTheme().resolveAttribute(
                com.google.android.material.R.attr.colorSurfaceVariant, colorSurface, true
        );
        button.getContext().getTheme().resolveAttribute(
                com.google.android.material.R.attr.colorPrimary, colorPrimary, true
        );

        int startColor = enable ? colorSurface.data : colorPrimary.data;
        int endColor = enable ? colorPrimary.data : colorSurface.data;

        button.setEnabled(enable);
        int animDuration = 380;

        PathInterpolator m3Interpolator = new PathInterpolator(0.2f, 0f, 0f, 1f);

        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), startColor, endColor);
        colorAnimation.setDuration(animDuration);
        colorAnimation.setInterpolator(m3Interpolator);

        colorAnimation.addUpdateListener(animator -> {
            int animatedColor = (int) animator.getAnimatedValue();
            button.setBackgroundTintList(ColorStateList.valueOf(animatedColor));
        });

        float startScale = enable ? 0.95f : 1.0f;
        float endScale = enable ? 1.0f : 0.95f;

        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", startScale, endScale);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", startScale, endScale);

        ObjectAnimator transformAnim = ObjectAnimator.ofPropertyValuesHolder(button, scaleX, scaleY);
        transformAnim.setDuration(animDuration);
        transformAnim.setInterpolator(m3Interpolator);

        colorAnimation.start();
        transformAnim.start();
    }

    public static boolean hasStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return true;
    }

    public static void requestStoragePermission(Activity activity) {
        toast(activity, "Please allow storage access.");
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(intent);
        } catch (Exception e) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            activity.startActivity(intent);
        }
    }

    public static void hideKeyboard(Activity activity) {
        View view = activity.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    public static void toast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    public static String getUserCopyClipBoard(Context context) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        return clipboard.hasPrimaryClip() ? clipboard.getPrimaryClip().getItemAt(0).getText().toString() : "";
    }
}