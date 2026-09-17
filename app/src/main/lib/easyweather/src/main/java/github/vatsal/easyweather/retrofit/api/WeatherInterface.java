package github.vatsal.easyweather.retrofit.api;

import github.vatsal.easyweather.retrofit.models.ForecastResponseModel;
import github.vatsal.easyweather.retrofit.models.WeatherResponseModel;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherInterface {

    @GET("weather")
    Call<WeatherResponseModel> getCityWeather(@Query("appid") String appid,
                                              @Query("q") String city);

    @GET("weather")
    Call<WeatherResponseModel> getLocationWeather(@Query("appid") String appid,
                                                  @Query("lat") String latitude,
                                                  @Query("lon") String longitude);

    @GET("forecast")
    Call<ForecastResponseModel> getCityForcast(@Query("appid") String appid,
                                               @Query("q") String city);

    @GET("forecast")
    Call<ForecastResponseModel> getLocationForecast(@Query("appid") String appid,
                                                    @Query("lat") String latitude,
                                                    @Query("lon") String longitude);

}
