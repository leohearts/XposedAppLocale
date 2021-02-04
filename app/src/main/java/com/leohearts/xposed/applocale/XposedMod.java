package com.leohearts.xposed.applocale;

import android.app.AndroidAppHelper;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.Locale;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedMod implements IXposedHookZygoteInit, IXposedHookLoadPackage {

    private static XSharedPreferences prefs;

    public String getPackageName() {
        return Application.getProcessName();
    }
    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        loadPrefs();

        try {
            XposedHelpers.findAndHookMethod(Locale.class, "getDefault", new XC_MethodHook() {
                        @Override
                        public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                            String packageName = getPackageName();
                            Locale locale = XposedMod.getPackageSpecificLocale(packageName);
                            boolean isActiveApp = AndroidAppHelper.currentPackageName().equals(packageName);
                            if (packageName != null && locale != null && isActiveApp) {
                                Log.i(Common.TAG, "beforeHookedMethod: Setting language of ".concat(packageName) + " to ".concat(locale.toString()));
                                param.setResult(locale);
                            }
                        }
                    }
            );
            XposedHelpers.findAndHookMethod(Locale.class, "getDefault", Locale.Category.class, new XC_MethodHook() {
                        @Override
                        public void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                            String packageName = getPackageName();
                            Locale locale = XposedMod.getPackageSpecificLocale(packageName);
                            boolean isActiveApp = AndroidAppHelper.currentPackageName().equals(packageName);
                            if (packageName != null && locale != null && isActiveApp) {
                                Log.i(Common.TAG, "beforeHookedMethod(2): Setting language of ".concat(packageName) + " to ".concat(locale.toString()));
                                param.setResult(locale);
                            }
                        }
                    }
            );
            XposedHelpers.findAndHookMethod(Resources.class, "getConfiguration", new XC_MethodHook() {
                        @Override
                        public void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                            String packageName = XposedMod.this.getPackageName();
                            Locale locale = XposedMod.getPackageSpecificLocale(packageName);
                            boolean isActiveApp = AndroidAppHelper.currentPackageName().equals(packageName);
                            if (packageName != null && locale != null && isActiveApp) {
                                Configuration oriConfiguration = (Configuration) param.getResult();
                                oriConfiguration.locale = locale;
                                Log.i(Common.TAG, "afterHookedMethod: Setting language of ".concat(packageName) + " (getConfiguration) to ".concat(locale.toString()));
                                param.setResult(oriConfiguration);
                            }
                        }
                    }
            );
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        prefs.reload();

        // Override the default Locale if one is defined (not res-related, here)
        Locale packageLocale = getPackageSpecificLocale(lpparam.packageName);
        if (packageLocale != null) {
            Locale.setDefault(packageLocale);
        }
    }

    @Nullable
    private static Locale getPackageSpecificLocale(String packageName) {
        String locale = prefs.getString(packageName, Common.DEFAULT_LOCALE);

        if (locale.contentEquals(Common.DEFAULT_LOCALE)) {
            return null;
        }

        String[] localeParts = locale.split("_", 3);
        String language = localeParts[0];
        String region = (localeParts.length >= 2) ? localeParts[1] : "";
        String variant = (localeParts.length >= 3) ? localeParts[2] : "";

        return new Locale(language, region, variant);
    }

    private static void loadPrefs() {
        prefs = new XSharedPreferences(Common.MY_PACKAGE_NAME, Common.PREFS);
    }
}
