package org.example;
import java.io.Serializable;

public class MusicData implements Serializable {

    private static final long serialVersionUID = 1L;

    private String title;
    private String artist;
    private String musicPath;

    // Constructors, getters, and setters

    public MusicData(String title, String artist, String musicPath) {
        this.title = title;
        this.artist = artist;
        this.musicPath = musicPath;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getMusicPath() {
        return musicPath;
    }

    public void setMusicPath(String musicPath) {
        this.musicPath = musicPath;
    }
}
