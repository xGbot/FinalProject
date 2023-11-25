package com.example.distributedmusicplayer;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private LinearLayout songTitle; // will be used to display list of songs

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        songTitle = findViewById(R.id.linearlayout_songs);
        Button get = findViewById(R.id.button_get);

        get.setOnClickListener(view -> GetSong());

    }

    public void GetSong() {
        String songTitle = "Action Strike";
        Runnable runnable = () -> {
            try {
                // Connect to Server
                URL url = new URL("http://10.0.2.2:8080/request-song" + "?songId=" + songTitle);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                try {
                    if (conn.getResponseCode() == 200) {
                        System.out.println(conn.getContentType());

                        // Set up MediaPlayer with the input stream
                        MediaPlayer mediaPlayer = new MediaPlayer();
                        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build());

                        mediaPlayer.setDataSource(url.toString());
                        mediaPlayer.prepare();
                        mediaPlayer.start();

                        while (mediaPlayer.isPlaying()) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        // Release resources when playback is complete
                        mediaPlayer.release();
                    }

                } catch (IOException e) {
                    Log.e("Error playing the streamed song", e.getMessage());
                }
            } catch (Exception e) {
                Log.e("Error making HTTP request", e.getMessage());
            }
        };

        Thread thread = new Thread(runnable);
        thread.start();

    }

}