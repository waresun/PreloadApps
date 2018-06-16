package com.android.asustore;

import android.app.Service;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.android.data.AppInfo;
import com.android.data.AppStoreSettings;
import com.android.data.AppStoreApplicationState;
import com.android.ui.allapps.AllAppsContainerView;
import com.thin.downloadmanager.DefaultRetryPolicy;
import com.thin.downloadmanager.DownloadRequest;
import com.thin.downloadmanager.RetryPolicy;
import android.util.Log;

import java.io.File;
import com.android.asustore.IDownloadCallBack;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final static String TAG = "PreInstaller";
    AllAppsContainerView containerView;
    PackageManager pm = null;
    private IDownloadService mService;

    private IDownloadCallBack.Stub downloadCallBack = new IDownloadCallBack.Stub() {

        /**
         * Demonstrates some basic types that you can use as parameters
         * and return values in AIDL.
         *
         * @param pkg
         * @param status
         * @param progress
         */
        @Override
        public void onDownloadDone(String pkg, int status, int progress) throws RemoteException {
            AppInfo info = new AppInfo();
            info.pkgName = pkg;
            info.status = status;
            info.progress = progress;
            Message msg = mHandler.obtainMessage(MSG_STATUS, info);
            mHandler.sendMessage(msg);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent= new Intent("com.android.asustore.Download");
        intent.setPackage("com.android.asustore");
        startService(intent);
        bindService(intent, mConnection, Service.BIND_AUTO_CREATE);
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
        pm = getPackageManager();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.out.println("######## onDestroy ######## ");
        if (mService != null) {
            try {
                mService.unRegisterDownloadCallback();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        unbindService(mConnection);
        mConnection = null;
        mHandler.removeCallbacksAndMessages(null);
    }
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.e(TAG, "onServiceConnected");
            if (downloadCallBack != null) {
                mService = IDownloadService.Stub.asInterface(iBinder);
                if (mService != null) {
                    try {
                        mService.setDownloadCallback(downloadCallBack);
                    } catch (RemoteException e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e(TAG, "onServiceDisconnected");
            if (mService != null) {
                try {
                    mService.unRegisterDownloadCallback();
                } catch (RemoteException e) {

                }
            }
            mService = null;
        }
    };
    @Override
    public void onClick(View v) {
        Object tag = v.getTag();
        final AppInfo info = (AppInfo) tag;
        Intent intent = pm.getLaunchIntentForPackage(info.pkgName);
        if (intent != null) {
            startActivity(intent);
            return;
        }
        Log.e("TEST", "link " + info.link);
        if (mService != null) {
            try {
                mService.startDownloadTask(info.pkgName);
            } catch (RemoteException e) {
                Log.e(TAG, e.getMessage());
            }
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
