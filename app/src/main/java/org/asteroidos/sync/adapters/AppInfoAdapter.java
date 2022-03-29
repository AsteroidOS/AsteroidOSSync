package org.asteroidos.sync.adapters;

// copied from https://github.com/jensstein/oandbackup, used under MIT license

import android.content.Context;

import androidx.annotation.NonNull;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Spinner;
import android.widget.TextView;

import org.asteroidos.sync.utils.AppInfo;

import org.asteroidos.sync.NotificationPreferences;
import org.asteroidos.sync.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AppInfoAdapter extends ArrayAdapter<AppInfo> {
    private final static String TAG = AppInfoAdapter.class.getSimpleName();

    private Context context;
    private ArrayList<AppInfo> items;
    private int iconSize, layout;

    public AppInfoAdapter(Context context, int layout, ArrayList<AppInfo> items) {
        super(context, layout, items);
        this.context = context;
        this.items = new ArrayList<>(items);
        this.layout = layout;

        try {
            DisplayMetrics metrics = new DisplayMetrics();
            ((android.app.Activity) context).getWindowManager().getDefaultDisplay().getMetrics(metrics);
            iconSize = 32 * (int) metrics.density;
        } catch (ClassCastException e) {
            iconSize = 32;
        }
    }

    public void add(AppInfo appInfo) {
        items.add(appInfo);
    }

    public AppInfo getItem(int pos) {
        return items.get(pos);
    }

    public int getCount() {
        return items.size();
    }

    @NonNull
    @Override
    public View getView(int pos, View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            assert inflater != null;
            convertView = inflater.inflate(layout, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.label = convertView.findViewById(R.id.label);
            viewHolder.icon = convertView.findViewById(R.id.icon);
            viewHolder.spinner = convertView.findViewById(R.id.notification_spinner);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        AppInfo appInfo = getItem(pos);
        if (appInfo != null) {
            if (appInfo.icon != null) {
                viewHolder.icon.setVisibility(View.VISIBLE); // to cancel View.GONE if it was set
                viewHolder.icon.setImageBitmap(appInfo.icon);
                LayoutParams lp = (LayoutParams) viewHolder.icon.getLayoutParams();
                lp.height = lp.width = iconSize;
                viewHolder.icon.setLayoutParams(lp);
            } else {
                viewHolder.icon.setVisibility(View.GONE);
            }
            viewHolder.label.setText(appInfo.getLabel());
            convertView.setTag(viewHolder);

            ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, android.R.id.text1);
            adapter.add(context.getResources().getString(R.string.notification_type_default));
            adapter.add(context.getResources().getString(R.string.notification_type_no_notif));
            adapter.add(context.getResources().getString(R.string.notification_type_silent));
            adapter.add(context.getResources().getString(R.string.notification_type_vibra));
            adapter.add(context.getResources().getString(R.string.notification_type_strong_vibra));
            adapter.add(context.getResources().getString(R.string.notification_type_ringtone));
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            viewHolder.spinner.setAdapter(adapter);
            NotificationPreferences.NotificationOption position =
                    NotificationPreferences.getNotificationPreferenceForApp(context, appInfo.getPackageName());
            viewHolder.spinner.setSelection(position.asInt());
            viewHolder.spinner.setOnItemSelectedListener(new MyOnItemSelectedListener(appInfo.getPackageName()) {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    NotificationPreferences.saveNotificationPreferenceForApp(context, packageName, i);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                    Log.i(TAG, "nothing selected");
                }
            });
        }
        return convertView;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new SeenPackagesFilter(NotificationPreferences.seenPackageNames(context));
    }

    public void restoreFilter() {
        getFilter().filter(null);
    }

    private static class ViewHolder {
        TextView label;
        ImageView icon;
        Spinner spinner;
    }

    abstract class MyOnItemSelectedListener implements AdapterView.OnItemSelectedListener {
        String packageName;

        MyOnItemSelectedListener(String packageName) {
            this.packageName = packageName;
        }
    }

    private class SeenPackagesFilter extends Filter {

        private List<String> seenPackages;

        private SeenPackagesFilter(List<String> seenPackages) {
            this.seenPackages = seenPackages;
        }

        @Override
        protected FilterResults performFiltering(CharSequence ignored) {
            FilterResults results = new FilterResults();
            ArrayList<AppInfo> newValues = new ArrayList<>();
            for (AppInfo value : items) {
                String packageName = value.getPackageName().toLowerCase(Locale.getDefault());
                if (seenPackages.contains(packageName))
                    newValues.add(value);
            }
            results.values = newValues;
            results.count = newValues.size();
            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            if (results.count > 0) {
                items.clear();
                for (AppInfo value : (ArrayList<AppInfo>) results.values)
                    add(value);

                notifyDataSetChanged();
            } else {
                items.clear();
                notifyDataSetInvalidated();
            }
        }
    }
}
