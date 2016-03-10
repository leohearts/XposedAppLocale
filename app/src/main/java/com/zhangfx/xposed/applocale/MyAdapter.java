package com.zhangfx.xposed.applocale;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

    private ArrayList<AppItem> appItemList = new ArrayList<>();
    private static Context context;
    private static PackageManager pm;
    private static SharedPreferences prefs;

    public MyAdapter(PackageManager pm, SharedPreferences prefs, ArrayList<AppItem> appItemList) {
        setHasStableIds(true);

        this.pm = pm;
        this.prefs = prefs;
        addAll(appItemList);
    }

    public void add(AppItem appItem) {
        appItemList.add(appItem);
        notifyDataSetChanged();
    }

    public void addAll(Collection<? extends AppItem> collection) {
        if (collection != null) {
            appItemList.addAll(collection);
            notifyDataSetChanged();
        }
    }

    public void addAll(AppItem... appItems) {
        addAll(Arrays.asList(appItems));
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();

        ViewHolder viewHolder = new ViewHolder(LayoutInflater.from(context).inflate(R.layout.app_item, parent, false));

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final AppItem app = getItem(position);

        holder.appIcon.setImageDrawable(pm.getApplicationIcon(app.getApplicationInfo()));
        holder.appLabel.setText((String) pm.getApplicationLabel(app.getApplicationInfo()));
        holder.appPackage.setText(app.getPackageInfo().packageName);
        holder.appLocale.setText(prefs.getString(app.getPackageInfo().packageName, Common.DEFAULT_LOCALE));
        holder.appLocale.setTag(app.getPackageInfo().packageName);

        holder.appLocale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                String currentLocale = prefs.getString(app.getPackageInfo().packageName, Common.DEFAULT_LOCALE);
                String[] defaultLocales = new LocaleList("").getLocaleCodes();
                String langs = prefs.getString("languages", "");
                if (!langs.isEmpty()) {
                    if (!currentLocale.contentEquals(Common.DEFAULT_LOCALE) && !langs.contains(currentLocale)) {
                        langs = currentLocale + "," + langs;
                    }

                    langs = Common.DEFAULT_LOCALE + "," + langs;

                    defaultLocales = langs.split(",");
                }

                final String[] locales = defaultLocales;

                int index = Arrays.asList(defaultLocales).indexOf(currentLocale);

                new AlertDialog.Builder(context)
                        .setTitle((String) pm.getApplicationLabel(app.getApplicationInfo()))
                        .setSingleChoiceItems(locales, index, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                TextView appLocale = (TextView) v;
                                appLocale.setText(locales[which]);

                                String packageName = (String) v.getTag();
                                String currentLocale = prefs.getString(packageName, Common.DEFAULT_LOCALE);

                                SharedPreferences.Editor prefsEditor = prefs.edit();
                                if (which == 0) {
                                    if (!currentLocale.contentEquals(Common.DEFAULT_LOCALE)) {
                                        prefsEditor.remove(packageName);
                                        prefsEditor.commit();
                                    }
                                } else {
                                    prefsEditor.putString(packageName, locales[which]);
                                    prefsEditor.commit();
                                }
                            }
                        })
                        .setNegativeButton(R.string.choose_languages_cancel, null)
                        .create().show();
            }
        });
    }

    public void clear() {
        appItemList.clear();
        notifyDataSetChanged();
    }

    public AppItem getItem(int position) {
        return appItemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).hashCode();
    }

    @Override
    public int getItemCount() {
        return appItemList == null? 0 : appItemList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appLabel;
        TextView appPackage;
        TextView appLocale;

        public ViewHolder(View view) {
            super(view);

            appIcon = (ImageView) view.findViewById(R.id.app_icon);
            appLabel = (TextView) view.findViewById(R.id.app_label);
            appPackage = (TextView) view.findViewById(R.id.app_package);
            appLocale = (TextView) view.findViewById(R.id.app_locale);
        }
    }
}
