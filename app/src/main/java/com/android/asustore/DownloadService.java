package com.android.asustore;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.data.AppInfo;
import com.android.data.AppStoreSettings;
import com.android.install.PackageInstallerService;
import com.android.task.TaskCallback;
import com.thin.downloadmanager.DefaultRetryPolicy;
import com.thin.downloadmanager.DownloadRequest;
import com.thin.downloadmanager.DownloadStatusListenerV1;
import com.thin.downloadmanager.RetryPolicy;
import com.thin.downloadmanager.ThinDownloadManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DownloadService extends Service implements TaskCallback {
    public static final String TAG = "DownloadService";
    private static final int DOWNLOAD_THREAD_POOL_SIZE = 2;
    private static final int MSG_QUIT = 100;
    private IDownloadCallBack mDownloadCallback = null;
    Handler mWorkerHandler;
    HandlerThread sWorkerThread = null;
    private IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() {

        @Override
        public void binderDied() {
            mDownloadCallback.asBinder().unlinkToDeath(this, 0);
            mDownloadCallback = null;
        }
    };
    DownloadService.MyDownloadDownloadStatusListenerV1
            myDownloadStatusListener = new MyDownloadDownloadStatusListenerV1(DownloadService.this);
    private IDownloadService.Stub mService = new IDownloadService.Stub() {
        @Override
        public void startDownloadTask(String pkg) throws RemoteException {
            final Cursor c = getContentResolver().query(AppStoreSettings.APKs.CONTENT_URI, null, "pkg=?", new String[] { pkg }, null);
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
            if (list.size() < 1) {
                return;
            }
            info = list.get(0);
            if (info.status == AppStoreSettings.APKs.STATUS_TYPE_DOWNLOADING) {
                return;
            }
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
        public void stopDownloadTask(String pkg) throws RemoteException {

        }

        @Override
        public void setDownloadCallback(IDownloadCallBack callback) throws RemoteException {
            if (mDownloadCallback == null) {
                mDownloadCallback = callback;
                mDownloadCallback.asBinder().linkToDeath(deathRecipient, 0);
            } else {
                throw new RemoteException("alreay in use.");
            }
        }

        @Override
        public void unRegisterDownloadCallback() throws RemoteException {
            mDownloadCallback = null;
        }
    };
    private ThinDownloadManager downloadManager;
    RetryPolicy retryPolicy;

    @Override
    public void onCreate() {
        super.onCreate();
        sWorkerThread = new HandlerThread("DownloadService");
        sWorkerThread.start();
        mWorkerHandler = new Handler(sWorkerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_QUIT:
                        if (!downloadManager.isRunning() && mDownloadCallback == null) {
                            stopSelf();
                        }
                        break;
                    default:
                        break;
                }
            }
        };
        downloadManager = new ThinDownloadManager(DOWNLOAD_THREAD_POOL_SIZE);
        retryPolicy = new DefaultRetryPolicy();
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "######## onDestroy ######## ");
        downloadManager.release();
        mWorkerHandler.removeCallbacksAndMessages(null);
        sWorkerThread.quitSafely();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mService;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (downloadManager != null && !downloadManager.isRunning()) {
            stopSelf();
        }
        return super.onUnbind(intent);
    }

    @Override
    public void onDownloadDone(AppInfo info, int status, int progress) {
        if (mDownloadCallback != null) {
            try {
                mDownloadCallback.onDownloadDone(info.pkgName, status, progress);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        if (status == AppStoreSettings.APKs.STATUS_TYPE_DOWNLOADFAILED) {
            if (!mWorkerHandler.hasMessages(MSG_QUIT)) {
                mWorkerHandler.sendEmptyMessageDelayed(MSG_QUIT, 10000);
            }
        }
    }

    @Override
    public void onInstallPackageDone(AppInfo info, Boolean result) {
        Log.e(TAG, String.format("pgk name %s install %B", info.pkgName, result));
        int status = result ? AppStoreSettings.APKs.STATUS_TYPE_INSTALLED : AppStoreSettings.APKs.STATUS_TYPE_INSTALLFAILED;
        if (mDownloadCallback != null) {
            try {
                mDownloadCallback.onDownloadDone(info.pkgName, status, 100);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        ContentValues values = new ContentValues();
        values.put(AppStoreSettings.APKs.STATUS, result ? AppStoreSettings.APKs.STATUS_TYPE_INSTALLED : AppStoreSettings.APKs.STATUS_TYPE_INSTALLFAILED);

        String[] pkgParams = new String[] { info.pkgName };

        final ContentResolver cr = getContentResolver();
        cr.update(AppStoreSettings.APKs.CONTENT_URI, values,
                "pkg=?", pkgParams);
        if (!mWorkerHandler.hasMessages(MSG_QUIT)) {
            mWorkerHandler.sendEmptyMessageDelayed(MSG_QUIT, 10000);
        }
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
                        Log.e(TAG, String.format("onDownloadComplete download %s %d", info.pkgName, result));
                        PackageInstallerService.installPackage(DownloadService.this
                                , request.getDestinationURI().getPath(), info.pkgName, info, DownloadService.this);
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
                callback.onDownloadDone(info, AppStoreSettings.APKs.STATUS_TYPE_DOWNLOADFAILED, 0);
            }
            ContentValues values = new ContentValues();
            values.put(AppStoreSettings.APKs.STATUS, AppStoreSettings.APKs.STATUS_TYPE_DOWNLOADFAILED);

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
}
