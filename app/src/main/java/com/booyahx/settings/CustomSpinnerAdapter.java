package com.booyahx.settings;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.booyahx.R;

public class CustomSpinnerAdapter extends ArrayAdapter<String> {

    private final Context context;
    private final String[] items;

    public CustomSpinnerAdapter(@NonNull Context context, String[] items) {
        super(context, R.layout.spinner_selected_item, items);
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // This is the closed/selected view
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(
                    R.layout.spinner_selected_item, parent, false
            );
        }

        // Ensure the view matches parent width
        convertView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView txtSpinnerSelected = convertView.findViewById(R.id.txtSpinnerSelected);
        txtSpinnerSelected.setText(items[position]);

        return convertView;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // This is the dropdown/expanded view with spacing
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(
                    R.layout.spinner_dropdown_item, parent, false
            );
        }

        // Ensure dropdown items match parent width
        convertView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView txtSpinnerItem = convertView.findViewById(R.id.txtSpinnerItem);
        txtSpinnerItem.setText(items[position]);

        return convertView;
    }
}