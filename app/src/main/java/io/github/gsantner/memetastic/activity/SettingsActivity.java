package io.github.gsantner.memetastic.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import net.gsantner.opoc.preference.GsPreferenceFragmentCompat;
import net.gsantner.opoc.util.AppSettingsBase;

import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.gsantner.memetastic.R;
import io.github.gsantner.memetastic.service.AssetUpdater;
import io.github.gsantner.memetastic.service.ThumbnailCleanupTask;
import io.github.gsantner.memetastic.util.AppSettings;
import io.github.gsantner.memetastic.util.PermissionChecker;

public class SettingsActivity extends AppCompatActivity {
    static final int ACTIVITY_ID = 10;

    static class RESULT {
        static final int NOCHANGE = -1;
        static final int CHANGE = 1;
        static final int CHANGE_RESTART = 2;
    }

    @BindView(R.id.settings__appbar)
    protected AppBarLayout appBarLayout;
    @BindView(R.id.settings__toolbar)
    protected Toolbar toolbar;

    private AppSettings appSettings;
    public static int activityRetVal = RESULT.NOCHANGE;

    public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.settings__activity);
        ButterKnife.bind(this);
        toolbar.setTitle(R.string.settings__settings);
        setSupportActionBar(toolbar);
        appSettings = AppSettings.get();
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_white_48px));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SettingsActivity.this.onBackPressed();
            }
        });
        activityRetVal = RESULT.NOCHANGE;
        showFragment(SettingsFragmentMaster.TAG, false);
    }

    protected void showFragment(String tag, boolean addToBackStack) {
        GsPreferenceFragmentCompat fragment = (GsPreferenceFragmentCompat) getSupportFragmentManager().findFragmentByTag(tag);
        if (fragment == null) {
            switch (tag) {
                case SettingsFragmentMaster.TAG:
                default:
                    fragment = new SettingsFragmentMaster();
                    toolbar.setTitle(R.string.settings__settings);
                    break;
            }
        }
        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        if (addToBackStack) {
            t.addToBackStack(tag);
        }
        t.replace(R.id.settings__fragment_container, fragment, tag).commit();
    }

    @Override
    protected void onStop() {
        setResult(activityRetVal);
        super.onStop();
    }

    public static class SettingsFragmentMaster extends GsPreferenceFragmentCompat {
        public static final String TAG = "SettingsFragmentMaster";

        @Override
        protected void onPreferenceChanged(SharedPreferences prefs, String key) {
            if (activityRetVal == RESULT.NOCHANGE) {
                activityRetVal = RESULT.CHANGE;
            }
        }

        @Override
        public int getPreferenceResourceForInflation() {
            return R.xml.preferences_master;
        }

        @Override
        public String getFragmentTag() {
            return TAG;
        }

        @Override
        protected AppSettingsBase getAppSettings(Context context) {
            return new AppSettings(context);
        }


        @SuppressLint("ApplySharedPref")
        @Override
        public Boolean onPreferenceClicked(android.support.v7.preference.Preference preference) {
            if (isAdded() && preference.hasKey()) {
                Context context = getActivity();
                AppSettings settings = AppSettings.get();
                String key = preference.getKey();


                if (eq(key, R.string.pref_key__memelist_view_type)) {

                    activityRetVal = RESULT.CHANGE_RESTART;
                }
                if (eq(key, R.string.pref_key__cleanup_thumbnails)) {
                    new ThumbnailCleanupTask(context).start();
                    return true;
                }
                if (eq(key, R.string.pref_key__is_overview_statusbar_hidden)) {
                    activityRetVal = RESULT.CHANGE_RESTART;
                }
                if (eq(key, R.string.pref_key__language)){
                    activityRetVal = RESULT.CHANGE_RESTART;
                }
                if (eq(key, R.string.pref_key__download_assets_try)) {
                    if (PermissionChecker.doIfPermissionGranted(getActivity())) {
                        Date zero = new Date(0);
                        settings.setLastArchiveCheckDate(zero);
                        settings.setLastArchiveDate(zero);
                        settings.getDefaultPreferences().edit().commit();
                        new AssetUpdater.UpdateThread(context, true).start();
                        getActivity().finish();
                    }
                }
            }
            return null;
        }
    }
}
