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
    private ImageView icon;
    private ImageView arrow;
    private LinearLayout textContainer;
    private boolean expanded;
    private View popupView;
    private PopupWindow popupWindow;

    public CustomDropdownItem(Context context) {
        super(context);
        init(context, null);
    }

    public CustomDropdownItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CustomDropdownItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.dropdown_item, this, true);
        View root = findViewById(R.id.root);
        icon = findViewById(R.id.img_icon);
        arrow = findViewById(R.id.img_arrow);
        textContainer = findViewById(R.id.text_container);
        TextView title = findViewById(R.id.txt_title);
        TextView subtitle = findViewById(R.id.txt_subtitle);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomDropdownItem);
            String titleText = a.getString(R.styleable.CustomDropdownItem_title);
            String subtitleText = a.getString(R.styleable.CustomDropdownItem_subtitle);
            int iconResId = a.getResourceId(R.styleable.CustomDropdownItem_icon, 0);
            title.setText(titleText);
            if (subtitleText == null || subtitleText.trim().isEmpty()) {
                subtitle.setVisibility(GONE);
            } else {
                subtitle.setText(subtitleText);
                subtitle.setVisibility(VISIBLE);
            }
            updateIcon(iconResId);
            a.recycle();
        }

        root.setOnClickListener(v -> {
            if (popupView == null || expanded) return;
            showPopupWindow();
        });
    }

    private void updateIcon(int iconResId) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) textContainer.getLayoutParams();

        if (iconResId != 0) {
            icon.setImageResource(iconResId);
            icon.setVisibility(VISIBLE);
            params.setMarginStart(dpToPx(16));
        } else {
            icon.setVisibility(GONE);
            params.setMarginStart(0);
        }
        textContainer.setLayoutParams(params);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    public void showPopupWindow(View popupView) {
        this.popupView = popupView;
    }

    public void dismissPopupWindow() {
        if (popupWindow != null && popupWindow.isShowing()) popupWindow.dismiss();
    }

    private void showPopupWindow() {
        popupWindow = new PopupWindow(popupView, dpToPx(280), ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setElevation(dpToPx(10));
        popupWindow.setOnDismissListener(() -> {
            expanded = false;
            arrow.animate().rotation(0f).setDuration(250).setInterpolator(new FastOutSlowInInterpolator()).start();
        });
        expanded = true;
        arrow.animate().rotation(180f).setDuration(250).setInterpolator(new FastOutSlowInInterpolator()).start();
        popupWindow.showAsDropDown(this, 0, dpToPx(4), Gravity.END);
    }
}