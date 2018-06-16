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
import android.support.v7.widget.RecyclerView;

import com.android.data.AppInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The alphabetically sorted list of applications.
 */
public class AlphabeticalAppsList {

    public static final String TAG = "AlphabeticalAppsList";
    private static final boolean DEBUG = false;

    /**
     * Info about a particular adapter item (can be either section or app)
     */
    public static class AdapterItem {
        /** Common properties */
        // The index of this adapter item in the list
        public int position;
        // The type of this item
        public int viewType;

        // The row that this item shows up on
        public int rowIndex;
        // The index of this app in the row
        public int rowAppIndex;
        // The associated AppInfo for the app
        public AppInfo appInfo = null;
        // The index of this app not including sections
        public int appIndex = -1;


        public static AdapterItem asApp(int pos, AppInfo appInfo, int appIndex) {
            AdapterItem item = new AdapterItem();
            item.viewType = AllAppsGridAdapter.ICON_VIEW_TYPE;
            item.position = pos;
            item.appInfo = appInfo;
            item.appIndex = appIndex;
            return item;
        }
    }


    private Context mContext;

    // The set of apps from the system not including predictions
    private final List<AppInfo> mApps = new ArrayList<>();
    // The set of filtered apps with the current filter
    private List<AppInfo> mFilteredApps = new ArrayList<>();
    // The current set of adapter items
    private List<AdapterItem> mAdapterItems = new ArrayList<>();


    private RecyclerView.Adapter mAdapter;
    private int mNumAppsPerRow;
    private int mNumAppRowsInAdapter;

    public AlphabeticalAppsList(Context context) {
        mContext = context;
    }

    /**
     * Sets the number of apps per row.
     */
    public void setNumAppsPerRow(int numAppsPerRow) {
        mNumAppsPerRow = numAppsPerRow;
        updateAdapterItems();
    }

    /**
     * Sets the adapter to notify when this dataset changes.
     */
    public void setAdapter(RecyclerView.Adapter adapter) {
        mAdapter = adapter;
    }

    /**
     * Returns all the apps.
     */
    public List<AppInfo> getApps() {
        return mApps;
    }

    /**
     * Returns the current filtered list of applications broken down into their sections.
     */
    public List<AdapterItem> getAdapterItems() {
        return mAdapterItems;
    }

    /**
     * Returns the number of rows of applications (not including predictions)
     */
    public int getNumAppRows() {
        return mNumAppRowsInAdapter;
    }

    /**
     * Returns the number of applications in this list.
     */
    public int getNumFilteredApps() {
        return mFilteredApps.size();
    }
    /**
     * Sets the current set of apps.
     */
    public void setApps(List<AppInfo> apps) {
        addApps(apps);
    }

    /**
     * Adds new apps to the list.
     */
    public void addApps(List<AppInfo> apps) {
        updateApps(apps);
    }

    /**
     * Updates existing apps in the list
     */
    public void updateApps(List<AppInfo> apps) {
        mApps.clear();
        mApps.addAll(apps);
        updateAdapterItems();
    }
    public void updateApp(AppInfo info) {
        boolean hasUpdate = false;
        int pos = 0;
        for (AppInfo e : mApps) {
            if (e.id == info.id) {
                e.status = info.status;
                e.progress = info.progress;
                hasUpdate = true;
                break;
            }
            pos++;
        }
        if (hasUpdate) {
            mAdapter.notifyItemChanged(pos);
        }
    }

    private void updateAdapterItems() {
        int position = 0;
        int appIndex = 0;

        mFilteredApps.clear();
        mAdapterItems.clear();

        // Recreate the filtered and sectioned apps (for convenience for the grid layout) from the
        // ordered set of sections
        for (AppInfo info : getFiltersAppInfos()) {

            // Create an app item
            AdapterItem appItem = AdapterItem.asApp(position++, info, appIndex++);
            mAdapterItems.add(appItem);
            mFilteredApps.add(info);
        }

        if (mNumAppsPerRow != 0) {
            // Update the number of rows in the adapter after we do all the merging (otherwise, we
            // would have to shift the values again)
            int numAppsInSection = 0;
            int numAppsInRow = 0;
            int rowIndex = -1;
            for (AdapterItem item : mAdapterItems) {
                item.rowIndex = 0;
                if (item.viewType == AllAppsGridAdapter.ICON_VIEW_TYPE) {
                    if (numAppsInSection % mNumAppsPerRow == 0) {
                        numAppsInRow = 0;
                        rowIndex++;
                    }
                    item.rowIndex = rowIndex;
                    item.rowAppIndex = numAppsInRow;
                    numAppsInSection++;
                    numAppsInRow++;
                }
            }
            mNumAppRowsInAdapter = rowIndex + 1;
        }

        // Refresh the recycler view
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    private List<AppInfo> getFiltersAppInfos() {
        return mApps;
    }
}