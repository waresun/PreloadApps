package com.android.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

import java.util.Arrays;

/**
 * Represents an item in the launcher.
 */
public class AppInfo {

    /**
     * Intent extra to store the profile. Format: UserHandle
     */
    static final String EXTRA_PROFILE = "profile";

    public static final int NO_ID = -1;

    /**
     * The id in the settings database for this item
     */
    public long id = NO_ID;

    public int itemType;

    public long container = NO_ID;

    /**
     * Iindicates the screen in which the shortcut appears.
     */
    public long screenId = -1;

    /**
     * Indicates the X position of the associated cell.
     */
    public int cellX = -1;

    /**
     * Indicates the Y position of the associated cell.
     */
    public int cellY = -1;

    /**
     * Indicates the X cell span.
     */
    public int spanX = 1;

    /**
     * Indicates the Y cell span.
     */
    public int spanY = 1;

    /**
     * Indicates the minimum X cell span.
     */
    public int minSpanX = 1;

    /**
     * Indicates the minimum Y cell span.
     */
    public int minSpanY = 1;

    /**
     * Indicates the position in an ordered list.
     */
    public int rank = 0;

    /**
     * Indicates that this item needs to be updated in the db
     */
    public boolean requiresDbUpdate = false;

    /**
     * Title of the item
     */
    public CharSequence title;
    public String link;

    /**
     * Content description of the item.
     */
    public CharSequence contentDescription;

    public String pkgName;
    /**
     * The position of the item in a drag-and-drop operation.
     */
    public int[] dropPos = null;
    public Bitmap iconBitmap;

    public AppInfo() {
            }

    AppInfo(AppInfo info) {
        copyFrom(info);
    }

    public void copyFrom(AppInfo info) {
        id = info.id;
        cellX = info.cellX;
        cellY = info.cellY;
        spanX = info.spanX;
        spanY = info.spanY;
        rank = info.rank;
        screenId = info.screenId;
        itemType = info.itemType;
        container = info.container;
        contentDescription = info.contentDescription;
    }

    public Intent getIntent() {
        throw new RuntimeException("Unexpected Intent");
    }

    /**
     * Write the fields of this item to the DB
     *
     * @param context A context object to use for getting UserManagerCompat
     * @param values
     */

    void onAddToDatabase(Context context, ContentValues values) {
        values.put(AppStoreSettings.BaseAppsColumns.ITEM_TYPE, itemType);
        values.put(AppStoreSettings.APKs.CONTAINER, container);
        values.put(AppStoreSettings.APKs.SCREEN, screenId);
        values.put(AppStoreSettings.APKs.CELLX, cellX);
        values.put(AppStoreSettings.APKs.CELLY, cellY);
        values.put(AppStoreSettings.APKs.SPANX, spanX);
        values.put(AppStoreSettings.APKs.SPANY, spanY);
        values.put(AppStoreSettings.APKs.RANK, rank);
        values.put(AppStoreSettings.APKs.PROFILE_ID, "");
    }

    static void writeBitmap(ContentValues values, Bitmap bitmap) {
        //TODO
        if (bitmap != null) {
/*            byte[] data = Utilities.flattenBitmap(bitmap);
            values.put(LauncherSettings.Favorites.ICON, data);*/
        }
    }

    /**
     * It is very important that sub-classes implement this if they contain any references
     * to the activity (anything in the view hierarchy etc.). If not, leaks can result since
     * ItemInfo objects persist across rotation and can hence leak by holding stale references
     * to the old view hierarchy / activity.
     */
    void unbind() {
    }

    @Override
    public String toString() {
        return "Item(id=" + this.id + " type=" + this.itemType + " container=" + this.container
                + " screen=" + screenId + " cellX=" + cellX + " cellY=" + cellY + " spanX=" + spanX
                + " spanY=" + spanY + " dropPos=" + Arrays.toString(dropPos)
                + ")";
    }

    /**
     * Whether this item is disabled.
     */
    public boolean isDisabled() {
        return false;
    }
    public String toComponentKey() {
        return pkgName;
    }
}
