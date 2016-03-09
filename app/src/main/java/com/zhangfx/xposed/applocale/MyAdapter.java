package com.zhangfx.xposed.applocale;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

    private ArrayList<AppItem> appItemList = new ArrayList<>();
    private static Context context;
    private static PackageManager pm;
    private static SharedPreferences prefs;
    private static LocaleList localeList;

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
        localeList = new LocaleList("");

        ViewHolder viewHolder = new ViewHolder(LayoutInflater.from(context).inflate(R.layout.app_item, parent, false));

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        AppItem app = getItem(position);

        holder.appIcon.setImageDrawable(pm.getApplicationIcon(app.getApplicationInfo()));
        holder.appLabel.setText((String) pm.getApplicationLabel(app.getApplicationInfo()));
        holder.appPackage.setText(app.getPackageInfo().packageName);
        holder.appLocale.setTag(app.getApplicationInfo().packageName);

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_activated_1, localeList.getLocaleCodes());
        holder.appLocale.setAdapter(dataAdapter);
        holder.appLocale.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String packageName = (String) parent.getTag();
                String currentLocale = prefs.getString(packageName, Common.DEFAULT_LOCALE);

                SharedPreferences.Editor editor = prefs.edit();
                if (position == 0) {
                    if (!currentLocale.contentEquals(Common.DEFAULT_LOCALE)) {
                        editor.remove(packageName);
                        editor.commit();
                    }
                } else {
                    editor.putString(packageName, localeList.getLocaleCodes()[position]);
                    editor.commit();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        String currentLocale = prefs.getString(app.getPackageInfo().packageName, Common.DEFAULT_LOCALE);
        holder.appLocale.setSelection(localeList.getLocalePos(currentLocale));
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
        Spinner appLocale;

        public ViewHolder(View view) {
            super(view);

            appIcon = (ImageView) view.findViewById(R.id.app_icon);
            appLabel = (TextView) view.findViewById(R.id.app_label);
            appPackage = (TextView) view.findViewById(R.id.app_package);
            appLocale = (Spinner) view.findViewById(R.id.app_locale);
        }
    }
}
