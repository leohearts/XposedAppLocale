package com.zhangfx.xposed.applocale;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;

public class AppItem {

    private PackageInfo packageInfo;
    private String appLabel;

    public AppItem(PackageInfo packageInfo, String appLabel) {
        this.packageInfo = packageInfo;
        this.appLabel = appLabel;
    }

    public PackageInfo getPackageInfo() {
        return packageInfo;
    }

    public ApplicationInfo getApplicationInfo() {
        return packageInfo.applicationInfo;
    }

    public String getAppLabel() {
        return appLabel;
    }
}
