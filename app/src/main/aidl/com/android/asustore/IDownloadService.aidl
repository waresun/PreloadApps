// IDownloadService.aidl
package com.android.asustore;
import com.android.asustore.IDownloadCallBack;

// Declare any non-default types here with import statements

interface IDownloadService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void startDownloadTask(String pkg);
    void stopDownloadTask(String pkg);
    void setDownloadCallback(IDownloadCallBack callback);
    void unRegisterDownloadCallback();
}
