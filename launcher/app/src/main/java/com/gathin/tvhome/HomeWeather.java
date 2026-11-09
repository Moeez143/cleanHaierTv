package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** A small, Home-only current-weather line. No service, location permission, or background polling. */
final class HomeWeather {
    private static final long REFRESH_MS = 30 * 60 * 1000L;
    private static final long RETRY_MS = 10 * 60 * 1000L;
    private final TextView label;
    private final String city;
    private final String endpoint;
    private final SharedPreferences cache;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private boolean active;
    private boolean loading;
    private long lastAttempt;

    HomeWeather(Context context, TextView label) {
        this.label = label;
        city = context.getString(R.string.weather_city).trim();
        String latitude = context.getString(R.string.latitude).trim();
        String longitude = context.getString(R.string.longitude).trim();
        cache = context.getSharedPreferences("home_weather", Context.MODE_PRIVATE);
        if (city.isEmpty() || latitude.isEmpty() || longitude.isEmpty()) {
            endpoint = null;
            label.setVisibility(View.GONE);
        } else {
            endpoint = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude
                    + "&longitude=" + longitude
                    + "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code"
                    + "&temperature_unit=celsius";
            label.setText(cache.getString("text", city + " · Weather loading…"));
            label.setVisibility(View.VISIBLE);
        }
    }

    void start() {
        active = true;
        tick();
    }

    void tick() {
        if (!active || endpoint == null || loading) return;
        long now = System.currentTimeMillis();
        long success = cache.getLong("updated", 0);
        if (success > 0 && now - success < REFRESH_MS) return;
        if (now - lastAttempt < RETRY_MS) return;
        lastAttempt = now;
        loading = true;
        worker.execute(() -> {
            String result = fetch();
            main.post(() -> {
                loading = false;
                if (result == null) {
                    if (cache.getString("text", null) == null && active) {
                        label.setText(city + " · Weather unavailable");
                    }
                    return;
                }
                cache.edit().putString("text", result).putLong("updated", System.currentTimeMillis()).apply();
                if (active) label.setText(result);
            });
        });
    }

    void stop() {
        active = false;
    }

    void close() {
        active = false;
        worker.shutdownNow();
    }

    private String fetch() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(endpoint).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("Accept", "application/json");
            if (connection.getResponseCode() != 200) return null;
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8))) {
                char[] chunk = new char[1024];
                int count;
                while ((count = reader.read(chunk)) != -1 && body.length() < 8192) {
                    body.append(chunk, 0, count);
                }
            }
            JSONObject current = new JSONObject(body.toString()).getJSONObject("current");
            int temperature = (int) Math.round(current.getDouble("temperature_2m"));
            int feels = (int) Math.round(current.getDouble("apparent_temperature"));
            int humidity = current.getInt("relative_humidity_2m");
            int code = current.getInt("weather_code");
            return String.format(Locale.US, "%s · %d°C · %s · Feels %d° · Humidity %d%%",
                    city, temperature, condition(code), feels, humidity);
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    static String condition(int code) {
        if (code == 0) return "Clear";
        if (code <= 2) return "Partly cloudy";
        if (code == 3) return "Cloudy";
        if (code == 45 || code == 48) return "Foggy";
        if (code >= 95) return "Thunderstorm";
        if (code >= 71 && code <= 77 || code == 85 || code == 86) return "Snow";
        if (code >= 51 && code <= 67 || code >= 80 && code <= 82) return "Rain";
        return "Weather";
    }
}
