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

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.szn.merger.R;

public class CustomDropdownItem extends LinearLayout {

    private ImageView icon;
    private ImageView arrow;
    private LinearLayout textContainer;

    private boolean expanded;
    private PopupWindow popupWindow;

    private String selectedOption;
    private String defaultOption;
    private View lastChecked;

    private String[] options;

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
            if (options == null || options.length == 0 || expanded) return;
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

    public void setOptions(String... options) {
        this.options = options;

        if (options == null || options.length == 0) {
            selectedOption = null;
            defaultOption = null;
            lastChecked = null;
            return;
        }

        defaultOption = options[0];

        if (selectedOption == null || !containsOption(selectedOption))
            selectedOption = defaultOption;

        ((TextView) findViewById(R.id.txt_title)).setText(selectedOption);
    }

    public void setDefaultOption(String option) {
        if (!containsOption(option)) return;

        defaultOption = option;
        selectedOption = option;
        ((TextView) findViewById(R.id.txt_title)).setText(option);
    }

    public String getDefaultOption() {
        return defaultOption;
    }

    public String getSelectedOption() {
        return selectedOption;
    }

    public void setSelectedOption(String option) {
        if (!containsOption(option)) return;

        selectedOption = option;
        ((TextView) findViewById(R.id.txt_title)).setText(option);
    }

    private boolean containsOption(String option) {
        if (options == null || option == null) return false;

        for (String value : options) {
            if (value.equals(option)) return true;
        }

        return false;
    }

    private void showPopupWindow() {
        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, dpToPx(8), 0, dpToPx(8));

        MaterialCardView card = new MaterialCardView(getContext());
        card.setLayoutParams(new ViewGroup.LayoutParams(dpToPx(280), ViewGroup.LayoutParams.WRAP_CONTENT));
        card.setRadius(dpToPx(16));
        card.setCardElevation(dpToPx(10));
        card.setPreventCornerOverlap(true);
        card.setClipToOutline(true);
        card.setStrokeWidth(0);
        card.setCardBackgroundColor(MaterialColors.getColor(card, com.google.android.material.R.attr.colorSurfaceVariant));
        card.addView(container);

        lastChecked = null;

        for (String option : options) {
            View row = LayoutInflater.from(getContext()).inflate(R.layout.item_dropdown_option, container, false);
            TextView optionText = row.findViewById(R.id.optionText);
            ImageView check = row.findViewById(R.id.check);

            optionText.setText(option);

            if (option.equals(selectedOption)) {
                check.setVisibility(VISIBLE);
                lastChecked = row;
            } else {
                check.setVisibility(GONE);
            }

            row.setOnClickListener(v -> {
                if (lastChecked == v) {
                    dismissPopupWindow();
                    return;
                }

                if (lastChecked != null) lastChecked.findViewById(R.id.check).setVisibility(GONE);

                v.findViewById(R.id.check).setVisibility(VISIBLE);
                lastChecked = v;
                selectedOption = option;
                ((TextView) findViewById(R.id.txt_title)).setText(option);

                dismissPopupWindow();
            });

            container.addView(row);
        }

        popupWindow = new PopupWindow(card, dpToPx(280), ViewGroup.LayoutParams.WRAP_CONTENT, true);
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

    public void dismissPopupWindow() {
        if (popupWindow != null && popupWindow.isShowing()) popupWindow.dismiss();
    }

    public View getLastChecked() {
        return lastChecked;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}