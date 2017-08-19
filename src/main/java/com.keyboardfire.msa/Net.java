package com.keyboardfire.msa;

import java.net.URL;
import java.net.MalformedURLException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieHandler;
import javax.net.ssl.HttpsURLConnection;

import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import android.os.StrictMode;

public class Net {

    public static void setup() {
        StrictMode.setThreadPolicy(
                new StrictMode.ThreadPolicy.Builder().permitAll().build());

        CookieManager cookieManager =
            new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);
    }

    public static String doGET(String url) {
        try {
            HttpsURLConnection conn = getConn(url);
            return readInputStream(conn.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String doPOST(String url, String data, String token) {
        try {
            HttpsURLConnection conn = getConn(url);
            conn.setRequestMethod("POST");
            if (token != null) {
                conn.setRequestProperty("RequestVerificationToken", token);
            }
            if (data != null) {
                conn.setRequestProperty("Content-Type", "application/json");
                OutputStream os = conn.getOutputStream();
                BufferedWriter out = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                out.write(data);
                out.flush();
                out.close();
                os.close();
            }
            return readInputStream(conn.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String doPOST(String url, String data) {
        return Net.doPOST(url, data, null);
    }

    private static HttpsURLConnection getConn(String urlStr) throws IOException {
        try {
            URL url = new URL(urlStr);
            return (HttpsURLConnection) url.openConnection();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String readInputStream(InputStream in) throws IOException {
        BufferedInputStream bufIn = new BufferedInputStream(in);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int len;
        while ((len = bufIn.read(buf)) != -1) out.write(buf, 0, len);
        return out.toString("UTF-8");
    }

}
