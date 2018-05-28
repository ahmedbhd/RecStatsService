package com.esprit.recstatsservice;

import android.app.Service;
import android.content.*;
import android.os.*;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.URISyntaxException;
import java.util.Random;

public class StatsJobService extends Service {
    private Socket mSocket;
    static String ch;
    static double duration=0;
    private static final String TAG = "StatsJobService";

    {
        try {

            mSocket = IO.socket("http://192.168.1.101:8088");
            Log.d(TAG, "Socket started");

        } catch (URISyntaxException e) {}
    }

    public StatsJobService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mSocket.connect();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String message = "StatsJobService onStartCommand()";

        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();

        Log.d(TAG, "onStartCommand()");
        sendRequest();
        return super.onStartCommand(intent, flags, startId);
    }

    void CallSocket( String chaine, double duration) throws URISyntaxException {


        JSONObject request=new JSONObject();
        try {
            Random r = new Random();
            int i = r.nextInt(5 - 1) + 1;
            request.put("recepteur", i);
            request.put("nom_chaine", chaine);
            request.put("duree", duration);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mSocket.emit("input", request);


    }

    private void sendRequest(){
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = String.format(
                "http://0.0.0.0:8080/jsonrpc?request=" +
                        "{\"jsonrpc\": \"2.0\", \"method\": \"Playlist.GetItems\", \"params\": { \"properties\": [ \"title\" ], \"playlistid\": 1}, \"id\": 1}"
        );
        Log.d("URL", url);
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

                                if (title.contains("-"))
                                    title= title.substring(0,title.indexOf("-")-1);
                                if(ch==null){
                                    ch=title;
                                }
                                if (!ch.equals(title)) {

                                    Log.d("New Channel is watching",title);
                                    ShowToast("New Channel is watching : "+title,0);
                                    try {
                                        ShowToast(ch+" saved ",duration);
                                        Log.d(TAG, "socket called");
                                        CallSocket(ch,duration);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    ch=title;
                                    duration=0;
                                }else {
                                    duration=duration+20;
                                    ShowToast(ch,duration);
                                }
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

    public void ShowToast(String ch, double duration){
        Toast.makeText(getApplicationContext(), ch+" "+duration, Toast.LENGTH_LONG).show();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}