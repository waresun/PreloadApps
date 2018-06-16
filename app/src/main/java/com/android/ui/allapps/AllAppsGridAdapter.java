/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.ui.allapps;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat.CollectionItemInfoCompat;
import android.support.v4.view.accessibility.AccessibilityRecordCompat;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Recycler;
import android.support.v7.widget.RecyclerView.State;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;

import com.android.data.AppInfo;

import java.util.HashMap;
import com.android.asustore.R;
import com.android.ui.BubbleTextView;


/**
 * The grid view adapter of all the apps.
 */
public class AllAppsGridAdapter extends RecyclerView.Adapter<AllAppsGridAdapter.ViewHolder> {

    public static final String TAG = "AppsGridAdapter";
    private static final boolean DEBUG = false;

    // A normal icon
    public static final int ICON_VIEW_TYPE = 1;
    public interface BindViewCallback {
        public void onBindView(ViewHolder holder);
    }

    /**
     * ViewHolder for each icon.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public View mContent;

        public ViewHolder(View v) {
            super(v);
            mContent = v;
        }
    }

    /**
     * A subclass of GridLayoutManager that overrides accessibility values during app search.
     */
    public class AppsGridLayoutManager extends GridLayoutManager {

        public AppsGridLayoutManager(Context context) {
            super(context, 1, GridLayoutManager.VERTICAL, false);
        }

        @Override
        public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(event);

            // Ensure that we only report the number apps for accessibility not including other
            // adapter views
            final AccessibilityRecordCompat record = AccessibilityEventCompat
                    .asRecord(event);

            // count the number of SECTION_BREAK_VIEW_TYPE that is wrongfully
            // initialized as a node (also a row) for talk back.
            int numEmptyNode = getEmptyRowForAccessibility(-1 /* no view type */);
            record.setFromIndex(event.getFromIndex() - numEmptyNode);
            record.setToIndex(event.getToIndex() - numEmptyNode);
            record.setItemCount(mApps.getNumFilteredApps());
        }

        @Override
        public void onInitializeAccessibilityNodeInfoForItem(Recycler recycler,
                                                             State state, View host, AccessibilityNodeInfoCompat info) {

            int viewType = getItemViewType(host);
            // Only initialize on node that is meaningful. Subtract empty row count.
            if (viewType == ICON_VIEW_TYPE) {
                super.onInitializeAccessibilityNodeInfoForItem(recycler, state, host, info);
                CollectionItemInfoCompat itemInfo = info.getCollectionItemInfo();
                if (itemInfo != null) {
                    final CollectionItemInfoCompat dstItemInfo = CollectionItemInfoCompat.obtain(
                            itemInfo.getRowIndex() - getEmptyRowForAccessibility(viewType),
                            itemInfo.getRowSpan(),
                            itemInfo.getColumnIndex(),
                            itemInfo.getColumnSpan(),
                            itemInfo.isHeading(),
                            itemInfo.isSelected());
                    info.setCollectionItemInfo(dstItemInfo);
                }
            }
        }

        @Override
        public int getRowCountForAccessibility(RecyclerView.Recycler recycler,
                                               RecyclerView.State state) {
            return super.getRowCountForAccessibility(recycler, state)
                    - getEmptyRowForAccessibility(-1 /* no view type */);
        }

        /**
         * Returns the total number of SECTION_BREAK_VIEW_TYPE that is wrongfully
         * initialized as a node (also a row) for talk back.
         */
        private int getEmptyRowForAccessibility(int viewType) {
            int numEmptyNode = 0;
            {
                // default all apps screen may have one or two SECTION_BREAK_VIEW
                numEmptyNode = 1;
                {
                    if (viewType == ICON_VIEW_TYPE) {
                        numEmptyNode = 1;
                    }
                }
            }
            return numEmptyNode;
        }
    }

    /**
     * Helper class to size the grid items.
     */
    public class GridSpanSizer extends GridLayoutManager.SpanSizeLookup {

        public GridSpanSizer() {
            super();
            setSpanIndexCacheEnabled(true);
        }

        @Override
        public int getSpanSize(int position) {
            switch (mApps.getAdapterItems().get(position).viewType) {
                case AllAppsGridAdapter.ICON_VIEW_TYPE:
                    return 1;
                default:
                    // Section breaks span the full width
                    return mAppsPerRow;
            }
        }
    }

    /**
     * Helper class to draw the section headers
     */
    public class GridItemDecoration extends RecyclerView.ItemDecoration {

        private static final boolean DEBUG_SECTION_MARGIN = false;
        private static final boolean FADE_OUT_SECTIONS = false;

        private HashMap<String, PointF> mCachedSectionBounds = new HashMap<>();
        private Rect mTmpBounds = new Rect();

        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                   RecyclerView.State state) {
            // Do nothing
        }
    }

    private final Context mContext;
    private final LayoutInflater mLayoutInflater;
    private final AlphabeticalAppsList mApps;
    private final GridLayoutManager mGridLayoutMgr;
    private final GridSpanSizer mGridSizer;
    private final GridItemDecoration mItemDecoration;
    private final View.OnTouchListener mTouchListener;
    private final View.OnClickListener mIconClickListener;

    private final Rect mBackgroundPadding = new Rect();
    private final boolean mIsRtl;

    private int mAppsPerRow;
    private BindViewCallback mBindViewCallback;

    public AllAppsGridAdapter(Context context, AlphabeticalAppsList apps,
                              View.OnTouchListener touchListener, View.OnClickListener iconClickListener) {
        Resources res = context.getResources();
        mContext = context;
        mApps = apps;
        mGridSizer = new GridSpanSizer();
        mGridLayoutMgr = new AppsGridLayoutManager(context);
        mGridLayoutMgr.setSpanSizeLookup(mGridSizer);
        mItemDecoration = new GridItemDecoration();
        mLayoutInflater = LayoutInflater.from(context);
        mTouchListener = touchListener;
        mIconClickListener = iconClickListener;
        mIsRtl = false;
    }

    /**
     * Sets the number of apps per row.
     */
    public void setNumAppsPerRow(int appsPerRow) {
        mAppsPerRow = appsPerRow;
        mGridLayoutMgr.setSpanCount(appsPerRow);
    }


    /**
     * Sets the callback for when views are bound.
     */
    public void setBindViewCallback(BindViewCallback cb) {
        mBindViewCallback = cb;
    }

    /**
     * Notifies the adapter of the background padding so that it can draw things correctly in the
     * item decorator.
     */
    public void updateBackgroundPadding(Rect padding) {
        mBackgroundPadding.set(padding);
    }

    /**
     * Returns the grid layout manager.
     */
    public GridLayoutManager getLayoutManager() {
        return mGridLayoutMgr;
    }

    /**
     * Returns the item decoration for the recycler view.
     */
    public RecyclerView.ItemDecoration getItemDecoration() {
        // We don't draw any headers when we are uncomfortably dense
        return mItemDecoration;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case ICON_VIEW_TYPE: {
                BubbleTextView icon = (BubbleTextView) mLayoutInflater.inflate(
                        R.layout.all_apps_icon, parent, false);
                icon.setOnTouchListener(mTouchListener);
                icon.setOnClickListener(mIconClickListener);
                icon.setFocusable(true);
                return new ViewHolder(icon);
            }
            default:
                throw new RuntimeException("Unexpected view type");
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case ICON_VIEW_TYPE: {
                AppInfo info = mApps.getAdapterItems().get(position).appInfo;
                BubbleTextView icon = (BubbleTextView) holder.mContent;
                icon.applyFromApplicationInfo(info);
                break;
            }
        }
        if (mBindViewCallback != null) {
            mBindViewCallback.onBindView(holder);
        }
    }

    @Override
    public boolean onFailedToRecycleView(ViewHolder holder) {
        // Always recycle and we will reset the view when it is bound
        return true;
    }

    @Override
    public int getItemCount() {
        return mApps.getAdapterItems().size();
    }

    @Override
    public int getItemViewType(int position) {
        AlphabeticalAppsList.AdapterItem item = mApps.getAdapterItems().get(position);
        return item.viewType;
    }
}