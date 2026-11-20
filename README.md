# Weather Positioning & Smart Travelling System

WeatherTravel is a Java web dashboard for checking conditions at a destination and choosing when to travel or spend time outdoors. Search by city or share your current location. Live reports show current conditions, a scrollable 24-hour forecast, an interactive temperature graph, and seven calendar days of forecasts including today. Recent manual city searches are available as shortcuts.

## Run from the VS Code terminal on macOS

Open the WeatherTravelSystem folder in VS Code. With the existing Java 21, Maven, Tomcat 10.1 and MySQL installation, run:

```bash
brew services start mysql
WEATHER_MODE=live ./scripts/run.sh start
```

Open [http://localhost:8080/weather-travel/](http://localhost:8080/weather-travel/). The start script builds the WAR, deploys it, and prints the Tomcat log path. If restarting an already-running instance, run the stop command first and wait for the old page to stop responding before starting again.

```bash
./scripts/run.sh stop
```

For a foreground run, use `WEATHER_MODE=live ./scripts/run.sh` and stop it with Ctrl+C. Live mode is the default. For an offline demo, start with `WEATHER_MODE=sample ./scripts/run.sh start`. Sample mode gives a labelled illustrative reading for a city. If the live provider fails during a city lookup, the app can also show a labelled sample reading. Sample readings have no hourly forecasts, seven-day forecasts, or travel windows.

For a fresh installation only, install Java 21 and run `brew install maven mysql tomcat@10`, start MySQL, and run `./scripts/setup-db.sh`. If your MySQL administrator has a password, use `./scripts/setup-db.sh -p`. The setup script creates the schema and generates a password for the application account in an ignored, owner-only file. The account can read and insert search history. You can also supply `DB_USER`, `DB_PASSWORD`, and optionally `DB_URL` through environment variables.

## Show actual database records during your viva

Search for a city, then run this in the VS Code terminal. The newest record appears first. MySQL prompts for the administrator password if you add `-p` to a password-protected installation.

```bash
mysql --user=root --default-character-set=utf8mb4 --table \
  --execute='SELECT id, city, searched_at FROM weather_travel.search_history ORDER BY id DESC;'
```

The query displays every saved entry. Manual city searches use JDBC `PreparedStatement` to insert a row. MySQL supplies its ID and timestamp. The dashboard shows recent city names as shortcuts. Current-location lookups use coordinates rounded to three decimal places, and those coordinates are kept out of search history.

## How the code works

1. **JSP builds the form.** The browser can send a city name or coordinates in a POST request. The **Use My Current Location** button requests browser permission first, then sends approximate coordinates directly from the browser to [BigDataCloud](https://www.bigdatacloud.com/geocoding-apis/free-reverse-geocode-to-city-api) to look up a name. If the name request fails, the app can still show a coordinate-based forecast.
2. **Tomcat calls the servlet.** `TravelServlet` validates the form and calls `WeatherService`. For a city search it also calls `SearchHistory.save()`, which opens a JDBC connection and inserts the search.
3. **Java gets and formats the weather.** `WeatherService` geocodes typed city names using [Open-Meteo Geocoding](https://open-meteo.com/en/docs/geocoding-api). It then requests the current, hourly, and daily data from [Open-Meteo Forecast](https://open-meteo.com/en/docs). It uses the destination's time zone to display the current local hour and select 24 hourly readings. The current rain chance comes from that hour's forecast. Daily rain chance is the maximum hourly probability reported for each day.
4. **Java suggests time windows.** `TravelAdvice` checks each hour for condition, rain chance, wind speed, and temperature. Travel windows use at most 30% rain chance and 35 km/h wind with temperatures from 5°C up to 35°C, exclusive of the upper limit. Outdoor windows are stricter and require daylight. Consecutive qualifying hours are combined. The precautions list flags forecast hazards. These are weather-based suggestions and road conditions and official alerts should be checked before travel.
5. **JSP displays the result.** The servlet attaches a `WeatherReport` to the request. JSTL reads the report and builds the metric panels and forecast cards. Small JavaScript code plots the same hourly temperatures in SVG and lets you explore by pointer or keyboard. Missing temperatures leave gaps in the line.

The weather and location providers require internet access. Browser location requires permission and a secure context such as localhost. If permission is denied or the location is unavailable, manual search remains available.

## Files changed in this upgrade

| File | Change |
| --- | --- |
| `src/main/java/edu/weathertravel/TravelServlet.java` | Handles city and coordinate requests, validation, and user-facing errors. |
| `src/main/java/edu/weathertravel/WeatherService.java` | Requests and converts current, hourly and daily Open-Meteo data. |
| `src/main/java/edu/weathertravel/WeatherReport.java` | Holds the weather fields, hourly points, daily summaries and advice for JSP. |
| `src/main/java/edu/weathertravel/TravelAdvice.java` | New rule-based calculator for travel windows and weather precautions. |
| `src/main/java/edu/weathertravel/SearchHistory.java` | Keeps JDBC city inserts and reads six distinct names for the recent-city shortcuts. |
| `src/main/java/edu/weathertravel/SearchEntry.java` | Removed the record model used by the previous on-page database table. |
| `src/main/webapp/WEB-INF/home.jsp` | Renders the new dashboard. |
| `src/main/webapp/assets/style.css` | Styles the green and lime responsive layout and weather icons. |
| `src/main/webapp/assets/dashboard.js` | New browser location, loading status, hourly scrolling and interactive graph code. |
| `README.md` | Documents the upgraded app and viva commands. |
