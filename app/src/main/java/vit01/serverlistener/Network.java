package vit01.serverlistener;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Network {
    static String appName = "ServerListener";

    public static String getFile(Context context, String url, String data) {
        ConnectivityManager connMgr;
        NetworkInfo networkInfo;

        connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            try {
                return downloadUrl(url, data);
            } catch (Exception exception) {
                Log.w(appName, "Throw Exception: " + exception);
                exception.printStackTrace();
                return null;
            }
        } else return null;
    }

    public static String downloadUrl(String myurl, String data) throws IOException {
        InputStream is = null;
        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);

            if (data == null) {
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
            } else {
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
                conn.setDoOutput(true);

                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                wr.write(data);
                wr.flush();
            }

            conn.connect();
            int response = conn.getResponseCode();

            Log.d(appName, "ServerResponse: " + response);
            is = conn.getInputStream();

            return readIt(is, 50000);
        } finally {
            if (is != null) is.close();
        }
    }

    public static String readIt(InputStream stream, int len) throws IOException {
        Reader reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[len];
        reader.read(buffer);
        return new String(buffer);
    }
}
