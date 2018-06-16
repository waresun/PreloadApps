// IDownloadCallBack.aidl
package com.android.asustore;

// Declare any non-default types here with import statements

interface IDownloadCallBack {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void onDownloadDone(String pkg, int status, int progress);
}
