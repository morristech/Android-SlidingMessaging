package com.klinker.android.messaging_sliding.slide_over;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import com.klinker.android.messaging_donate.R;

public class SlideOverSettings  extends PreferenceActivity {

    public static Context context;
    public SharedPreferences sharedPrefs;

    public boolean showAll;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.slideover_settings);
        setTitle(R.string.slide_over);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        showAll = sharedPrefs.getBoolean("show_advanced_settings", false);

        Preference side = findPreference("slideover_side");
        side.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                restartHalo();
                return true;
            }
        });

        Preference sliver = findPreference("slideover_sliver");
        sliver.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                restartHalo();
                return true;
            }
        });

        Preference alignment = findPreference("slideover_vertical");
        alignment.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                restartHalo();
                return true;
            }
        });

        Preference activation = findPreference("slideover_activation");
        activation.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                restartHalo();
                return true;
            }
        });

        Preference breakPoint = findPreference("slideover_break_point");
        breakPoint.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                restartHalo();
                return true;
            }
        });

        Preference haptic = findPreference("slideover_haptic_feedback");
        haptic.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                restartHalo();
                return true;
            }
        });

        if (!showAll) {
            getPreferenceScreen().removePreference(findPreference("slideover_break_point"));
            getPreferenceScreen().removePreference(findPreference("slideover_secondary_action"));
            getPreferenceScreen().removePreference(findPreference("slideover_haptic_feedback"));
        }
    }

    public void restartHalo() {
        Intent service = new Intent();
        service.setAction("com.klinker.android.messaging.STOP_HALO");
        sendBroadcast(service);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (sharedPrefs.getBoolean("slideover_enabled", false)) {
                    Intent service = new Intent(getApplicationContext(), com.klinker.android.messaging_sliding.slide_over.SlideOverService.class);
                    startService(service);
                }
            }
        }, 500);
    }
}