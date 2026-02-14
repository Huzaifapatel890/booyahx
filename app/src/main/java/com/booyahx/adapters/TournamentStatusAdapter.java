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

    public class TournamentStatusAdapter extends ArrayAdapter<TournamentStatusAdapter.StatusItem> {

        public static class StatusItem {
            public String apiValue;
            public String displayName;

            public StatusItem(String apiValue, String displayName) {
                this.apiValue = apiValue;
                this.displayName = displayName;
            }
        }

        public TournamentStatusAdapter(@NonNull Context context, @NonNull List<StatusItem> items) {
            super(context, 0, items);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.spinner_tournament_status_selected, parent, false);
            }

            StatusItem item = getItem(position);
            TextView txtSelected = convertView.findViewById(R.id.txtSpinnerSelected);

            if (item != null) {
                txtSelected.setText(item.displayName);
            }

            return convertView;
        }

        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.spinner_tournament_status_item, parent, false);
            }

            StatusItem item = getItem(position);
            TextView txtItem = convertView.findViewById(R.id.txtSpinnerItem);

            if (item != null) {
                txtItem.setText(item.displayName);
            }

            return convertView;
        }
    }
