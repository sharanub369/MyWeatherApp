package app.weather.sharan.com.myweatherapp.ui.task;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import app.weather.sharan.com.myweatherapp.R;
import app.weather.sharan.com.myweatherapp.ui.util.Constants;

/**
 * Created by sharana.b on 9/20/2016.
 */
public class LongTermWeatherForReceiverTask extends AsyncTask<String, String, String> {

    private Context context;
    private SharedPreferences sp;

    public LongTermWeatherForReceiverTask() {
    }

    public LongTermWeatherForReceiverTask(Context context) {
        this.context = context;
        sp = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    protected String doInBackground(String... params) {
        String language = Locale.getDefault().getLanguage();
        String response = "";
        if (language.equalsIgnoreCase("cs")) language = "cz";
        String apiKey = sp.getString(Constants.KEY_API_KEY, context.getResources().getString(R.string.apiKey));
        try {
            URL url = new URL(Constants.KEY_URL_BASE_LONG + (sp.getString(Constants.KEY_CITY, Constants.DEFAULT_CITY) + "&lang=" + language + "&appid=" + apiKey));
            Log.d("LONGTERM==", url + "==url");
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            if (httpURLConnection.getResponseCode() == 200) {
                InputStreamReader inputStream = new InputStreamReader(httpURLConnection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(inputStream);
                String line = "";
                while ((line = bufferedReader.readLine()) != null) {
                    response += line + "\n";
                }
                Log.d("LONGTERM==", response + "==response");
                if (!response.isEmpty()) {
                    sp.edit().putString(Constants.KEY_LONG_TERM_WEATHER_RESPONSE, response).apply();
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
