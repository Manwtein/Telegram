package org.telegram.ui.Stories.recorder;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import java.util.ArrayList;
import org.telegram.messenger.AndroidUtilities;
import static org.telegram.messenger.AndroidUtilities.dp;
import org.telegram.messenger.LocaleController;
import static org.telegram.messenger.LocaleController.getString;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ScaleStateListAnimator;

public class MediaRecordPreviewButtons extends FrameLayout {

    public static final int BUTTON_PAINT = 0;
    public static final int BUTTON_TEXT = 1;
    public static final int BUTTON_STICKER = 2;
    public static final int BUTTON_ADJUST = 3;
    public static final int BUTTON_SHARE = 4;
    public ImageView sendButton;
    private View shadowView;
    private ArrayList<ButtonView> buttons = new ArrayList<>();
    private Theme.ResourcesProvider resourcesProvider;

    private Utilities.Callback<Integer> onClickListener;
    private boolean isShareEnabled = true;
    private float appearT;
    private boolean appearing;
    private ValueAnimator appearAnimator;

    public MediaRecordPreviewButtons(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);

        this.resourcesProvider = resourcesProvider;
        shadowView = new View(context);
        shadowView.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{0x66000000, 0x00000000}));
        addView(shadowView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL));

        addButton(BUTTON_PAINT, R.drawable.media_draw, LocaleController.getString(R.string.AccDescrPaint));
        addButton(BUTTON_STICKER, R.drawable.msg_photo_sticker, LocaleController.getString(R.string.AccDescrStickers));
        addButton(BUTTON_TEXT, R.drawable.msg_photo_text2, LocaleController.getString(R.string.AccDescrPlaceText));
        addButton(BUTTON_ADJUST, R.drawable.msg_photo_settings, LocaleController.getString(R.string.AccDescrPhotoAdjust));

        sendButton = new ImageView(getContext());
        sendButton.setScaleType(ImageView.ScaleType.CENTER);
        sendButton.setBackground(Theme.createSimpleSelectorCircleDrawable(dp(48), getThemedColor(Theme.key_chat_editMediaButton), getThemedColor(Build.VERSION.SDK_INT >= 21 ? Theme.key_dialogFloatingButtonPressed : Theme.key_chat_editMediaButton)));
        sendButton.setImageResource(R.drawable.msg_input_send_mini);
        sendButton.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_dialogFloatingIcon), PorterDuff.Mode.MULTIPLY));
        addView(sendButton, LayoutHelper.createFrame(48, 48, Gravity.RIGHT | Gravity.BOTTOM, 0, 0, 14, 2.33f));
        sendButton.setContentDescription(getString("Send", R.string.Send));
        ScaleStateListAnimator.apply(sendButton);
        updateAppearT();
    }

    private int getThemedColor(int key) {
        if (resourcesProvider != null) {
            return resourcesProvider.getColor(key);
        }
        return Theme.getColor(key);
    }

    private boolean isFiltersVisible() {
        for (int i = 0; i < buttons.size(); ++i) {
            ButtonView button = buttons.get(i);
            if (button.id == BUTTON_ADJUST) {
                return button.getVisibility() == View.VISIBLE;
            }
        }
        return false;
    }

    public void setFiltersVisible(boolean visible) {
        for (int i = 0; i < buttons.size(); ++i) {
            ButtonView button = buttons.get(i);
            if (button.id == BUTTON_ADJUST) {
                button.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
        }
    }

    public void setShareText(String text, boolean arrow) {
    }

    private void addButton(int id, int resId, CharSequence contentDescription) {
        ButtonView btn = new ButtonView(getContext(), id, resId);
        btn.setContentDescription(contentDescription);
        buttons.add(btn);
        addView(btn);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int w = right - left;
        final int h = bottom - top;

        shadowView.layout(0, 0, w, h);
        sendButton.layout(w - sendButton.getMeasuredWidth(), (h - sendButton.getMeasuredHeight()) / 2, w, (h + sendButton.getMeasuredHeight()) / 2);

        int W = w - dp(10 + 10 + 12.33f) - sendButton.getMeasuredWidth();
        int visibleButtons = 0;
        for (int i = 0; i < buttons.size(); ++i) {
            ButtonView button = buttons.get(i);
            if (button.getVisibility() == View.VISIBLE) {
                visibleButtons++;
            }
        }
        int maxPossibleMargin = visibleButtons < 2 ? 0 : (W - visibleButtons * dp(40)) / (visibleButtons - 1);
        int margin = Math.min(dp(isFiltersVisible() ? 20 : 30), maxPossibleMargin);

        int t = (h - dp(40)) / 2, b = (h + dp(40)) / 2;
        for (int i = 0, x = dp(12.33f) + (!isFiltersVisible() ? (W - visibleButtons * dp(40) - (visibleButtons - 1) * margin) / 2 : 0); i < buttons.size(); ++i) {
            if (buttons.get(i).getVisibility() != View.VISIBLE) continue;
            buttons.get(i).layout(x, t, x + dp(40), b);
            x += dp(40) + margin;
        }
    }

    public void setOnClickListener(Utilities.Callback<Integer> onClickListener) {
        this.onClickListener = onClickListener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(dp(52), MeasureSpec.EXACTLY)
        );
    }

    public boolean isShareEnabled() {
        return isShareEnabled;
    }

    public void setShareEnabled(boolean enabled) {
        if (isShareEnabled != enabled) {
            isShareEnabled = enabled;
//            shareButton.enabled = enabled;
//            shareButton.invalidate();
        }
    }

    public void appear(boolean appear, boolean animated) {
        if (appearing == appear) {
            return;
        }
        if (appearAnimator != null) {
            appearAnimator.cancel();
        }
        appearing = appear;
        if (animated) {
            appearAnimator = ValueAnimator.ofFloat(appearT, appear ? 1 : 0);
            appearAnimator.addUpdateListener(anm -> {
                appearT = (float) anm.getAnimatedValue();
                updateAppearT();
            });
            if (appearing) {
                appearAnimator.setDuration(450);
                appearAnimator.setInterpolator(new LinearInterpolator());
            } else {
                appearAnimator.setDuration(350);
                appearAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
            }
            appearAnimator.start();
        } else {
            appearT = appear ? 1 : 0;
            updateAppearT();
        }
    }

    private void updateAppearT() {
        shadowView.setAlpha(appearT);
        shadowView.setTranslationY((1f - appearT) * dp(16));
        for (int i = 1; i < getChildCount(); ++i) {
            View child = getChildAt(i);
            float t = appearT;
            if (appearing) {
                t = AndroidUtilities.cascade(appearT, i - 1, getChildCount() - 1, 3);
                t = CubicBezierInterpolator.EASE_OUT_QUINT.getInterpolation(t);
            }
            child.setAlpha(t);
            child.setTranslationY((1f - t) * dp(24));
        }
    }

    private class ButtonView extends ImageView {
        public final int id;

        public ButtonView(Context context, int id, int resId) {
            super(context);
            this.id = id;

            setBackground(Theme.createSelectorDrawable(Theme.ACTION_BAR_WHITE_SELECTOR_COLOR));
            setScaleType(ScaleType.CENTER);
            setImageResource(resId);
            setColorFilter(new PorterDuffColorFilter(0xffffffff, PorterDuff.Mode.MULTIPLY));

            setOnClickListener(e -> {
                if (appearing && onClickListener != null) {
                    onClickListener.run(id);
                }
            });
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(dp(40), dp(40));
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            info.setClassName("android.widget.Button");
        }
    }
}
