package github.vatsal.easyweather.Helper;

/**
 * Created by
 * --Vatsal Bajpai on
 * --26/06/16 at
 * --7:13 PM in
 */
public class TempUnitConverter {

    public static Double convertToCelsius(String kelvin) throws NumberFormatException {
        double inKelvin;
        try {
            inKelvin = Double.parseDouble(kelvin);
        } catch (NumberFormatException e) {
            throw e;
        }
        return inKelvin - 273.15;
    }

    public static Double convertToFahrenheit(String kelvin) throws NumberFormatException {
        double inKelvin;
        try {
            inKelvin = Double.parseDouble(kelvin);
        } catch (NumberFormatException e) {
            throw e;
        }
        return (inKelvin - 273.15) * 1.8000 + 32.00;
    }

}
