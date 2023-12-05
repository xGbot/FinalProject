package com.example.distributedmusicplayer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedReader;
import java.io.File;
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
        ImageButton refresh = findViewById(R.id.button_refresh);
        Button downloads = findViewById(R.id.button_downloads);

        refresh.setOnClickListener(v -> RetrieveSongs());

        downloads.setOnClickListener(v -> {
            // Get the public external storage directory and music directory location
            File externalStorageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
            File musicDirectory = new File(externalStorageDir, "MyMusic");
            String titles = ReturnTitles(musicDirectory);
            DisplayTitles(titles);
        });
    }

    private String ReturnTitles(File musicDirectory) {

        File[] files = musicDirectory.listFiles();
        StringBuilder stringBuilder = new StringBuilder();

        if (musicDirectory.exists() && musicDirectory.isDirectory() && files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().toLowerCase().endsWith(".mp3")) {
                    String curName = file.getName().substring(0, file.getName().lastIndexOf('.'));
                    stringBuilder.append(curName + ',');
                }
            }
            // Remove the trailing comma if the string is not empty
            if (stringBuilder.length() > 0) {
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            }
        }

        String titles = stringBuilder.toString();
        return titles;
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
        songTitle.removeAllViews();

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