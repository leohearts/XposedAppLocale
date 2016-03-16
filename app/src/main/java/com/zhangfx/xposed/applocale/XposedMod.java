package com.zhangfx.xposed.applocale;

import android.annotation.SuppressLint;
import android.app.AndroidAppHelper;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.os.Build;
import android.util.DisplayMetrics;

import java.util.Locale;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class XposedMod implements IXposedHookZygoteInit, IXposedHookLoadPackage {

    private static XSharedPreferences prefs;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        loadPrefs();

        try {
            findAndHookMethod(
                    Resources.class, "updateConfiguration",
                    Configuration.class, DisplayMetrics.class, "android.content.res.CompatibilityInfo",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (param.args[0] == null) {
                                return;
                            }

                            String packageName;
                            Resources res = ((Resources) param.thisObject);
                            if (res instanceof XResources) {
                                packageName = ((XResources) res).getPackageName();
                            } else if (res instanceof XModuleResources) {
                                return;
                            } else {
                                try {
                                    packageName = XResources.getPackageNameDuringConstruction();
                                } catch (IllegalStateException e) {
                                    return;
                                }
                            }

                            String hostPackageName = AndroidAppHelper.currentPackageName();
                            boolean isActiveApp = hostPackageName.equals(packageName);

                            Configuration newConfig = null;

                            if (packageName != null) {
                                Locale loc = getPackageSpecificLocale(packageName);
                                if (loc != null) {
                                    newConfig = new Configuration((Configuration) param.args[0]);

                                    setConfigurationLocale(newConfig, loc);

                                    if (isActiveApp) {
                                        Locale.setDefault(loc);
                                    }
                                }
                            }

                            if (newConfig != null) {
                                param.args[0] = newConfig;
                            }
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    @SuppressLint("NewApi")
    private void setConfigurationLocale(Configuration config, Locale loc) {
        config.locale = loc;

        if (Build.VERSION.SDK_INT >= 17) {
            // Don't use setLocale() in order not to trigger userSetLocale
            config.setLayoutDirection(loc);
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

    public static void loadPrefs() {
        prefs = new XSharedPreferences(Common.MY_PACKAGE_NAME, Common.PREFS);
        prefs.makeWorldReadable();
    }
}
