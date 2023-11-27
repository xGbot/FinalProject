package com.example.distributedmusicplayer;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

public class ListItemAdapter extends ArrayAdapter<String> {
    private View.OnClickListener mListener;
    private List<String> items;
    public ListItemAdapter(Context context, List<String> items, View.OnClickListener listener) {
        super(context, 0);
        mListener = listener;
        this.items = items;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_layout, parent, true);
        }

        String name = getItem(position);
        TextView titleView = convertView.findViewById(R.id.title);
        titleView.setText(name);

        ImageView imageView = convertView.findViewById(R.id.play_button);
        imageView.setTag(name);
        imageView.setOnClickListener(mListener);

        return convertView;
    }
}
