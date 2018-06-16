
package com.android.data;

import android.net.Uri;
import android.provider.BaseColumns;


/**
 * Settings related utilities.
 */
public class AppStoreSettings {
    public static final String AUTHORITY = "com.android.asustore.settings".intern();
    /** Columns required on table staht will be subject to backup and restore. */
    static interface ChangeLogColumns extends BaseColumns {
        /**
         * The time of the last update to this row.
         * <P>Type: INTEGER</P>
         */
        public static final String MODIFIED = "modified";
    }

    static interface BaseAppsColumns extends ChangeLogColumns {
        /**
         * Descriptive name of the gesture that can be displayed to the user.
         * <P>Type: TEXT</P>
         */
        public static final String TITLE = "title";

        /**
         * The Intent URL of the gesture, describing what it points to. This
         * value is given to {@link android.content.Intent#parseUri(String, int)} to create
         * an Intent that can be launched.
         * <P>Type: TEXT</P>
         */
        public static final String INTENT = "intent";

        /**
         * The type of the gesture
         *
         * <P>Type: INTEGER</P>
         */
        public static final String ITEM_TYPE = "itemType";

        /**
         * The gesture is an application
         */
        public static final int ITEM_TYPE_APPLICATION = 0;

        /**
         * The gesture is an application created shortcut
         */
        public static final int ITEM_TYPE_SHORTCUT = 1;

        /**
         * The icon type.
         * <P>Type: INTEGER</P>
         */
        public static final String ICON_TYPE = "iconType";

        /**
         * The icon is a resource identified by a package name and an integer id.
         */
        public static final int ICON_TYPE_RESOURCE = 0;

        /**
         * The icon is a bitmap.
         */
        public static final int ICON_TYPE_BITMAP = 1;

        /**
         * The icon package name, if icon type is ICON_TYPE_RESOURCE.
         * <P>Type: TEXT</P>
         */
        public static final String ICON_PACKAGE = "iconPackage";

        /**
         * The icon resource id, if icon type is ICON_TYPE_RESOURCE.
         * <P>Type: TEXT</P>
         */
        public static final String ICON_RESOURCE = "iconResource";

        /**
         * The custom icon bitmap, if icon type is ICON_TYPE_BITMAP.
         * <P>Type: BLOB</P>
         */
        public static final String ICON = "icon";
    }

  /**
     * APKs.
     */
    public static final class APKs implements BaseAppsColumns {

        public static final String TABLE_NAME = "apks";

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" +
                AUTHORITY + "/" + TABLE_NAME);

        /**
         * The content:// style URL for a given row, identified by its id.
         *
         * @param id The row id.
         *
         * @return The unique content URL for the specified row.
         */
        public static Uri getContentUri(long id) {
            return Uri.parse("content://" + AUTHORITY +
                    "/" + TABLE_NAME + "/" + id);
        }

        public static final String URL = "url";

        /**
         * The container holding the favorite
         * <P>Type: INTEGER</P>
         */
        public static final String CONTAINER = "container";

        /**
         * The icon is a resource identified by a package name and an integer id.
         */
        public static final int CONTAINER_DESKTOP = -100;
        public static final int CONTAINER_HOTSEAT = -101;

        static final String containerToString(int container) {
            switch (container) {
                case CONTAINER_DESKTOP: return "desktop";
                case CONTAINER_HOTSEAT: return "hotseat";
                default: return String.valueOf(container);
            }
        }

        /**
         * The screen holding the favorite (if container is CONTAINER_DESKTOP)
         * <P>Type: INTEGER</P>
         */
        public static final String SCREEN = "screen";

        /**
         * The X coordinate of the cell holding the favorite
         * (if container is CONTAINER_HOTSEAT or CONTAINER_HOTSEAT)
         * <P>Type: INTEGER</P>
         */
        public static final String CELLX = "cellX";

        /**
         * The Y coordinate of the cell holding the favorite
         * (if container is CONTAINER_DESKTOP)
         * <P>Type: INTEGER</P>
         */
        public static final String CELLY = "cellY";

        /**
         * The X span of the cell holding the favorite
         * <P>Type: INTEGER</P>
         */
        public static final String SPANX = "spanX";

        /**
         * The Y span of the cell holding the favorite
         * <P>Type: INTEGER</P>
         */
        public static final String SPANY = "spanY";

        /**
         * The profile id of the item in the cell.
         * <P>
         * Type: INTEGER
         * </P>
         */
        public static final String PROFILE_ID = "profileId";

        /**
         * The favorite is a user created folder
         */
        public static final int ITEM_TYPE_FOLDER = 2;

        /**
         * The favorite is a widget
         */
        public static final int ITEM_TYPE_APPWIDGET = 4;

        /**
         * The favorite is a custom widget provided by the launcher
         */
        public static final int ITEM_TYPE_CUSTOM_APPWIDGET = 5;

        /**
         * The URI associated with the app. It is used, for instance, by
         * live folders to find the content provider.
         * <P>Type: TEXT</P>
         */
        public static final String LINK = "link";
        public static final String STATUS = "status";
        public static final int STATUS_TYPE_UNKNOWN = 0;
        public static final int STATUS_TYPE_DOWNLOADING = 1;
        public static final int STATUS_TYPE_PAUSED = 2;
        public static final int STATUS_TYPE_DOWNLOADED = 3;
        public static final int STATUS_TYPE_FAILED = 4;
        public static final int STATUS_TYPE_INSTALLED = 5;
        public static final String PKG = "pkg";

        /**
         * Boolean indicating that his item was restored and not yet successfully bound.
         * <P>Type: INTEGER</P>
         */
        public static final String RESTORED = "restored";

        /**
         * Indicates the position of the item inside an auto-arranged view like folder or hotseat.
         * <p>Type: INTEGER</p>
         */
        public static final String RANK = "rank";

        /**
         * Stores general flag based options for {@link ItemInfo}s.
         * <p>Type: INTEGER</p>
         */
        public static final String OPTIONS = "options";
    }

    /**
     * Launcher settings
     */
    public static final class Settings {

        public static final Uri CONTENT_URI = Uri.parse("content://" +
                AUTHORITY + "/settings");

        public static final String METHOD_GET_BOOLEAN = "get_boolean_setting";
        public static final String METHOD_SET_BOOLEAN = "set_boolean_setting";

        public static final String EXTRA_VALUE = "value";
        public static final String EXTRA_DEFAULT_VALUE = "default_value";

        // Extra for set_boolean method to also notify the backup manager of the change.
        public static final String NOTIFY_BACKUP = "notify_backup";
    }
}