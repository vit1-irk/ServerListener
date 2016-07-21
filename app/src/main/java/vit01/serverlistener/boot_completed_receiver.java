package vit01.serverlistener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class boot_completed_receiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            Intent alarmStart = new Intent(context, AlarmService.class);
            context.startService(alarmStart);
        }
    }
}