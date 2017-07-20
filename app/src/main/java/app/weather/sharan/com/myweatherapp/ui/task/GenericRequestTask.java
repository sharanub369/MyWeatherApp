package app.weather.sharan.com.myweatherapp.ui.task;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;

import app.weather.sharan.com.myweatherapp.R;
import app.weather.sharan.com.myweatherapp.ui.activities.MainActivity;
import app.weather.sharan.com.myweatherapp.ui.util.Constants;

/**
 * Created by sharana.b on 9/17/2016.
 */
public abstract class GenericRequestTask extends AsyncTask<String, String, TaskOutput> {

    private URL url;
    ProgressDialog progressDialog;
    Context context;
    MainActivity activity;
    public int loading = 0;

    public GenericRequestTask(Context context, MainActivity activity, ProgressDialog progressDialog) {
        this.context = context;
        this.activity = activity;
        this.progressDialog = progressDialog;
    }

    @Override
    protected void onPreExecute() {
        incLoadingCounter();
        if (!progressDialog.isShowing()) {
            progressDialog.setMessage(context.getString(R.string.downloading_data));
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
        }
    }

    @Override
    protected TaskOutput doInBackground(String... params) {
        TaskOutput output = new TaskOutput();

        String response = "";
        String[] coords=null;
        if (params != null && params.length > 0) {
            String param0 = params[0];
            if (Constants.KEY_CACHED_RESPONSE.equalsIgnoreCase(param0)) {
                response = params[1];
                output.taskResult = TaskResult.SUCCESS;
            } else if (param0.equalsIgnoreCase("coords")) {
                coords = new String[]{params[1], params[2]};
            }
        }

        if (response.isEmpty()) {
            try {
                url = getURL(coords);
                Log.d("TESTING1", url + "==url");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                if (urlConnection.getResponseCode() == 200) {
                    InputStreamReader streamReader = new InputStreamReader(urlConnection.getInputStream());
                    BufferedReader bufferedReader = new BufferedReader(streamReader);
                    String line = "";
                    while ((line = bufferedReader.readLine()) != null) {
                        response += line + "\n";
                    }
                    bufferedReader.close();
                    urlConnection.disconnect();

                    output.taskResult = TaskResult.SUCCESS;
                    activity.saveLastUpdateTime(PreferenceManager.getDefaultSharedPreferences(activity));
                } else if (urlConnection.getResponseCode() == 429) {
                    Log.d("REQUEST", "To many requests........");
                    output.taskResult = TaskResult.TOO_MANY_REQUESTS;
                } else {
                    Log.d("REQUEST", "Bad Response........");
                    output.taskResult = TaskResult.BAD_RESPONSE;
                }
            } catch (Exception exp) {
                exp.printStackTrace();
                output.taskResult = TaskResult.IO_EXCEPTION;
            }

            if (TaskResult.SUCCESS == output.taskResult) {
                // Parse JSON data
                ParseResult parseResult = parseResponse(response);
                if (parseResult.CITY_NOT_FOUND.equals(parseResult)) {
                    // Retain previously specified city if current one was not recognized
                    restorePreviousCity();
                }
                output.parseResult = parseResult;
            }
        }
        return output;
    }

    @Override
    protected void onPostExecute(TaskOutput taskOutput) {
        super.onPostExecute(taskOutput);
        if (loading == 1) {
            progressDialog.dismiss();
        }
        decLoadingCounter();

        updateMainUI();

        handleTaskOutput(taskOutput);
    }

    protected final void handleTaskOutput(TaskOutput output) {
        switch (output.taskResult) {
            case SUCCESS: {
                ParseResult parseResult = output.parseResult;
                if (ParseResult.CITY_NOT_FOUND.equals(parseResult)) {
                    Snackbar.make(activity.findViewById(android.R.id.content), context.getString(R.string.msg_city_not_found), Snackbar.LENGTH_LONG).show();
                } else if (ParseResult.JSON_EXCEPTION.equals(parseResult)) {
                    Snackbar.make(activity.findViewById(android.R.id.content), context.getString(R.string.msg_err_parsing_json), Snackbar.LENGTH_LONG).show();
                }
                break;
            }
            case TOO_MANY_REQUESTS: {
                Snackbar.make(activity.findViewById(android.R.id.content), context.getString(R.string.msg_too_many_requests), Snackbar.LENGTH_LONG).show();
                break;
            }
            case BAD_RESPONSE: {
                Snackbar.make(activity.findViewById(android.R.id.content), context.getString(R.string.invalid_api_key), Snackbar.LENGTH_LONG).show();
                break;
            }
            case IO_EXCEPTION: {
                Snackbar.make(activity.findViewById(android.R.id.content), context.getString(R.string.msg_connection_not_available), Snackbar.LENGTH_LONG).show();
                break;
            }
        }
    }

    private void restorePreviousCity() {
        if (!TextUtils.isEmpty(activity.recentCity)) {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putString("city", activity.recentCity);
            editor.commit();
            activity.recentCity = "";
        }
    }

    private URL getURL(String[] cords) throws UnsupportedEncodingException, MalformedURLException {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String apiKey = sp.getString("apiKey", activity.getResources().getString(R.string.apiKey));
        StringBuilder urlBuilder = new StringBuilder("http://api.openweathermap.org/data/2.5/");
        urlBuilder.append(getAPIName()).append("?");

        if (cords!= null) {
            urlBuilder.append("lat=").append(cords[0]).append("&lon=").append(cords[1]);
        } else {
            final String city = sp.getString("city", Constants.DEFAULT_CITY);
            urlBuilder.append("q=").append(URLEncoder.encode(city, "UTF-8"));
        }

        urlBuilder.append("&lang=").append(getLanguage());
        urlBuilder.append("&mode=json");
        urlBuilder.append("&appid=").append(apiKey);
        return new URL(urlBuilder.toString());
    }

    private String getLanguage() {
        String language = Locale.getDefault().getLanguage();
        if (language.equals("cs")) {
            language = "cz";
        }
        return language;
    }

    private void incLoadingCounter() {
        loading++;
    }

    private void decLoadingCounter() {
        loading--;
    }

    protected abstract String getAPIName();

    protected abstract ParseResult parseResponse(String response);

    protected void updateMainUI() {
    }
}
