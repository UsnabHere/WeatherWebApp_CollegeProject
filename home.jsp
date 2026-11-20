<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="Check destination weather and find the best time to head out.">
    <title>Weather Positioning &amp; Smart Travelling System</title>
    <link rel="stylesheet" href="<c:url value='/assets/style.css'/>">
    <script defer src="<c:url value='/assets/dashboard.js'/>"></script>
</head>
<body>
    <header class="topbar wrap">
        <a class="brand" href="<c:url value='/'/>"><span class="brand-icon" aria-hidden="true">☼</span> WeatherTravel</a>
        <span class="header-mode"><span aria-hidden="true"></span>${liveEnabled ? 'Live weather mode' : 'Sample weather mode'}</span>
    </header>
    <main class="wrap">
        <section class="hero" aria-labelledby="page-title">
            <div class="art" aria-hidden="true"><i></i><b></b><span></span></div>
            <div class="hero-content">
                <p class="kicker">Weather Positioning &amp; Smart Travelling System</p>
                <h1 id="page-title">Know the weather.<br><em>Then go.</em></h1>
                <form class="lookup" method="post" action="<c:url value='/'/>">
                    <label for="city">Destination city</label>
                    <div class="search">
                        <input id="city" name="city" type="search" placeholder="e.g. Kolkata" required maxlength="100"
                               value="<c:out value='${city}'/>">
                        <button class="primary" type="submit">Check weather <span aria-hidden="true">→</span></button>
                    </div>
                </form>
                <c:if test="${liveEnabled}">
                    <form id="location-form" class="lookup location-form" method="post" action="<c:url value='/'/>">
                        <input type="hidden" name="mode" value="location">
                        <input type="hidden" name="latitude">
                        <input type="hidden" name="longitude">
                        <input type="hidden" name="place">
                        <button id="locate" class="secondary" type="button" aria-describedby="location-help">
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true">
                                <circle cx="12" cy="12" r="7"/><circle cx="12" cy="12" r="2"/>
                                <path d="M12 2v3m0 14v3M2 12h3m14 0h3"/>
                            </svg>
                            Use My Current Location
                        </button>
                        <span id="location-help" class="location-help">Shares your approximate location for weather and city lookup.</span>
                        <noscript><p class="form-message">Enable JavaScript to use your current location.</p></noscript>
                    </form>
                </c:if>
                <p id="lookup-status" class="form-message" role="status" hidden></p>
                <c:if test="${not empty error}"><p class="form-message" role="alert"><c:out value="${error}"/></p></c:if>
            </div>
        </section>

        <c:if test="${not empty recentCities or not empty databaseMessage}">
            <section class="history" aria-label="Recent searches">
                <c:if test="${not empty recentCities}">
                    <span class="history-label">Recent searches</span>
                    <form class="lookup chips" method="post" action="<c:url value='/'/>">
                        <c:forEach items="${recentCities}" var="recentCity">
                            <button class="chip" name="city" value="<c:out value='${recentCity}'/>">
                                <c:out value="${recentCity}"/> <span aria-hidden="true">↗</span>
                            </button>
                        </c:forEach>
                    </form>
                </c:if>
                <c:if test="${not empty databaseMessage}">
                    <p class="muted" role="status"><c:out value="${databaseMessage}"/></p>
                </c:if>
            </section>
        </c:if>

        <c:choose>
            <c:when test="${empty report}">
                <section class="panel empty-state">
                    <span class="weather-icon icon-partly" aria-hidden="true"></span>
                    <div><h2>Destination outlook</h2>
                    <p class="muted">Search for a city to see its current conditions, forecast and travel suggestions.</p></div>
                </section>
            </c:when>
            <c:otherwise>
                <section id="outlook" class="panel current" aria-labelledby="weather-title">
                    <div class="section-head">
                        <div>
                            <p class="kicker">${report.live ? 'Current conditions' : 'Sample weather'}</p>
                            <h2 id="weather-title"><c:out value="${report.place}"/></h2>
                            <c:if test="${report.live}">
                                <p class="muted"><c:out value="${report.details.updated}"/> · <c:out value="${report.details.zone}"/></p>
                            </c:if>
                            <c:if test="${not empty locationNote}"><p class="muted"><c:out value="${locationNote}"/></p></c:if>
                        </div>
                        <span class="tag">${report.live ? 'Open-Meteo' : 'Sample reading'}</span>
                    </div>
                    <div class="current-grid">
                        <div class="reading">
                            <span class="weather-icon icon-${report.weather.icon}" aria-hidden="true"></span>
                            <div>
                                <p class="temperature"><c:choose><c:when test="${not empty report.temperature}">${report.temperature}<span>°C</span></c:when><c:otherwise>—</c:otherwise></c:choose></p>
                                <p class="condition"><c:out value="${report.weather.label}"/></p>
                            </div>
                        </div>
                        <c:if test="${report.live}">
                            <dl class="metrics">
                                <div><dt>Feels like</dt><dd>${empty report.details.feelsLike ? '—' : report.details.feelsLike += '°C'}</dd></div>
                                <div><dt>Humidity</dt><dd>${empty report.details.humidity ? '—' : report.details.humidity += '%'}</dd></div>
                                <div><dt>Wind speed</dt><dd>${empty report.details.wind ? '—' : report.details.wind += ' km/h'}</dd></div>
                                <div><dt>Rain chance · this hour</dt><dd>${empty report.details.rain ? '—' : report.details.rain += '%'}</dd></div>
                                <div><dt>Sunrise</dt><dd><c:out value="${report.details.sunrise}"/></dd></div>
                                <div><dt>Sunset</dt><dd><c:out value="${report.details.sunset}"/></dd></div>
                            </dl>
                        </c:if>
                    </div>
                    <c:if test="${not report.live}">
                        <p class="muted">Forecasts and travel time slots are available with live weather.</p>
                    </c:if>
                </section>
                <c:if test="${report.live}">
                    <div class="dashboard">
                        <section class="panel hourly" aria-labelledby="hourly-title">
                            <div class="section-head">
                                <div><h2 id="hourly-title">Next 24 hours</h2><p class="muted">Local time · °C</p></div>
                                <div class="scroll-controls" hidden>
                                    <button type="button" class="secondary" data-scroll="-1" aria-label="Earlier hours">←</button>
                                    <button type="button" class="secondary" data-scroll="1" aria-label="Later hours">→</button>
                                </div>
                            </div>
                            <c:choose>
                                <c:when test="${empty report.hours}"><p class="muted">Hourly forecast unavailable.</p></c:when>
                                <c:otherwise>
                                    <div id="chart" class="chart" hidden>
                                        <p id="chart-value" class="chart-value" role="status"></p>
                                        <svg id="temperature-graph" role="img" aria-label="Hourly temperature line graph"></svg>
                                        <div class="chart-controls">
                                            <label for="chart-hour">Explore by hour</label>
                                            <input type="range" id="chart-hour" min="0" step="1" value="0">
                                        </div>
                                    </div>
                                    <ul id="hour-list" class="hour-list" tabindex="0" aria-label="Hourly forecast cards">
                                        <c:forEach items="${report.hours}" var="hour">
                                            <li class="hour-card" data-temp="${hour.temperature}" data-time="${hour.time}" data-label="<c:out value='${hour.label}'/>">
                                                <span class="hour-time" title="<c:out value='${hour.label}'/>"><c:out value="${hour.time}"/></span>
                                                <span class="weather-icon icon-${hour.weather.icon}" aria-hidden="true"></span>
                                                <strong>${empty hour.temperature ? '—' : hour.temperature += '°'}</strong>
                                                <span class="hour-condition"><c:out value="${hour.weather.label}"/></span>
                                                <span class="probability">${empty hour.rain ? '—' : hour.rain += '%'} rain</span>
                                            </li>
                                        </c:forEach>
                                    </ul>
                                </c:otherwise>
                            </c:choose>
                        </section>

                        <section class="panel daily" aria-labelledby="daily-title">
                            <div class="section-head">
                                <div><h2 id="daily-title">Seven-day forecast</h2><p class="muted">Daily high / low · °C</p></div>
                            </div>
                            <ul class="day-list">
                                <c:forEach items="${report.days}" var="day">
                                    <li class="day-row">
                                        <div><strong><c:out value="${day.name}"/></strong><span class="muted"><c:out value="${day.date}"/></span></div>
                                        <div class="day-weather"><span class="weather-icon icon-${day.weather.icon}" aria-hidden="true"></span>
                                            <span><c:out value="${day.weather.label}"/></span>
                                        </div>
                                        <div class="day-values"><strong>${empty day.high ? '—' : day.high += '°'} / ${empty day.low ? '—' : day.low += '°'}</strong>
                                            <span class="probability">${empty day.rain ? '—' : day.rain += '%'} rain</span>
                                        </div>
                                    </li>
                                </c:forEach>
                            </ul>
                        </section>
                    </div>

                    <section class="panel travel" aria-labelledby="travel-title">
                        <div class="section-head">
                            <div><h2 id="travel-title">Plan your trip</h2><p class="muted">Weather-based suggestions for the next 24 hours. Conditions can change. Check local alerts and road conditions before travelling.</p></div>
                        </div>
                        <div class="travel-grid">
                            <div><h3>Travel windows</h3>
                                <c:choose>
                                    <c:when test="${empty report.travelWindows}"><p class="muted">No upcoming window meets the weather criteria.</p></c:when>
                                    <c:otherwise><ul class="slots"><c:forEach items="${report.travelWindows}" var="window"><li><c:out value="${window}"/></li></c:forEach></ul></c:otherwise>
                                </c:choose>
                            </div>
                            <div><h3>Outdoor activity windows</h3>
                                <c:choose>
                                    <c:when test="${empty report.outdoorWindows}"><p class="muted">No daylight window meets the weather criteria.</p></c:when>
                                    <c:otherwise><ul class="slots"><c:forEach items="${report.outdoorWindows}" var="window"><li><c:out value="${window}"/></li></c:forEach></ul></c:otherwise>
                                </c:choose>
                            </div>
                            <div><h3>Precautions</h3>
                                <c:choose>
                                    <c:when test="${empty report.hours}"><p class="muted">Hourly data unavailable.</p></c:when>
                                    <c:when test="${empty report.precautions}"><p class="muted">No weather precautions flagged in the available forecast.</p></c:when>
                                    <c:otherwise><ul class="precautions"><c:forEach items="${report.precautions}" var="tip"><li><c:out value="${tip}"/></li></c:forEach></ul></c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                    </section>
                </c:if>
            </c:otherwise>
        </c:choose>
    </main>
    <footer class="wrap">
        <span>WeatherTravel</span>
        <c:if test="${liveEnabled}"><span>Weather by <a href="https://open-meteo.com/">Open-Meteo</a> · Location names by <a href="https://www.bigdatacloud.com/">BigDataCloud</a></span></c:if>
    </footer>
</body>
</html>
