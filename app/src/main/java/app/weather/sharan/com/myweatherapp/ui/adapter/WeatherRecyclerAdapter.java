package app.weather.sharan.com.myweatherapp.ui.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import app.weather.sharan.com.myweatherapp.R;
import app.weather.sharan.com.myweatherapp.ui.activities.MainActivity;
import app.weather.sharan.com.myweatherapp.ui.model.WeatherModel;
import app.weather.sharan.com.myweatherapp.ui.model.WeatherViewHolder;
import app.weather.sharan.com.myweatherapp.ui.util.Constants;
import app.weather.sharan.com.myweatherapp.ui.util.UnitConverter;

/**
 * Created by sharana.b on 9/19/2016.
 */
public class WeatherRecyclerAdapter extends RecyclerView.Adapter<WeatherViewHolder> {

    private WeatherModel model;
    private List<WeatherModel> itemList;
    private Context context;
    private SharedPreferences sp;

    @Override
    public WeatherViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.custom_recycle_single_row, null);
        WeatherViewHolder viewHolder = new WeatherViewHolder(view);
        return viewHolder;
    }

    public WeatherRecyclerAdapter(MainActivity mainActivity, List<WeatherModel> listItems) {
        this.context = mainActivity;
        itemList = listItems;
        sp = PreferenceManager.getDefaultSharedPreferences(context);
    }


    @Override
    public void onBindViewHolder(WeatherViewHolder holder, int position) {
        model = itemList.get(position);

        //For Tempreture
        float temp = UnitConverter.convertTemperature(Float.parseFloat(model.getTemperature()), sp);
        if (sp.getBoolean(Constants.KEY_TEMP_INTEGER, false)) {
            temp = Math.round(temp);
        }

        Double rain = Double.parseDouble(model.getRain());
        String rainString = UnitConverter.getRainString(rain, sp);

        // Wind
        double wind;
        try {
            wind = Double.parseDouble(model.getWind());
        } catch (Exception e) {
            e.printStackTrace();
            wind = 0;
        }
        wind = UnitConverter.convertWind(wind, sp);

        //For Pressure
        Float pressure = UnitConverter.convertPressure((float) Double.parseDouble(model.getPressure()), sp);
        TimeZone timeZone = TimeZone.getDefault();
        String defaultDateFormat = context.getResources().getStringArray(R.array.dateFormatsValues)[0];
        String dateFormat = sp.getString(Constants.KEY_DATE_FORMAT, defaultDateFormat);
        Log.d("dateFormatCustom", dateFormat + "==dateFormatCustom");
        if ("Custom".equalsIgnoreCase(dateFormat)) {
            dateFormat = sp.getString(Constants.KEY_DATE_CUSTOM_FORMAT, defaultDateFormat);
        }

        String dateString;
        try {
            SimpleDateFormat resultFormat = new SimpleDateFormat(dateFormat);
            resultFormat.setTimeZone(timeZone);
            dateString = resultFormat.format(model.getDate());
            if (dateFormat.contains("E")) {
                String[] dateValue = dateString.split(" ");
                String dayString = dateValue[0];
                dateString = dayString + " " + dateValue[1];
            }
        } catch (IllegalArgumentException e) {
            dateString = context.getResources().getString(R.string.error_dateFormat);
        }

        if (sp.getBoolean(Constants.KEY_DAYS_DIFFERENTIATE_COLOR, false)) {
            Date now = new Date();
            /* Unfortunately, the getColor() that takes a theme (the next commented line) is Android 6.0 only, so we have to do it manually
             * customViewHolder.itemView.setBackgroundColor(context.getResources().getColor(R.attr.colorTintedBackground, context.getTheme())); */
            int color = 0;
            if (model.getNumDaysFrom(now) > 1) {
                TypedArray ta = context.obtainStyledAttributes(new int[]{R.attr.colorTintedBackground, R.attr.colorBackground});
                if (model.getNumDaysFrom(now) % 2 == 1) {
                   //  color = ta.getColor(0, context.getResources().getColor(R.color.colorTintedBackground));
                   // color = context.getDrawable(R.drawable.ic_background_1);
                } else {
                    /* We must explicitly set things back, because RecyclerView seems to reuse views and
                     * without restoring back the "normal" color, just about everything gets tinted if we
                     * scroll a couple of times! */
                    // color = ta.getColor(1, context.getResources().getColor(R.color.colorBackground));
                    //  color = context.getColor(R.color.colorBackground);
                }
                ta.recycle();
                if (color != 0)
                    //holder.itemView.setBackgroundDrawable(color);
                 holder.itemView.setBackgroundColor(color);
            }
        }

        holder.itemDate.setText(dateString);

        if (sp.getBoolean("displayDecimalZeroes", false)) {
            holder.itemTemperature.setText(new DecimalFormat("#.0").format(temp) + " °" + sp.getString(Constants.KEY_TEMP_UNIT, "C"));
        } else {
            holder.itemTemperature.setText(new DecimalFormat("#.#").format(temp) + " °" + sp.getString(Constants.KEY_TEMP_UNIT, "C"));
        }

        String description = "";
        if (model.getDescription().equalsIgnoreCase("scattered clouds")) {
            description = context.getString(R.string.scattered_clouds_string);
        } else if (model.getDescription().equalsIgnoreCase("clear sky")) {
            description = context.getString(R.string.clear_sky_string);
        } else if (model.getDescription().equalsIgnoreCase("light rain")) {
            description = context.getString(R.string.light_rain_string);
        } else if (model.getDescription().equalsIgnoreCase("overcast clouds")) {
            description = context.getString(R.string.overcast_clouds_string);
        } else if (model.getDescription().equalsIgnoreCase("broken clouds")) {
            description = context.getString(R.string.broken_clouds_string);
        } else if (model.getDescription().equalsIgnoreCase("few clouds")) {
            description = context.getString(R.string.few_clouds_string);
        }
        if (!description.equalsIgnoreCase("")) {
            if (rainString.equalsIgnoreCase("")) {
                holder.itemDescription.setText(description);
            } else {
                holder.itemDescription.setText(model.getDescription().substring(0, 1).toUpperCase() + model.getDescription().substring(1) + rainString);
            }
        } else {
            holder.itemDescription.setText(model.getDescription().substring(0, 1).toUpperCase() + model.getDescription().substring(1) + rainString);
        }
//        holder.itemDescription.setText(model.getDescription().substring(0, 1).toUpperCase() +
//                model.getDescription().substring(1) + rainString);
        Typeface weatherFont = Typeface.createFromAsset(context.getAssets(), "fonts/weather.ttf");
        holder.itemIcon.setTypeface(weatherFont);
        holder.itemIcon.setText(model.getIcon());
        if (sp.getString(Constants.KEY_SPEED_UNIT, "m/s").equals("bft")) {
            holder.itemyWind.setText(context.getString(R.string.wind) + ": " +
                    UnitConverter.getBeaufortName((int) wind) + " " + MainActivity.getWindDirectionString(sp, context, model));
        } else {
            holder.itemyWind.setText(context.getString(R.string.wind) + ": " + new DecimalFormat("#.0").format(wind) + " " +
                    MainActivity.localize(sp, context, Constants.KEY_SPEED_UNIT, "m/s")
                    + " " + MainActivity.getWindDirectionString(sp, context, model));
        }
        holder.itemPressure.setText(context.getString(R.string.pressure) + ": " + new DecimalFormat("#.0").format(pressure) + " " +
                MainActivity.localize(sp, context, Constants.KEY_PRESSURE_UNIT, "hPa"));
        holder.itemHumidity.setText(context.getString(R.string.humidity) + ": " + model.getHumidity() + " %");
    }

    @Override
    public int getItemCount() {
        return (itemList != null ? itemList.size() : 0);
    }
}
