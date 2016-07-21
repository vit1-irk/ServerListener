package vit01.serverlistener;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.widget.Toast;

public class AlarmService extends Service {
    AlarmManager alarmManager;
    Intent jobIntent;
    PendingIntent jobPendingIntent;
    SharedPreferences preferenceManager;
    int fireDuration;
    boolean shouldStart;

    @Override
    public void onCreate() {
        super.onCreate();
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        jobIntent = new Intent(this, workerJob.class);
        preferenceManager = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        shouldStart = preferenceManager.getBoolean("job_enable", false);
        fireDuration = preferenceManager.getInt("update_data_interval", 10);

        if (shouldStart) {
            startAlarm();
        } else {
            stopAlarm();
        }
        return START_NOT_STICKY;
    }

    public void startAlarm() {
        jobPendingIntent = PendingIntent.getBroadcast(
                this.getApplicationContext(),
                12309,
                jobIntent,
                0);

        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 1000 * 60, // запустится после одной минуты
                1000 * 60 * fireDuration, // интервал обновления
                jobPendingIntent);
        Toast.makeText(AlarmService.this, "ServerListener (пере)запускается", Toast.LENGTH_SHORT).show();
    }

    public void stopAlarm() {
        alarmManager.cancel(jobPendingIntent);
    }
}