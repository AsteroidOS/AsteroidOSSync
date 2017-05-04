package org.asteroidos.sync;

// copied from https://github.com/jensstein/oandbackup, used under MIT license

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

public class AppInfo
        implements Comparable<AppInfo>, Parcelable
{
    String label, packageName;
    private boolean system, installed, checked, disabled;
    public Bitmap icon;

    public AppInfo(String packageName, String label, boolean system, boolean installed)
    {
        this.label = label;
        this.packageName = packageName;
        this.system = system;
        this.installed = installed;
    }
    public String getPackageName()
    {
        return packageName;
    }
    public String getLabel()
    {
        return label;
    }

    public void setDisabled(boolean disabled)
    {
        this.disabled = disabled;
    }
    public boolean isDisabled()
    {
        return disabled;
    }
    public boolean isSystem()
    {
        return system;
    }
    public boolean isInstalled()
    {
        return installed;
    }
    public int compareTo(AppInfo appInfo)
    {
        return label.compareToIgnoreCase(appInfo.getLabel());
    }
    public String toString()
    {
        return label + " : " + packageName;
    }
    public int describeContents()
    {
        return 0;
    }
    public void writeToParcel(Parcel out, int flags)
    {
        out.writeString(label);
        out.writeString(packageName);
        out.writeBooleanArray(new boolean[] {system, installed, checked});
        out.writeParcelable(icon, flags);
    }
    public static final Parcelable.Creator<AppInfo> CREATOR = new Parcelable.Creator<AppInfo>()
    {
        public AppInfo createFromParcel(Parcel in)
        {
            return new AppInfo (in);
        }
        public AppInfo[] newArray(int size)
        {
            return new AppInfo[size];
        }
    };
    protected AppInfo(Parcel in)
    {
        label = in.readString();
        packageName = in.readString();
        boolean[] bools = new boolean[4];
        in.readBooleanArray(bools);
        system = bools[0];
        installed = bools[1];
        checked = bools[2];
        icon = (Bitmap) in.readParcelable(getClass().getClassLoader());
    }
}
