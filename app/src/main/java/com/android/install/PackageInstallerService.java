package com.android.install;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;

import android.util.Log;

import com.android.data.AppInfo;
import com.android.task.TaskCallback;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PackageInstallerService {
    static final String TAG = "PackageInstallerService";
    private static final String ACTION_INSTALL_COMPLETE = "ACTION_INSTALL_COMPLETE";
    public static boolean installPackage(Context context, String path, String packageName, AppInfo info, TaskCallback taskCallback)
            throws IOException {
        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        packageInstaller.registerSessionCallback(new MySessionCallback(context, path, info, taskCallback));
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName(packageName);
        // set params
        final int sessionId = packageInstaller.createSession(params);
        Log.d(TAG, String.format("start install %s", path));
        final File file = new File(path);
        final InputStream in = new FileInputStream(file);
        final long sizeBytes = file.length();
        PackageInstaller.Session session = packageInstaller.openSession(sessionId);
        OutputStream out = session.openWrite("PackageInstallerService", 0, sizeBytes);
        final byte[] buffer = new byte[65536];
        try {
            int c;
            while ((c = in.read(buffer)) != -1) {
                out.write(buffer, 0, c);
            }
            session.fsync(out);
        } finally {
            in.close();
            out.close();
        }
        session.commit(createIntentSender(context, sessionId));
        return true;
    }

    private static IntentSender createIntentSender(Context context, int sessionId) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                new Intent(ACTION_INSTALL_COMPLETE),
                0);
        return pendingIntent.getIntentSender();
    }

    private static final class MySessionCallback extends PackageInstaller.SessionCallback {
        private Context context;
        private String mPath;
        private AppInfo mInfo;
        private TaskCallback mCallback;
        public MySessionCallback(Context context, String path, AppInfo info, TaskCallback taskCallback) {
            this.context = context;
            mPath = path;
            mInfo = info;
            mCallback = taskCallback;
        }
        @Override
        public void onCreated(int i) {

        }

        @Override
        public void onBadgingChanged(int i) {

        }

        @Override
        public void onActiveChanged(int i, boolean b) {

        }

        @Override
        public void onProgressChanged(int i, float v) {

        }

        @Override
        public void onFinished(int i, boolean b) {
            File destinationFile = new File(mPath);
            if (destinationFile.exists()) {
                destinationFile.delete();
            }
            if (mCallback != null) {
                mCallback.onInstallPackageDone(mInfo, b);
            }
            context.getPackageManager().getPackageInstaller().unregisterSessionCallback(this);
        }
    }
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int statusCode = intent.getIntExtra(
                    PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
            onPackageInstalled(statusCode);
        }
    };
    private void onPackageInstalled(int code) {
        switch (code) {
            case PackageInstaller.STATUS_SUCCESS:
                break;
            case PackageInstaller.STATUS_FAILURE_STORAGE:
                break;
            default:
                break;
        }
    }
}
