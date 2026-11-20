package edu.weathertravel;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import static edu.weathertravel.WeatherReport.Hour;

public class TravelAdvice {
    private static final DateTimeFormatter SLOT = DateTimeFormatter.ofPattern("EEE HH:mm z", Locale.ENGLISH);

    public List<String> windows(List<Hour> hours, ZonedDateTime now, boolean outdoors) {
        List<String> windows = new ArrayList<>();
        ZonedDateTime start = null;
        ZonedDateTime end = null;
        for (Hour hour : hours) {
            ZonedDateTime from = hour.time().isBefore(now) ? now : hour.time();
            if (!suitable(hour, outdoors) || (end != null && !end.isEqual(from))) {
                if (start != null) windows.add(format(start, end));
                if (windows.size() == 3) return windows;
                start = null;
            }
            if (suitable(hour, outdoors)) {
                if (start == null) start = from;
                end = hour.time().plusHours(1);
            }
        }
        if (start != null) windows.add(format(start, end));
        return windows;
    }

    private boolean suitable(Hour hour, boolean outdoors) {
        return hour.code() != null && hour.code() <= 3
                && hour.rain() != null && hour.rain() <= (outdoors ? 20 : 30)
                && hour.wind() != null && hour.wind() <= (outdoors ? 25 : 35)
                && hour.temperature() != null
                && hour.temperature() >= (outdoors ? 10 : 5)
                && hour.temperature() < (outdoors ? 32 : 35)
                && (!outdoors || hour.daylight());
    }

    private String format(ZonedDateTime start, ZonedDateTime end) {
        return SLOT.format(start) + " – " + SLOT.format(end);
    }

    public List<String> precautions(List<Hour> hours) {
        var advice = new LinkedHashSet<String>();
        for (Hour hour : hours) {
            Integer code = hour.code();
            if (code != null && code >= 95) {
                advice.add("Thunderstorms: move outdoor plans indoors and check local alerts.");
            }
            if (code != null && (code == 45 || code == 48)) {
                advice.add("Fog: allow extra time and check visibility before driving.");
            }
            if ((code != null && ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)))
                    || (hour.rain() != null && hour.rain() >= 50)) {
                advice.add("Rain possible: carry waterproof layers and allow for slippery roads.");
            }
            if (hour.temperature() != null && hour.temperature() >= 35) {
                advice.add("High heat: carry water, take shade breaks and favour cooler hours.");
            }
            if (hour.temperature() != null && hour.temperature() <= 5) {
                advice.add("Cold conditions: pack warm layers and check for ice.");
            }
            if (hour.wind() != null && hour.wind() > 35) {
                advice.add("Strong winds: check local advisories before exposed outdoor activities.");
            }
        }
        return List.copyOf(advice);
    }
}
