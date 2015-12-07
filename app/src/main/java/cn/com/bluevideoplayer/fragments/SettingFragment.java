package cn.com.bluevideoplayer.fragments;

import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;

import cn.com.bluevideoplayer.R;
import cn.com.bluevideoplayer.util.SpManager;
import cn.com.bluevideoplayer.util.ToastManager;

public class SettingFragment extends PreferenceFragment {
    Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this.getActivity();
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        Preference preference0 = findPreference("clear");

        preference0
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {

                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        SpManager.clear(mContext);
                        ToastManager.show(mContext, "清除完成");
                        return false;
                    }
                });
    }

}
