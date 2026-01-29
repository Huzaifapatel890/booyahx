package com.booyahx.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.booyahx.R;

import java.util.List;

public class HostStatusSpinnerAdapter extends ArrayAdapter<String> {

    private final LayoutInflater inflater;

    public HostStatusSpinnerAdapter(
            @NonNull Context context,
            @NonNull List<String> items
    ) {
        super(context, 0, items);
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        TextView textView;

        if (convertView == null) {
            textView = (TextView) inflater.inflate(R.layout.item_host_spinner_selected, parent, false);
        } else {
            textView = (TextView) convertView;
        }

        textView.setText(getItem(position));
        textView.setTextColor(Color.WHITE);

        return textView;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        TextView textView;

        if (convertView == null) {
            textView = (TextView) inflater.inflate(R.layout.item_host_spinner_dropdown, parent, false);
        } else {
            textView = (TextView) convertView;
        }

        textView.setText(getItem(position));
        textView.setTextColor(Color.WHITE);

        return textView;
    }
}