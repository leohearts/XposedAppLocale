package com.zhangfx.xposed.applocale;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;

public class AppItem {

    private PackageInfo packageInfo;
    private String lang;

    public AppItem(PackageInfo packageInfo, String lang) {
        this.packageInfo = packageInfo;
        this.lang = lang;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public PackageInfo getPackageInfo() {
        return packageInfo;
    }

    public ApplicationInfo getApplicationInfo() {
        return packageInfo.applicationInfo;
    }

}
