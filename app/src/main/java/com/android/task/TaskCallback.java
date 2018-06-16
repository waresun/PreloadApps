package com.android.task;

import com.android.data.AppInfo;

public interface TaskCallback {
    void onDownloadDone(AppInfo info, int status, int progress);
    void onInstallPackageDone(AppInfo info, Boolean result);
}
