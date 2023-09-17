package com.rusj.myapplication;

public class VideoItem {
    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    private String videoId;
    public VideoItem(String videoId) {
        this.videoId = videoId;
    }

    public String getVideoId() {
        return videoId;
    }
}
