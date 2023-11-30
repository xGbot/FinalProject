package com.example.distributedmusicplayer;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class SongActivity extends AppCompatActivity {

    MediaPlayer mediaPlayer;
    Boolean loaded = false;

    HttpURLConnection conn = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song);

        Intent data = getIntent();
        String name = data.getStringExtra("name");
        TextView title = findViewById(R.id.textview_title);
        title.setText(name);

        // Load the song into media player
        if (!loaded) {
            GetSong(name);
        }

        // Get media buttons
        SeekBar seekbar = findViewById(R.id.seekbar);
        FloatingActionButton play = findViewById(R.id.button_play);
        FloatingActionButton rewind = findViewById(R.id.button_rewind);
        FloatingActionButton forward = findViewById(R.id.button_forward);
        Button back = findViewById(R.id.button_back);

        // Setup SeekBar
        /* SEEK BAR DON'T WORK

        if (loaded && mediaPlayer != null) {
            seekbar.setMax(mediaPlayer.getDuration());
            seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (mediaPlayer.isPlaying() || mediaPlayer.getCurrentPosition() > 0) {
                        mediaPlayer.seekTo(progress);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                GetSong(name);
            });
        }

        // Update the SeekBar progress in thread
        new Thread(() -> {
            while (mediaPlayer != null) {
                try {
                    // Update the SeekBar position
                    runOnUiThread(() -> seekbar.setProgress(mediaPlayer.getCurrentPosition()));
                    Thread.sleep(1000);  // Update every second
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
         */


        // Go back to home screen
        back.setOnClickListener(v -> {
            if (mediaPlayer != null){
                mediaPlayer.release();
            }
            loaded = false;
            finish();
        });

        // Pause/Play song
        play.setOnClickListener(v -> {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                play.setImageDrawable(getDrawable(R.drawable.baseline_play));
            } else if (mediaPlayer != null && loaded) {
                mediaPlayer.start();
                play.setImageDrawable(getDrawable(R.drawable.baseline_pause));
            } else {
                Toast.makeText(this, "Loading, please wait", Toast.LENGTH_SHORT).show();
            }
        });

        // Rewind song by 10s
        /* REWIND/FORWARD BUTTONS DON'T WORK


        rewind.setOnClickListener(v -> {
            if (mediaPlayer != null && mediaPlayer.isPlaying() || mediaPlayer.getCurrentPosition() > 0) {
                mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() - 10000);
            }
        });

        // Fast forward song by 10s


        forward.setOnClickListener(v -> {
            if (mediaPlayer.isPlaying() || mediaPlayer.getCurrentPosition() > 0 && mediaPlayer.getCurrentPosition() < mediaPlayer.getDuration()) {
                mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() + 10000);
            }
        });*/
    }

    private class LoadSongTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            String name = params[0];
            try {
                // Connect to Server
                URL url = new URL("http://10.0.2.2:8080/request-song" + "?songId=" + name);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                if (!isCancelled()) {
                    try {
                        if (conn.getResponseCode() == 200) {
                            // Set up MediaPlayer with the input stream
                            mediaPlayer = new MediaPlayer();
                            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .build());

                            System.out.println(url.toString());
                            mediaPlayer.setDataSource(url.toString());
                            mediaPlayer.setOnPreparedListener(mp -> {
                                loaded = true;
                            });

                            mediaPlayer.prepareAsync();
                        }
                    } catch (IOException e) {
                        Log.e("Error playing the streamed song", e.getMessage());
                    }
                } else {
                    if (mediaPlayer != null) {
                        mediaPlayer.release();
                        mediaPlayer = null;
                        loaded = false;
                    }
                }
            } catch (IOException e) {
                Log.e("Error making HTTP request", e.getMessage());
            }
            return null;
        }
    }

    public void GetSong(String name) {
        Runnable runnable = () -> {
            try {
                // Connect to Server
                URL url = new URL("http://10.0.0.222:8080/request-song" + "?songId=" + name);
                conn = (HttpURLConnection) url.openConnection();

                if (!Thread.currentThread().isInterrupted()) {
                    try {
                        if (conn.getResponseCode() == 200) {
                            // Set up MediaPlayer with the input stream
                            System.out.println("RESPONSE: " + conn.getResponseMessage());
                            mediaPlayer = new MediaPlayer();
                            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .build());

                            mediaPlayer.setDataSource(url.toString());
                            mediaPlayer.setOnPreparedListener(mp -> {
                                loaded = true;
                            });

                            mediaPlayer.prepareAsync();

                        }
                    } catch (IOException e) {
                        Log.e("Error playing the streamed song", e.getMessage());
                    }
                } else {
                    mediaPlayer.release();
                    mediaPlayer = null;
                    loaded = false;
                }
            } catch (IOException e) {
                Log.e("Error making HTTP request", e.getMessage());
            }
        };

        Thread thread = new Thread(runnable);
        thread.start();
    }


    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (conn != null) {
            conn.disconnect();
        }
        loaded = false;
    }
}