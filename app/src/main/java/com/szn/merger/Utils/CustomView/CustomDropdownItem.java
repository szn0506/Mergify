package com.szn.merger.Utils.CustomView;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.szn.merger.R;

public class CustomDropdownItem extends LinearLayout {

    private LinearLayout dropdown;
    private ImageView arrow;
    private boolean expanded;

    public CustomDropdownItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.dropdown_item, this, true);

        View root = findViewById(R.id.root);
        dropdown = findViewById(R.id.dropdown);
        arrow = findViewById(R.id.img_arrow);

        TextView title = findViewById(R.id.txt_title);
        TextView subtitle = findViewById(R.id.txt_subtitle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomDropdownItem);

        String titleText = a.getString(R.styleable.CustomDropdownItem_title);
        String subtitleText = a.getString(R.styleable.CustomDropdownItem_subtitle);

        title.setText(titleText);

        if (subtitleText == null || subtitleText.trim().isEmpty()) {
            subtitle.setVisibility(GONE);
        } else {
            subtitle.setText(subtitleText);
            subtitle.setVisibility(VISIBLE);
        }

        a.recycle();

        dropdown.setVisibility(GONE);

        root.setOnClickListener(v -> {
            if (expanded) {
                collapse();
            } else {
                expand();
            }
        });
    }

    private void expand() {
        expanded = true;
        dropdown.setVisibility(VISIBLE);

        dropdown.post(() -> {
            int height = dropdown.getMeasuredHeight();

            dropdown.getLayoutParams().height = 0;
            dropdown.requestLayout();

            ValueAnimator animator = ValueAnimator.ofInt(0, height);
            animator.setDuration(250);
            animator.setInterpolator(new FastOutSlowInInterpolator());

            animator.addUpdateListener(a -> {
                dropdown.getLayoutParams().height = (int) a.getAnimatedValue();
                dropdown.requestLayout();
            });

            animator.start();
        });

        arrow.animate()
                .rotation(180f)
                .setDuration(250)
                .setInterpolator(new FastOutSlowInInterpolator())
                .start();
    }

    private void collapse() {
        expanded = false;

        ValueAnimator animator = ValueAnimator.ofInt(dropdown.getHeight(), 0);
        animator.setDuration(250);
        animator.setInterpolator(new FastOutSlowInInterpolator());

        animator.addUpdateListener(a -> {
            dropdown.getLayoutParams().height = (int) a.getAnimatedValue();
            dropdown.requestLayout();
        });

        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                dropdown.setVisibility(GONE);
                dropdown.getLayoutParams().height = LayoutParams.WRAP_CONTENT;
            }
        });

        animator.start();

        arrow.animate()
                .rotation(0f)
                .setDuration(250)
                .setInterpolator(new FastOutSlowInInterpolator())
                .start();
    }

    public void showPopupWindow(View popupView) {
        PopupWindow popupWindow = new PopupWindow(
                popupView,
                (int) (280 * getResources().getDisplayMetrics().density),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setElevation(10 * getResources().getDisplayMetrics().density);
        popupWindow.showAsDropDown(this, 0, 4, Gravity.END);
    }
}