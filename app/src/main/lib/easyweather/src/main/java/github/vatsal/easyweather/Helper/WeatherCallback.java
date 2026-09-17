package github.vatsal.easyweather.Helper;

import github.vatsal.easyweather.retrofit.models.WeatherResponseModel;

/**
 * Created by
 --Vatsal Bajpai on
 --6/23/2016 at
 --4:29 PM
 */
public abstract class WeatherCallback {

    public abstract void success(WeatherResponseModel response);

    public abstract void failure(String message);
}
