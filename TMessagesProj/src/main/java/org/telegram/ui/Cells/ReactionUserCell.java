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
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;

public class ReactionUserCell extends FrameLayout {

    private BackupImageView avatarImageView;
    private BackupImageView imageView;
    private SimpleTextView nameTextView;

    private AvatarDrawable avatarDrawable;
    private TLRPC.User currentUser;

    private CharSequence currentName;
    private CharSequence currentStatus;
    private int currentId;
    private TLRPC.Document currentDocument;

    private boolean selfAsSavedMessages;

    private String lastName;
    private int lastStatus;
    private TLRPC.FileLocation lastAvatar;

    private int currentAccount = UserConfig.selectedAccount;

    private int statusColor;
    private int statusOnlineColor;

    private boolean needDivider;

    public ReactionUserCell(Context context, int padding, int checkbox, boolean admin) {
        super(context);

        avatarDrawable = new AvatarDrawable();

        avatarImageView = new BackupImageView(context);
        avatarImageView.setRoundRadius(AndroidUtilities.dp(24));
        addView(avatarImageView, LayoutHelper.createFrame(46, 46, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, LocaleController.isRTL ? 0 : 7 + padding, 6, LocaleController.isRTL ? 7 + padding : 0, 0));

        nameTextView = new SimpleTextView(context);
        nameTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        nameTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        nameTextView.setTextSize(16);
        nameTextView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP);
        addView(nameTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 20, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, LocaleController.isRTL ? 28 + (checkbox == 2 ? 18 : 0) : (64 + padding), 10, LocaleController.isRTL ? (64 + padding) : 28 + (checkbox == 2 ? 18 : 0), 0));

        imageView = new BackupImageView(context);
        avatarImageView.setRoundRadius(AndroidUtilities.dp(24));
        addView(imageView, LayoutHelper.createFrame(
                24,
                24,
                (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.CENTER_VERTICAL,
                LocaleController.isRTL ? 7 + padding : 0,
                0,
                LocaleController.isRTL ? 0 : 7 + padding,
                0)
        );
        setFocusable(true);
    }

    public void setAvatarPadding(int padding) {
        LayoutParams layoutParams = (LayoutParams) avatarImageView.getLayoutParams();
        layoutParams.leftMargin = AndroidUtilities.dp(LocaleController.isRTL ? 0 : 7 + padding);
        layoutParams.rightMargin = AndroidUtilities.dp(LocaleController.isRTL ? 7 + padding : 0);
        avatarImageView.setLayoutParams(layoutParams);

        layoutParams = (LayoutParams) nameTextView.getLayoutParams();

        layoutParams.leftMargin = AndroidUtilities.dp(LocaleController.isRTL ? 28 : (64 + padding));
        layoutParams.rightMargin = AndroidUtilities.dp(LocaleController.isRTL ? (64 + padding) : 28);
    }

    public CharSequence getName() {
        return nameTextView.getText();
    }

    public TLRPC.User getUser() {
        return currentUser;
    }

    public void setData(TLRPC.User user, TLRPC.Document document) {
        if (user == null) {
            currentName = null;
            currentUser = null;
            nameTextView.setText("");
            avatarImageView.setImageDrawable(null);
            currentDocument = document;
        } else {
            currentName = ContactsController.formatName(user.first_name, user.last_name);
            currentDocument = document;
            currentUser = user;
        }
        update(0);
    }

    public Object getCurrentObject() {
        return currentUser;
    }

    public void setNameTypeface(Typeface typeface) {
        nameTextView.setTypeface(typeface);
    }

    public void setCurrentId(int id) {
        currentId = id;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(58) + (needDivider ? 1 : 0), MeasureSpec.EXACTLY));
    }

    public void setStatusColors(int color, int onlineColor) {
        statusColor = color;
        statusOnlineColor = onlineColor;
    }

    public void update(int mask) {
        TLRPC.FileLocation photo = null;
        String newName = null;
        TLRPC.User currentUser = null;
        TLRPC.Chat currentChat = null;
        if (this.currentUser instanceof TLRPC.User) {
            currentUser = (TLRPC.User) this.currentUser;
            if (currentUser.photo != null) {
                photo = currentUser.photo.photo_small;
            }
        }

        if (mask != 0) {
            boolean continueUpdate = false;
            if ((mask & MessagesController.UPDATE_MASK_AVATAR) != 0) {
                if (lastAvatar != null && photo == null || lastAvatar == null && photo != null || lastAvatar != null && (lastAvatar.volume_id != photo.volume_id || lastAvatar.local_id != photo.local_id)) {
                    continueUpdate = true;
                }
            }
            if (currentUser != null && !continueUpdate && (mask & MessagesController.UPDATE_MASK_STATUS) != 0) {
                int newStatus = 0;
                if (currentUser.status != null) {
                    newStatus = currentUser.status.expires;
                }
                if (newStatus != lastStatus) {
                    continueUpdate = true;
                }
            }
            if (!continueUpdate && currentName == null && lastName != null && (mask & MessagesController.UPDATE_MASK_NAME) != 0) {
                if (currentUser != null) {
                    newName = UserObject.getUserName(currentUser);
                } else {
                    newName = currentChat.title;
                }
                if (!newName.equals(lastName)) {
                    continueUpdate = true;
                }
            }
            if (!continueUpdate) {
                return;
            }
        }

        ((LayoutParams) nameTextView.getLayoutParams()).topMargin = AndroidUtilities.dp(10);
        if (currentUser != null) {
            if (selfAsSavedMessages && UserObject.isUserSelf(currentUser)) {
                nameTextView.setText(LocaleController.getString("SavedMessages", R.string.SavedMessages), true);
                avatarDrawable.setAvatarType(AvatarDrawable.AVATAR_TYPE_SAVED);
                avatarImageView.setImage(null, "50_50", avatarDrawable, currentUser);
                ((LayoutParams) nameTextView.getLayoutParams()).topMargin = AndroidUtilities.dp(19);
                return;
            }
            avatarDrawable.setInfo(currentUser);
            if (currentUser.status != null) {
                lastStatus = currentUser.status.expires;
            } else {
                lastStatus = 0;
            }
        } else if (currentChat != null) {
            avatarDrawable.setInfo(currentChat);
        } else if (currentName != null) {
            avatarDrawable.setInfo(currentId, currentName.toString(), null);
        } else {
            avatarDrawable.setInfo(currentId, "#", null);
        }

        if (currentName != null) {
            lastName = null;
            nameTextView.setText(currentName);
        } else {
            if (currentUser != null) {
                lastName = newName == null ? UserObject.getUserName(currentUser) : newName;
            } else if (currentChat != null) {
                lastName = newName == null ? currentChat.title : newName;
            } else {
                lastName = "";
            }
            nameTextView.setText(lastName);
        }

//        if (imageView.getVisibility() == VISIBLE && currentDocument == null || imageView.getVisibility() == GONE && currentDocument != null) {
        imageView.setVisibility(currentDocument == null ? GONE : VISIBLE);
        ImageLocation imageLocation = ImageLocation.getForDocument(currentDocument);
        imageView.setImage(imageLocation, "66_66", null, null, currentUser);
//        }

        lastAvatar = photo;
        if (currentUser != null) {
            avatarImageView.setForUserOrChat(currentUser, avatarDrawable);
        } else if (currentChat != null) {
            avatarImageView.setForUserOrChat(currentChat, avatarDrawable);
        } else {
            avatarImageView.setImageDrawable(avatarDrawable);
        }

        nameTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
//            adminTextView.setTextColor(Theme.getColor(Theme.key_profile_creatorIcon));
    }

    public void setSelfAsSavedMessages(boolean value) {
        selfAsSavedMessages = value;
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
//        if (needDivider) {
//            canvas.drawLine(LocaleController.isRTL ? 0 : AndroidUtilities.dp(68), getMeasuredHeight() - 1, getMeasuredWidth() - (LocaleController.isRTL ? AndroidUtilities.dp(68) : 0), getMeasuredHeight() - 1, Theme.dividerPaint);
//        }
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
    }
}
