package org.fanchuan.reactiontest;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity {

    private final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        Log.d(TAG, "Options menu inflated");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_help:
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.help_title);
                builder.setMessage(R.string.help_text);
                builder.setPositiveButton(android.R.string.ok, null);
                builder.show();
                break;
            case R.id.action_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        //User has started the test or the test is underway and is clicking as fast as possible
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            StatusAreaFragment statusAreaFragment = (StatusAreaFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_status_area);
            statusAreaFragment.reactionTimer.onTestControlEvent();
        }
        return true;
    }

    public static class StatusAreaFragment extends Fragment implements ReactionTimer.Callback {

        private final String TAG = StatusAreaFragment.class.getSimpleName();

        //Persistent items saved to shared preferences (best time, and settings)
        private ReactionTimer reactionTimer;
        private SharedPreferences prefs;
        private long bestTime;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            this.setRetainInstance(true);

            //Each state has its own unique text description
            this.reactionTimer = new ReactionTimer(this, getResources().getStringArray(R.array.state_descriptions));

            //Retrieve preferences (saved best reaction time)
            try {
                prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                bestTime = prefs.getLong(getResources().getString(R.string.keyBestTime), 10000); //default reaction time if not saved
                Log.d(TAG, "bestTime: " + bestTime);
                //We can't call showBestTime() yet because root View isn't inflated until onCreateView();
            } catch (Exception e) {
                Log.e(TAG, "Unable to get best time", e);
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            super.onCreateView(inflater, container, savedInstanceState);
            // Inflate the layout for this fragment
            return inflater.inflate(R.layout.fragment_status_area, container, false);
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            showBestTime();
        }

        @Override
        public void onDestroy() {
            /* If setRetainInstance (true), this isn't called when parent Activity is destroyed and recreated
            due to configuration change (orientation, etc.) but only when the Activity is destroyed for good
            (user quits app).
             */
            super.onDestroy();

            //Check to see if user wants to clear preferences
            try {
                if (prefs.getBoolean(getResources().getString(R.string.keyClearBest), false)) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.remove(getResources().getString(R.string.keyBestTime)); //clears best time here
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
                        editor.apply();
                    else
                        editor.commit();
                }
            } catch (Exception e) {
                Log.e(TAG, "Problem clearing preferences on exit", e);
            }
        }

        private void showBestTime() {
            /* Cannot call before root View is inflated
            Shows the best time stored in member var bestTime in the status area, after formatting.
            */
            try {
                TextView vw = (TextView) getView().findViewById(R.id.bestTime);
                String bestTimeText = String.format(getResources().getString(R.string.bestTime), bestTime);
                vw.setText(bestTimeText);
            } catch (Exception e) {
                Log.e(TAG, "Unable to display bestTime in status area", e);
            }
        }

        @Deprecated
        public void showEndTestNotification(String reactionTimeText) {
        /* If user checked show notification checkbox in settings, generates a sample notification, notifying of the reaction time.
        * Worked in several Genymotion API18 emulators and Moto Xoom tablet but threw Exception on LG API10 phone
        * my guess is because API10 doesn't support Jelly Bean style notifications and wants a .contentIntent() to launch
        * an activity from the notification.
        * Deprecated and not used as it was only to learn notifications and served no useful purpose in the app */
            String keyNotification = getResources().getString(R.string.keyNotification);
            boolean settingNotification = prefs.getBoolean(keyNotification, false);
            Log.d(TAG, keyNotification + ": " + String.valueOf(settingNotification));
            if (settingNotification) {
                try {
                    Activity parentActivity = getActivity();
                    NotificationCompat.Builder notifyReactionTimeBuilder = new NotificationCompat.Builder(parentActivity)
                            .setSmallIcon(android.R.drawable.status_bar_item_background)
                            .setContentText(reactionTimeText)
                            .setContentTitle(this.getString(R.string.app_name));
                    NotificationManager mNotificationManager = (NotificationManager) parentActivity.getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.notify(0, notifyReactionTimeBuilder.build());
                } catch (Exception e) {
                    Log.w(TAG, "Unable to display score notification", e);
                }
            }
        }

        public void submitLatestTime(long latestTime, String reactionTimeText) {
        /* Check latestTime versus bestTime, if so, update bestTime and persist in preferences
            Check Shared Preferences: does user wants a notification displayed?
         */
            if (latestTime < bestTime) {
                bestTime = latestTime;
                Log.d(TAG, "submitLatestTime: " + latestTime);
                try {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putLong(getResources().getString(R.string.keyBestTime), latestTime);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
                        editor.apply();
                    else
                        editor.commit();
                } catch (Exception e) {
                    Log.e(TAG, "Unable to save best time", e);
                }
                Log.i(TAG, "Committed to preferences latestTime: " + latestTime);
                showBestTime();
            }
            /* If notification setting is on, displays whether latestTime is bestTime or not
            * Removed as it served no useful purpose */
            //showEndTestNotification(reactionTimeText);
        }

        public synchronized void updateUiStatus(String statusText) {

            //Observer method: publish reaction test status to the status area
            try {
                final TextView VW_STATUS = (TextView) getView().findViewById(R.id.status);
                VW_STATUS.setText(statusText);
            } catch (Exception e) {
                Log.e(TAG, "Unable to update UI status");
            }
        }
    }

    public static class SettingsActivity extends PreferenceActivity {
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
                android.app.Fragment fragment = new MyPreferencesFragment();
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

}