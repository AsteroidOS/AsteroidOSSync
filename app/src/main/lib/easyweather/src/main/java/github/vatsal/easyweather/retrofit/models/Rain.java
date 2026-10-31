package github.vatsal.easyweather.retrofit.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by
 * --Vatsal Bajpai under
 * --AppyWare on
 * --23/06/16 at
 * --1:50 AM in
 * --OpenWeatherMapDemo
 */

public class Rain {

    @SerializedName("3h")
    @Expose
    private Double _3h;

    public Double get3h() {
        return _3h;
    }

    public void set3h(Double _3h) {
        this._3h = _3h;
    }

}