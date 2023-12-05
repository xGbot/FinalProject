package com.example.distributedmusicplayer;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;

public class SongActivity extends AppCompatActivity {

    MediaPlayer mediaPlayer;
    Boolean loaded = false;
    private static final int BLUETOOTH_REQUEST = 100;

    HttpURLConnection conn = null;
    File tempFile;

    SeekBar seekbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song);

        Intent data = getIntent();
        String name = data.getStringExtra("name");
        TextView title = findViewById(R.id.textview_title);
        title.setText(name);

        // Load the song into media player
        if (checkDownload(name)) {
            try {
                setupMediaPlayer(name);
            } catch (IOException e) {
                System.out.println("Failed");
            }
        } else {
            if (!loaded) {
                new LoadSongTask().execute(name);
            }
        }

        // Get media buttons
        seekbar = findViewById(R.id.seekbar);
        FloatingActionButton play = findViewById(R.id.button_play);
        FloatingActionButton rewind = findViewById(R.id.button_rewind);
        FloatingActionButton forward = findViewById(R.id.button_forward);
        Button back = findViewById(R.id.button_back);
        ImageButton bluetooth = findViewById(R.id.button_bluetooth);
        ImageButton download = findViewById(R.id.button_download);

        // Setup bluetooth
        bluetooth.setOnClickListener(v -> {
            BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
            if (ba != null) {
                if (!ba.isEnabled()) {
                    Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBT, BLUETOOTH_REQUEST);
                } else {
                    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, BLUETOOTH_REQUEST);
                    }
                    Set<BluetoothDevice> pairedDevices = ba.getBondedDevices();

                    for (BluetoothDevice device : pairedDevices) {
                        String deviceName = device.getName();
                        String deviceAddress = device.getAddress();
                        System.out.println("NAME: " + deviceName + " ADDR: " + deviceAddress);
                    }
                }
            } else {
                Toast.makeText(getApplicationContext(), "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            }
        });

        // Download music onto device
        download.setOnClickListener(v -> {
            // Get the public external storage directory
            File externalStorageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);

            // Create subdirectory named "MyMusic"
            File musicDirectory = new File(externalStorageDir, "MyMusic");

            // Check if directory exists, if not create it
            if (musicDirectory.exists()) {
                if (tempFile != null) {
                    File moveFile = new File(musicDirectory, name + ".mp3");

                    if (!moveFile.exists()) {
                        try {
                            copyFile(tempFile, moveFile);
                        } catch (IOException e) {
                            System.out.println("Failed to copy");
                        }
                    } else {
                        Toast.makeText(this, "File already downloaded", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                if (musicDirectory.mkdirs()) {
                    Log.e("MusicDirectory", "Created directory");
                } else {
                    Log.e("MusicDirectory", "Error creating directory");
                }
            }
        });

        // Setup user seekbar changes
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (mediaPlayer.isPlaying() || mediaPlayer.getCurrentPosition() > 0) {
                        mediaPlayer.seekTo(progress);
                        seekBar.setProgress(mediaPlayer.getCurrentPosition());
                    }
                } else {
                    seekBar.setProgress(progress);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Go back to home screen
        back.setOnClickListener(v -> {
            if (mediaPlayer != null){
                mediaPlayer.release();
                mediaPlayer = null;
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
                seekProgress();
            } else {
                Toast.makeText(this, "Loading, please wait", Toast.LENGTH_SHORT).show();
            }
        });

        // Rewind song by 10s
        rewind.setOnClickListener(v -> {
            if (mediaPlayer != null && mediaPlayer.isPlaying() || mediaPlayer.getCurrentPosition() > 0) {
                mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() - 10000);
                seekbar.setProgress(seekbar.getProgress() - 10000);
            }
        });

        // Fast forward song by 10s
        forward.setOnClickListener(v -> {
            if (mediaPlayer.isPlaying() || mediaPlayer.getCurrentPosition() > 0 && mediaPlayer.getCurrentPosition() < mediaPlayer.getDuration()) {
                mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() + 10000);
                seekbar.setProgress(seekbar.getProgress() + 10000);
            }
        });
    }

    private class LoadSongTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            String name = params[0];
            try {
                // Connect to Server
                URL url = new URL("http://10.0.0.222:8080/request-song" + "?songId=" + name);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                if (!isCancelled()) {
                    try {
                        if (conn.getResponseCode() == 200) {
                            tempFile = File.createTempFile("temp_audio", ".mp3", getCacheDir());

                            // Write the incoming audio to temporary file
                            try (InputStream inputStream = conn.getInputStream();
                                 FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
                                byte[] buffer = new byte[1024];
                                int bytesRead;

                                while ((bytesRead = inputStream.read(buffer)) != -1) {
                                    fileOutputStream.write(buffer, 0, bytesRead);
                                }
                            }

                            // Set up MediaPlayer with the input stream
                            mediaPlayer = new MediaPlayer();
                            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .build());

                            mediaPlayer.setDataSource(tempFile.getAbsolutePath());
                            mediaPlayer.setOnPreparedListener(mp -> {
                                loaded = true;
                                seekbar.setMax(mediaPlayer.getDuration());
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

    public void copyFile(File source, File dest) throws IOException {

        FileInputStream in = new FileInputStream(source);
        try {
            FileOutputStream out = new FileOutputStream(dest);
            try {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    public void seekProgress(){
        // Update the SeekBar progress in thread
        new Thread(() -> {
            while (mediaPlayer != null && mediaPlayer.isPlaying()) {
                // Update the SeekBar position
                runOnUiThread(() -> seekbar.setProgress(mediaPlayer.getCurrentPosition()));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println(e.getMessage());
                }
            }
        }).start();
    }

    public boolean checkDownload (String name) {
        // Get the public external storage directory
        File externalStorageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        File musicDirectory = new File(externalStorageDir, "MyMusic");
        File requestedFile = new File(musicDirectory, name + ".mp3");

        if (requestedFile.exists()) {
            return true;
        }

        return false;
    }

    public void setupMediaPlayer(String name) throws IOException{
        // Set up MediaPlayer with the downloaded file
        File externalStorageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        File musicDirectory = new File(externalStorageDir, "MyMusic");
        File musicFile = new File(musicDirectory, name + ".mp3");

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build());

        mediaPlayer.setDataSource(String.valueOf(musicFile));
        mediaPlayer.setOnPreparedListener(mp -> {
            loaded = true;
            seekbar.setMax(mediaPlayer.getDuration());
        });

        mediaPlayer.prepareAsync();
    }

    protected void onDestroy() {
        super.onDestroy();

        File cacheDir = getCacheDir();
        File[] files = cacheDir.listFiles();

        if (files != null) {
            for (File file : files) {
                // Check if the file name starts with "temp_audio" and ends with ".mp3"
                if (file.getName().startsWith("temp_audio") && file.getName().endsWith(".mp3")) {
                    // Delete the temporary file
                    boolean deleted = file.delete();
                    if (deleted) {
                        Log.d("TempFileDeletion", "Temporary file deleted successfully");
                    } else {
                        Log.e("TempFileDeletion", "Unable to delete temporary file");
                    }
                }
            }
        }

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