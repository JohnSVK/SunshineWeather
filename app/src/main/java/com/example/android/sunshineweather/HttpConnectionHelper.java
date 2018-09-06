package com.example.android.sunshineweather;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class HttpConnectionHelper {

    HttpConnectionHelper(){
    }

    public static String requestGetResult(String requestUrl){
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String resultJsonStr = null;
        try {
            //build url
            URL url = new URL(requestUrl);

            //create GET request connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            //read input stream
            InputStream inputStream = urlConnection.getInputStream();
            StringBuilder buffer = new StringBuilder();
            if(inputStream == null){
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while((line = reader.readLine()) != null){
                buffer.append(line).append("\n");
            }

            if(buffer.length() == 0){
                return null;
            }
            resultJsonStr = buffer.toString();
        } catch (MalformedURLException e) {
            Log.e("HTTP Connection", "ERROR: Wrong URL");
        } catch (IOException e) {
            Log.e("HTTP Connection", "ERROR: Input stream");
        } finally {
            if(urlConnection != null)
                urlConnection.disconnect();
            if(reader != null){
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e("HTTP Connection", "ERROR: Closing stream");
                }
            }
        }
        return resultJsonStr;
    }
}
