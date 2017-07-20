package app.weather.sharan.com.myweatherapp.ui.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.WebView;

import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnMenuTabClickListener;

import app.weather.sharan.com.myweatherapp.R;
import app.weather.sharan.com.myweatherapp.ui.util.Constants;
import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by sharana.b on 9/23/2016.
 */
public class WeatherMapActivity extends AppCompatActivity {

    private SharedPreferences sp;

    @InjectView(R.id.mapWebViewId)
    WebView webView;

    private String API_KEY;
    private double longitude, latitude;
    private BottomBar mBottomBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        ButterKnife.inject(this);
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        API_KEY = sp.getString(Constants.KEY_API_KEY, getString(R.string.apiKey));
        longitude = sp.getFloat(Constants.KEY_LONGITUDE, 0);
        latitude = sp.getFloat(Constants.KEY_LATITUDE, 0);

        String url = "file:///android_asset/map.html?lat=" + longitude + "&lon=" + latitude + "&appid=" + API_KEY;
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl(url);

        mBottomBar = BottomBar.attach(this, savedInstanceState);
        mBottomBar.setItems(R.menu.menu_map_bottom);
        mBottomBar.setOnMenuTabClickListener(new OnMenuTabClickListener() {
            @Override
            public void onMenuTabSelected(@IdRes int menuItemId) {
                if (menuItemId == R.id.map_rain) {
                    webView.loadUrl("javascript:map.removeLayer(windLayer);map.removeLayer(tempLayer);map.addLayer(rainLayer);");
                } else if (menuItemId == R.id.map_wind) {
                    webView.loadUrl("javascript:map.removeLayer(rainLayer);map.removeLayer(tempLayer);map.addLayer(windLayer);");
                } else if (menuItemId == R.id.map_temperature) {
                    webView.loadUrl("javascript:map.removeLayer(windLayer);map.removeLayer(rainLayer);map.addLayer(tempLayer);");
                }
            }

            @Override
            public void onMenuTabReSelected(@IdRes int menuItemId) {
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // mBottomBar.onSaveInstanceState(outState);
    }
}
