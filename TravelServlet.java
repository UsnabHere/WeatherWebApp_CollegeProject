package edu.weathertravel;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.regex.Pattern;

@WebServlet("")
public class TravelServlet extends HttpServlet {
    private static final Pattern CITY = Pattern.compile("[\\p{L}\\p{M}\\p{N} .’'\\-]+");
    private final SearchHistory history = new SearchHistory();
    private final WeatherService weather = new WeatherService();
    private final boolean liveEnabled = "live".equalsIgnoreCase(
            System.getenv().getOrDefault("WEATHER_MODE", "live"));

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        render(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        if ("location".equals(request.getParameter("mode"))) {
            locationSearch(request, response);
        } else {
            citySearch(request, response);
        }
        render(request, response);
    }

    private void citySearch(HttpServletRequest request, HttpServletResponse response) {
        String city = parameter(request, "city");
        if (city.length() > 100 || !CITY.matcher(city).matches()
                || city.codePoints().noneMatch(Character::isLetter)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error", "Enter a city name between 1 and 100 characters.");
            return;
        }

        request.setAttribute("city", city);
        try {
            request.setAttribute("report", liveEnabled ? weather.current(city) : weather.sample(city));
        } catch (WeatherService.CityNotFoundException e) {
            request.setAttribute("error", "City not found. Check the spelling or try a nearby city.");
        } catch (IOException | RuntimeException e) {
            log("Live city forecast unavailable.", e);
            request.setAttribute("error", "Live weather is temporarily unavailable. Showing a sample reading.");
            request.setAttribute("report", weather.sample(city));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            request.setAttribute("error", "Weather lookup was interrupted. Please try again.");
        }

        try {
            history.save(city);
        } catch (SQLException e) {
            log("Could not save search.", e);
            request.setAttribute("databaseMessage", "This search could not be saved.");
        }
    }

    private void locationSearch(HttpServletRequest request, HttpServletResponse response) {
        double latitude;
        double longitude;
        try {
            latitude = coordinate(request, "latitude", 90);
            longitude = coordinate(request, "longitude", 180);
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("error", "The location coordinates are invalid. Try locating again or enter a city.");
            return;
        }
        if (!liveEnabled) {
            request.setAttribute("error", "Current location weather requires live mode. You can still search for a sample city.");
            return;
        }

        String place = parameter(request, "place");
        if (place.isBlank() || place.length() > 150 || place.codePoints().anyMatch(Character::isISOControl)) {
            place = "Current location";
            request.setAttribute("locationNote", "Location name unavailable. Weather is based on your coordinates.");
        }
        try {
            request.setAttribute("report", weather.current(latitude, longitude, place));
        } catch (IOException | RuntimeException e) {
            log("Location forecast unavailable.", e);
            request.setAttribute("error", "Weather for your location is unavailable. Try again or enter a city.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            request.setAttribute("error", "Location weather lookup was interrupted. Please try again.");
        }
    }

    private double coordinate(HttpServletRequest request, String name, double limit) {
        double number = Double.parseDouble(parameter(request, name));
        if (!Double.isFinite(number) || Math.abs(number) > limit) {
            throw new IllegalArgumentException("Invalid coordinate");
        }
        return number;
    }

    private String parameter(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.strip();
    }

    private void render(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("liveEnabled", liveEnabled);
        try {
            request.setAttribute("recentCities", history.recentCities());
        } catch (SQLException e) {
            log("Could not read recent searches.", e);
            if (request.getAttribute("databaseMessage") == null) {
                request.setAttribute("databaseMessage", "Recent searches are temporarily unavailable.");
            }
        }
        request.getRequestDispatcher("/WEB-INF/home.jsp").forward(request, response);
    }
}
