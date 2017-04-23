package dk.jens.backup.adapters;

import android.content.Context;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import dk.jens.backup.AppInfo;
import org.asteroidos.sync.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class AppInfoAdapter extends ArrayAdapter<AppInfo>
{
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
    @Override
    public View getView(int pos, View convertView, ViewGroup parent)
    {
        ViewHolder viewHolder;
        if(convertView == null)
        {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(layout, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.label = (TextView) convertView.findViewById(R.id.label);
            viewHolder.packageName = (TextView) convertView.findViewById(R.id.packageName);
            viewHolder.versionName = (TextView) convertView.findViewById(R.id.versionCode);
            viewHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
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
            viewHolder.packageName.setText(appInfo.getPackageName());
            if(appInfo.isInstalled())
            {
                int color = appInfo.isSystem() ? Color.rgb(198, 91, 112) : Color.rgb(14, 158, 124);
                if(appInfo.isDisabled())
                    color = Color.rgb(7, 87, 117);
                viewHolder.packageName.setTextColor(color);
            }
            else
            {
                viewHolder.packageName.setTextColor(Color.GRAY);
            }
        }
        return convertView;
    }
    static class ViewHolder
    {
        TextView label;
        TextView packageName;
        TextView versionName;
        ImageView icon;
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

    public void filterAppType(int options)
    {
        ArrayList<AppInfo> notInstalled = new ArrayList<AppInfo>();
        items.clear();
        switch(options)
        {
            case 0: // all apps
                for(AppInfo appInfo : originalValues)
                {
                    if(appInfo.isInstalled())
                    {
                        add(appInfo);
                    }
                    else
                    {
                        notInstalled.add(appInfo);
                    }
                }
                addAll(notInstalled);
                break;
            case 1: // user apps
                for(AppInfo appInfo : originalValues)
                {
                    if(!appInfo.isSystem())
                    {
                        if(appInfo.isInstalled())
                        {
                            add(appInfo);
                        }
                        else
                        {
                            notInstalled.add(appInfo);
                        }
                    }
                }
                addAll(notInstalled);
                break;
            case 2: // system apps
                for(AppInfo appInfo : originalValues)
                {
                    if(appInfo.isSystem())
                    {
                        if(appInfo.isInstalled())
                        {
                            add(appInfo);
                        }
                        else
                        {
                            notInstalled.add(appInfo);
                        }
                    }
                }
                addAll(notInstalled);
                break;
        }
        notifyDataSetChanged();
    }
}
