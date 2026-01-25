package com.booyahx.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.booyahx.R;

import java.util.List;

public class UniversalSpinnerAdapter extends ArrayAdapter<UniversalSpinnerAdapter.SpinnerItem> {

    public static class SpinnerItem {
        public String apiValue;
        public String displayName;

        public SpinnerItem(String apiValue, String displayName) {
            this.apiValue = apiValue;
            this.displayName = displayName;
        }
    }

    public UniversalSpinnerAdapter(@NonNull Context context, @NonNull List<SpinnerItem> items) {
        super(context, 0, items);
    }

    public int getPosition(String apiValue) {
        if (apiValue == null) return -1;

        for (int i = 0; i < getCount(); i++) {
            SpinnerItem item = getItem(i);
            if (item != null && item.apiValue.equalsIgnoreCase(apiValue)) {
                return i;
            }
        }
        return -1;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.spinner_tournament_status_selected, parent, false);
        }

        SpinnerItem item = getItem(position);
        TextView txtSelected = convertView.findViewById(R.id.txtSpinnerSelected);

        if (item != null && txtSelected != null) {
            txtSelected.setText(item.displayName);
            txtSelected.setTextColor(0xFFFFFFFF);
            txtSelected.setSingleLine(true);
            txtSelected.setMaxLines(1);
        }

        convertView.setBackgroundColor(android.graphics.Color.TRANSPARENT);

        return convertView;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.spinner_tournament_status_item, parent, false);
        }

        SpinnerItem item = getItem(position);
        TextView txtItem = convertView.findViewById(R.id.txtSpinnerItem);

        if (item != null && txtItem != null) {
            txtItem.setText(item.displayName);
            txtItem.setTextColor(0xFFFFFFFF);
        }

        return convertView;
    }
}