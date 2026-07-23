package name.monwf.customiuizer.mods.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import java.util.ArrayList;

public class StepCounterController {
    private static final ArrayList<TextView> stepViewList = new ArrayList<TextView>();
    private static Handler mHandler;
    private static Runnable updateStepsRunnable;
    private static String stepsWithGoal;
    private static Context sContext;
    private static BroadcastReceiver sTimeTickReceiver;

    public static void updateSteps(Context context) {
        if (stepViewList.isEmpty() || context == null) return;
        Uri uri = Uri.parse("content://com.mi.health.provider.main/activity/steps/brief");
        try {
            Cursor cursor = context.getContentResolver().query(uri, new String[]{"steps","goal"}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                String stepCount = cursor.getString(0);
                String stepGoal = cursor.getString(1);
                cursor.close();
                String newText = stepCount + "/" + stepGoal;
                if (newText.equals(stepsWithGoal)) {
                    return;
                }
                stepsWithGoal = newText;
                for (TextView tv:stepViewList) {
                    tv.setText(newText);
                }
            }
        } catch (Throwable t) {
            XposedHelpers.log(t);
        }
    }

    public static void initContext(Context context) {
        if (sContext != null && sTimeTickReceiver != null) {
            try {
                sContext.unregisterReceiver(sTimeTickReceiver);
            } catch (Throwable ignored) {}
            if (mHandler != null && updateStepsRunnable != null) {
                mHandler.removeCallbacks(updateStepsRunnable);
            }
        }
        sContext = context.getApplicationContext();
        sTimeTickReceiver = new BroadcastReceiver() {
            public void onReceive(final Context context, Intent intent) {
                updateSteps(sContext);
            }
        };
        sContext.registerReceiver(sTimeTickReceiver, new IntentFilter("android.intent.action.TIME_TICK"));
        Looper looper = Looper.myLooper();
        if (looper == null) looper = Looper.getMainLooper();
        if (mHandler == null) mHandler = new Handler(looper);
        updateStepsRunnable = new Runnable() {
            @Override
            public void run() {
                updateSteps(sContext);
            }
        };
    }

    public static void removeStepViewByTag(String tag) {
        TextView toRemove = null;
        for (TextView tv : stepViewList) {
            if (tag.equals(tv.getTag())) {
                toRemove = tv;
                break;
            }
        }
        if (toRemove != null) stepViewList.remove(toRemove);
        if (stepViewList.isEmpty() && mHandler != null && updateStepsRunnable != null) {
            mHandler.removeCallbacks(updateStepsRunnable);
        }
    }

    public static void addStepView(TextView sv) {
        if (sContext == null || sv == null) return;
        for (TextView tv : stepViewList) {
            if (sv == tv) return;
        }
        stepViewList.add(sv);
        if (mHandler != null && updateStepsRunnable != null) {
            mHandler.removeCallbacks(updateStepsRunnable);
            mHandler.postDelayed(updateStepsRunnable, 3000L);
        }
    }
}
