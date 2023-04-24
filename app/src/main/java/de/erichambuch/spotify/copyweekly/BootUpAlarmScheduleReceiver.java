package de.erichambuch.spotify.copyweekly;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * BroadcastRegister der beim Start des Mobiltelefons ausgeführt wird.
 */
public class BootUpAlarmScheduleReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "de.erichambuch.spotifyupdate";
    private static final int NOTIFICATION_ID = CHANNEL_ID.hashCode();

    public static class MyNotificationPublisher extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, Intent intent) {
            if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(AppInfo.PREFS_NOTIFY_ME, false)) {
                final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                Notification.Builder builder;
                // Notication Channel ab Android 8
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, context.getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription(context.getString(R.string.app_name));
                notificationManager.createNotificationChannel(channel);
                builder = new Notification.Builder(context, CHANNEL_ID);
                builder
                        .setContentTitle(context.getString(R.string.notification_title))
                        .setContentText(context.getString(R.string.notification_text))
                        .setAutoCancel(true)
                        .setSmallIcon(R.mipmap.ic_launcher);
                final Intent callIntent = new Intent(context, MainActivity.class);
                final PendingIntent activity = PendingIntent.getActivity(context, NOTIFICATION_ID, callIntent, PendingIntent.FLAG_CANCEL_CURRENT  | PendingIntent.FLAG_IMMUTABLE);
                builder.setContentIntent(activity);
                final Notification notification = builder.build();
                notificationManager.notify(NOTIFICATION_ID, notification);
            }
        }
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        // just make sure we are getting the right intent (better safe than sorry)
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            registerNotificationAlarm(context);
        }
    }

    static void registerNotificationAlarm(Context context) {
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(AppInfo.PREFS_NOTIFY_ME, false)) {
            Intent notificationIntent = new Intent(context, MyNotificationPublisher.class);
            PendingIntent
                pendingIntent = PendingIntent.getBroadcast(context, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT |
                        (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0));
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 19);
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY); // Sonntags 19:00
            long millis = calendar.getTimeInMillis();
            if ( millis < System.currentTimeMillis() ) // is last week
                millis += 7*24*3600*1000; // 7 days
            String date = DateFormat.getDateInstance().format(new Date(millis));
            Log.i(AppInfo.APP_NAME, "Scheduled for "+date);
            // und alle 7 Tage...
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, millis, 7 * 24 * 3600 * 1000, pendingIntent);
        }
    }
}
