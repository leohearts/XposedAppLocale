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

    public MyAdapter(Context context, PackageManager pm, SharedPreferences prefs, ArrayList<AppItem> appItemList) {
        setHasStableIds(true);

        this.context = context;
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
        holder.appLabel.setText(app.getAppLabel());
        holder.appPackage.setText(app.getPackageInfo().packageName);

        String appLocale = prefs.getString(app.getPackageInfo().packageName, Common.DEFAULT_LOCALE);
        holder.appLocale.setText(appLocale);
        if (appLocale.equals(Common.DEFAULT_LOCALE)) {
            holder.appLocale.setTextColor(context.getResources().getColor(R.color.colorPrimary));
        } else {
            holder.appLocale.setTextColor(context.getResources().getColor(R.color.colorAccent));
        }

        holder.itemView.setTag(app);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final AppItem app = (AppItem) v.getTag();
                String currentLocale = prefs.getString(app.getPackageInfo().packageName, Common.DEFAULT_LOCALE);
                String[] defaultLocales = new LocaleList(context, "").getLocaleCodes();
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
                                TextView appLocale = (TextView) v.findViewById(R.id.app_locale);
                                appLocale.setText(locales[which]);

                                String packageName = app.getPackageInfo().packageName;
                                String currentLocale = prefs.getString(packageName, Common.DEFAULT_LOCALE);

                                SharedPreferences.Editor prefsEditor = prefs.edit();
                                if (which == 0) {
                                    if (!currentLocale.contentEquals(Common.DEFAULT_LOCALE)) {
                                        prefsEditor.remove(packageName);
                                        prefsEditor.commit();
                                    }
                                    appLocale.setTextColor(context.getResources().getColor(R.color.colorPrimary));
                                } else {
                                    prefsEditor.putString(packageName, locales[which]);
                                    prefsEditor.commit();
                                    appLocale.setTextColor(context.getResources().getColor(R.color.colorAccent));
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

            appIcon = view.findViewById(R.id.app_icon);
            appLabel = view.findViewById(R.id.app_label);
            appPackage = view.findViewById(R.id.app_package);
            appLocale = view.findViewById(R.id.app_locale);
        }
    }
}
