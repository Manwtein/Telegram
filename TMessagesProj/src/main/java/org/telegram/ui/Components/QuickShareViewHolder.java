package org.telegram.ui.Components;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import static org.telegram.messenger.AndroidUtilities.dp;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;

@SuppressLint("ViewConstructor")
public class QuickShareViewHolder extends FrameLayout {

    private final int currentType;
    private final int contactSizeDp;
    private final int imageBottomMargin;
    private final int textViewHeight;
    private final int textViewBottomMargin;
    private final int maxWidth;
    private final ValueAnimator textViewAnimator = ValueAnimator.ofFloat(0f, 1f).setDuration(200);
    private final ValueAnimator imageAlphaAnimator = ValueAnimator.ofFloat(0.5f, 1f).setDuration(150);
    private final ValueAnimator imageSizeAnimator = ValueAnimator.ofFloat(1f, 1.1f).setDuration(150);
    public TLRPC.Dialog did;
    public boolean clicked;
    public BackupImageView imageView;
    public final AvatarDrawable avatarDrawable = new AvatarDrawable() {
        @Override
        public void invalidateSelf() {
            super.invalidateSelf();
            imageView.invalidate();
        }
    };
    public float radius = dp(72);
    public TextViewHolder textViewGroup;
    private Boolean imageSizeAnimationStarted = false;
    private Boolean imageAlphaAnimationStarted = true;
    private Boolean textAnimationStarted = false;

    public QuickShareViewHolder(QuickShareContainerLayout parentLayout,
                                int contactSizeDp,
                                int imageBottomMargin,
                                int textViewHeight,
                                int textViewBottomMargin,
                                int maxWidth,
                                Theme.ResourcesProvider themeDelegate) {
        super(parentLayout.getContext());
        this.currentType = 0;
        this.contactSizeDp = contactSizeDp;
        this.imageBottomMargin = imageBottomMargin;
        this.textViewHeight = textViewHeight;
        this.textViewBottomMargin = textViewBottomMargin;
        this.maxWidth = maxWidth;
        init(parentLayout.getContext(), themeDelegate);
    }

    public void init(Context context, Theme.ResourcesProvider themeDelegate) {
        imageView = new BackupImageView(context);
        imageView.setRoundRadius(dp(28));
        ViewGroup.LayoutParams layoutParams = LayoutHelper.createFrame(contactSizeDp, contactSizeDp, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0, 0, imageBottomMargin);
        addView(imageView, layoutParams);
        textViewGroup = new TextViewHolder(context, textViewBottomMargin, themeDelegate);
        ViewGroup.LayoutParams viewLayoutParams = new LayoutParams(maxWidth, textViewHeight, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        addView(textViewGroup, viewLayoutParams);
        textViewGroup.setAlpha(0f);
        textViewGroup.setScaleX(0f);
        textViewGroup.setScaleY(0f);
        imageView.setAlpha(1f);
        textViewAnimator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();
            textViewGroup.setAlpha(animatedValue);
            textViewGroup.setScaleX(animatedValue);
            textViewGroup.setScaleY(animatedValue);
            textViewGroup.invalidate();
        });
        imageAlphaAnimator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();
            imageView.setAlpha(animatedValue);
        });
        imageSizeAnimator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();
            imageView.setScaleX(animatedValue);
            imageView.setScaleY(animatedValue);
        });
        setClipToPadding(false);
        setClipChildren(false);
    }

    public void setChosen(Boolean chosen) {
        runTextAnimation(chosen);
        runImageSizeAnimation(chosen);
        runImageAlphaAnimation(chosen);
    }

    public void setAlphaImageStateChosen() {
        runTextAnimation(false);
        runImageSizeAnimation(false);
        runImageAlphaAnimation(true);
    }

    public void runTextAnimation(Boolean start) {
        if (textAnimationStarted) {
            if (!start) {
                textViewAnimator.reverse();
            }
        } else if (start) {
            if (textViewAnimator.isRunning()) {
                textViewAnimator.reverse();
            } else {
                textViewAnimator.start();
            }
        }
        textAnimationStarted = start;
    }

    public void runImageAlphaAnimation(Boolean start) {
        if (imageAlphaAnimationStarted) {
            if (!start) {
                imageAlphaAnimator.reverse();
            }
        } else if (start) {
            if (imageAlphaAnimator.isRunning()) {
                imageAlphaAnimator.reverse();
            } else {
                imageAlphaAnimator.start();
            }
        }
        imageAlphaAnimationStarted = start;
    }

    public void runImageSizeAnimation(Boolean start) {
        if (imageSizeAnimationStarted) {
            if (!start) {
                imageSizeAnimator.reverse();
            }
        } else if (start) {
            if (imageSizeAnimator.isRunning()) {
                imageSizeAnimator.reverse();
            } else {
                imageSizeAnimator.start();
            }
        }
        imageSizeAnimationStarted = start;
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        super.dispatchDraw(canvas);
    }

    public void setName(String name) {
        textViewGroup.setText(name);
    }

    public int getCurrentType() {
        return currentType;
    }

    public int getTextViewRealWidth() {
        int width = (int) Math.ceil(textViewGroup.textPaint.measureText(textViewGroup.text));
        return width + textViewGroup.padding * 2;
    }

    @SuppressLint("AppCompatCustomView")
    public class TextViewHolder extends View {

        private final Theme.ResourcesProvider themeDelegate;
        private final int textViewBottomMargin;
        public int padding = dp(8);
        RectF localRect = new RectF();
        TextPaint textPaint = (TextPaint) getThemedPaint(Theme.key_paint_chatActionText);
        private String text = "";
        private StaticLayout textLayout;

        public TextViewHolder(@NonNull Context context,
                              int textViewBottomMargin,
                              Theme.ResourcesProvider themeDelegate) {
            super(context);
            this.themeDelegate = themeDelegate;
            this.textViewBottomMargin = textViewBottomMargin;
        }

        public void setText(String text) {
            if (!text.equals(this.text)) {
                int width = (int) Math.ceil(textPaint.measureText(text));
                textLayout = new StaticLayout(text, textPaint, width, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                textPaint.linkColor = textPaint.getColor();
            }
            this.text = text;
        }

        @Override
        protected void onDraw(@NonNull Canvas canvas) {
            Paint backgroundPaint = getThemedPaint(Theme.key_paint_chatActionBackground);
            int oldAlpha = backgroundPaint.getAlpha();
            int width = (int) Math.ceil(textPaint.measureText(text));
            int measuredWidth = getMeasuredWidth();
            float halfOfWidth = (float) measuredWidth / 2;
            float halfOfTextWidth = (float) width / 2;
            localRect.set(halfOfWidth - halfOfTextWidth - padding, 0f, halfOfWidth + halfOfTextWidth + padding, getMeasuredHeight() - textViewBottomMargin);
            float lineHeight = textPaint.descent() - textPaint.ascent();
            backgroundPaint.setAlpha((int) (getAlpha() * 0.8f * 255));
            int count = canvas.save();
            canvas.drawRoundRect(localRect, radius, radius, backgroundPaint);
            canvas.translate(localRect.left + padding, (localRect.height() - lineHeight) / 2);
            textLayout.draw(canvas);
            if (oldAlpha >= 0) {
                backgroundPaint.setAlpha(oldAlpha);
            }
            canvas.restoreToCount(count);
            super.onDraw(canvas);
        }

        protected Paint getThemedPaint(String paintKey) {
            Paint paint = themeDelegate != null ? themeDelegate.getPaint(paintKey) : null;
            return paint != null ? paint : Theme.getThemePaint(paintKey);
        }
    }
}
