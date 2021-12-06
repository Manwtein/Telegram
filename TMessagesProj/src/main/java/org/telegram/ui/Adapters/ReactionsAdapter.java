/*
 * This is the source code of Telegram for Android v. 2.0.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Adapters;

import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Cells.ReactionCell;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

public class ReactionsAdapter extends RecyclerListView.SelectionAdapter implements NotificationCenter.NotificationCenterDelegate {

    private int currentAccount = UserConfig.selectedAccount;
    private Context mContext;
    private ArrayList<MediaDataController.KeywordResult> keywordResults;
    private ReactionsAdapterDelegate delegate;

    private boolean visible;

    public ArrayList<TLRPC.TL_availableReaction> reactions = new ArrayList<>();

    public interface ReactionsAdapterDelegate {
        void needChangePanelVisibility(boolean show);
    }

    public ReactionsAdapter(Context context, ReactionsAdapterDelegate delegate) {
        mContext = context;
        this.delegate = delegate;
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.reactionsLoaded);
    }

    public void onDestroy() {
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.reactionsLoaded);
    }

    @Override
    public void didReceivedNotification(int id, int account, final Object... args) {
        if (id == NotificationCenter.reactionsLoaded) {
            // TODO 03/12/2021 Fuji team, RIDER-:
//            if (reactions.isEmpty()) {
//                reactions.addAll(((TLRPC.TL_messages_availableReactions) args[0]).reactions);
//                notifyDataSetChanged();
//            }
        }
    }

    public void hide() {
        if (visible && keywordResults != null && !keywordResults.isEmpty()) {
            visible = false;
            delegate.needChangePanelVisibility(false);
        }
    }

    @Override
    public int getItemCount() {
        return reactions.size();
    }

    @Override
    public boolean isEnabled(RecyclerView.ViewHolder holder) {
        return false; // TODO 29/11/2021 Fuji team, RIDER-: check
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        return new RecyclerListView.Holder(new ReactionCell(mContext, 40, 40));
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ReactionCell cell = (ReactionCell) holder.itemView;
        cell.setSticker(reactions.get(position).select_animation, null);
        cell.setReactionSymbol(reactions.get(position).reaction);
    }
}
