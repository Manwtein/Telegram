/*
 * This is the source code of Telegram for Android v. 2.0.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Cells;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DocumentObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.SvgHelper;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieDrawable;

public class ReactionCell extends FrameLayout {

    private static AccelerateInterpolator interpolator = new AccelerateInterpolator(0.5f);
    private BackupImageView imageView;
    private TLRPC.Document sticker;
    private Object parentObject;
    private long lastUpdateTime;
    private boolean scaled;
    private float scale;
    private long time = 0;
    private boolean clearsInputField;
    private int width;
    private int height;
    private String reactionSymbol;

    public ReactionCell(Context context) {
        super(context);

        imageView = new BackupImageView(context);
        imageView.setAspectFit(true);
        imageView.setLayerNum(1);
        addView(imageView);
//        addView(imageView, LayoutHelper.createFrame(24, 24, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 0));
//        setFocusable(true);
    }

    public ReactionCell(Context context, int width, int height) {
        super(context);

        imageView = new BackupImageView(context);
        imageView.setAspectFit(true);
        imageView.setLayerNum(1);
//        imageView.setBackgroundColor(Color.MAGENTA);

//        setBackgroundColor(Color.RED);
//        addView(imageView);
        this.width = width;
        this.height = height;
        addView(imageView, LayoutHelper.createFrame(width - 8, height - 8, Gravity.CENTER, 0, 0, 0, 0));
//        setBackgroundColor(Color.BLUE);

//        setFocusable(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (width != 0 && height != 0) {
            super.onMeasure(
                    MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(width) /*+ getPaddingLeft() + getPaddingRight()*/, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(height), MeasureSpec.EXACTLY)
            );
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

//        super.onMeasure(MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(24) /*+ getPaddingLeft() + getPaddingRight()*/, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(24), MeasureSpec.EXACTLY));
    }

    @Override
    public void setPressed(boolean pressed) {
        if (imageView.getImageReceiver().getPressed() != pressed) {
            imageView.getImageReceiver().setPressed(pressed ? 1 : 0);
            imageView.invalidate();
        }
        super.setPressed(pressed);
    }

    public void pause() {
        imageView.getImageReceiver().setAllowStartLottieAnimation(false);
        if (imageView.getImageReceiver().getLottieAnimation() != null) {
            imageView.getImageReceiver().getLottieAnimation().stop();
        }
    }

    public void resume() {
        imageView.getImageReceiver().setAllowStartLottieAnimation(true);
        final RLottieDrawable lottieAnimation = imageView.getImageReceiver().getLottieAnimation();
        if (lottieAnimation != null) {
            lottieAnimation.start();
            lottieAnimation.setOnFinishCallback(this::pause, lottieAnimation.getFramesCount() - 2);
        }
    }

    public boolean isClearsInputField() {
        return clearsInputField;
    }

    public void setClearsInputField(boolean value) {
        clearsInputField = value;
    }

    public void setSticker(TLRPC.Document document, Object parent) {
        pause();
        parentObject = parent;
        if (document != null) {
            TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 90);
            SvgHelper.SvgDrawable svgThumb = DocumentObject.getSvgThumb(document, Theme.key_windowBackgroundGray, 1.0f);
            if (MessageObject.canAutoplayAnimatedSticker(document)) {
                if (svgThumb != null) {
                    imageView.setImage(ImageLocation.getForDocument(document), "80_80", null, svgThumb, parentObject);
                } else if (thumb != null) {
                    imageView.setImage(ImageLocation.getForDocument(document), "80_80", ImageLocation.getForDocument(thumb, document), null, 0, parentObject);
                } else {
                    imageView.setImage(ImageLocation.getForDocument(document), "80_80", null, null, parentObject);
                }
            } else {
                if (svgThumb != null) {
                    if (thumb != null) {
                        imageView.setImage(ImageLocation.getForDocument(thumb, document), null, "webp", svgThumb, parentObject);
                    } else {
                        imageView.setImage(ImageLocation.getForDocument(document), null, "webp", svgThumb, parentObject);
                    }
                } else {
                    imageView.setImage(ImageLocation.getForDocument(thumb, document), null, "webp", null, parentObject);
                }
            }
        }
        sticker = document;
        Drawable background = getBackground();
        if (background != null) {
            background.setAlpha(230);
            background.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chat_stickersHintPanel), PorterDuff.Mode.MULTIPLY));
        }
    }

    public TLRPC.Document getSticker() {
        return sticker;
    }

    public Object getParentObject() {
        return parentObject;
    }

    public void setScaled(boolean value) {
        scaled = value;
        lastUpdateTime = System.currentTimeMillis();
        invalidate();
    }

    public boolean showingBitmap() {
        return imageView.getImageReceiver().getBitmap() != null;
    }

    public MessageObject.SendAnimationData getSendAnimationData() {
        ImageReceiver imageReceiver = imageView.getImageReceiver();
        if (!imageReceiver.hasNotThumb()) {
            return null;
        }
        MessageObject.SendAnimationData data = new MessageObject.SendAnimationData();
        int[] position = new int[2];
        imageView.getLocationInWindow(position);
        data.x = imageReceiver.getCenterX() + position[0];
        data.y = imageReceiver.getCenterY() + position[1];
        data.width = imageReceiver.getImageWidth();
        data.height = imageReceiver.getImageHeight();
        return data;
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        boolean result = super.drawChild(canvas, child, drawingTime);
        if (child == imageView && (scaled && scale != 0.8f || !scaled && scale != 1.0f)) {
            long newTime = System.currentTimeMillis();
            long dt = (newTime - lastUpdateTime);
            lastUpdateTime = newTime;
            if (scaled && scale != 0.8f) {
                scale -= dt / 400.0f;
                if (scale < 0.8f) {
                    scale = 0.8f;
                }
            } else {
                scale += dt / 400.0f;
                if (scale > 1.0f) {
                    scale = 1.0f;
                }
            }
            imageView.setScaleX(scale);
            imageView.setScaleY(scale);
            imageView.invalidate();
            invalidate();
        }
        return result;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        if (sticker == null)
            return;
        String emoji = null;
        for (int a = 0; a < sticker.attributes.size(); a++) {
            TLRPC.DocumentAttribute attribute = sticker.attributes.get(a);
            if (attribute instanceof TLRPC.TL_documentAttributeSticker) {
                emoji = attribute.alt != null && attribute.alt.length() > 0 ? attribute.alt : null;
            }
        }
        if (emoji != null)
            info.setText(emoji + " " + LocaleController.getString("AttachSticker", R.string.AttachSticker));
        else
            info.setText(LocaleController.getString("AttachSticker", R.string.AttachSticker));
        info.setEnabled(true);
    }

    public void setReactionSymbol(String reactionSymbol) {
        this.reactionSymbol = reactionSymbol;
    }

    public String getReactionSymbol() {
        return reactionSymbol;
    }
}
