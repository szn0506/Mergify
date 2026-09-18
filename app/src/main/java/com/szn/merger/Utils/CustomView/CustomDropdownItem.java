package com.szn.merger.Utils.CustomView;

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

    private ImageView arrow;
    private boolean expanded;
    private View popupView;

    public CustomDropdownItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.dropdown_item, this, true);

        View root = findViewById(R.id.root);
        arrow = findViewById(R.id.img_arrow);

        TextView title = findViewById(R.id.txt_title);
        TextView subtitle = findViewById(R.id.txt_subtitle);

        TypedArray a = context.obtainStyledAttributes(
                attrs,
                R.styleable.CustomDropdownItem
        );

        String titleText =
                a.getString(R.styleable.CustomDropdownItem_title);

        String subtitleText =
                a.getString(R.styleable.CustomDropdownItem_subtitle);

        title.setText(titleText);

        if (subtitleText == null || subtitleText.trim().isEmpty()) {
            subtitle.setVisibility(GONE);
        } else {
            subtitle.setText(subtitleText);
            subtitle.setVisibility(VISIBLE);
        }

        a.recycle();

        root.setOnClickListener(v -> {
            if (popupView == null) {
                return;
            }

            if (expanded) {
                return;
            }

            showPopupWindow();
        });
    }

    public void showPopupWindow(View popupView) {
        this.popupView = popupView;
    }

    private void showPopupWindow() {
        PopupWindow popupWindow = new PopupWindow(
                popupView,
                (int) (280 * getResources().getDisplayMetrics().density),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        popupWindow.setBackgroundDrawable(
                new ColorDrawable(Color.TRANSPARENT)
        );

        popupWindow.setOutsideTouchable(true);

        popupWindow.setElevation(
                10 * getResources().getDisplayMetrics().density
        );

        popupWindow.setOnDismissListener(() -> {
            expanded = false;

            arrow.animate()
                    .rotation(0f)
                    .setDuration(250)
                    .setInterpolator(new FastOutSlowInInterpolator())
                    .start();
        });

        expanded = true;

        arrow.animate()
                .rotation(180f)
                .setDuration(250)
                .setInterpolator(new FastOutSlowInInterpolator())
                .start();

        popupWindow.showAsDropDown(
                this,
                0,
                4,
                Gravity.END
        );
    }
}