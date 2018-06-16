package com.android.data;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.os.Process;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;

import com.novoda.android.AppItem;
import com.novoda.android.AppListItem;


import java.util.ArrayList;


public class AppStoreProvider extends ContentProvider {
    private static final String TAG = "AppStoreProvider";
    private static final boolean LOGD = false;

    private static final int DATABASE_VERSION = 1;

    static final String TABLE_FAVORITES = AppStoreSettings.APKs.TABLE_NAME;
    static final String EMPTY_DATABASE_CREATED = "EMPTY_DATABASE_CREATED";

    private static final String RESTRICTION_PACKAGE_NAME = "workspace.configuration.package.name";
    private static final String SHARED_PREFERENCES_KEY = "com.android.launcher3.prefs";
    protected DatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        final Context context = getContext();
        StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskWrites();
        mOpenHelper = new DatabaseHelper(context);
        StrictMode.setThreadPolicy(oldPolicy);
        AppStoreApplicationState.setLauncherProvider(this);
        return true;
    }

    public boolean wasNewDbCreated() {
        return mOpenHelper.wasNewDbCreated();
    }

    @Override
    public String getType(Uri uri) {
        SqlArguments args = new SqlArguments(uri, null, null);
        if (TextUtils.isEmpty(args.where)) {
            return "vnd.android.cursor.dir/" + args.table;
        } else {
            return "vnd.android.cursor.item/" + args.table;
        }
    }

    /**
     * Overridden in tests
     */
    protected synchronized void createDbIfNotExists() {
        if (mOpenHelper == null) {
            mOpenHelper = new DatabaseHelper(getContext());
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(args.table);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Cursor result = qb.query(db, projection, args.where, args.args, null, null, sortOrder);
        result.setNotificationUri(getContext().getContentResolver(), uri);

        return result;
    }

    static long dbInsertAndCheck(DatabaseHelper helper,
                                        SQLiteDatabase db, String table, String nullColumnHack, ContentValues values) {
        if (values == null) {
            throw new RuntimeException("Error: attempting to insert null values");
        }
        if (!values.containsKey(AppStoreSettings.ChangeLogColumns._ID)) {
            throw new RuntimeException("Error: attempting to add item without specifying an id");
        }
        helper.checkId(table, values);
        return db.insert(table, nullColumnHack, values);
    }

    private void reFreshUIIfExternal() {
        /*if (Utilities.ATLEAST_MARSHMALLOW && Binder.getCallingPid() != Process.myPid()) {
            LauncherAppState app = LauncherAppState.getInstanceNoCreate();
            if (app != null) {
                app.reloadWorkspace();
            }
        }*/
        //TODO
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        SqlArguments args = new SqlArguments(uri);

        if (Binder.getCallingPid() != Process.myPid()) {
            return null;
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        addModifiedTime(initialValues);
        final long rowId = dbInsertAndCheck(mOpenHelper, db, args.table, null, initialValues);
        if (rowId < 0) return null;

        uri = ContentUris.withAppendedId(uri, rowId);
        notifyListeners();

        reFreshUIIfExternal();
        return uri;
    }


    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        SqlArguments args = new SqlArguments(uri);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            int numValues = values.length;
            for (int i = 0; i < numValues; i++) {
                addModifiedTime(values[i]);
                if (dbInsertAndCheck(mOpenHelper, db, args.table, null, values[i]) < 0) {
                    return 0;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        notifyListeners();
        reFreshUIIfExternal();
        return values.length;
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentProviderResult[] result =  super.applyBatch(operations);
            db.setTransactionSuccessful();
            reFreshUIIfExternal();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = db.delete(args.table, args.where, args.args);
        if (count > 0) notifyListeners();

        reFreshUIIfExternal();
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);

        addModifiedTime(values);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = db.update(args.table, values, args.where, args.args);
        if (count > 0) notifyListeners();

        reFreshUIIfExternal();
        return count;
    }

    /**
     * Overridden in tests
     */
    protected void notifyListeners() {
    }

    static void addModifiedTime(ContentValues values) {
        values.put(AppStoreSettings.ChangeLogColumns.MODIFIED, System.currentTimeMillis());
    }

    /**
     * Clears all the data for a fresh start.
     */
    synchronized public void createEmptyDB() {
        mOpenHelper.createEmptyDB(mOpenHelper.getWritableDatabase());
    }

    public void clearFlagEmptyDbCreated() {
        getPrefs(getContext()).edit().remove(EMPTY_DATABASE_CREATED).commit();
    }
    public static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(
                SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
    }

    synchronized public void loadDefaultAPKsIfNecessary() {
        SharedPreferences sp = getPrefs(getContext());

        if (sp.getBoolean(EMPTY_DATABASE_CREATED, false)) {
            Log.d(TAG, "loading default workspace");

            createEmptyDB();
            mOpenHelper.loadFavorites(mOpenHelper.getWritableDatabase(),
                    new DefaultXMLLoader(getContext()));
            clearFlagEmptyDbCreated();
        }
    }

    public void deleteDatabase() {
        // Are you sure? (y/n)
        mOpenHelper.createEmptyDB(mOpenHelper.getWritableDatabase());
    }

    protected static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String LAUNCHER_DB = "appstore.db";
        private final Context mContext;
        private long mMaxItemId = -1;

        private boolean mNewDbCreated = false;


        DatabaseHelper(Context context) {
            super(context, LAUNCHER_DB, null, DATABASE_VERSION);
            mContext = context;

            if (!tableExists(TABLE_FAVORITES)) {
                Log.e(TAG, "Tables are missing after onCreate has been called. Trying to recreate");
                // This operation is a no-op if the table already exists.
                addFavoritesTable(getWritableDatabase(), true);
            }

            if (mMaxItemId == -1) {
                mMaxItemId = initializeMaxItemId(getWritableDatabase());
            }
        }

        /**
         * Constructor used only in tests.
         */
        public DatabaseHelper(Context context, String tableName) {
            super(context, tableName, null, DATABASE_VERSION);
            mContext = context;
            mMaxItemId = initializeMaxItemId(getWritableDatabase());
        }

        private boolean tableExists(String tableName) {
            Cursor c = getReadableDatabase().query(
                    true, "sqlite_master", new String[] {"tbl_name"},
                    "tbl_name = ?", new String[] {tableName},
                    null, null, null, null, null);
            try {
                return c.getCount() > 0;
            } finally {
                c.close();
            }
        }

        public boolean wasNewDbCreated() {
            return mNewDbCreated;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            if (LOGD) Log.d(TAG, "creating new launcher database");

            mMaxItemId = 1;
            mNewDbCreated = true;

            addFavoritesTable(db, false);

            // Fresh and clean launcher DB.
            mMaxItemId = initializeMaxItemId(db);
            onEmptyDbCreated();
        }

        /**
         * Overriden in tests.
         */
        protected void onEmptyDbCreated() {
            // Set the flag for empty DB
            getPrefs(mContext).edit().putBoolean(EMPTY_DATABASE_CREATED, true).commit();
        }

        protected long getDefaultUserSerial() {
            return 0;
        }

        private void addFavoritesTable(SQLiteDatabase db, boolean optional) {
            String ifNotExists = optional ? " IF NOT EXISTS " : "";
            db.execSQL("CREATE TABLE " + ifNotExists + TABLE_FAVORITES + " (" +
                    "_id INTEGER PRIMARY KEY," +
                    "title TEXT," +
                    "intent TEXT," +
                    "container INTEGER," +
                    "screen INTEGER," +
                    "cellX INTEGER," +
                    "cellY INTEGER," +
                    "spanX INTEGER," +
                    "spanY INTEGER," +
                    "itemType INTEGER," +
                    "iconType INTEGER," +
                    "iconPackage TEXT," +
                    "iconResource TEXT," +
                    "icon BLOB," +
                    " TEXT," +
                    "status INTEGER NOT NULL DEFAULT 0," +
                    "link TEXT," +
                    "progress INTEGERNOT NULL DEFAULT 0," +
                    "pkg TEXT," +
                    "modified INTEGER NOT NULL DEFAULT 0," +
                    "restored INTEGER NOT NULL DEFAULT 0," +
                    "profileId INTEGER DEFAULT " + getDefaultUserSerial() + "," +
                    "rank INTEGER NOT NULL DEFAULT 0," +
                    "options INTEGER NOT NULL DEFAULT 0" +
                    ");");
        }


        private void setFlagJustLoadedOldDb() {
            getPrefs(mContext).edit().putBoolean(EMPTY_DATABASE_CREATED, false).commit();
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (LOGD) Log.d(TAG, "onUpgrade triggered: " + oldVersion);
            // DB was not upgraded
            Log.w(TAG, "Destroying all old data.");
            createEmptyDB(db);
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // This shouldn't happen -- throw our hands up in the air and start over.
            Log.w(TAG, "Database version downgrade from: " + oldVersion + " to " + newVersion +
                    ". Wiping databse.");
            createEmptyDB(db);
        }

        /**
         * Clears all the data for a fresh start.
         */
        public void createEmptyDB(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES);
            onCreate(db);
        }


        public void checkId(String table, ContentValues values) {
            long id = values.getAsLong(AppStoreSettings.BaseAppsColumns._ID);
            mMaxItemId = Math.max(id, mMaxItemId);
        }

        private long initializeMaxItemId(SQLiteDatabase db) {
            return getMaxId(db, TABLE_FAVORITES);
        }

        int loadFavorites(SQLiteDatabase db, DefaultXMLLoader loader) {
            AppListItem appList = loader.loadXML();
            ContentValues values = new ContentValues();
            int i = 0;
            for (AppItem e : appList.appItems) {
                values.clear();
                values.put(AppStoreSettings.APKs._ID, i);
                values.put(AppStoreSettings.APKs.PKG, e.pkg.toString());
                values.put(AppStoreSettings.APKs.ICON_RESOURCE, e.icon.toString());
                values.put(AppStoreSettings.APKs.LINK, e.link.toString());
                values.put(AppStoreSettings.APKs.TITLE, e.title.toString());
                if (dbInsertAndCheck(this, db, TABLE_FAVORITES, null, values) < 0) {
                    throw new RuntimeException("Failed initialize screen table"
                            + "from default layout");
                }
                i++;
            }

            // Ensure that the max ids are initialized
            mMaxItemId = initializeMaxItemId(db);

            return appList.appItems.size();
        }
    }

    /**
     * @return the max _id in the provided table.
     */
    static long getMaxId(SQLiteDatabase db, String table) {
        Cursor c = db.rawQuery("SELECT MAX(_id) FROM " + table, null);
        // get the result
        long id = -1;
        if (c != null && c.moveToNext()) {
            id = c.getLong(0);
        }
        if (c != null) {
            c.close();
        }

        if (id == -1) {
            throw new RuntimeException("Error: could not query max id in " + table);
        }

        return id;
    }

    static class SqlArguments {
        public final String table;
        public final String where;
        public final String[] args;

        SqlArguments(Uri url, String where, String[] args) {
            if (url.getPathSegments().size() == 1) {
                this.table = url.getPathSegments().get(0);
                this.where = where;
                this.args = args;
            } else if (url.getPathSegments().size() != 2) {
                throw new IllegalArgumentException("Invalid URI: " + url);
            } else if (!TextUtils.isEmpty(where)) {
                throw new UnsupportedOperationException("WHERE clause not supported: " + url);
            } else {
                this.table = url.getPathSegments().get(0);
                this.where = "_id=" + ContentUris.parseId(url);
                this.args = null;
            }
        }

        SqlArguments(Uri url) {
            if (url.getPathSegments().size() == 1) {
                table = url.getPathSegments().get(0);
                where = null;
                args = null;
            } else {
                throw new IllegalArgumentException("Invalid URI: " + url);
            }
        }
    }
}