package com.esprit.recstatsservice;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created by Ghassen on 25/05/2018.
 */

public class StatsJobService extends JobService {
    private static final String TAG = "ExampleJobService";
    private boolean jobCancelled = false;

    @Override
    public boolean onStartJob(JobParameters params) {
        Toast.makeText(getApplicationContext(), "Job started", Toast.LENGTH_LONG).show();
        doBackgroundWork(params);

        return true;
    }

    private void doBackgroundWork(final JobParameters params) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (jobCancelled) {
                    return;
                }

                Log.d(TAG, getIP());
                sendRequest();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //Toast.makeText(this.getContext(), "Hiiiiiiiiiiiiiiiiii", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Job finished");

                jobFinished(params, false);
            }
        }).start();
    }

    private void sendRequest(){
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = String.format(
                "http://192.168.1.103:8080/jsonrpc?request=" +
                        "{\"jsonrpc\": \"2.0\", \"method\": \"Playlist.GetItems\", \"params\": { \"properties\": [ \"title\" ], \"playlistid\": 1}, \"id\": 1}"
        );

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    public void onResponse(String response) {
                        System.out.println(response);
                        try {
                            JSONObject object = new JSONObject(response);
                            JSONObject result =  object.getJSONObject( "result" );

                            if (!result.isNull("items")){
                                JSONArray items  = result.getJSONArray("items");
                                String title = items.getJSONObject(0).getString("title");
                                Log.d("title", "onResponse: "+title);
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Handle error
                        System.out.println("Erreur "+error.getMessage());

                    }
                });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);

    }

    private String getIP(){
        try {
            Enumeration e = NetworkInterface.getNetworkInterfaces();
            while(e.hasMoreElements())
            {
                NetworkInterface n = (NetworkInterface) e.nextElement();
                Enumeration ee = n.getInetAddresses();
                int h = 0;
                while (ee.hasMoreElements())
                {
                    h++;
                    InetAddress i = (InetAddress) ee.nextElement();
                    if (h==2)
                        return i.getHostAddress();
                }
            }
        } catch (SocketException ex) {
            Log.e("SocketException", ex.toString());
        }
        return "0.0.0.0";
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "Job cancelled before completion");
        jobCancelled = true;
        return true;
    }
}
