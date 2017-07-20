package app.weather.sharan.com.myweatherapp.ui.task;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONArray;
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
 * Created by sharana.b on 9/19/2016.
 */
public class LongTermWeatherTask extends GenericRequestTask {

    private Context context;
    private Activity activity;
    private ProgressDialog progressDialog;
    private TaskCompleted taskCompletedListener;
    private WeatherModel weatherModel;
    private SharedPreferences sp;


    private List<WeatherModel> longTermWeather = new ArrayList<>();
    private List<WeatherModel> longTermTodayWeather = new ArrayList<>();
    private List<WeatherModel> longTermTomorrowWeather = new ArrayList<>();

    public LongTermWeatherTask(Context context, MainActivity activity, ProgressDialog progressDialog) {
        super(context, activity, progressDialog);
        this.context = context;
        this.activity = activity;
        this.progressDialog = progressDialog;
        this.taskCompletedListener = activity;
        sp = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    protected String getAPIName() {
        return "forecast";
    }

    @Override
    protected ParseResult parseResponse(String response) {
        return parseLongTermJson(response);
    }

    private ParseResult parseLongTermJson(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            if (jsonObject != null) {
                String code = jsonObject.getString("cod");
                if ("404".equalsIgnoreCase(code)) {
                    if (longTermWeather == null) {
                        longTermWeather = new ArrayList<WeatherModel>();
                        longTermTodayWeather = new ArrayList<WeatherModel>();
                        longTermTomorrowWeather = new ArrayList<WeatherModel>();
                    }
                    return ParseResult.CITY_NOT_FOUND;
                }

                longTermWeather = new ArrayList<WeatherModel>();
                longTermTodayWeather = new ArrayList<WeatherModel>();
                longTermTomorrowWeather = new ArrayList<WeatherModel>();

                JSONArray weatherList = jsonObject.getJSONArray("list");
                for (int i = 0; i < weatherList.length(); i++) {
                    weatherModel = new WeatherModel();

                    JSONObject listItem = weatherList.getJSONObject(i);
                    JSONObject mainObject = listItem.getJSONObject("main");

                    weatherModel.setTemperature(mainObject.getString("temp"));
                    weatherModel.setDate(listItem.getString("dt"));

                    weatherModel.setDescription(listItem.getJSONArray("weather").getJSONObject(0).getString("description"));
                    JSONObject windObject = listItem.getJSONObject("wind");
                    if (windObject != null) {
                        weatherModel.setWind(windObject.getString("speed"));
                        if (windObject.has("deg")) {
                            weatherModel.setWindDirectionDegree(Double.parseDouble(windObject.getString("deg")));
                        } else {
                            weatherModel.setWindDirectionDegree(0.0);
                        }
                    }

                    weatherModel.setHumidity(mainObject.getString("humidity"));
                    weatherModel.setPressure(mainObject.getString("pressure"));

                    String rain = "";
                    try {
                        JSONObject rainObject = mainObject.getJSONObject("rain");
                        JSONObject snowObject = mainObject.getJSONObject("snow");
                        if (rainObject != null) {
                            rain = getRainString(rainObject);
                        } else if (snowObject != null) {
                            rain = getRainString(snowObject);
                        }
                    } catch (Exception exp) {
                        rain = "0";
                    }
                    weatherModel.setRain(rain);

                    final String id = listItem.getJSONArray("weather").getJSONObject(0).getString("id");
                    weatherModel.setId(id);

                    String dateMsString = listItem.getString("dt") + "000";
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(Long.parseLong(dateMsString));
                    weatherModel.setIcon(setWeatherIcon(Integer.parseInt(id), calendar.get(Calendar.HOUR_OF_DAY)));
                    Calendar today = Calendar.getInstance();
                    if (calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                        longTermTodayWeather.add(weatherModel);
                    } else if (calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) + 1) {
                        longTermTomorrowWeather.add(weatherModel);
                    } else {
                        longTermWeather.add(weatherModel);
                    }
                }

                sp.edit().putString(Constants.KEY_LONG_TERM_WEATHER_RESPONSE, response).apply();
            }
        } catch (Exception exp) {
            exp.printStackTrace();
            return ParseResult.JSON_EXCEPTION;
        }
        return ParseResult.OK;
    }

    @Override
    protected void updateMainUI() {
        taskCompletedListener.onTaskCompleted(this);
    }

    private String setWeatherIcon(int originalId, int hoursOfDay) {
        int id = originalId / 100;
        String icon = "";
        if (originalId == 800) {
            if (hoursOfDay >= 7 && hoursOfDay < 20) {
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

    private String getRainString(JSONObject rainObject) {
        String rain = "";
        if (rainObject != null) {
            rain = rainObject.optString("3h", "fail");
            if ("fail".equalsIgnoreCase(rain)) {
                rain = rainObject.optString("1h", "0");
            }
        }
        return rain;
    }

    public List<WeatherModel> getLongTermTodayWeatherList() {
        return longTermTodayWeather;
    }

    public List<WeatherModel> getLongTermTomorrowWeatherList() {
        return longTermTomorrowWeather;
    }

    public List<WeatherModel> getLongTermWeatherList() {
        return longTermWeather;
    }
}
