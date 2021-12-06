/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;

public class ReactionsSpan extends View {

    private long uid;
    private String key;
    private static TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private static Paint backPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF rect = new RectF();
    private ImageReceiver imageReceiver;
    private StaticLayout nameLayout;
    private AvatarDrawable avatarDrawable;
//    private ContactsController.Contact currentContact;
    private int textWidth;
    private float textX;
    private float progress;
    private boolean choosing;
    private long lastUpdateTime;
    private int[] colors = new int[8];

    public ReactionsSpan(Context context, TLRPC.TL_reactionCount reactionCount/*, TLRPC.Document document*/) {
        super(context);

        textPaint.setTextSize(AndroidUtilities.dp(14));

        String firstName = String.valueOf(reactionCount.count); // TODO 03/12/2021 Fuji team, RIDER-:

        Object imageParent;

        avatarDrawable = new AvatarDrawable();
        avatarDrawable.setTextSize(AndroidUtilities.dp(12));
        avatarDrawable.setSmallSize(true);

//        avatarDrawable.setInfo(user);
//        firstName = UserObject.getFirstName(user);
//        ImageLocation imageLocation = ImageLocation.getForDocument(document);
//        imageParent = user;

        imageReceiver = new ImageReceiver();
        imageReceiver.setParentView(this);
        imageReceiver.setImageCoords(0, 0, AndroidUtilities.dp(32), AndroidUtilities.dp(32));

        int maxNameWidth;
        if (AndroidUtilities.isTablet()) {
            maxNameWidth = AndroidUtilities.dp(530 - 32 - 18 - 57 * 2) / 2;
        } else {
            maxNameWidth = (Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y) - AndroidUtilities.dp(32 + 18 + 57 * 2)) / 2;
        }

        CharSequence name = TextUtils.ellipsize(firstName.replace('\n', ' '), textPaint, maxNameWidth, TextUtils.TruncateAt.END);
        nameLayout = new StaticLayout(name, textPaint, 1000, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        if (nameLayout.getLineCount() > 0) {
            textWidth = (int) Math.ceil(nameLayout.getLineWidth(0));
            textX = -nameLayout.getLineLeft(0);
        }
//        imageReceiver.setImage(imageLocation, "50_50", avatarDrawable, 0, null, null, 1); // TODO 03/12/2021 Fuji team, RIDER-check:
        updateColors(); // TODO 03/12/2021 Fuji team, RIDER-: check uid in GroupCreateSpan
    }

    public void updateColors() { // TODO 03/12/2021 Fuji team, RIDER-: check logic
        int color = avatarDrawable.getColor();
        int back = Theme.getColor(Theme.key_groupcreate_spanBackground);
        int delete = Theme.getColor(Theme.key_groupcreate_spanDelete);
        colors[0] = Color.red(back);
        colors[1] = Color.red(color);
        colors[2] = Color.green(back);
        colors[3] = Color.green(color);
        colors[4] = Color.blue(back);
        colors[5] = Color.blue(color);
        colors[6] = Color.alpha(back);
        colors[7] = Color.alpha(color);
//        deleteDrawable.setColorFilter(new PorterDuffColorFilter(delete, PorterDuff.Mode.MULTIPLY));
        backPaint.setColor(back);
    }

    public boolean isChosen() {
        return choosing;
    }

    public void startDeleteAnimation() {
        if (choosing) {
            return;
        }
        choosing = true;
        lastUpdateTime = System.currentTimeMillis();
        invalidate();
    }

    public void cancelDeleteAnimation() {
        if (!choosing) {
            return;
        }
        choosing = false;
        lastUpdateTime = System.currentTimeMillis();
        invalidate();
    }

    public long getUid() {
        return uid;
    }

    public String getKey() {
        return key;
    }

//    public ContactsController.Contact getContact() {
//        return currentContact;
//    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(AndroidUtilities.dp(32 + 25) + textWidth, AndroidUtilities.dp(32));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (choosing && progress != 1.0f || !choosing && progress != 0.0f) {
            long newTime = System.currentTimeMillis();
            long dt = newTime - lastUpdateTime;
            if (dt < 0 || dt > 17) {
                dt = 17;
            }
            if (choosing) {
                progress += dt / 120.0f;
                if (progress >= 1.0f) {
                    progress = 1.0f;
                }
            } else {
                progress -= dt / 120.0f;
                if (progress < 0.0f) {
                    progress = 0.0f;
                }
            }
            invalidate();
        }
        canvas.save();
        rect.set(0, 0, getMeasuredWidth(), AndroidUtilities.dp(32));
        backPaint.setColor(Color.argb(colors[6] + (int) ((colors[7] - colors[6]) * progress), colors[0] + (int) ((colors[1] - colors[0]) * progress), colors[2] + (int) ((colors[3] - colors[2]) * progress), colors[4] + (int) ((colors[5] - colors[4]) * progress)));
        canvas.drawRoundRect(rect, AndroidUtilities.dp(16), AndroidUtilities.dp(16), backPaint);
        imageReceiver.draw(canvas);
        if (progress != 0) {
            int color = avatarDrawable.getColor();
            float alpha = Color.alpha(color) / 255.0f;
            backPaint.setColor(color);
            backPaint.setAlpha((int) (255 * progress * alpha));
            canvas.drawCircle(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), backPaint);
            canvas.save();
            canvas.rotate(45 * (1.0f - progress), AndroidUtilities.dp(16), AndroidUtilities.dp(16));
//            deleteDrawable.setBounds(AndroidUtilities.dp(11), AndroidUtilities.dp(11), AndroidUtilities.dp(21), AndroidUtilities.dp(21));
//            deleteDrawable.setAlpha((int) (255 * progress));
//            deleteDrawable.draw(canvas);
            canvas.restore();
        }
        canvas.translate(textX + AndroidUtilities.dp(32 + 9), AndroidUtilities.dp(8));
        int text = Theme.getColor(Theme.key_groupcreate_spanText);
        int textSelected = Theme.getColor(Theme.key_avatar_text);
        textPaint.setColor(ColorUtils.blendARGB(text, textSelected, progress));

        nameLayout.draw(canvas);
        canvas.restore();
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setText(nameLayout.getText());
        if (isChosen() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            info.addAction(new AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK.getId(), LocaleController.getString("Delete", R.string.Delete)));
    }
}
