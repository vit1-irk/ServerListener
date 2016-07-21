package vit01.serverlistener;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;

public class workerJob extends BroadcastReceiver {
    private final static AtomicInteger c = new AtomicInteger(0);
    long[] vibrate_pattern = {1000, 1000};
    boolean vibrate = false;
    SharedPreferences prefManager;
    String appName = "ServerListener";
    String custom_intent = "null";

    public static int getNotificationID() {
        return c.incrementAndGet();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .permitAll().build();
        StrictMode.setThreadPolicy(policy);

        prefManager = PreferenceManager.getDefaultSharedPreferences(context);
        vibrate = prefManager.getBoolean("vibrate", true);
        custom_intent = prefManager.getString("custom_intent_action", "null");

        String address = prefManager.getString("server_adress", "NULL");
        String errors_behaviour = prefManager.getString("errors_behaviour", "do_nothing");
        boolean report_errors = (errors_behaviour.equals("send_notify"));

        if (address.equals("NULL")) {
            if (report_errors)
                Show_Notification(context, "ServerListener", "Что-то не так с адресом сервера!", false);
            return;
        }

        int data_exchange_type = prefManager.getInt("data_exchange_type", 0);
        switch (data_exchange_type) {
            case 0:
                pass_to_JSON_api(context, address, prefManager, report_errors);
                break;
            case 1:
                pass_to_xc_api(context, address, prefManager, report_errors);
                break;
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void Show_Notification(Context context, String title, String text, boolean big_text) {
        android.support.v4.app.NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                        .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                        .setContentTitle(title)
                        .setContentText(text);

        if (vibrate) {
            mBuilder.setVibrate(vibrate_pattern);
        }

        if (big_text) {
            mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(text));
        }

        Intent resultIntent;

        if (!custom_intent.equals("null")) {
            try {
                resultIntent = Intent.parseUri(custom_intent, 0);
            } catch (URISyntaxException e) {
                Toast.makeText(context, "Не могу запустить выбранное приложение", Toast.LENGTH_SHORT).show();
                Log.e(appName, "Can not run intent " + e.toString());
                resultIntent = new Intent(context, MainActivity.class);
                e.printStackTrace();
            }
        } else resultIntent = new Intent(context, MainActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent navigateIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(navigateIntent);
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(getNotificationID(), mBuilder.build());
    }

    public void pass_to_JSON_api(Context context, String url, SharedPreferences prefManager, boolean report_errors) {
        String currentToken = prefManager.getString("ts_id", "0");
        JSONObject firstJson = new JSONObject();

        try {
            firstJson.put("ts_id", currentToken);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String toServer = "ts_id=" + URLEncoder.encode(firstJson.toString());
        String address = prefManager.getString("server_adress", "null");
        address = address.replaceAll("\\{ts_id\\}", currentToken);

        String server_answer = Network.getFile(context, address, toServer);

        if (server_answer == null) {
            if (report_errors)
                Show_Notification(context, appName, "Error getting server info (json api)", false);
            return;
        }

        JSONObject parsed_answer;
        JSONArray json_notifications;

        SharedPreferences.Editor editor = prefManager.edit();

        try {
            parsed_answer = new JSONObject(server_answer);
            String new_token = parsed_answer.getString("current_ts_id");
            editor.putString("ts_id", new_token);
            editor.apply();

            json_notifications = parsed_answer.getJSONArray("notifications");
        } catch (JSONException e) {
            Log.e(appName, "Json error" + e.toString());
            if (report_errors)
                Show_Notification(context, appName, "Error parsing json" + e.toString(), true);
            e.printStackTrace();
            return;
        }

        try {
            for (int i = 0; i < json_notifications.length(); i++) {
                JSONObject element = json_notifications.getJSONObject(i);
                String title = element.getString("title");
                String text = element.getString("text");

                Show_Notification(context, title, text, true);
            }
        } catch (JSONException e) {
            Log.e(appName, "Json parsing error" + e.toString());

            if (report_errors)
                Show_Notification(context, appName, "Error parsing json" + e.toString(), true);
            e.printStackTrace();
        }
    }

    public void pass_to_xc_api(Context context, String url, SharedPreferences prefManager, boolean report_errors) {
        SharedPreferences.Editor editor = prefManager.edit();

        String server_answer = Network.getFile(context, url, null);
        if (server_answer == null) {
            if (report_errors)
                Show_Notification(context, appName, "Error getting server info (json api)", false);
            return;
        }

        String local_xc_data = prefManager.getString("xc_data", "null");

        editor.putString("xc_data", server_answer);
        editor.apply();

        if (local_xc_data.equals("null")) {
            // Если получили данные в первый раз, то отслеживание не нужно: выходим
            return;
        }

        String[] local_xc_lines = local_xc_data.split("\n");
        String[] remote_xc_lines = server_answer.split("\n");

        if (local_xc_lines.length != remote_xc_lines.length) {
            return;
            // Значит пользователь просто обновил список эх. Продолжать не следует
        }

        Hashtable<String, Integer> remote_xc_dict = new Hashtable<>();
        Hashtable<String, Integer> local_xc_dict = new Hashtable<>();

        try {
            xc_parse_values(remote_xc_dict, remote_xc_lines);
            xc_parse_values(local_xc_dict, local_xc_lines);
        } catch (Exception e) {
            Log.e(appName, "Exception: " + e);
            if (report_errors)
                Show_Notification(context, appName, "Exception: " + e.toString(), true);
            return;
        }

        Hashtable difference = assoc_difference(local_xc_dict, remote_xc_dict);

        if (difference.size() > 0) {
            Enumeration result_echoareas = difference.keys();

            Integer total = 0;
            String notification_text = "";

            while (result_echoareas.hasMoreElements()) {
                String echoarea = result_echoareas.nextElement().toString();
                Integer next = (Integer) difference.get(echoarea);

                total += next;
                notification_text += echoarea + ": " + String.valueOf(next) + "\n";
            }

            if (notification_text.length() > 0) // костыль с последним переносом строки
                notification_text = notification_text.substring(0, notification_text.length() - 1);

            Show_Notification(context, "Новые сообщения: " + total.toString(), notification_text, true);
        }
    }

    private void xc_parse_values(Hashtable<String, Integer> htable, String[] lines) {
        for (String line : lines) {
            String[] pieces = line.split(":");
            if (pieces.length < 2) continue;

            int value = Integer.parseInt(pieces[1]);

            htable.put(pieces[0], value);
        }
    }

    private Hashtable assoc_difference(Hashtable<String, Integer> first, Hashtable<String, Integer> second) {
        Hashtable<String, Integer> result = new Hashtable<>();

        Enumeration keys_local = first.keys();

        while (keys_local.hasMoreElements()) {
            String key_string = keys_local.nextElement().toString();

            Integer firstValue = first.get(key_string);
            if (!second.containsKey(key_string)) continue;
            Integer secondValue = second.get(key_string);

            if (secondValue == null) continue;

            if (secondValue > firstValue) {
                result.put(key_string, secondValue - firstValue);
            }
        }

        return result;
    }
}