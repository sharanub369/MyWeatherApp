package app.weather.sharan.com.myweatherapp.ui.activities;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.db.chart.Tools;
import com.db.chart.model.LineSet;
import com.db.chart.view.ChartView;
import com.db.chart.view.LineChartView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.ValueDependentColor;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import app.weather.sharan.com.myweatherapp.R;
import app.weather.sharan.com.myweatherapp.ui.model.WeatherModel;
import app.weather.sharan.com.myweatherapp.ui.task.ParseResult;
import app.weather.sharan.com.myweatherapp.ui.util.Constants;
import app.weather.sharan.com.myweatherapp.ui.util.UnitConverter;
import butterknife.ButterKnife;
import butterknife.InjectView;


/**
 * Created by sharana.b on 9/23/2016.
 */
public class GraphActivity extends AppCompatActivity {

    SharedPreferences sp;

    int theme;

    ArrayList<WeatherModel> weatherList = new ArrayList<>();

    float minTemp = 100000;
    float maxTemp = 0;

    float minRain = 100000;
    float maxRain = 0;

    float minPressure = 100000;
    float maxPressure = 0;

    private Boolean isLineChart = true;

    @InjectView(R.id.lineChartLyt)
    LinearLayout lineChartLyt;
    @InjectView(R.id.barChartLyt)
    LinearLayout barChartLyt;

    @InjectView(R.id.tempBarGraph)
    GraphView tempBarGraph;

    @InjectView(R.id.tempUnitTxt)
    TextView tempUnitTxt;
    @InjectView(R.id.pressureUnitTxt)
    TextView pressureUnitTxt;
    @InjectView(R.id.rainUnitTxt)
    TextView rainUnitTxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        setTheme(theme = getTheme(prefs.getString("theme", "fresh")));
        boolean darkTheme = theme == R.style.AppTheme_NoActionBar_Dark ||
                theme == R.style.AppTheme_NoActionBar_Classic_Dark;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
        ButterKnife.inject(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.graph_toolbar);
        toolbar.setTitle(getString(R.string.action_graphs));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (darkTheme) {
            toolbar.setPopupTheme(R.style.AppTheme_PopupOverlay_Dark);
        }

        sp = PreferenceManager.getDefaultSharedPreferences(GraphActivity.this);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        lineChartLyt.setVisibility(View.VISIBLE);
        barChartLyt.setVisibility(View.GONE);
        createLineCharts();
    }

    public void createLineCharts() {
        String lastLongterm = sp.getString(Constants.KEY_LONG_TERM_WEATHER_RESPONSE, "");
        if (parseLongTermJson(lastLongterm) == ParseResult.OK) {
            temperatureLineGraph();
            rainLineGraph();
            pressureLineGraph();
        } else {
            Snackbar.make(findViewById(android.R.id.content), R.string.msg_err_parsing_json, Snackbar.LENGTH_LONG).show();
        }
    }

    private void temperatureLineGraph() {

        String tempUnit = sp.getString(Constants.KEY_TEMP_UNIT, "°C");
        tempUnitTxt.setText("°" + tempUnit);

        LineChartView lineChartView = (LineChartView) findViewById(R.id.graph_temperature);

        // Data
        LineSet dataset = new LineSet();
        for (int i = 0; i < weatherList.size(); i++) {
            float temperature = UnitConverter.convertTemperature(Float.parseFloat(weatherList.get(i).getTemperature()), sp);

            if (temperature < minTemp) {
                minTemp = temperature;
            }

            if (temperature > maxTemp) {
                maxTemp = temperature;
            }

            dataset.addPoint(getDateLabel(weatherList.get(i), i), (float) ((Math.ceil(temperature / 2)) * 2));
        }
        dataset.setSmooth(true);
        dataset.setColor(Color.parseColor("#FF5722"));
        dataset.setThickness(4);

        lineChartView.addData(dataset);

        // Grid
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setColor(Color.parseColor("#333333"));
        paint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));
        paint.setStrokeWidth(1);
        lineChartView.setGrid(ChartView.GridType.HORIZONTAL, paint);
        lineChartView.setBorderSpacing(Tools.fromDpToPx(10));
        lineChartView.setAxisBorderValues((int) minTemp - 2, (int) maxTemp + 2);
        lineChartView.setStep(2);
        lineChartView.setXAxis(false);
        lineChartView.setYAxis(false);

        lineChartView.show();
    }

    private void rainLineGraph() {

        String windSpeedUnit = sp.getString(Constants.KEY_LENGTH_UNIT, "mm");
        rainUnitTxt.setText(windSpeedUnit);
        LineChartView lineChartView = (LineChartView) findViewById(R.id.graph_rain);
        // lineChartView.dismiss();
        // Data
        LineSet dataset = new LineSet();
        for (int i = 0; i < weatherList.size(); i++) {
            float rain = Float.parseFloat(weatherList.get(i).getRain());

            if (rain < minRain) {
                minRain = rain;
            }

            if (rain > maxRain) {
                maxRain = rain;
            }

            dataset.addPoint(getDateLabel(weatherList.get(i), i), rain);
        }
        dataset.setSmooth(true);
        dataset.setColor(Color.parseColor("#2196F3"));
        dataset.setThickness(4);

        lineChartView.addData(dataset);

        // Grid
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setColor(Color.parseColor("#333333"));
        paint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));
        paint.setStrokeWidth(1);
        lineChartView.setGrid(ChartView.GridType.HORIZONTAL, paint);
        lineChartView.setBorderSpacing(Tools.fromDpToPx(10));
        lineChartView.setAxisBorderValues((int) minRain - 1, (int) maxRain + 2);
        lineChartView.setStep(1);
        lineChartView.setXAxis(false);
        lineChartView.setYAxis(false);

        lineChartView.show();
    }

    private void pressureLineGraph() {

        String windSpeedUnit = sp.getString(Constants.KEY_PRESSURE_UNIT, "hpa");
        pressureUnitTxt.setText(windSpeedUnit);

        LineChartView lineChartView = (LineChartView) findViewById(R.id.graph_pressure);
        lineChartView.destroyDrawingCache();
        // Data
        LineSet dataset = new LineSet();
        for (int i = 0; i < weatherList.size(); i++) {
            float pressure = UnitConverter.convertPressure(Float.parseFloat(weatherList.get(i).getPressure()), sp);

            if (pressure < minPressure) {
                minPressure = pressure;
            }

            if (pressure > maxPressure) {
                maxPressure = pressure;
            }

            dataset.addPoint(getDateLabel(weatherList.get(i), i), pressure);
        }
        dataset.setSmooth(true);
        dataset.setColor(Color.parseColor("#4CAF50"));
        dataset.setThickness(4);

        lineChartView.addData(dataset);

        // Grid
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setColor(Color.parseColor("#333333"));
        paint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));
        paint.setStrokeWidth(1);
        lineChartView.setGrid(ChartView.GridType.HORIZONTAL, paint);
        lineChartView.setBorderSpacing(Tools.fromDpToPx(10));
        lineChartView.setAxisBorderValues((int) minPressure - 1, (int) maxPressure + 1);
        lineChartView.setStep(2);
        lineChartView.setXAxis(false);
        lineChartView.setYAxis(false);

        lineChartView.show();
    }


    public void createBarCharts() {
        if (weatherList.size() > 0) {
            temperatureBarGraph();
        }
    }

    public void temperatureBarGraph() {

        List<Date> dateList = new ArrayList<Date>();
        for (int i = 0; i < weatherList.size(); i++) {
            dateList.add(weatherList.get(i).getDate());
        }
        BarGraphSeries<DataPoint> bar_series = new BarGraphSeries<DataPoint>(new DataPoint[]{
                new DataPoint(dateList.get(0).getTime(), 0),
                new DataPoint(dateList.get(0).getTime(), UnitConverter.convertTemperature(Float.parseFloat(weatherList.get(0).getTemperature()), sp)),
                new DataPoint(dateList.get(1).getTime(), UnitConverter.convertTemperature(Float.parseFloat(weatherList.get(1).getTemperature()), sp)),
                new DataPoint(dateList.get(2).getTime(), UnitConverter.convertTemperature(Float.parseFloat(weatherList.get(2).getTemperature()), sp)),
        });

        tempBarGraph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.HORIZONTAL);
        tempBarGraph.getGridLabelRenderer().setHorizontalLabelsVisible(true);
        tempBarGraph.getGridLabelRenderer().setVerticalLabelsVisible(true);
        tempBarGraph.addSeries(bar_series);

        // set date label formatter
        tempBarGraph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(this));
        tempBarGraph.getGridLabelRenderer().setNumHorizontalLabels(3); // only 4 because of the space

        // set manual x bounds to have nice steps
        tempBarGraph.getViewport().setMinX(dateList.get(0).getTime());
        tempBarGraph.getViewport().setMaxX(dateList.get(2).getTime());
        tempBarGraph.getViewport().setXAxisBoundsManual(true);

        // styling
        bar_series.setValueDependentColor(new ValueDependentColor<DataPoint>() {
            @Override
            public int get(DataPoint data) {
                return Color.rgb((int) data.getX() * 255 / 4, (int) Math.abs(data.getY() * 255 / 6), 100);
            }
        });

        bar_series.setSpacing(40);

        // draw values on top
        bar_series.setDrawValuesOnTop(true);
        bar_series.setValuesOnTopColor(Color.RED);
        bar_series.setValuesOnTopSize(15);
    }

    public ParseResult parseLongTermJson(String result) {
        int i;
        try {
            JSONObject reader = new JSONObject(result);

            final String code = reader.optString("cod");
            if ("404".equals(code)) {
                return ParseResult.CITY_NOT_FOUND;
            }

            JSONArray list = reader.getJSONArray("list");
            for (i = 0; i < list.length(); i++) {
                WeatherModel weather = new WeatherModel();

                JSONObject listItem = list.getJSONObject(i);
                JSONObject main = listItem.getJSONObject("main");

                JSONObject windObj = listItem.optJSONObject("wind");
                weather.setWind(windObj.getString("speed"));

                weather.setPressure(main.getString("pressure"));
                weather.setHumidity(main.getString("humidity"));

                JSONObject rainObj = listItem.optJSONObject("rain");
                JSONObject snowObj = listItem.optJSONObject("snow");
                if (rainObj != null) {
                    weather.setRain(getRainString(rainObj));
                } else {
                    weather.setRain(getRainString(snowObj));
                }

                weather.setDate(listItem.getString("dt"));
                weather.setTemperature(main.getString("temp"));

                weatherList.add(weather);
            }
        } catch (JSONException e) {
            Log.e("JSONException Data", result);
            e.printStackTrace();
            return ParseResult.JSON_EXCEPTION;
        }

        return ParseResult.OK;
    }

    String previous = "";

    public String getDateLabel(WeatherModel weather, int i) {
        if ((i + 4) % 4 == 0) {
            SimpleDateFormat resultFormat = new SimpleDateFormat("E");
            resultFormat.setTimeZone(TimeZone.getDefault());
            String output = resultFormat.format(weather.getDate());
            if (!output.equals(previous)) {
                previous = output;
                return output;
            } else {
                return "";
            }
        } else {
            return "";
        }
    }

    private int getTheme(String themePref) {
        switch (themePref) {
            case "dark":
                return R.style.AppTheme_NoActionBar_Dark;
            case "classic":
                return R.style.AppTheme_NoActionBar_Classic;
            case "classicdark":
                return R.style.AppTheme_NoActionBar_Classic_Dark;
            default:
                return R.style.AppTheme_NoActionBar;
        }
    }

    public static String getRainString(JSONObject rainObj) {
        String rain = "0";
        if (rainObj != null) {
            rain = rainObj.optString("3h", "fail");
            if ("fail".equals(rain)) {
                rain = rainObj.optString("1h", "0");
            }
        }
        return rain;
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.men_graphs, menu);
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        int id = item.getItemId();
//        switch (id) {
//            case R.id.action_barChart:
//                if (isLineChart) {
//                    isLineChart = false;
//                    lineChartLyt.setVisibility(View.GONE);
//                    barChartLyt.setVisibility(View.VISIBLE);
//                    createBarCharts();
//                }
//                break;
//            case R.id.action_lineChart:
//                if (!isLineChart) {
//                    isLineChart = true;
//                    lineChartLyt.setVisibility(View.VISIBLE);
//                    barChartLyt.setVisibility(View.GONE);
//                    createLineCharts();
//                }
//                break;
//        }
//        return super.onOptionsItemSelected(item);
 //   }
}
