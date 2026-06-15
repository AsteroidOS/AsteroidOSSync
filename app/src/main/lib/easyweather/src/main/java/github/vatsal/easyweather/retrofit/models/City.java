package github.vatsal.easyweather.retrofit.models;

/**
 * Created by
 * --Vatsal Bajpai under
 * --AppyWare on
 * --23/06/16 at
 * --1:49 AM in
 * --OpenWeatherMapDemo
 */
public class City {
    private Coord coord;

    private String id;

    private Sys sys;

    private String name;

    private String population;

    private String country;

    public Coord getCoord() {
        return coord;
    }

    public void setCoord(Coord coord) {
        this.coord = coord;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Sys getSys() {
        return sys;
    }

    public void setSys(Sys sys) {
        this.sys = sys;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPopulation() {
        return population;
    }

    public void setPopulation(String population) {
        this.population = population;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    @Override
    public String toString() {
        return "ClassPojo [coord = " + coord + ", id = " + id + ", sys = " + sys + ", name = " + name + ", population = " + population + ", country = " + country + "]";
    }
}
