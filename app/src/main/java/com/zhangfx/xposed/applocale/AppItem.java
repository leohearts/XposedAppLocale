package com.zhangfx.xposed.applocale;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;

public class AppItem {

    private PackageInfo packageInfo;

    public AppItem(PackageInfo packageInfo) {
        this.packageInfo = packageInfo;
    }

    public PackageInfo getPackageInfo() {
        return packageInfo;
    }

    public ApplicationInfo getApplicationInfo() {
        return packageInfo.applicationInfo;
    }

}
