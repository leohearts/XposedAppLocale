package com.zhangfx.xposed.applocale;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.MenuItemCompat;
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
import java.util.List;

@SuppressLint("WorldReadableFiles")
public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    private static File prefsFile = new File(Environment.getDataDirectory(), String.format("data/%s/shared_prefs/%s.xml", Common.MY_PACKAGE_NAME, Common.PREFS));
    private SharedPreferences mPrefs;
    private ArrayList<String> languages;
    private boolean[] checkItems;
    private boolean[] tmpCheckItems;

    private MyAdapter myAdapter;
    private ArrayList<AppItem> appItemList;
    private PackageManager pm;

    private ProgressDialog mProgressDialog;
    private SearchView mSearchView;
    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        prefsFile.setReadable(true, false);
        mPrefs = getSharedPreferences(Common.PREFS, Context.MODE_WORLD_READABLE);

        languages = new ArrayList<>();
        LocaleList localeList = new LocaleList(getApplicationContext(), "");
        languages.addAll(localeList.getDescriptionList());
        languages.remove(0);

        checkItems = new boolean[languages.size()];
        String[] langs = mPrefs.getString("languages", "").split(",");
        for (int i = 0; i < langs.length; i++) {
            int index = languages.indexOf(localeList.getDescriptionList().get(localeList.getLocalePos(langs[i])));
            if (index > -1) {
                checkItems[index] = true;
            }
        }

        tmpCheckItems = Arrays.copyOf(checkItems, checkItems.length);

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addItemDecoration(new DividerDecoration(this));

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRecyclerView.scrollToPosition(0);
            }
        });

        pm = getPackageManager();
        List<PackageInfo> packages = pm.getInstalledPackages(0);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setMax(packages.size());
        mProgressDialog.setMessage(getString(R.string.loading_apps));
        mProgressDialog.setCancelable(false);

        new GetAppsTask().execute(packages);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem item = (MenuItem) menu.findItem(R.id.action_search);
        mSearchView = (SearchView) MenuItemCompat.getActionView(item);
        mSearchView.setOnQueryTextListener(this);

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
                                ArrayList<String> langs = new ArrayList<>();
                                for (int i = 0; i < checkItems.length; i++) {
                                    if (checkItems[i]) {
                                        langs.add(localeList.getLocaleCodes()[localeList.getDescriptionList().indexOf(languages.get(i))]);
                                    }
                                }

                                SharedPreferences.Editor prefsEditor = mPrefs.edit();
                                prefsEditor.putString("languages", TextUtils.join(",", langs));
                                prefsEditor.commit();
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        myAdapter.clear();

        ArrayList<AppItem> subAppItemList = new ArrayList<>();

        String query = newText.toLowerCase();

        for (AppItem appItem : appItemList) {
            if (appItem.getPackageInfo().packageName.toLowerCase().contains(query)
                    || appItem.getAppLabel().toLowerCase().contains(query)) {
                subAppItemList.add(appItem);
            }
        }

        myAdapter.addAll(subAppItemList);
        mRecyclerView.scrollToPosition(0);

        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    private class GetAppsTask extends AsyncTask<List<PackageInfo>, Integer, ArrayList<AppItem>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog.show();
        }

        @Override
        protected ArrayList<AppItem> doInBackground(List<PackageInfo>... params) {
            ArrayList<AppItem> appItems = new ArrayList<>();

            int i = 1;
            for (PackageInfo packageInfo : params[0]) {
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
        protected void onPostExecute(ArrayList<AppItem> appItems) {
            super.onPostExecute(appItems);
            mProgressDialog.dismiss();

            appItemList = appItems;

            Collections.sort(appItemList, new Comparator<AppItem>() {
                @Override
                public int compare(AppItem lhs, AppItem rhs) {
                    return lhs.getAppLabel().compareToIgnoreCase(rhs.getAppLabel());
                }
            });

            mRecyclerView.setAdapter(myAdapter = new MyAdapter(getApplicationContext(), pm, mPrefs, appItemList));
        }
    }
}
