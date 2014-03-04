package org.fanchuan.reactiontest;

import android.os.Handler;
import android.os.SystemClock;

import java.util.Random;

class ReactionTimer {

    final short STATE_IDLE = 0; // Idle, waiting for user to press button
    private short activityState = STATE_IDLE;
    final short STATE_DELAY = 1; // User pressed begin test button, random delay
    final short STATE_TESTING = 2; // Reaction timer is running, waiting for button press
    final short STATE_FINISHED = 3; // Congratulate user
    String[] stateDescriptions;
    Callback observer;
    final Runnable BEGIN_TEST = new Runnable() {
        public void run() {
            activityState = STATE_TESTING;
            timeTestStart = SystemClock.elapsedRealtime();
            handlerTest.post(UPDATE_UI_STATUS);
        }
    };
    final Runnable UPDATE_UI_STATUS = new Runnable() {
        public void run() {
            observer.updateUiStatus(stateDescriptions[activityState]);
        }
    };

    private long timeTestStart = 0;
    private Handler handlerTest = new Handler(); //must maintain handler state, used for reaction timing
    private Random randTimer = new Random();


    ReactionTimer(Callback observer, String[] stateDescriptions) {
        this.observer = observer;
        this.stateDescriptions = stateDescriptions;
        handlerTest.post(UPDATE_UI_STATUS);
    }

    void onTestControlEvent() {
        //User has activated a widget for the test to proceed
        final int baseDelayMs = 1000;
        final int delayMultiplierMs = 1500;
        final Runnable END_TEST = new

                Runnable() {
                    public void run() {
                        activityState = STATE_FINISHED;
                        long timeElapsed = SystemClock.elapsedRealtime() - timeTestStart;
                        String reactionTimeText = String.format(stateDescriptions[STATE_FINISHED], timeElapsed);
                        observer.submitLatestTime(timeElapsed, reactionTimeText);
                        observer.updateUiStatus(reactionTimeText);
                    }
                };

        switch (activityState) {
            case STATE_IDLE:
                activityState = STATE_DELAY;
                handlerTest.post(UPDATE_UI_STATUS);
                int flagDelay_ms = Math.round(delayMultiplierMs * randTimer.nextFloat() + baseDelayMs);
                handlerTest.postDelayed(BEGIN_TEST, flagDelay_ms);
                break;
            case STATE_DELAY:
                //If user clicks during the delay period, that resets the test.
                handlerTest.removeCallbacksAndMessages(null);
                activityState = STATE_IDLE;
                handlerTest.post(UPDATE_UI_STATUS);
                break;
            case STATE_TESTING:
                //Reaction testing in progress
                handlerTest.post(END_TEST);
                break;
            case STATE_FINISHED:
                activityState = STATE_IDLE;
                handlerTest.post(UPDATE_UI_STATUS);
                break;
        }
    }

    interface Callback {
        //the "hosting" class must implement this interface to receive UI callbacks.
        void submitLatestTime(long timeElapsed, String reactionTimeText);

        void updateUiStatus(String statusText);
    }
}

