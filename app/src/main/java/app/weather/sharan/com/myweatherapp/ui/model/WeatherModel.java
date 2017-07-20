package app.weather.sharan.com.myweatherapp.ui.model;

import android.content.Context;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import app.weather.sharan.com.myweatherapp.R;

/**
 * Created by Sharan on 9/18/2016.
 */
public class WeatherModel {


    public enum WindDirection {
        // don't change order
        NORTH, NORTH_NORTH_EAST, NORTH_EAST, EAST_NORTH_EAST,
        EAST, EAST_SOUTH_EAST, SOUTH_EAST, SOUTH_SOUTH_EAST,
        SOUTH, SOUTH_SOUTH_WEST, SOUTH_WEST, WEST_SOUTH_WEST,
        WEST, WEST_NORTH_WEST, NORTH_WEST, NORTH_NORTH_WEST;

        public static WindDirection byDegree(double degree) {
            return byDegree(degree, WindDirection.values().length);
        }

        public static WindDirection byDegree(double degree, int numberOfDirections) {
            WindDirection[] directions = WindDirection.values();
            int availableNumberOfDirections = directions.length;

            int direction = windDirectionDegreeToIndex(degree, numberOfDirections)
                    * availableNumberOfDirections / numberOfDirections;

            return directions[direction];
        }

        public String getLocalizedString(Context context) {
            // usage of enum.ordinal() is not recommended, but whatever
            return context.getResources().getStringArray(R.array.windDirections)[ordinal()];
        }

        public String getArrow(Context context) {
            // usage of enum.ordinal() is not recommended, but whatever
            return context.getResources().getStringArray(R.array.windDirectionArrows)[ordinal() / 2];
        }
    }

    // you may use values like 4, 8, etc. for numberOfDirections
    public static int windDirectionDegreeToIndex(double degree, int numberOfDirections) {
        // to be on the safe side
        degree %= 360;
        if (degree < 0) degree += 360;

        degree += 180 / numberOfDirections; // add offset to make North start from 0

        int direction = (int) Math.floor(degree * numberOfDirections / 360);

        return direction % numberOfDirections;
    }

    public long getNumDaysFrom(Date initialDate) {
        Calendar initial = Calendar.getInstance();
        initial.setTime(initialDate);
        initial.set(Calendar.MILLISECOND, 0);
        initial.set(Calendar.SECOND, 0);
        initial.set(Calendar.MINUTE, 0);
        initial.set(Calendar.HOUR_OF_DAY, 0);

        Calendar me = Calendar.getInstance();
        me.setTime(this.date);
        me.set(Calendar.MILLISECOND, 0);
        me.set(Calendar.SECOND, 0);
        me.set(Calendar.MINUTE, 0);
        me.set(Calendar.HOUR_OF_DAY, 0);

        return Math.round((me.getTimeInMillis() - initial.getTimeInMillis()) / 86400000.0);
    }

    private String city;
    private String country;
    private Date date;
    private String temperature;
    private String description;
    private String wind;
    private Double windDirectionDegree;
    private String pressure;
    private String humidity;
    private String rain;
    private String id;
    private String icon;
    private String lastUpdated;
    private Date sunrise;
    private Date sunset;

    public void setCity(String city) {
        this.city = city;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setDate(String dateString) {
        try {
            this.date = new Date(Long.parseLong(dateString) * 1000);
        } catch (Exception exp) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd--MM--yyyy HH:mm:ss", Locale.ENGLISH);
            try {
                this.date = dateFormat.parse(dateString);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setWind(String wind) {
        this.wind = wind;
    }

    public void setWindDirectionDegree(Double windDirectionDegree) {
        this.windDirectionDegree = windDirectionDegree;
    }

    public void setPressure(String pressure) {
        this.pressure = pressure;
    }

    public void setHumidity(String humidity) {
        this.humidity = humidity;
    }

    public void setRain(String rain) {
        this.rain = rain;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public void setSunrise(String dateString) {
        try {
            this.sunrise = new Date(Long.parseLong(dateString) * 1000);
        } catch (Exception exp) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd--MM--yyyy HH:mm:ss", Locale.ENGLISH);
            try {
                this.sunrise = dateFormat.parse(dateString);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    public void setSunset(String dateString) {
        try {
            this.sunset = new Date(Long.parseLong(dateString) * 1000);
        } catch (Exception exp) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd--MM--yyyy HH:mm:ss", Locale.ENGLISH);
            try {
                this.sunset = dateFormat.parse(dateString);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }



    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public Date getDate() {
        return date;
    }

    public String getTemperature() {
        return temperature;
    }

    public String getDescription() {
        return description;
    }

    public String getWind() {
        return wind;
    }

    public Double getWindDirectionDegree() {
        return windDirectionDegree;
    }

    public String getPressure() {
        return pressure;
    }

    public String getHumidity() {
        return humidity;
    }

    public String getRain() {
        return rain;
    }

    public String getId() {
        return id;
    }

    public String getIcon() {
        return icon;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public Date getSunrise() {
        return sunrise;
    }

    public Date getSunset() {
        return sunset;
    }

    public boolean isWindDirectionAvailable() {
        return windDirectionDegree != null;
    }

    public WindDirection getWindDirection(int numberOfDirections) {
        return WindDirection.byDegree(windDirectionDegree, numberOfDirections);
    }

    public WindDirection getWindDirection() {
        return WindDirection.byDegree(windDirectionDegree);
    }
}
