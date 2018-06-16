package com.android.asustore;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.android.data.AppInfo;
import com.android.data.AppStoreSettings;
import com.android.install.PackageInstallerService;
import com.android.data.AppStoreApplicationState;
import com.android.task.TaskCallback;
import com.android.ui.allapps.AllAppsContainerView;
import com.thin.downloadmanager.DefaultRetryPolicy;
import com.thin.downloadmanager.DownloadRequest;
import com.thin.downloadmanager.DownloadStatusListenerV1;
import com.thin.downloadmanager.RetryPolicy;
import com.thin.downloadmanager.ThinDownloadManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, TaskCallback {

    private final static String TAG = "PreInstaller";
    private ThinDownloadManager downloadManager;
    private static final int DOWNLOAD_THREAD_POOL_SIZE = 4;
    AllAppsContainerView containerView;

    MyDownloadDownloadStatusListenerV1
            myDownloadStatusListener = new MyDownloadDownloadStatusListenerV1(this);

    Handler mWorkerHandler;
    static final HandlerThread sWorkerThread = new HandlerThread("worker");
    static {
        sWorkerThread.start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mWorkerHandler = new Handler(sWorkerThread.getLooper());
        AppStoreApplicationState.getLauncherProvider().loadDefaultAPKsIfNecessary();
        containerView = (AllAppsContainerView) findViewById(R.id.apps_view);
        containerView.setOnClickListener(this);
        final Cursor c = getContentResolver().query(AppStoreSettings.APKs.CONTENT_URI, null, null, null, null);
        AppInfo info;
        final int idIndex = c.getColumnIndexOrThrow
                (AppStoreSettings.APKs._ID);
        final int titleIndex = c.getColumnIndexOrThrow
                (AppStoreSettings.APKs.TITLE);
        final int pkgIndex = c.getColumnIndexOrThrow
                (AppStoreSettings.APKs.PKG);
        final int linkIndex = c.getColumnIndexOrThrow
                (AppStoreSettings.APKs.LINK);
        final int statusIndex = c.getColumnIndexOrThrow
                (AppStoreSettings.APKs.STATUS);
        List<AppInfo> list = new ArrayList<AppInfo>();
        while (c.moveToNext()) {
            info = new AppInfo();
            info.id = c.getInt(idIndex);
            info.title = c.getString(titleIndex);
            info.pkgName = c.getString(pkgIndex);
            info.iconBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
            info.link = c.getString(linkIndex);
            info.status = c.getInt(statusIndex);
            list.add(info);
        }
        containerView.setApps(list);

        downloadManager = new ThinDownloadManager(DOWNLOAD_THREAD_POOL_SIZE);
        RetryPolicy retryPolicy = new DefaultRetryPolicy();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.out.println("######## onDestroy ######## ");
        mHandler.removeCallbacksAndMessages(null);
        mWorkerHandler.removeCallbacksAndMessages(null);
        downloadManager.release();
    }

    @Override
    public void onClick(View v) {
        Object tag = v.getTag();
        final AppInfo info = (AppInfo) tag;
        Log.e("TEST", "link " + info.link);

        RetryPolicy retryPolicy = new DefaultRetryPolicy();

        File filesDir = getExternalFilesDir("");

        Uri downloadUri = Uri.parse(info.link);
        String fileName = String.format("/%s.apk", info.pkgName);
        Uri destinationUri = Uri.parse(filesDir+fileName);
        final DownloadRequest downloadRequest1 = new DownloadRequest(downloadUri)
                .setDestinationURI(destinationUri).setPriority(DownloadRequest.Priority.LOW)
                .setRetryPolicy(retryPolicy)
                .setDownloadContext(info)
                .setStatusListener(myDownloadStatusListener);
        downloadManager.add(downloadRequest1);
    }

    @Override
    public void onDownloadDone(AppInfo info, int status, int progress) {
        info.status = status;
        info.progress = progress;
        Message msg = mHandler.obtainMessage(MSG_STATUS, info);
        mHandler.sendMessage(msg);
    }

    @Override
    public void onInstallPackageDone(AppInfo info, Boolean result) {
        Log.e(TAG, String.format("pgk name %s install %B", info.pkgName, result));
        info.status = result ? AppStoreSettings.APKs.STATUS_TYPE_INSTALLED : AppStoreSettings.APKs.STATUS_TYPE_INSTALLFAILED;
        info.progress = 100;
        Message msg = mHandler.obtainMessage(MSG_STATUS, info);
        mHandler.sendMessage(msg);
        ContentValues values = new ContentValues();
        values.put(AppStoreSettings.APKs.STATUS, result ? AppStoreSettings.APKs.STATUS_TYPE_INSTALLED : AppStoreSettings.APKs.STATUS_TYPE_INSTALLFAILED);

        String[] pkgParams = new String[] { info.pkgName };

        final ContentResolver cr = getContentResolver();
        int ret = cr.update(AppStoreSettings.APKs.CONTENT_URI, values,
                "pkg=?", pkgParams);
        Log.e(TAG, String.format("failed install %s %d", info.pkgName, ret));
    }

    class MyDownloadDownloadStatusListenerV1 implements DownloadStatusListenerV1 {

        private int oldProgress = 0;
        private TaskCallback callback;
        public MyDownloadDownloadStatusListenerV1(TaskCallback callback) {
            this.callback = callback;
        }
        @Override
        public void onDownloadComplete(final DownloadRequest request) {
            mWorkerHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        AppInfo info = (AppInfo) request.getDownloadContext();
                        if (callback != null) {
                            callback.onDownloadDone(info, AppStoreSettings.APKs.STATUS_TYPE_DOWNLOADED, 0);
                        }
                        ContentValues values = new ContentValues();
                        values.put(AppStoreSettings.APKs.STATUS, AppStoreSettings.APKs.STATUS_TYPE_DOWNLOADED);

                        String[] pkgParams = new String[] { info.pkgName };

                        final ContentResolver cr = getContentResolver();
                        int result = cr.update(AppStoreSettings.APKs.CONTENT_URI, values,
                                "pkg=?", pkgParams);
                        Log.e(TAG, String.format("done download %s %d", info.pkgName, result));
                        PackageInstallerService.installPackage(MainActivity.this
                                , request.getDestinationURI().getPath(), info.pkgName, info, MainActivity.this);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void onDownloadFailed(DownloadRequest request, int errorCode, String errorMessage) {
            AppInfo info = (AppInfo) request.getDownloadContext();
            if (callback != null) {
                callback.onDownloadDone(info, AppStoreSettings.APKs.STATUS_TYPE_FAILED, 0);
            }
            ContentValues values = new ContentValues();
            values.put(AppStoreSettings.APKs.STATUS, AppStoreSettings.APKs.STATUS_TYPE_FAILED);

            String[] pkgParams = new String[] { info.pkgName };

            final ContentResolver cr = getContentResolver();
            int result = cr.update(AppStoreSettings.APKs.CONTENT_URI, values,
                    "pkg=?", pkgParams);
            Log.e(TAG, String.format("failed download %s %d", info.pkgName, result));
        }

        @Override
        public void onProgress(DownloadRequest request, long totalBytes, long downloadedBytes, int progress) {
            int id = request.getDownloadId();
            AppInfo info = (AppInfo) request.getDownloadContext();
            if (callback != null) {
                callback.onDownloadDone(info, AppStoreSettings.APKs.STATUS_TYPE_DOWNLOADING, progress);
            }
            if (progress - oldProgress < 5) {
                return;
            }
            oldProgress = progress;
            ContentValues values = new ContentValues();
            values.put(AppStoreSettings.APKs.PROGRESS, progress);
            values.put(AppStoreSettings.APKs.STATUS, AppStoreSettings.APKs.STATUS_TYPE_DOWNLOADING);

            String[] pkgParams = new String[] { info.pkgName };

            final ContentResolver cr = getContentResolver();
            int result = cr.update(AppStoreSettings.APKs.CONTENT_URI, values,
                    "pkg=?", pkgParams);
            Log.e(TAG, String.format("download %s %d %d", info.pkgName, result, progress));
        }
    }
    private static final int MSG_STATUS = 100;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_STATUS:
                    if (containerView != null) {
                        containerView.updateItem((AppInfo)msg.obj);
                    }
                    break;
                default:
                    break;
            }
        }
    };
}
