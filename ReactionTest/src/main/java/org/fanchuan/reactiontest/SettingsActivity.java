package org.fanchuan.reactiontest;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SettingsActivity extends PreferenceActivity {
    final String TAG = SettingsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            showPreferencesPreHoneycomb();
        } else {
            showPreferencesFragmentStyle(savedInstanceState);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void showPreferencesFragmentStyle(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            FragmentTransaction transaction = getFragmentManager()
                    .beginTransaction();
            Fragment fragment = new MyPreferencesFragment();
            transaction.replace(android.R.id.content, fragment);
            transaction.commit();
        }

    }

    @SuppressWarnings("deprecation")
    private void showPreferencesPreHoneycomb() {
        Log.d("TAG", "Build.VERSION.SDK_INT: " + Integer.toString(Build.VERSION.SDK_INT));
        addPreferencesFromResource(R.xml.preferences);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class MyPreferencesFragment extends PreferenceFragment {
        final String TAG = MyPreferencesFragment.class.getSimpleName();

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            Log.d(TAG, "Attached to activity: " + activity.getClass().getSimpleName());
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            this.addPreferencesFromResource(R.xml.preferences);
            return super.onCreateView(inflater, container, savedInstanceState);
        }
    }
}
