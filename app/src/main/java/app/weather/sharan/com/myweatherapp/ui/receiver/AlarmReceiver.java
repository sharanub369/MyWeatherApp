package app.weather.sharan.com.myweatherapp.ui.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Date;

import app.weather.sharan.com.myweatherapp.ui.task.LongTermWeatherForReceiverTask;
import app.weather.sharan.com.myweatherapp.ui.task.TodayWeatherForReceiverTask;
import app.weather.sharan.com.myweatherapp.ui.util.Constants;
import app.weather.sharan.com.myweatherapp.ui.util.NetworkState;

/**
 * Created by sharana.b on 9/20/2016.
 */
public class AlarmReceiver extends BroadcastReceiver {

    private static final int REQUEST_CODE = 896523;

    private Context context;
    private SharedPreferences sp;
    private NetworkState networkState;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        networkState = new NetworkState(context);
        try {
            sp = PreferenceManager.getDefaultSharedPreferences(context);
            if (intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {
                String timeInterval = sp.getString(Constants.KEY_REFRESH_INTERVAL, "1");
                if (!timeInterval.equalsIgnoreCase("0")) {
                    setRecurringAlarm(context);
                    if (networkState.isNetworkAvailable()) {
                        getWeatherReport();
                    }
                }
            } else if (ConnectivityManager.CONNECTIVITY_ACTION.equalsIgnoreCase(intent.getAction())) {
                String timeInterval = sp.getString(Constants.KEY_REFRESH_INTERVAL, "1");
                if (!timeInterval.equalsIgnoreCase("0")) {
                    getWeatherReport();
                }
            } else {
                getWeatherReport();
            }
        } catch (Exception exp) {

        }
    }

    public static void setRecurringAlarm(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String intervalTime = sp.getString(Constants.KEY_REFRESH_INTERVAL, "1");
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        long intervalTimeInMillis = getIntervalTimeInMillis(intervalTime);
        if (intervalTimeInMillis <= 0) {
            //Cancel alarm
            alarmManager.cancel(pendingIntent);
        } else {
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + intervalTimeInMillis, intervalTimeInMillis, pendingIntent);
        }
    }

    private static long getIntervalTimeInMillis(String intervalTime) {
        int interval = Integer.parseInt(intervalTime);
        switch (interval) {
            case 0:
                return 0;
            case 15:
                return AlarmManager.INTERVAL_FIFTEEN_MINUTES;
            case 30:
                return AlarmManager.INTERVAL_HALF_HOUR;
            case 1:
                return AlarmManager.INTERVAL_HOUR;
            case 12:
                return AlarmManager.INTERVAL_HALF_DAY;
            case 24:
                return AlarmManager.INTERVAL_DAY;
            default:
                return interval * 3600000;
        }
    }

    public void getWeatherReport() {
        boolean failed = false;
        if (networkState.isNetworkAvailable()) {
            new TodayWeatherForReceiverTask(context).execute();
            new LongTermWeatherForReceiverTask(context).execute();
        } else {
            failed = true;
        }
        sp.edit().putBoolean(Constants.KEY_BG_REFRESH_FAIL, failed).apply();
    }
}
