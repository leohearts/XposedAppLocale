package com.zhangfx.xposed.applocale;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    private SharedPreferences mPrefs;
    private ArrayList<String> languages;
    private boolean[] checkItems;
    private boolean[] tmpCheckItems;

    private MyAdapter myAdapter;
    private ArrayList<AppItem> appItemList;
    private PackageManager pm;

    private SearchView mSearchView;
    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mPrefs = getSharedPreferences(Common.PREFS, MODE_WORLD_READABLE);

        languages = new ArrayList<>();
        LocaleList localeList = new LocaleList("");
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

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRecyclerView.scrollToPosition(0);
            }
        });

        pm = getPackageManager();
        List<PackageInfo> packages = pm.getInstalledPackages(0);

        appItemList = new ArrayList<>();
        for (PackageInfo packageInfo : packages) {
            if (packageInfo.applicationInfo.enabled) {
                appItemList.add(new AppItem(packageInfo));
            }
        }

        Collections.sort(appItemList, new Comparator<AppItem>() {
            @Override
            public int compare(AppItem lhs, AppItem rhs) {
                return lhs.getPackageInfo().packageName.compareToIgnoreCase(rhs.getPackageInfo().packageName);
            }
        });

        mRecyclerView.setAdapter(myAdapter = new MyAdapter(pm, mPrefs, appItemList));

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addItemDecoration(new DividerDecoration(this));
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

                                LocaleList localeList = new LocaleList("");
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
                    || pm.getApplicationLabel(appItem.getApplicationInfo()).toString().toLowerCase().contains(query)) {
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
}
