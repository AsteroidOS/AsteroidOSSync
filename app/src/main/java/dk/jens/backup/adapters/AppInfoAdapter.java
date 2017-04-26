package dk.jens.backup.adapters;

// copied from https://github.com/jensstein/oandbackup, used under MIT license

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import dk.jens.backup.AppInfo;
import org.asteroidos.sync.R;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Map;

public class AppInfoAdapter extends ArrayAdapter<AppInfo>
{
    final static String TAG = "AsteroidOS Sync";
    final int DEFAULT  = 0;
    final int NO_NOTIFICATIONS = 1;
    final int SILENT_NOTIFICATION = 2;
    final int SMALL_VIBRATION = 3;
    final int LARGE_VIBRATION = 4;

    public static final String PREFS_NAME = "NotificationPreferences";
    public static final String PREFS_NOTIFICATIONS = "notifications";


    Context context;
    ArrayList<AppInfo> items;
    int iconSize, layout;
    String currentFilter;

    private ArrayList<AppInfo> originalValues;
    private MyArrayFilter mFilter;
    public AppInfoAdapter(Context context, int layout, ArrayList<AppInfo> items)
    {
        super(context, layout, items);
        this.context = context;
        this.items = new ArrayList<AppInfo>(items);
        this.layout = layout;

        originalValues = new ArrayList<AppInfo>(items);
        try
        {
            DisplayMetrics metrics = new DisplayMetrics();
            ((android.app.Activity) context).getWindowManager().getDefaultDisplay().getMetrics(metrics);
            iconSize = 32 * (int) metrics.density;
        }
        catch(ClassCastException e)
        {
            iconSize = 32;
        }
    }
    public void add(AppInfo appInfo)
    {
        items.add(appInfo);
    }
    public void addAll(ArrayList<AppInfo> list)
    {
        for(AppInfo appInfo : list)
        {
            items.add(appInfo);
        }
    }
    public AppInfo getItem(int pos)
    {
        return items.get(pos);
    }
    public int getCount()
    {
        return items.size();
    }

    @NonNull
    @Override
    public View getView(int pos, View convertView, @NonNull ViewGroup parent)
    {
        ViewHolder viewHolder;
        if(convertView == null)
        {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(layout, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.label = (TextView) convertView.findViewById(R.id.label);
            viewHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
            viewHolder.spinner = (Spinner) convertView.findViewById(R.id.notification_spinner);
            convertView.setTag(viewHolder);
        }
        else
        {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        AppInfo appInfo = getItem(pos);
        if(appInfo != null)
        {
            if(appInfo.icon != null)
            {
                viewHolder.icon.setVisibility(View.VISIBLE); // to cancel View.GONE if it was set
                viewHolder.icon.setImageBitmap(appInfo.icon);
                LayoutParams lp = (LayoutParams) viewHolder.icon.getLayoutParams();
                lp.height = lp.width = iconSize;
                viewHolder.icon.setLayoutParams(lp);
            }
            else
            {
                viewHolder.icon.setVisibility(View.GONE);
            }
            viewHolder.label.setText(appInfo.getLabel());
            convertView.setTag(viewHolder);
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context,
                    R.array.notification_types_array, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            viewHolder.spinner.setAdapter(adapter);
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String notificationPrefsAsString = prefs.getString(PREFS_NOTIFICATIONS, "{}");
            Gson gson = new Gson();
            Type stringIntMap = new TypeToken<Map<String, Integer>>(){}.getType();
            Map<String,Integer> map = gson.fromJson(notificationPrefsAsString, stringIntMap);
            Integer position = map.get(appInfo.getPackageName());
            viewHolder.spinner.setSelection(position != null ? position : DEFAULT);
            viewHolder.spinner.setOnItemSelectedListener(new MyOnItemSelectedListener(appInfo.getPackageName()) {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    if (i == DEFAULT) return;

                    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    String notificationPrefsAsString = prefs.getString(PREFS_NOTIFICATIONS, "{}");
                    Gson gson = new Gson();
                    Type stringIntMap = new TypeToken<Map<String, Integer>>(){}.getType();
                    Map<String,Integer> map = gson.fromJson(notificationPrefsAsString, stringIntMap);
                    map.put(packageName, i);

                    SharedPreferences.Editor editor = prefs.edit();
                    String jsonString = gson.toJson(map);
                    editor.putString(PREFS_NOTIFICATIONS, jsonString);
                    editor.apply();

                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                    Log.i(TAG, "nothing selected");
                }
            });
        }
        return convertView;
    }
    abstract class MyOnItemSelectedListener implements AdapterView.OnItemSelectedListener {
        String packageName;
        MyOnItemSelectedListener(String packageName) {
            this.packageName = packageName;
        }
    }
    static class ViewHolder
    {
        TextView label;
        ImageView icon;
        Spinner spinner;
    }
    @Override
    public Filter getFilter()
    {
        if(mFilter == null)
        {
            mFilter = new MyArrayFilter();
        }
        return mFilter;
    }
    private class MyArrayFilter extends Filter
    {
        @Override
        protected FilterResults performFiltering(CharSequence prefix)
        {
            FilterResults results = new FilterResults();
            if(originalValues == null)
            {
                originalValues = new ArrayList<AppInfo>(items);
            }
            ArrayList<AppInfo> newValues = new ArrayList<AppInfo>();
            if(prefix != null && prefix.length() > 0)
            {
                String prefixString = prefix.toString().toLowerCase();
                for(AppInfo value : originalValues)
                {
                    String packageName = value.getPackageName().toLowerCase();
                    String label = value.getLabel().toLowerCase();
                    if((packageName.contains(prefixString) || label.contains(prefixString)) && !newValues.contains(value))
                    {
                        newValues.add(value);
                    }
                }
                results.values = newValues;
                results.count = newValues.size();
            }
            else
            {
                results.values = new ArrayList<AppInfo>(originalValues);
                results.count = originalValues.size();
            }
            return results;
        }
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results)
        {
            currentFilter = constraint.toString();
            ArrayList<AppInfo> notInstalled = new ArrayList<AppInfo>();
            if(results.count > 0)
            {
                items.clear();
                for(AppInfo value : (ArrayList<AppInfo>) results.values)
                {
                    if(value.isInstalled())
                    {
                        add(value);
                    }
                    else
                    {
                        notInstalled.add(value);
                    }
                }
                addAll(notInstalled);
                notifyDataSetChanged();
            }
            else
            {
                items.clear();
                notifyDataSetInvalidated();
            }
        }
    }
}
