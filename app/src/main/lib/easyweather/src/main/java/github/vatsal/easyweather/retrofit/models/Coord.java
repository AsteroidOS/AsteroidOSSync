package github.vatsal.easyweather.retrofit.models;

/**
 * Created by
 * --Vatsal Bajpai under
 * --AppyWare on
 * --22/06/16 at
 * --8:23 PM in
 * --OpenWeatherMapDemo
 */
public class Coord {
    private String lon;

    private String lat;

    public String getLon() {
        return lon;
    }

    public void setLon(String lon) {
        this.lon = lon;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    @Override
    public String toString() {
        return "ClassPojo [lon = " + lon + ", lat = " + lat + "]";
    }
}
