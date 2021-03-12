package com.m2049r.xmrwallet.layout;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

public abstract class DiffCallback<T> extends DiffUtil.Callback {

    protected final List<T> mOldList;
    protected final List<T> mNewList;

    public DiffCallback(List<T> oldList, List<T> newList) {
        this.mOldList = oldList;
        this.mNewList = newList;
    }

    @Override
    public int getOldListSize() {
        return mOldList.size();
    }

    @Override
    public int getNewListSize() {
        return mNewList.size();
    }

    public abstract boolean areItemsTheSame(int oldItemPosition, int newItemPosition);

    public abstract boolean areContentsTheSame(int oldItemPosition, int newItemPosition);
}