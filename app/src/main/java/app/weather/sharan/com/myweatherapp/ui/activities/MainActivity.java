package app.weather.sharan.com.myweatherapp.ui.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.api.GoogleAPI;
import com.google.api.GoogleAPIException;
import com.google.api.translate.Language;
import com.google.api.translate.Translate;
import com.google.api.translate.TranslateV2;

import java.security.Permission;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import app.weather.sharan.com.myweatherapp.R;
import app.weather.sharan.com.myweatherapp.ui.adapter.ViewPageAdapter;
import app.weather.sharan.com.myweatherapp.ui.adapter.WeatherRecyclerAdapter;
import app.weather.sharan.com.myweatherapp.ui.fragment.RecyclerViewFragment;
import app.weather.sharan.com.myweatherapp.ui.model.WeatherModel;
import app.weather.sharan.com.myweatherapp.ui.receiver.AlarmReceiver;
import app.weather.sharan.com.myweatherapp.ui.task.LongTermWeatherTask;
import app.weather.sharan.com.myweatherapp.ui.task.TodayWeatherTask;
import app.weather.sharan.com.myweatherapp.ui.util.Constants;
import app.weather.sharan.com.myweatherapp.ui.util.NetworkState;
import app.weather.sharan.com.myweatherapp.ui.util.TaskCompleted;
import app.weather.sharan.com.myweatherapp.ui.util.UnitConverter;
import butterknife.ButterKnife;
import butterknife.InjectView;

public class MainActivity extends AppCompatActivity implements TaskCompleted, LocationListener {

    private static final String TAG = "MyWeatherApp";
    protected static final int MY_PERMISSIONS_ACCESS_FINE_LOCATION = 1;

    // Time in milliseconds; only reload weather if last update is longer ago than this value
    private static final int NO_UPDATE_REQUIRED_THRESHOLD = 200000;

    private NetworkState networkState;
    private ProgressDialog mProgressDialog;
    private SharedPreferences prefs;
    private int theme;

    //Initialize view Elements
    @InjectView(R.id.mainLyt)
    CoordinatorLayout mainLyt;
    @InjectView(R.id.toolbar)
    Toolbar toolbar;
    @InjectView(R.id.todayTempTxt)
    TextView todayTempTxt;
    @InjectView(R.id.todayDescriptionTxt)
    TextView todayDescriptionTxt;
    @InjectView(R.id.todayWindTxt)
    TextView todayWindTxt;
    @InjectView(R.id.todayPressureTxt)
    TextView todayPressureTxt;
    @InjectView(R.id.todayHumidityTxt)
    TextView todayHumidityTxt;
    @InjectView(R.id.todaySunriseTxt)
    TextView todaySunriseTxt;
    @InjectView(R.id.todaySunsetTxt)
    TextView todaySunsetTxt;
    @InjectView(R.id.todayIconTxt)
    TextView todayIconTxt;
    @InjectView(R.id.lastUpdateTimeTxt)
    TextView lastUpdateTimeTxt;
    @InjectView(R.id.tabsLyt)
    TabLayout tabsLyt;
    @InjectView(R.id.viewPager)
    ViewPager viewPager;

    private static boolean mappingsInitialised = false;
    private static Map<String, Integer> speedUnits = new HashMap<String, Integer>();
    private static Map<String, Integer> pressureUnits = new HashMap<String, Integer>();

    public String recentCity = "";
    private String updatedDay;
    private String mLatitude;
    private String mLongitude;

    private String[] coords;

    private List<WeatherModel> longTermWeather = new ArrayList<>();
    private List<WeatherModel> longTermTodayWeather = new ArrayList<>();
    private List<WeatherModel> longTermTomorrowWeather = new ArrayList<>();

    private Boolean isActivityDestroyed = false;
    private Boolean isFromAutoLocation = false;

    View appView;
    LocationManager mLocationManager;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;
    private Typeface mFont;
    private String mLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize the associated SharedPreferences file with default values
        PreferenceManager.setDefaultValues(this, R.xml.prefs, false);
        //Setting theme
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        setTheme(theme = getTheme(prefs.getString("theme", "fresh")));
        boolean darkTheme = theme == R.style.AppTheme_NoActionBar_Dark || theme == R.style.AppTheme_NoActionBar_Classic_Dark;
        //set view for launcher activity
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
        //Checking for selected language
        mLanguage = prefs.getString(Constants.KEY_LANGUAGE, "English");
        setSelectedLanguage(mLanguage);
        mProgressDialog = new ProgressDialog(this);
        //set them for action bar title(toolbar)
        setSupportActionBar(toolbar);
        if (darkTheme) {
            toolbar.setPopupTheme(R.style.AppTheme_AppBarOverlay);
        }
        isActivityDestroyed = false;
        networkState = new NetworkState(this);
        initMappings();

        try {
            Bundle bundle = getIntent().getExtras();
            isFromAutoLocation = bundle.getBoolean("FROMAUTOLOCATION", false);
            if (isFromAutoLocation) {
                mLatitude = String.valueOf(prefs.getFloat(Constants.KEY_LATITUDE, 0));
                mLongitude = String.valueOf(prefs.getFloat(Constants.KEY_LONGITUDE, 0));
            }
        } catch (Exception exp) {
            isFromAutoLocation = false;
        } finally {
            // Preload data from cache
            preloadWeather();
            updateLastUpdateTime();

            //set alarm to Auto update receiver weather report
            AlarmReceiver.setRecurringAlarm(this);
        }

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    private void preloadWeather() {
        if (!isFromAutoLocation) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            String lastTodayWeather = sp.getString(Constants.KEY_TODAY_WEATHER_RESPONSE, "");
            String[] params;
            if (!lastTodayWeather.isEmpty()) {
                updateLastUpdateTime();
                params = new String[]{Constants.KEY_CACHED_RESPONSE, lastTodayWeather};
                new TodayWeatherTask(this, this, mProgressDialog, this).execute(params);
            }

            String longTermWeatherResponse = sp.getString(Constants.KEY_LONG_TERM_WEATHER_RESPONSE, "");
            if (!longTermWeatherResponse.isEmpty()) {
                params = new String[]{Constants.KEY_CACHED_RESPONSE, longTermWeatherResponse};
                new LongTermWeatherTask(this, this, mProgressDialog).execute(params);
            }
        } else {
            coords = new String[]{"coords", mLatitude, mLongitude};
            new TodayWeatherTask(this, this, mProgressDialog, this).execute(coords);
            new LongTermWeatherTask(this, this, mProgressDialog).execute(coords);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getTheme(PreferenceManager.getDefaultSharedPreferences(this).getString("theme", "fresh")) != theme) {
            // Restart activity to apply theme
            overridePendingTransition(0, 0);
            finish();
            overridePendingTransition(0, 0);
            startActivity(getIntent());
        } else if (networkState.isNetworkAvailable() && shouldUpdate()) {
            getTodayWeatherReport();
            getLongTermWeather();
        }
    }

    private void setSelectedLanguage(String mLanguage) {
        String currentLanguage = "en";
        if (mLanguage.equalsIgnoreCase("Kannada")) {
            currentLanguage = "kn";
            mFont = Typeface.createFromAsset(getAssets(), "fonts/akshar.ttf");
        } else if (mLanguage.equalsIgnoreCase("Telugu")) {
            currentLanguage = "te";
            mFont = Typeface.createFromAsset(getAssets(), "fonts/gautami.ttf");
        } else if (mLanguage.equalsIgnoreCase("English")) {
            mFont = null;
        }
        Locale locale = new Locale(currentLanguage);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }

    private int getTheme(String theme) {
        switch (theme) {
            case "fresh":
                return R.style.AppTheme_NoActionBar;
            case "classic":
                return R.style.AppTheme_Classic;
            case "classicdark":
                return R.style.AppTheme_Classic_Dark;
            case "dark":
                return R.style.AppTheme_Dark;
            default:
                return R.style.AppTheme_NoActionBar;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_refresh:
                if (networkState.isNetworkAvailable()) {
                    getTodayWeatherReport();
                    getLongTermWeather();
                } else {
                    Snackbar.make(mainLyt, R.string.msg_connection_not_available, Snackbar.LENGTH_LONG).show();
                }
                break;
            case R.id.action_search:
                searchCities();
                return true;

            case R.id.action_settings:
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                break;

            case R.id.action_location:
                getCityByLocation();
                break;

            case R.id.action_map:
                Intent intent1 = new Intent(this, WeatherMapActivity.class);
                startActivity(intent1);
                break;

            case R.id.action_graphs:
                Intent intent2 = new Intent(this, GraphActivity.class);
                startActivity(intent2);
                break;

            case R.id.action_about:
                showAboutUsDialog();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    public void getCityByLocation() {
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_ACCESS_FINE_LOCATION);
            }
        } else {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(getString(R.string.getting_location));
            mProgressDialog.setTitle("Location");
            mProgressDialog.setCancelable(false);
            mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    mLocationManager.removeUpdates(MainActivity.this);
                }
            });

            mProgressDialog.show();

            if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
            } else if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            } else {
                requestToSetLocationSetting();
            }
        }
    }

    public void requestToSetLocationSetting() {
        final AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
        mBuilder.setTitle(getString(R.string.location_settings));
        mBuilder.setMessage(R.string.location_settings_message);
        mBuilder.setPositiveButton(getString(R.string.location_settings_button), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCALE_SETTINGS);
                startActivity(intent);
            }
        });
        mBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        mBuilder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_ACCESS_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCityByLocation();
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean isUpdateAuto = sp.getBoolean(Constants.KEY_UPDATE_LOCATION_AUTOMATICALLY, false);
        double longitude, latitude;
        longitude = location.getLongitude();
        latitude = location.getLatitude();
        if (longitude != 0 && latitude != 0) {
            sp.edit().putFloat(Constants.KEY_LATITUDE, (float) latitude).putFloat(Constants.KEY_LONGITUDE, (float) longitude).apply();
            isFromAutoLocation = true;
            mLongitude = String.valueOf(longitude);
            mLatitude = String.valueOf(latitude);
            getTodayWeatherReport();
            getLongTermWeather();
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLocationManager.removeUpdates(MainActivity.this);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }


    private void searchCities() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(this.getString(R.string.search_title));
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setMaxLines(1);
        input.setSingleLine(true);
        alert.setView(input, 32, 0, 32, 0);
        alert.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String enteredCity = input.getText().toString();
                if (!enteredCity.isEmpty()) {
                    saveLocation(enteredCity);
                }
            }
        });
        alert.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Cancelled
            }
        });
        alert.show();
    }

    private void saveLocation(String enteredCity) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        recentCity = sp.getString(Constants.KEY_CITY, Constants.DEFAULT_CITY);
        isFromAutoLocation = false;
        sp.edit().putString(Constants.KEY_CITY, enteredCity).apply();
        if (!recentCity.equalsIgnoreCase(enteredCity)) {
            getTodayWeatherReport();
            getLongTermWeather();
        }
    }

    public void initMappings() {
        if (mappingsInitialised)
            return;
        mappingsInitialised = true;
        speedUnits.put("m/s", R.string.speed_unit_mps);
        speedUnits.put("kph", R.string.speed_unit_kph);
        speedUnits.put("mph", R.string.speed_unit_mph);

        pressureUnits.put("hpa", R.string.pressure_unit_hpa);
        pressureUnits.put("kpa", R.string.pressure_unit_kpa);
        pressureUnits.put("mm Hg", R.string.pressure_unit_mmhg);
    }

    private boolean shouldUpdate() {
        // Update if never checked or last update is longer ago than specified threshold
        long lastUpdateTime = PreferenceManager.getDefaultSharedPreferences(this).getLong(Constants.KEY_LAST_UPDATE_TIME, -1);
        Boolean isCityChanged = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.KEY_CITY_CHANGED, false);
        //return isCityChanged || lastUpdateTime < 0 || (Calendar.getInstance().getTimeInMillis() - lastUpdateTime) < NO_UPDATE_REQUIRED_THRESHOLD;
        return true;
    }

    public void getTodayWeatherReport() {
        if (!isFromAutoLocation) {
            new TodayWeatherTask(this, this, mProgressDialog, this).execute();
        } else {
            coords = new String[]{"coords", mLatitude, mLongitude};
            new TodayWeatherTask(this, this, mProgressDialog, this).execute(coords);
        }
    }

    public void getLongTermWeather() {
        if (!isFromAutoLocation) {
            new LongTermWeatherTask(this, this, mProgressDialog).execute();
        } else {
            coords = new String[]{"coords", mLatitude, mLongitude};
            new LongTermWeatherTask(this, this, mProgressDialog).execute(coords);
        }

    }

    public void saveLastUpdateTime(SharedPreferences sp) {
        SharedPreferences.Editor spEdit = sp.edit();
        spEdit.putLong(Constants.KEY_LAST_UPDATE_TIME, Calendar.getInstance().getTimeInMillis()).apply();
    }

    public void updateTodayWeatherUI(WeatherModel model) {
        try {
            if (model.getCountry().isEmpty()) {
                preloadWeather();
                return;
            }
        } catch (Exception e) {
            //preloadWeather();
            return;
        }

        String city = model.getCity();
        String country = model.getCountry();
        recentCity = city;
        prefs.edit().putString(Constants.KEY_CITY, recentCity).apply();
        //  getSupportActionBar().setTitle(city + (country.isEmpty() ? "" : "," + country));
        TextView actionBarTitleTxt = (TextView) toolbar.getChildAt(0);
        actionBarTitleTxt.setTypeface(mFont);
        actionBarTitleTxt.setText(city + (country.isEmpty() ? "" : "," + country));

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        float temperature = UnitConverter.convertTemperature((Float.parseFloat(model.getTemperature())), sp);
        if (sp.getBoolean(Constants.KEY_TEMP_INTEGER, false)) {
            temperature = Math.round(temperature);
        }

        //For Rain
        Double rain = Double.parseDouble(model.getRain());
        String rainString = UnitConverter.getRainString(rain, sp);

        Log.d("rainString=", model.getDescription() + "==rainString");
        //For wind
        double wind;
        try {
            wind = Double.parseDouble(model.getWind());
        } catch (Exception exp) {
            exp.printStackTrace();
            wind = 0;
        }
        wind = UnitConverter.convertWind(wind, sp);

        //For Pressure
        double pressure = UnitConverter.convertPressure(Float.parseFloat(model.getPressure()), sp);

        todayTempTxt.setText(new DecimalFormat("#.#").format(temperature) + " °" + (sp.getString(Constants.KEY_TEMP_UNIT, "C")));

        String description = "";
        if (model.getDescription().equalsIgnoreCase("scattered clouds")) {
            description = getString(R.string.scattered_clouds_string);
        } else if (model.getDescription().equalsIgnoreCase("clear sky")) {
            description = getString(R.string.clear_sky_string);
        } else if (model.getDescription().equalsIgnoreCase("light rain")) {
            description = getString(R.string.light_rain_string);
        } else if (model.getDescription().equalsIgnoreCase("overcast clouds")) {
            description = getString(R.string.overcast_clouds_string);
        } else if (model.getDescription().equalsIgnoreCase("broken clouds")) {
            description = getString(R.string.broken_clouds_string);
        } else if (model.getDescription().equalsIgnoreCase("few clouds")) {
            description = getString(R.string.few_clouds_string);
        }


        if (!description.equalsIgnoreCase("")) {
            if (rainString.equalsIgnoreCase("")) {
                todayDescriptionTxt.setText(description);
            } else {
                todayDescriptionTxt.setText(model.getDescription().substring(0, 1).toUpperCase() + model.getDescription().substring(1) + rainString);
            }
        } else {
            todayDescriptionTxt.setText(model.getDescription().substring(0, 1).toUpperCase() + model.getDescription().substring(1) + rainString);
        }

        if (sp.getString(Constants.KEY_SPEED_UNIT, "m/s").equals("bft")) {
            todayWindTxt.setText(getString(R.string.wind) + ": " +
                    UnitConverter.getBeaufortName((int) wind) +
                    (model.isWindDirectionAvailable() ? " " + getWindDirectionString(sp, this, model) : ""));
        } else {
            todayWindTxt.setText(getString(R.string.wind) + ": " + new DecimalFormat("#.#").format(wind) + " " +
                    localize(sp, Constants.KEY_SPEED_UNIT, "m/s") +
                    (model.isWindDirectionAvailable() ? " " + getWindDirectionString(sp, this, model) : ""));
        }

        DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(getApplicationContext());
        todayPressureTxt.setText(getString(R.string.pressure) + ": " + new DecimalFormat("#.0").format(pressure) + " " +
                localize(sp, Constants.KEY_PRESSURE_UNIT, "hPa"));
        todayHumidityTxt.setText(getString(R.string.humidity) + ": " + model.getHumidity() + " %");
        todaySunriseTxt.setText(getString(R.string.sunrise) + ": " + timeFormat.format(model.getSunrise()));
        todaySunsetTxt.setText(getString(R.string.sunset) + ": " + timeFormat.format(model.getSunset()));
        todayIconTxt.setText(model.getIcon());
        Typeface typeface = Typeface.createFromAsset(this.getAssets(), "fonts/weather.ttf");
        todayIconTxt.setTypeface(typeface);
    }

    @Override
    public void onTaskCompleted(AsyncTask task) {
        if (task instanceof TodayWeatherTask) {
            WeatherModel model = ((TodayWeatherTask) task).getWeatherModel();
            updateTodayWeatherUI(model);
            updateLastUpdateTime();
        }

        if (task instanceof LongTermWeatherTask) {
            LongTermWeatherTask taskObj = (LongTermWeatherTask) task;
            longTermTodayWeather = taskObj.getLongTermTodayWeatherList();
            longTermTomorrowWeather = taskObj.getLongTermTomorrowWeatherList();
            longTermWeather = taskObj.getLongTermWeatherList();
            updateLongTermUI();
        }
    }

    private void updateLongTermUI() {
        if (isActivityDestroyed)
            return;

        ViewPageAdapter viewPagerAdapter = new ViewPageAdapter(getSupportFragmentManager());

        Bundle bundleToday = new Bundle();
        bundleToday.putInt(Constants.KEY_DAY, 0);
        RecyclerViewFragment recyclerViewFragmentToday = new RecyclerViewFragment();
        recyclerViewFragmentToday.setArguments(bundleToday);
        viewPagerAdapter.addFragment(recyclerViewFragmentToday, getString(R.string.today));

        Bundle bundleTomorrow = new Bundle();
        bundleTomorrow.putInt(Constants.KEY_DAY, 1);
        RecyclerViewFragment recyclerViewFragmentTomorrow = new RecyclerViewFragment();
        recyclerViewFragmentTomorrow.setArguments(bundleTomorrow);
        viewPagerAdapter.addFragment(recyclerViewFragmentTomorrow, getString(R.string.tomorrow));

        Bundle bundleLater = new Bundle();
        bundleLater.putInt(Constants.KEY_DAY, 2);
        RecyclerViewFragment recyclerViewFragment = new RecyclerViewFragment();
        recyclerViewFragment.setArguments(bundleLater);
        viewPagerAdapter.addFragment(recyclerViewFragment, getString(R.string.later));

        int currentPage = viewPager.getCurrentItem();

        viewPagerAdapter.notifyDataSetChanged();
        viewPager.setAdapter(viewPagerAdapter);
        tabsLyt.setupWithViewPager(viewPager);

        if (currentPage == 0 && longTermTodayWeather.isEmpty()) {
            currentPage = 1;
        }
        viewPager.setCurrentItem(currentPage, false);
    }

    public RecyclerView.Adapter getAdapter(int day) {
        WeatherRecyclerAdapter weatherRecyclerAdapter;
        if (day == 0) {
            weatherRecyclerAdapter = new WeatherRecyclerAdapter(this, longTermTodayWeather);
        } else if (day == 1) {
            weatherRecyclerAdapter = new WeatherRecyclerAdapter(this, longTermTomorrowWeather);
        } else {
            weatherRecyclerAdapter = new WeatherRecyclerAdapter(this, longTermWeather);
        }
        return weatherRecyclerAdapter;
    }

    private void updateLastUpdateTime() {
        Long timeInMillis = PreferenceManager.getDefaultSharedPreferences(this).getLong(Constants.KEY_LAST_UPDATE_TIME, -1);
        if (timeInMillis < 0) {
            lastUpdateTimeTxt.setText("");
        } else {
            String timeFormat = formatTimeWithDay(this, timeInMillis);
            lastUpdateTimeTxt.setText(String.format(Locale.ENGLISH, getResources().getString(R.string.last_update), updatedDay) + ":" + timeFormat);
        }
    }

    private String formatTimeWithDay(MainActivity mainActivity, Long timeInMillis) {
        Calendar now = Calendar.getInstance();
        Calendar oldTimeCalender = new GregorianCalendar();
        oldTimeCalender.setTimeInMillis(timeInMillis);
        Date oldTimeDate = new Date(timeInMillis);
        String timeFormat = android.text.format.DateFormat.getTimeFormat(this).format(oldTimeDate);
        if (now.get(Calendar.YEAR) == oldTimeCalender.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) == oldTimeCalender.get(Calendar.DAY_OF_YEAR)) {
            updatedDay = getResources().getString(R.string.today_string);
            return timeFormat;
        } else {
            updatedDay = oldTimeCalender.get(Calendar.DAY_OF_WEEK) + "";
            return android.text.format.DateFormat.getDateFormat(this).format(oldTimeDate) + " " + timeFormat;
        }
    }

    private String localize(SharedPreferences sp, String preferenceKey, String defaultValueKey) {
        return localize(sp, this, preferenceKey, defaultValueKey);
    }

    public static String localize(SharedPreferences sp, Context context, String preferenceKey, String defaultValueKey) {
        String preferenceValue = sp.getString(preferenceKey, defaultValueKey);
        String result = preferenceValue;
        if (Constants.KEY_SPEED_UNIT.equals(preferenceKey)) {
            if (speedUnits.containsKey(preferenceValue)) {
                result = context.getString(speedUnits.get(preferenceValue));
            }
        } else if (Constants.KEY_PRESSURE_UNIT.equals(preferenceKey)) {
            if (pressureUnits.containsKey(preferenceValue)) {
                result = context.getString(pressureUnits.get(preferenceValue));
            }
        }
        return result;
    }

    private Object getWindDirectionString(SharedPreferences sp, MainActivity mainActivity, WeatherModel weather) {
        try {
            if (Double.parseDouble(weather.getWind()) != 0) {
                String pref = sp.getString(Constants.KEY_WIND_DIRECTION_FORMAT, null);
                if ("arrow".equals(pref)) {
                    return weather.getWindDirection(8).getArrow(this);
                } else if ("abbr".equals(pref)) {
                    return weather.getWindDirection().getLocalizedString(this);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isActivityDestroyed = true;
    }


    public static String getWindDirectionString(SharedPreferences sp, Context context, WeatherModel weather) {
        try {
            if (Double.parseDouble(weather.getWind()) != 0) {
                String pref = sp.getString(Constants.KEY_WIND_DIRECTION_FORMAT, null);
                if ("arrow".equals(pref)) {
                    return weather.getWindDirection(8).getArrow(context);
                } else if ("abbr".equals(pref)) {
                    return weather.getWindDirection().getLocalizedString(context);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }


    public void showAboutUsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.custom_dialog_view, null);
        builder.setView(dialogView);

        TextView aboutUsDialogTxt = (TextView) dialogView.findViewById(R.id.aboutUsDialogTxt);
        String aboutUsStr1 = getString(R.string.string_about);
        String aboutUsStr2 = "<font color=\"blue\">" + getString(R.string.string_app_dev_name) + "</font>";
        String aboutUsStrFinal = String.format(aboutUsStr1, aboutUsStr2);
        aboutUsDialogTxt.setText(Html.fromHtml(aboutUsStrFinal), TextView.BufferType.SPANNABLE);

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}
