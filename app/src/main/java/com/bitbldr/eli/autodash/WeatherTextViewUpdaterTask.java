package com.bitbldr.eli.autodash;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

public final class WeatherTextViewUpdaterTask extends AsyncTask<Void, Void, String> {
    private TextView weatherButton;

    public WeatherTextViewUpdaterTask(TextView weatherButton) {
        this.weatherButton = weatherButton;
    }

    protected void onPreExecute() {
        // do nothing, for now
    }

    protected String doInBackground(Void... urls) {
        try {
            String API_KEY = "175638bf5ad25874";
            URL url = new URL("http://api.wunderground.com/api/" + API_KEY + "/conditions/q/PA/Pittsburgh.json");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                bufferedReader.close();
                return stringBuilder.toString();
            }
            finally{
                urlConnection.disconnect();
            }
        }
        catch(Exception e) {
            Log.e("ERROR", e.getMessage(), e);
            return null;
        }
    }

    protected void onPostExecute(String response) {
        if(response == null) {
            Log.e("ERROR", "Weather response is null");
            return;
        }

        try {
            JSONObject object = (JSONObject) new JSONTokener(response).nextValue();
            int currentTemp_f = object.getJSONObject("current_observation").getInt("temp_f");
            String currentConditions = object.getJSONObject("current_observation").getString("icon");

            weatherButton.setText(currentTemp_f + "° " + GetWeatherConditionsIcon(currentConditions, false));
        } catch (JSONException e) {
            // Appropriate error handling code
        }
    }

    private static String GetWeatherConditionsIcon(String condition, boolean isNight) {
        HashMap<String, String> weatherIconMap = new HashMap<>();

        if (isNight) {
            weatherIconMap.put("chanceflurries", "{wic-day-snow-wind}");
            weatherIconMap.put("chancerain", "{wic-night-alt-rain}");
            weatherIconMap.put("chancesleet", "{wic-night-alt-rain-mix}");
            weatherIconMap.put("chancesnow", "{wic-night-alt-snow-wind}");
            weatherIconMap.put("chancetstorms", "{wic-storm-showers}");
            weatherIconMap.put("mostlycloudy", "{wic-cloudy}");
            weatherIconMap.put("mostlysunny", "{wic-night-alt-cloudy}");
            weatherIconMap.put("partlycloudy", "{wic-cloud}");
            weatherIconMap.put("partlysunny", "{wic-night-alt-cloudy}");
            weatherIconMap.put("clear", "{wic-night-clear}");
            weatherIconMap.put("cloudy", "{wic-cloudy}");
            weatherIconMap.put("flurries", "{wic-snow-wind}");
            weatherIconMap.put("fog", "{wic-fog}");
            weatherIconMap.put("hazy", "{wic-cloudy-windy}");
            weatherIconMap.put("sleet", "{wic-rain-mix}");
            weatherIconMap.put("rain", "{wic-raindrops}");
            weatherIconMap.put("snow", "{wic-snowflake-cold}");
            weatherIconMap.put("sunny", "{wic-night-clear}");
            weatherIconMap.put("tstorms", "{wic-thunderstorm}");
        }
        else {
            weatherIconMap.put("chanceflurries", "{wic-day-snow-wind}");
            weatherIconMap.put("chancerain", "{wic-day-rain}");
            weatherIconMap.put("chancesleet", "{wic-day-sleet}");
            weatherIconMap.put("chancesnow", "{wic-day-snow}");
            weatherIconMap.put("chancetstorms", "{wic-day-storm-showers}");
            weatherIconMap.put("mostlycloudy", "{wic-cloudy}");
            weatherIconMap.put("mostlysunny", "{wic-day-sunny-overcast}");
            weatherIconMap.put("partlycloudy", "{wic-cloud}");
            weatherIconMap.put("partlysunny", "{wic-day-cloudy}");
            weatherIconMap.put("clear", "{wic-day-sunny}");
            weatherIconMap.put("cloudy", "{wic-cloudy}");
            weatherIconMap.put("flurries", "{wic-snow-wind}");
            weatherIconMap.put("fog", "{wic-fog}");
            weatherIconMap.put("hazy", "{wic-cloudy-windy}");
            weatherIconMap.put("sleet", "{wic-rain-mix}");
            weatherIconMap.put("rain", "{wic-raindrops}");
            weatherIconMap.put("snow", "{wic-snowflake-cold}");
            weatherIconMap.put("sunny", "{wic-day-sunny}");
            weatherIconMap.put("tstorms", "{wic-thunderstorm}");
        }

        if (weatherIconMap.get(condition) == null) {
            return "{wic-na}";
        }

        return weatherIconMap.get(condition);
    }

}
