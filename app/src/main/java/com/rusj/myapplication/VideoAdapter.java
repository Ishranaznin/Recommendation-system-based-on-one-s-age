package com.rusj.myapplication;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;

import java.util.List;

// VideoAdapter.java
public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {
    private List<VideoItem> videoItems;

    public VideoAdapter(List<VideoItem> videoItems) {
        this.videoItems = videoItems;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {

        holder.videoWeb.loadData(videoItems.get(position).getVideoId(), "text/html", "utf-8");

    }

    @Override
    public int getItemCount() {
        return videoItems.size();
    }

    public class VideoViewHolder extends RecyclerView.ViewHolder {
        WebView videoWeb;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);

            videoWeb = (WebView) itemView.findViewById(R.id.videoWebView);

            videoWeb.getSettings().setJavaScriptEnabled(true);
            videoWeb.setWebChromeClient(new WebChromeClient() {

            });


        }
    }
}



