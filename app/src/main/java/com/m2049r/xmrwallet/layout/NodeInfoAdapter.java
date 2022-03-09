/*
 * Copyright (c) 2018 m2049r
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.m2049r.xmrwallet.layout;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.data.NodeInfo;
import com.m2049r.xmrwallet.dialog.HelpFragment;
import com.m2049r.xmrwallet.util.NetCipherHelper;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class NodeInfoAdapter extends RecyclerView.Adapter<NodeInfoAdapter.ViewHolder> {
    public interface OnInteractionListener {
        void onInteraction(View view, NodeInfo item);

        boolean onLongInteraction(View view, NodeInfo item);
    }

    private final List<NodeInfo> nodeItems = new ArrayList<>();
    private final OnInteractionListener listener;

    private final FragmentActivity activity;

    public NodeInfoAdapter(FragmentActivity activity, OnInteractionListener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    public void notifyItemChanged(NodeInfo nodeInfo) {
        final int pos = nodeItems.indexOf(nodeInfo);
        if (pos >= 0) notifyItemChanged(pos);
    }

    private static class NodeDiff extends DiffCallback<NodeInfo> {

        public NodeDiff(List<NodeInfo> oldList, List<NodeInfo> newList) {
            super(oldList, newList);
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return mOldList.get(oldItemPosition).equals(mNewList.get(newItemPosition));
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            final NodeInfo oldItem = mOldList.get(oldItemPosition);
            final NodeInfo newItem = mNewList.get(newItemPosition);
            return (oldItem.getTimestamp() == newItem.getTimestamp())
                    && (oldItem.isTested() == newItem.isTested())
                    && (oldItem.isValid() == newItem.isValid())
                    && (oldItem.getResponseTime() == newItem.getResponseTime())
                    && (oldItem.isSelected() == newItem.isSelected())
                    && (oldItem.getName().equals(newItem.getName()));
        }
    }

    @Override
    public @NonNull
    ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_node, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final @NonNull ViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return nodeItems.size();
    }

    public void addNode(NodeInfo node) {
        List<NodeInfo> newItems = new ArrayList<>(nodeItems);
        if (!nodeItems.contains(node))
            newItems.add(node);
        setNodes(newItems); // in case the nodeinfo has changed
    }

    public void setNodes(Collection<NodeInfo> newItemsCollection) {
        List<NodeInfo> newItems;
        if (newItemsCollection != null) {
            newItems = new ArrayList<>(newItemsCollection);
            Collections.sort(newItems, NodeInfo.BestNodeComparator);
        } else {
            newItems = new ArrayList<>();
        }
        final NodeInfoAdapter.NodeDiff diffCallback = new NodeInfoAdapter.NodeDiff(nodeItems, newItems);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        nodeItems.clear();
        nodeItems.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
    }

    public void setNodes() {
        setNodes(nodeItems);
    }

    private boolean itemsClickable = true;

    public void allowClick(boolean clickable) {
        itemsClickable = clickable;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        final ImageButton ibBookmark;
        final View pbBookmark;
        final TextView tvName;
        final TextView tvInfo;
        final ImageView ivPing;
        NodeInfo nodeItem;

        ViewHolder(View itemView) {
            super(itemView);
            ibBookmark = itemView.findViewById(R.id.ibBookmark);
            pbBookmark = itemView.findViewById(R.id.pbBookmark);
            tvName = itemView.findViewById(R.id.tvName);
            tvInfo = itemView.findViewById(R.id.tvInfo);
            ivPing = itemView.findViewById(R.id.ivPing);
            ibBookmark.setOnClickListener(v -> {
                nodeItem.toggleFavourite();
                showStar();
                if (!nodeItem.isFavourite()) {
                    nodeItem.setSelected(false);
                    setNodes(nodeItems);
                }
            });
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        private void showStar() {
            if (nodeItem.isFavourite()) {
                ibBookmark.setImageResource(R.drawable.ic_favorite_24dp);
            } else {
                ibBookmark.setImageResource(R.drawable.ic_favorite_border_24dp);
            }
        }

        void bind(int position) {
            nodeItem = nodeItems.get(position);
            tvName.setText(nodeItem.getName());
            ivPing.setImageResource(getPingIcon(nodeItem));
            if (nodeItem.isTested()) {
                if (nodeItem.isValid()) {
                    nodeItem.showInfo(tvInfo);
                } else {
                    nodeItem.showInfo(tvInfo, getResponseErrorText(activity, nodeItem.getResponseCode()), true);
                }
            } else {
                nodeItem.showInfo(tvInfo);
            }
            itemView.setSelected(nodeItem.isSelected());
            itemView.setClickable(itemsClickable);
            itemView.setEnabled(itemsClickable);
            ibBookmark.setClickable(itemsClickable);
            pbBookmark.setVisibility(nodeItem.isSelecting() ? View.VISIBLE : View.INVISIBLE);
            showStar();
        }

        @Override
        public void onClick(View view) {
            if (listener != null) {
                int position = getAdapterPosition(); // gets item position
                if (position != RecyclerView.NO_POSITION) { // Check if an item was deleted, but the user clicked it before the UI removed it
                    final NodeInfo node = nodeItems.get(position);
                    if (node.isOnion()) {
                        switch (NetCipherHelper.getStatus()) {
                            case NOT_INSTALLED:
                                HelpFragment.display(activity.getSupportFragmentManager(), R.string.help_tor);
                                return;
                            case DISABLED:
                                HelpFragment.display(activity.getSupportFragmentManager(), R.string.help_tor_enable);
                                return;
                        }
                    }
                    node.setSelecting(true);
                    allowClick(false);
                    listener.onInteraction(view, node);
                }
            }
        }

        @Override
        public boolean onLongClick(View view) {
            if (listener != null) {
                int position = getAdapterPosition(); // gets item position
                if (position != RecyclerView.NO_POSITION) { // Check if an item was deleted, but the user clicked it before the UI removed it
                    return listener.onLongInteraction(view, nodeItems.get(position));
                }
            }
            return false;
        }
    }

    static public int getPingIcon(NodeInfo nodeInfo) {
        if (nodeInfo.isUnauthorized()) {
            return R.drawable.ic_wifi_lock;
        }
        if (nodeInfo.isValid()) {
            final double ping = nodeInfo.getResponseTime();
            if (ping < NodeInfo.PING_GOOD) {
                return R.drawable.ic_wifi_4_bar;
            } else if (ping < NodeInfo.PING_MEDIUM) {
                return R.drawable.ic_wifi_3_bar;
            } else if (ping < NodeInfo.PING_BAD) {
                return R.drawable.ic_wifi_2_bar;
            } else {
                return R.drawable.ic_wifi_1_bar;
            }
        } else {
            return R.drawable.ic_wifi_off;
        }
    }

    static public String getResponseErrorText(Context ctx, int responseCode) {
        if (responseCode == 0) {
            return ctx.getResources().getString(R.string.node_general_error);
        } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            return ctx.getResources().getString(R.string.node_auth_error);
        } else if (responseCode == 418) {
            return ctx.getResources().getString(R.string.node_tor_error);
        } else {
            return ctx.getResources().getString(R.string.node_test_error, responseCode);
        }
    }
}
