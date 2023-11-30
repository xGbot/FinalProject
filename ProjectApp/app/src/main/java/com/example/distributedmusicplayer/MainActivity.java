package com.example.distributedmusicplayer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private LinearLayout songTitle; // will be used to display list of songs

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        songTitle = findViewById(R.id.listview_songs);
        Button refresh = findViewById(R.id.button_refresh);

        refresh.setOnClickListener(v -> RetrieveSongs());

    }

    public void RetrieveSongs() {
        Runnable runnable = () -> {
            try {
                // Connect to Server
                URL url = new URL("http://10.0.0.222:8080/get-song-titles");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                try {
                    InputStream in = conn.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));

                    String titles = br.readLine();

                    if (!titles.isEmpty()) {
                        Handler handler = new Handler(getMainLooper());
                        handler.post(() -> {
                            DisplayTitles(titles);
                        });
                    }

                } catch (IOException e) {
                    Log.e("Error retrieving titles", e.getMessage());
                }
            } catch (Exception e) {
                Log.e("Error making HTTP request", e.getMessage());
            }
        };

        Thread thread = new Thread(runnable);
        thread.start();
    }

    public void DisplayTitles(String titles) {
        String[] splitTitles = titles.split(",");
        List<String> songNames = Arrays.asList(splitTitles);

        // Inflate the list layout

        for (String song: songNames) {
            LayoutInflater inflater = LayoutInflater.from(this);
            View dynamicView = inflater.inflate(R.layout.list_layout, songTitle, false);
            TextView title = dynamicView.findViewById(R.id.title);
            title.setText(song);

            FloatingActionButton fab = dynamicView.findViewById(R.id.play_button);
            fab.setOnClickListener(v -> {
                Intent intent = new Intent(getApplicationContext(), SongActivity.class);
                intent.putExtra("name", song);
                startActivity(intent);
            });

            songTitle.addView(dynamicView);
        }
    }

}