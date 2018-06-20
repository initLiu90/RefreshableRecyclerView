package com.lzp.test.refreshablerecyclerview;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.lzp.test.refreshablerecyclerview.item.Item;
import com.lzp.test.refreshablerecyclerview.item.ItemBanner;
import com.lzp.test.refreshablerecyclerview.item.ItemContent;
import com.lzp.test.refreshablerecyclerview.item.ItemDivider;
import com.lzp.test.refreshablerecyclerview.item.ItemHeader;

import java.util.List;

/**
 * Created by lillian on 2018/6/2.
 */

public class MyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Item> mDatas;

    private static final int TYPE_BANNER = 1;
    private static final int TYPE_CONTENT = 2;
    private static final int TYPE_DIVIDER = 3;
    private static final int TYPE_HEADER = 4;

    public void setData(List<Item> datas) {
        mDatas = datas;
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = null;
        RecyclerView.ViewHolder holder = null;
        if (viewType == TYPE_BANNER) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_banner, parent, false);
            holder = new BannerViewHolder(view);
        } else if (viewType == TYPE_CONTENT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_content, parent, false);
            holder = new ContentViewHolder(view);
        } else if (viewType == TYPE_DIVIDER) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_divider, parent, false);
            holder = new DividerViewHolder(view);
        } else if (viewType == TYPE_HEADER) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_header, parent, false);
            holder = new HeaderViewHolder(view);
        }

        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (mDatas.get(position) instanceof ItemContent) {
            ((ContentViewHolder) holder).content.setText((String) mDatas.get(position).item);
        } else if (mDatas.get(position) instanceof ItemBanner) {

        } else if (mDatas.get(position) instanceof ItemDivider) {

        } else if (mDatas.get(position) instanceof ItemHeader) {

        }
    }

    @Override
    public int getItemCount() {
        return mDatas.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mDatas.get(position) instanceof ItemBanner)
            return TYPE_BANNER;
        if (mDatas.get(position) instanceof ItemDivider)
            return TYPE_DIVIDER;
        if (mDatas.get(position) instanceof ItemContent)
            return TYPE_CONTENT;
        if (mDatas.get(position) instanceof ItemHeader)
            return TYPE_HEADER;
        return 0;
    }

    public static class ContentViewHolder extends RecyclerView.ViewHolder {
        TextView content;

        public ContentViewHolder(View itemView) {
            super(itemView);
            content = (TextView) itemView.findViewById(R.id.item_content_text);
        }
    }

    public static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView banner;

        public BannerViewHolder(View itemView) {
            super(itemView);

        }
    }

    public static class DividerViewHolder extends RecyclerView.ViewHolder {

        public DividerViewHolder(View itemView) {
            super(itemView);

        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {

        public HeaderViewHolder(View itemView) {
            super(itemView);

        }
    }
}
