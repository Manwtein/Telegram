package org.telegram.ui;

import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import java.util.ArrayList;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.INavigationLayout;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.QuickShare.QuickShareBtnAttributes;
import org.telegram.ui.Components.QuickShare.QuickShareOpenEffectOverlay;
import org.telegram.ui.Components.QuickShareContainerLayout;
import org.telegram.ui.Components.QuickShareContainerLayout.QuickShareContainerListener;

public class QuickShareMenu {

    public final static int TYPE_REACTIONS = 0;
    public final static int TYPE_MENTIONS = 1;
    public final static String QUICK_SHARE_TAG = "QUICK_SHARE_TAG";

    public static ActionBarPopupWindow show(
            int direction,
            INavigationLayout navigationLayout,
            FrameLayout contentView,
            View cellView,
            float touchX,
            float shareButtonTopY,
            Theme.ResourcesProvider resourcesProvider,
            ArrayList<MessageObject> sendingMessageObjects,
            ChatActivity parentFragment,
            QuickShareBtnAttributes btnAttributes,
            QuickShareContainerListener quickShareContainerListener
    ) {
        FrameLayout windowContentView = new FrameLayout(contentView.getContext());
        windowContentView.setMinimumWidth(AndroidUtilities.dp(200)); // TODO: 20.10.2024
        QuickShareContainerLayout quickShareContainerLayout = new QuickShareContainerLayout(contentView.getContext(),
                resourcesProvider,
                quickShareContainerListener,
                sendingMessageObjects,
                parentFragment);
        quickShareContainerLayout.setMinimumWidth(AndroidUtilities.dp(200)); // TODO: 20.10.2024
        windowContentView.addView(quickShareContainerLayout, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT, 0, 0, 8, 0)); // TODO: 20.10.2024 fix margin
        ActionBarPopupWindow scrimPopupWindow = new ActionBarPopupWindow(windowContentView, LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);
        windowContentView.setClipToPadding(false);
        windowContentView.setClipChildren(false);
        scrimPopupWindow.setPauseNotifications(true);
        scrimPopupWindow.setDismissAnimationDuration(220);
        scrimPopupWindow.setOutsideTouchable(true);
        scrimPopupWindow.setClippingEnabled(true);
        scrimPopupWindow.setAnimationStyle(R.style.PopupContextAnimation);
        scrimPopupWindow.setFocusable(true);
        scrimPopupWindow.setTag(QUICK_SHARE_TAG);
        windowContentView.measure(View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000), View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000), View.MeasureSpec.AT_MOST));
        scrimPopupWindow.setInputMethodMode(ActionBarPopupWindow.INPUT_METHOD_NOT_NEEDED);
        scrimPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED);
        scrimPopupWindow.getContentView().setFocusableInTouchMode(true);
        float x = touchX;
        float y = shareButtonTopY - windowContentView.getMeasuredHeight() - AndroidUtilities.dp(16);
//        if (AndroidUtilities.isTablet()) {
//            View v = navigationLayout.getView(); // TODO: 20.10.2024 check
//            x += v.getX() + v.getPaddingLeft();
//            y += v.getY() + v.getPaddingTop();
//        }
        btnAttributes.buttonParentTopY = (int) shareButtonTopY;
        btnAttributes.maxParentX = contentView.getRight();
        if (y < btnAttributes.maxParentY) {
            btnAttributes.bottomDirection = true;
            y = shareButtonTopY + btnAttributes.rectF.height() + AndroidUtilities.dp(16);
            QuickShareOpenEffectOverlay.show(
                    quickShareContainerLayout,
                    cellView,
                    parentFragment,
                    (int) x,
                    (int) y,
                    btnAttributes
            );
            QuickShareOpenEffectOverlay.startAnimation(cellView);
            scrimPopupWindow.showAtLocation(contentView, Gravity.LEFT | Gravity.TOP, (int) x, (int) y);
        } else {
            btnAttributes.bottomDirection = false;
            QuickShareOpenEffectOverlay.show(
                    quickShareContainerLayout,
                    cellView,
                    parentFragment,
                    (int) x,
                    (int) y,
                    btnAttributes
            );
            QuickShareOpenEffectOverlay.startAnimation(cellView);
            scrimPopupWindow.showAtLocation(contentView, Gravity.LEFT | Gravity.TOP, (int) x, (int) y);
        }
        return scrimPopupWindow;
    }
}
