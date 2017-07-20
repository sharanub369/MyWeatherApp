package app.weather.sharan.com.myweatherapp.ui.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import app.weather.sharan.com.myweatherapp.R;
import app.weather.sharan.com.myweatherapp.ui.task.TodayWeatherTask;
import app.weather.sharan.com.myweatherapp.ui.util.Constants;

/**
 * Created by sharana.b on 9/20/2016.
 */
public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener, LocationListener {

    // Thursday 2016-01-14 16:00:00
    private static final Date SAMPLE_DATE = new Date(1452805200000l);
    protected static final int REQUEST_CODE_ACCESS_FINE_LOCATION = 1;
    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute

    private LocationManager locationManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(getTheme(PreferenceManager.getDefaultSharedPreferences(this).getString("theme", "fresh")));
        super.onCreate(savedInstanceState);

        LinearLayout root = (LinearLayout) findViewById(android.R.id.list).getParent().getParent().getParent();
        View bar = LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
        root.addView(bar, 0);
        Toolbar toolbar = (Toolbar) findViewById(R.id.settings_toolbar);
        toolbar.setTitle(getString(R.string.action_settings));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        addPreferencesFromResource(R.xml.prefs);
    }

    @Override
    protected void onResume() {
        super.onResume();

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        setCustomDateEnabled();
        updateDateFormatList();

        // Set summaries to current value
        setListPreferenceSummary(Constants.KEY_LANGUAGE);
        setListPreferenceSummary(Constants.KEY_TEMP_UNIT);
        setListPreferenceSummary(Constants.KEY_LENGTH_UNIT);
        setListPreferenceSummary(Constants.KEY_SPEED_UNIT);
        setListPreferenceSummary(Constants.KEY_PRESSURE_UNIT);
        setListPreferenceSummary(Constants.KEY_REFRESH_INTERVAL);
        setListPreferenceSummary(Constants.KEY_WIND_DIRECTION_FORMAT);
        setListPreferenceSummary(Constants.KEY_THEME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    private void setListPreferenceSummary(String preferenceKey) {
        ListPreference preference = (ListPreference) findPreference(preferenceKey);
        preference.setSummary(preference.getEntry());
    }

    private int getTheme(String theme) {
        switch (theme) {
            case "fresh":
                return R.style.AppTheme_NoActionBar;
            case "classic":
                return R.style.AppTheme_Classic;
            case "classicdark":
                return R.style.AppTheme_Classic_Dark;
            default:
                return R.style.AppTheme_NoActionBar;
        }
    }

    private void setCustomDateEnabled() {
        SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
        Preference customDatePref = findPreference(Constants.KEY_DATE_CUSTOM_FORMAT);
        customDatePref.setEnabled("custom".equals(sp.getString(Constants.KEY_DATE_FORMAT, "")));
    }

    public void updateDateFormatList() {

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        Resources res = this.getResources();

        ListPreference customDateList = (ListPreference) findPreference(Constants.KEY_DATE_FORMAT);
        String[] dateFormatValues = res.getStringArray(R.array.dateFormatsValues);
        String[] dateFormatEntries = new String[dateFormatValues.length];

        EditTextPreference customDatePref = (EditTextPreference) findPreference(Constants.KEY_DATE_CUSTOM_FORMAT);
        customDatePref.setDefaultValue(dateFormatValues[0]);

        SimpleDateFormat sdf = new SimpleDateFormat();
        for (int i = 0; i < dateFormatValues.length; i++) {
            String value = dateFormatValues[i];
            if (value.equalsIgnoreCase("custom")) {
                String customDateFormat = "";
                try {
                    sdf.applyPattern(sp.getString(Constants.KEY_DATE_CUSTOM_FORMAT, dateFormatValues[0]));
                    customDateFormat = sdf.format(SAMPLE_DATE);
                } catch (Exception exp) {
                    customDateFormat = res.getString(R.string.error_dateFormat);
                }

                dateFormatEntries[i] = String.format("%s:\n%s", res.getString(R.string.setting_dateFormatCustom), customDateFormat);
            } else {
                sdf.applyPattern(value);
                dateFormatEntries[i] = sdf.format(SAMPLE_DATE);
            }
        }

        customDateList.setDefaultValue(dateFormatValues[0]);
        customDateList.setEntries(dateFormatEntries);

        customDateList.setSummary(res.getString(R.string.setting_dateFormatCustom));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d("KEY_TEST=", key + "===key");
        switch (key) {
            case Constants.KEY_LANGUAGE:
                setListPreferenceSummary(key);
                revertBack();
                break;
            case Constants.KEY_TEMP_UNIT:
            case Constants.KEY_LENGTH_UNIT:
            case Constants.KEY_SPEED_UNIT:
            case Constants.KEY_PRESSURE_UNIT:
            case Constants.KEY_REFRESH_INTERVAL:
            case Constants.KEY_WIND_DIRECTION_FORMAT:
                setListPreferenceSummary(key);
                break;
            case Constants.KEY_DATE_FORMAT:
                setListPreferenceSummary(key);
                Intent intent=new Intent(this,MainActivity.class);
                startActivity(intent);
                break;
            case Constants.KEY_UPDATE_LOCATION_AUTOMATICALLY:
                if (sharedPreferences.getBoolean(key, false) == true) {
                    requestReadLocationPermission();
                    CheckBoxPreference checkBox = (CheckBoxPreference) findPreference("updateLocationAutomatically");
                    checkBox.setChecked(true);
                }
                break;
            case Constants.KEY_FORMAT_TEMP_INT:
                if (sharedPreferences.getBoolean(Constants.KEY_FORMAT_TEMP_INT, false) == true) {
                    Intent intent1 = new Intent(this, MainActivity.class);
                    startActivity(intent1);
                }
                break;
            case Constants.KEY_API_KEY:
                checkApiKey();
                break;

        }
    }

    private void revertBack() {
        Intent intent=new Intent(this,MainActivity.class);
        startActivity(intent);
    }

    private void requestReadLocationPermission() {
        System.out.println("Calling request location permission=678=" + Manifest.permission.ACCESS_COARSE_LOCATION + "==" + PackageManager.PERMISSION_GRANTED);
        // Here, thisActivity is the current activity
        try {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.


                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                            REQUEST_CODE_ACCESS_FINE_LOCATION);

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            }
        } finally {
            privacyGuardWorkaround();
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        System.out.println("Calling request location permission requestCode=" + requestCode);
        if (requestCode == MainActivity.MY_PERMISSIONS_ACCESS_FINE_LOCATION) {
            boolean permissionGranted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            CheckBoxPreference checkBox = (CheckBoxPreference) findPreference("updateLocationAutomatically");
            checkBox.setChecked(permissionGranted);
            if (permissionGranted) {
                privacyGuardWorkaround();
            }
        }
    }

    private void privacyGuardWorkaround() {
        System.out.println("Calling request location permission privacyGuardWorkaroundprivacyGuardWorkaroundprivacyGuardWorkaround=");
        // Workaround for CM privacy guard. Register for location updates in order for it to ask us for permission
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
        } catch (SecurityException e) {
            // This will most probably not happen, as we just got granted the permission
        }
    }


    @Override
    public void onLocationChanged(Location location) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean isUpdateAuto = sp.getBoolean(Constants.KEY_UPDATE_LOCATION_AUTOMATICALLY, false);
        double longitude, latitude;
        if (isUpdateAuto == true) {
            longitude = location.getLongitude();
            latitude = location.getLatitude();
            if ((sp.getFloat(Constants.KEY_LATITUDE, 0) != latitude && (sp.getFloat(Constants.KEY_LATITUDE, 0) != 0) && (sp.getFloat(Constants.KEY_LONGITUDE, 0) != longitude) && (sp.getFloat(Constants.KEY_LONGITUDE, 0) != 0))) {
                sp.edit().putBoolean(Constants.KEY_UPDATE_LOCATION_AUTOMATICALLY, true).putFloat(Constants.KEY_LATITUDE, (float) latitude).putFloat(Constants.KEY_LONGITUDE, (float) longitude).apply();
                Intent intent = new Intent(this, MainActivity.class);
                Bundle bundle = new Bundle();
                bundle.putBoolean("FROMAUTOLOCATION", true);
                intent.putExtras(bundle);
                startActivity(intent);
            } else {
                if (locationManager != null) {
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
                    locationManager.removeUpdates(this);
                }
            }
        }
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

    public void checkApiKey() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
//        if (sp.getString(Constants.KEY_API_KEY, "").equalsIgnoreCase("")) {
//            sp.edit().remove(Constants.KEY_API_KEY);
//        }
    }
}
