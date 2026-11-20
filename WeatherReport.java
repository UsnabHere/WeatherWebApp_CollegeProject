package edu.weathertravel;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public record WeatherReport(String place, Integer temperature, Condition weather, boolean live,
                            Details details, List<Hour> hours, List<Day> days,
                            List<String> travelWindows, List<String> outdoorWindows, List<String> precautions) {
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter HOUR_LABEL = DateTimeFormatter.ofPattern("EEE HH:mm z", Locale.ENGLISH);

    static String clock(ZonedDateTime time) {
        return CLOCK.format(time);
    }

    public String getPlace() { return place; }
    public Integer getTemperature() { return temperature; }
    public Condition getWeather() { return weather; }
    public boolean isLive() { return live; }
    public Details getDetails() { return details; }
    public List<Hour> getHours() { return hours; }
    public List<Day> getDays() { return days; }
    public List<String> getTravelWindows() { return travelWindows; }
    public List<String> getOutdoorWindows() { return outdoorWindows; }
    public List<String> getPrecautions() { return precautions; }

    public record Condition(String label, String icon) {
        public String getLabel() { return label; }
        public String getIcon() { return icon; }
    }

    public record Details(String updated, String zone, Integer feelsLike, Integer humidity,
                          Integer wind, Integer rain, String sunrise, String sunset) {
        public String getUpdated() { return updated; }
        public String getZone() { return zone; }
        public Integer getFeelsLike() { return feelsLike; }
        public Integer getHumidity() { return humidity; }
        public Integer getWind() { return wind; }
        public Integer getRain() { return rain; }
        public String getSunrise() { return sunrise; }
        public String getSunset() { return sunset; }
    }

    public record Hour(ZonedDateTime time, Integer temperature, Integer wind, Integer rain,
                       Integer code, boolean daylight, Condition weather) {
        public String getTime() { return clock(time); }
        public String getLabel() { return HOUR_LABEL.format(time); }
        public Integer getTemperature() { return temperature; }
        public Integer getRain() { return rain; }
        public Condition getWeather() { return weather; }
    }

    public record Day(String name, String date, Condition weather, Integer high, Integer low, Integer rain) {
        public String getName() { return name; }
        public String getDate() { return date; }
        public Condition getWeather() { return weather; }
        public Integer getHigh() { return high; }
        public Integer getLow() { return low; }
        public Integer getRain() { return rain; }
    }
}
