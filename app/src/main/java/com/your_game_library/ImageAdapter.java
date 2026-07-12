package com.your_game_library;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import java.util.List;

public class ImageAdapter extends BaseAdapter {

    private Context context;
    private List<Bitmap> images;

    public ImageAdapter(Context context, List<Bitmap> images) {
        this.context = context;
        this.images = images;
    }

    @Override
    public int getCount() { return images.size(); }

    @Override
    public Object getItem(int position) { return images.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            imageView = new ImageView(context);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(400, 400));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(8, 8, 8, 8);
        } else {
            imageView = (ImageView) convertView;
        }
        imageView.setImageBitmap(images.get(position));
        return imageView;
    }
}
