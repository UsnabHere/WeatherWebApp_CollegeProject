package edu.weathertravel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import static edu.weathertravel.WeatherReport.*;

public class WeatherService {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter LOCAL_TIME = DateTimeFormatter.ofPattern("EEE, HH:mm z", Locale.ENGLISH);
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    private final TravelAdvice advice = new TravelAdvice();

    public WeatherReport sample(String city) {
        int variant = Math.floorMod(city.toLowerCase(Locale.ROOT).hashCode(), 4);
        return new WeatherReport(city, 22 + variant * 3, condition(new int[]{0, 2, 45, 61}[variant], true),
                false, null, List.of(), List.of(), List.of(), List.of(), List.of());
    }

    public WeatherReport current(String city) throws IOException, InterruptedException {
        String name = URLEncoder.encode(city, StandardCharsets.UTF_8);
        var places = fetch("https://geocoding-api.open-meteo.com/v1/search?count=1&language=en&name=" + name)
                .getAsJsonArray("results");
        if (places == null || places.isEmpty()) {
            throw new CityNotFoundException();
        }
        var place = places.get(0).getAsJsonObject();
        String location = place.get("name").getAsString();
        if (place.has("country")) {
            location += ", " + place.get("country").getAsString();
        }
        return current(place.get("latitude").getAsDouble(), place.get("longitude").getAsDouble(), location);
    }

    public WeatherReport current(double latitude, double longitude, String place)
            throws IOException, InterruptedException {
        String url = "https://api.open-meteo.com/v1/forecast?timezone=auto&timeformat=unixtime&forecast_days=7"
                + "&latitude=" + latitude + "&longitude=" + longitude
                + "&current=temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,weather_code,is_day"
                + "&hourly=temperature_2m,weather_code,precipitation_probability,wind_speed_10m,is_day"
                + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,sunrise,sunset";
        JsonObject forecast = fetch(url);
        ZoneId zone = ZoneId.of(forecast.get("timezone").getAsString());
        ZoneOffset dailyOffset = ZoneOffset.ofTotalSeconds(forecast.get("utc_offset_seconds").getAsInt());
        JsonObject current = forecast.getAsJsonObject("current");
        JsonObject daily = forecast.getAsJsonObject("daily");
        ZonedDateTime now = Instant.now().atZone(zone);
        ZonedDateTime updated = Instant.ofEpochSecond(current.get("time").getAsLong()).atZone(zone);
        List<Hour> hours = hours(forecast.getAsJsonObject("hourly"), now.truncatedTo(ChronoUnit.HOURS));
        List<Day> days = days(daily, dailyOffset);
        Integer rain = hours.isEmpty() || !hours.getFirst().time().isEqual(now.truncatedTo(ChronoUnit.HOURS))
                ? null : hours.getFirst().rain();
        var details = new Details(LOCAL_TIME.format(updated), zone.getId(),
                rounded(current, "apparent_temperature"), rounded(current, "relative_humidity_2m"),
                rounded(current, "wind_speed_10m"), rain, sunTime(daily, "sunrise", zone),
                sunTime(daily, "sunset", zone));
        return new WeatherReport(place, rounded(current, "temperature_2m"),
                condition(rounded(current, "weather_code"), Integer.valueOf(1).equals(rounded(current, "is_day"))),
                true, details, hours, days, advice.windows(hours, now, false), advice.windows(hours, now, true),
                advice.precautions(hours));
    }

    private List<Hour> hours(JsonObject hourly, ZonedDateTime start) {
        var times = hourly.getAsJsonArray("time");
        List<Hour> hours = new ArrayList<>();
        Instant end = start.toInstant().plus(24, ChronoUnit.HOURS);
        for (int i = 0; i < times.size(); i++) {
            Instant instant = Instant.ofEpochSecond(times.get(i).getAsLong());
            if (instant.isBefore(start.toInstant())) continue;
            if (!instant.isBefore(end)) break;
            Integer code = rounded(hourly, "weather_code", i);
            boolean daylight = Integer.valueOf(1).equals(rounded(hourly, "is_day", i));
            hours.add(new Hour(instant.atZone(start.getZone()), rounded(hourly, "temperature_2m", i),
                    rounded(hourly, "wind_speed_10m", i), rounded(hourly, "precipitation_probability", i),
                    code, daylight, condition(code, daylight)));
        }
        return hours;
    }

    private List<Day> days(JsonObject daily, ZoneOffset offset) {
        var times = daily.getAsJsonArray("time");
        List<Day> days = new ArrayList<>();
        for (int i = 0; i < times.size(); i++) {
            var date = Instant.ofEpochSecond(times.get(i).getAsLong()).atOffset(offset).toLocalDate();
            days.add(new Day(DAY.format(date), DATE.format(date),
                    condition(rounded(daily, "weather_code", i), true),
                    rounded(daily, "temperature_2m_max", i), rounded(daily, "temperature_2m_min", i),
                    rounded(daily, "precipitation_probability_max", i)));
        }
        return days;
    }

    private String sunTime(JsonObject daily, String key, ZoneId zone) {
        var value = daily.getAsJsonArray(key).get(0);
        return value.isJsonNull() ? "Unavailable" : clock(Instant.ofEpochSecond(value.getAsLong()).atZone(zone));
    }

    private Integer rounded(JsonObject fields, String key) {
        var value = fields.get(key);
        return value == null || value.isJsonNull() ? null : Math.round(value.getAsFloat());
    }

    private Integer rounded(JsonObject fields, String key, int index) {
        var value = fields.getAsJsonArray(key).get(index);
        return value.isJsonNull() ? null : Math.round(value.getAsFloat());
    }

    private JsonObject fetch(String url) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(7)).build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Weather service returned HTTP " + response.statusCode());
        }
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private Condition condition(Integer code, boolean daylight) {
        if (code == null) return new Condition("Unavailable", "unknown");
        return switch (code) {
            case 0 -> new Condition("Clear skies", daylight ? "sun" : "moon");
            case 1, 2 -> new Condition("Partly cloudy", daylight ? "partly" : "cloud");
            case 3 -> new Condition("Overcast", "cloud");
            case 45, 48 -> new Condition("Fog", "fog");
            case 51, 53, 55 -> new Condition("Drizzle", "rain");
            case 56, 57, 66, 67 -> new Condition("Freezing rain", "rain");
            case 61, 63, 65, 80, 81, 82 -> new Condition("Rain", "rain");
            case 71, 73, 75, 77, 85, 86 -> new Condition("Snow", "snow");
            case 95, 96, 99 -> new Condition("Thunderstorms", "storm");
            default -> new Condition("Unavailable", "unknown");
        };
    }

    public static class CityNotFoundException extends IOException {
        public CityNotFoundException() { super("No matching city"); }
    }
}
