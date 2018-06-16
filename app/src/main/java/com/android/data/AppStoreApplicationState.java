package com.android.data;

import java.lang.ref.WeakReference;

public class AppStoreApplicationState {
    private static WeakReference<AppStoreProvider> sAppStoreProvider;
    static void setLauncherProvider(AppStoreProvider provider) {
        sAppStoreProvider = new WeakReference<AppStoreProvider>(provider);
    }

    public static AppStoreProvider getLauncherProvider() {
        return sAppStoreProvider.get();
    }
}
