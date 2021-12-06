/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Cells;

import android.content.Context;
import android.graphics.Canvas;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.Switch;

public class ReactionCheckCell extends FrameLayout {

    private TextView textView;
    //    private TextView valueTextView;
    @SuppressWarnings("FieldCanBeLocal")
    private ReactionCell reactionCell;
    private Switch checkBox;
    private boolean needDivider;
    private boolean drawLine = true;
    private boolean isMultiline;
    private int currentHeight;

    public ReactionCheckCell(Context context) {
        this(context, 21, 70);
    }

    public ReactionCheckCell(Context context, int padding, int height) {
        super(context);
        setWillNotDraw(false);
        currentHeight = height;

        reactionCell = new ReactionCell(context);
        addView(reactionCell, LayoutHelper.createFrame(36, 36, Gravity.CENTER_VERTICAL, 20, 0, 0, 0));

        textView = new TextView(context);
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        textView.setLines(1);
        textView.setMaxLines(1);
        textView.setSingleLine(true);
        textView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,
                LayoutHelper.MATCH_PARENT,
                (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP,
                LocaleController.isRTL ? 64 : (21 + 56), // TODO 29/11/2021 Fuji team, RIDER-: check rtl
                0,
                LocaleController.isRTL ? 21 : 64,
                0)
        );

        checkBox = new Switch(context);
        checkBox.setColors(Theme.key_switchTrack, Theme.key_switchTrackChecked, Theme.key_windowBackgroundWhite, Theme.key_windowBackgroundWhite);
        addView(checkBox, LayoutHelper.createFrame(37, 40, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.CENTER_VERTICAL, 21, 0, 21, 0));
        checkBox.setFocusable(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (isMultiline) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        } else {
            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(currentHeight), MeasureSpec.EXACTLY));
        }
    }

    public void setTextAndValueAndCheck(String text, boolean checked, boolean divider) {
        setTextAndValueAndCheck(text, checked, 0, false, divider);
    }

    public void setTextAndValueAndCheck(String text, boolean checked, int iconType, boolean divider) {
        setTextAndValueAndCheck(text, checked, iconType, false, divider);
    }

    public void setTextAndValueAndCheck(String text, boolean checked, int iconType, boolean multiline, boolean divider) {
        textView.setText(text);
        checkBox.setChecked(checked, iconType, false);
        needDivider = divider;
        isMultiline = multiline;
        checkBox.setContentDescription(text);
    }

    public void setChecked(boolean checked, int iconType) {
        checkBox.setChecked(checked, iconType, true);
    }

    public boolean isChecked() {
        return checkBox.isChecked();
    }

    public void setChecked(boolean checked) {
        checkBox.setChecked(checked, true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (needDivider) {
            canvas.drawLine(
                    LocaleController.isRTL ? 0 : AndroidUtilities.dp(20),
                    getMeasuredHeight() - 1,
                    getMeasuredWidth() - (LocaleController.isRTL ? AndroidUtilities.dp(20) : 0),
                    getMeasuredHeight() - 1,
                    Theme.dividerPaint
            );
        }
    }

    public void pause() {
        reactionCell.pause();
    }

    public void resume() {
        reactionCell.resume();
    }

    public void setSticker(TLRPC.Document sticker, Object o, String reactionSymbol) {
        reactionCell.setReactionSymbol(reactionSymbol);
        reactionCell.setSticker(sticker, o);
    }

    public String getReactionSymbol() {
        return reactionCell.getReactionSymbol();
    }
}
