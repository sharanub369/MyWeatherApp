package app.weather.sharan.com.myweatherapp.ui.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by sharana.b on 9/17/2016.
 */
public class NetworkState {

    private Context context;

    public NetworkState() {

    }

    public NetworkState(Context context) {
        this.context = context;
    }

    public Boolean isNetworkAvailable() {
        ConnectivityManager mConnectionManger = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = mConnectionManger.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}
