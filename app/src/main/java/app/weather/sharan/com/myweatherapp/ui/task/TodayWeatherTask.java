package app.weather.sharan.com.myweatherapp.ui.task;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import app.weather.sharan.com.myweatherapp.R;
import app.weather.sharan.com.myweatherapp.ui.activities.MainActivity;
import app.weather.sharan.com.myweatherapp.ui.model.WeatherModel;
import app.weather.sharan.com.myweatherapp.ui.util.Constants;
import app.weather.sharan.com.myweatherapp.ui.util.TaskCompleted;

/**
 * Created by Sharan on 9/18/2016.
 */
public class TodayWeatherTask extends GenericRequestTask {

    public TaskCompleted taskCompletedListener;
    public WeatherModel weatherModel;
    private Activity activity;
    private Context context;
    private ProgressDialog progressDialog;

    private int loading;

    private List<WeatherModel> longTermWeather = new ArrayList<>();
    private List<WeatherModel> longTermTodayWeather = new ArrayList<>();
    private List<WeatherModel> longTermTomorrowWeather = new ArrayList<>();

    public TodayWeatherTask(MainActivity activity, Context context, ProgressDialog progressDialog, MainActivity mainActivity) {
        super(context, activity, progressDialog);
        this.activity = activity;
        this.context = context;
        this.progressDialog = progressDialog;
        this.taskCompletedListener = mainActivity;
        weatherModel = new WeatherModel();
    }

    @Override
    protected void onPreExecute() {
        loading = 0;
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(TaskOutput taskOutput) {
        super.onPostExecute(taskOutput);
    }

    @Override
    protected String getAPIName() {
        return "weather";
    }

    @Override
    protected void updateMainUI() {
        taskCompletedListener.onTaskCompleted(this);
    }

    @Override
    protected ParseResult parseResponse(String response) {
        return parseTodayWeather(response);
    }

    private ParseResult parseTodayWeather(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            final String cod = jsonObject.getString("cod");
            if ("404".equals(cod)) {
                return ParseResult.CITY_NOT_FOUND;
            }

            String cityName = jsonObject.getString("name");
            String countryName = "";
            JSONObject countryObject = jsonObject.getJSONObject("sys");
            if (countryObject != null) {
                countryName = countryObject.getString("country");
                weatherModel.setCity(cityName);
                weatherModel.setCountry(countryName);
                weatherModel.setSunrise(countryObject.getString("sunrise"));
                weatherModel.setSunset(countryObject.getString("sunset"));
            }

            JSONObject coordinatesObject = jsonObject.getJSONObject("coord");
            if (coordinatesObject != null) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
                sp.edit().putFloat(Constants.KEY_LATITUDE, (float) coordinatesObject.getDouble("lat")).putFloat(Constants.KEY_LONGITUDE, (float) coordinatesObject.getDouble("lon")).apply();
            }

            JSONObject mainObject = jsonObject.getJSONObject("main");
            if (mainObject != null) {
                weatherModel.setTemperature(mainObject.getString("temp"));
            }

            weatherModel.setDescription(jsonObject.getJSONArray("weather").getJSONObject(0).getString("description"));
            JSONObject windObject = jsonObject.getJSONObject("wind");
            if (windObject != null) {
                weatherModel.setWind(windObject.getString("speed"));
                if (windObject.has("deg")) {
                    weatherModel.setWindDirectionDegree(windObject.getDouble("deg"));
                } else {
                    weatherModel.setWindDirectionDegree(null);
                }
            }
            weatherModel.setPressure(mainObject.getString("pressure"));
            weatherModel.setHumidity(mainObject.getString("humidity"));

            String rain = "";
            try {
                JSONObject rainObject = jsonObject.getJSONObject("rain");
                if (rainObject != null) {
                    rain = getStringRain(rainObject);
                } else {
                    JSONObject snowObject = jsonObject.getJSONObject("snow");
                    if (rainObject != null) {
                        rain = getStringRain(snowObject);
                    } else {
                        rain = "0";
                    }
                }
            } catch (Exception exp) {
                exp.printStackTrace();
                rain = "0";
            }
            if (rain.equalsIgnoreCase("fail"))
                rain = "0";
            weatherModel.setRain(rain);
            final String id = jsonObject.getJSONArray("weather").getJSONObject(0).getString("id");
            weatherModel.setId(id);

            weatherModel.setIcon(setWeatherIcon(Integer.parseInt(id), Calendar.getInstance().get(Calendar.HOUR_OF_DAY)));

            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            sp.edit().putString(Constants.KEY_TODAY_WEATHER_RESPONSE, response).apply();

        } catch (Exception exp) {
            exp.printStackTrace();
            return ParseResult.JSON_EXCEPTION;
        }
        return ParseResult.OK;
    }

    private String setWeatherIcon(int originalId, int hoursOfDay) {
        int id = originalId / 100;
        String icon = "";
        if (id == 800) {
            if (hoursOfDay >= 7 && hoursOfDay <= 20) {
                icon = context.getString(R.string.weather_sunny);
            } else {
                icon = context.getString(R.string.weather_clear_night);
            }
        } else {
            switch (id) {
                case 2:
                    icon = context.getString(R.string.weather_thunder);
                    break;
                case 3:
                    icon = context.getString(R.string.weather_drizzle);
                    break;
                case 7:
                    icon = context.getString(R.string.weather_foggy);
                    break;
                case 8:
                    icon = context.getString(R.string.weather_cloudy);
                    break;
                case 6:
                    icon = context.getString(R.string.weather_snowy);
                    break;
                case 5:
                    icon = context.getString(R.string.weather_rainy);
                    break;
            }
        }
        return icon;
    }

    private String getStringRain(JSONObject rainObject) {
        String rain = "0";
        if (rainObject != null) {
            rain = rainObject.optString("3h", "fail");
        } else {
            rain = rainObject.optString("1h", "0");
        }
        return rain;
    }

    public WeatherModel getWeatherModel() {
        return weatherModel;
    }
}
