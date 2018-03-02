package com.flo354.xposed.applocale;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

@SuppressLint({"WorldReadableFiles", "SetWorldReadable"})
public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    private static File PREFS_FILE = new File(Environment.getDataDirectory(), String.format("data/%s/shared_prefs/%s.xml", Common.MY_PACKAGE_NAME, Common.PREFS));

    private SharedPreferences mPrefs;

    private List<String> languages;

    private List<AppItem> appItemList;

    private boolean[] checkItems;

    private boolean[] tmpCheckItems;

    private RecyclerView mRecyclerView;


    private String filterQuery;

    private boolean showSystemApps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        PREFS_FILE.setReadable(true, false);
        mPrefs = getSharedPreferences(Common.PREFS, Context.MODE_WORLD_READABLE);

        filterQuery = "";

        languages = new LinkedList<>();
        LocaleList localeList = new LocaleList(getApplicationContext(), "");
        languages.addAll(localeList.getDescriptionList());
        languages.remove(0);

        appItemList = new LinkedList<>();

        checkItems = new boolean[languages.size()];
        String[] languages = mPrefs.getString("languages", "").split(",");
        for (String lang : languages) {
            int index = this.languages.indexOf(localeList.getDescriptionList().get(localeList.getLocalePos(lang)));
            if (index > -1) {
                checkItems[index] = true;
            }
        }

        tmpCheckItems = Arrays.copyOf(checkItems, checkItems.length);

        mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addItemDecoration(new DividerDecoration(this));
        mRecyclerView.setAdapter(new MyAdapter(this));

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRecyclerView.scrollToPosition(0);
            }
        });

        new GetAppsTask(this, (MyAdapter) mRecyclerView.getAdapter(), appItemList).execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        ((SearchView) menu.findItem(R.id.action_search).getActionView()).setOnQueryTextListener(this);
        menu.findItem(R.id.action_show_system_apps).setChecked(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_language:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.choose_languages)
                        .setMultiChoiceItems(languages.toArray(new String[languages.size()]), tmpCheckItems, new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                tmpCheckItems[which] = isChecked;
                            }
                        })
                        .setPositiveButton(R.string.choose_languages_save, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                checkItems = Arrays.copyOf(tmpCheckItems, tmpCheckItems.length);

                                LocaleList localeList = new LocaleList(getApplicationContext(), "");
                                ArrayList<String> languages = new ArrayList<>();
                                for (int i = 0; i < checkItems.length; i++) {
                                    if (checkItems[i]) {
                                        languages.add(localeList.getLocaleCodes()[localeList.getDescriptionList().indexOf(MainActivity.this.languages.get(i))]);
                                    }
                                }

                                SharedPreferences.Editor prefsEditor = mPrefs.edit();
                                prefsEditor.putString("languages", TextUtils.join(",", languages));
                                prefsEditor.apply();
                            }
                        })
                        .setNegativeButton(R.string.choose_languages_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                tmpCheckItems = Arrays.copyOf(checkItems, checkItems.length);
                            }
                        })
                        .create().show();
                return true;
            case R.id.action_show_system_apps:
                item.setChecked(!item.isChecked());
                showSystemApps = item.isChecked();
                filterApps();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        filterQuery = newText.toLowerCase();
        filterApps();
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    private void filterApps() {
        List<AppItem> subAppItemList = new LinkedList<>();

        for (AppItem appItem : appItemList) {
            if (appItem.getPackageInfo().packageName.toLowerCase().contains(filterQuery)
                    || appItem.getAppLabel().toLowerCase().contains(filterQuery)) {
                subAppItemList.add(appItem);
            }
        }

        if (!showSystemApps) {
            List<AppItem> subAppItemList2 = new LinkedList<>(subAppItemList);
            for (AppItem appItem: subAppItemList2) {
                if ((appItem.getApplicationInfo().flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                    subAppItemList.remove(appItem);
                }
            }
        }

        ((MyAdapter) mRecyclerView.getAdapter()).clear();
        ((MyAdapter) mRecyclerView.getAdapter()).addAll(subAppItemList);
        mRecyclerView.scrollToPosition(0);
    }

    private static class GetAppsTask extends AsyncTask<Void, Integer, List<AppItem>> {

        private final PackageManager pm;

        private final ProgressDialog mProgressDialog;

        private final MyAdapter adapter;

        private final List<AppItem> appItemList;

        private GetAppsTask(Context context, MyAdapter adapter, List<AppItem> appItemList) {
            pm = context.getPackageManager();
            this.adapter = adapter;
            this.appItemList = appItemList;

            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setMessage(context.getString(R.string.loading_apps));
            mProgressDialog.setCancelable(false);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog.show();
        }

        @Override
        protected List<AppItem> doInBackground(Void... params) {
            List<AppItem> appItems = new ArrayList<>();
            List<PackageInfo> packages = pm.getInstalledPackages(0);
            mProgressDialog.setMax(packages.size());

            int i = 1;
            for (PackageInfo packageInfo : pm.getInstalledPackages(0)) {
                if (packageInfo.applicationInfo.enabled) {
                    appItems.add(new AppItem(packageInfo, pm.getApplicationLabel(packageInfo.applicationInfo).toString()));
                }

                publishProgress(i++);
            }

            return appItems;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            mProgressDialog.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(List<AppItem> appItems) {
            super.onPostExecute(appItems);

            Collections.sort(appItems, new Comparator<AppItem>() {
                @Override
                public int compare(AppItem lhs, AppItem rhs) {
                    return lhs.getAppLabel().compareToIgnoreCase(rhs.getAppLabel());
                }
            });

            adapter.addAll(appItems);
            appItemList.addAll(appItems);
            mProgressDialog.dismiss();
        }
    }
}
