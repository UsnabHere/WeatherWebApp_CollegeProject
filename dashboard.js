"use strict";

const statusMessage = document.querySelector("#lookup-status");
const locate = document.querySelector("#locate");
const locationForm = document.querySelector("#location-form");
let locating = false;

function showStatus(text, loading) {
    statusMessage.textContent = text;
    statusMessage.hidden = !text;
    statusMessage.classList.toggle("is-loading", loading);
}

for (const form of document.querySelectorAll(".lookup")) {
    form.addEventListener("submit", event => {
        if (locating && form !== locationForm) {
            event.preventDefault();
            return;
        }
        if (locate) locate.disabled = true;
        showStatus("Loading weather…", true);
    });
}

window.addEventListener("pageshow", () => {
    locating = false;
    if (locate) locate.disabled = false;
    showStatus("", false);
});

async function resolvePlace(latitude, longitude) {
    const query = new URLSearchParams({ latitude, longitude, localityLanguage: "en" });
    const url = "https://api.bigdatacloud.net/data/reverse-geocode-client?" + query;
    try {
        const response = await fetch(url, { signal: AbortSignal.timeout(3500) });
        if (!response.ok) return "";
        const data = await response.json();
        const city = data.city || data.locality || data.principalSubdivision;
        if (!city) return "";
        return [city, data.countryName].filter(Boolean).join(", ");
    } catch {
        return "";
    }
}

if (locate) {
    locate.addEventListener("click", () => {
        if (!navigator.geolocation || !window.isSecureContext) {
            showStatus("Location access is unavailable here. Enter a city instead.", false);
            return;
        }

        locating = true;
        locate.disabled = true;
        showStatus("Waiting for your location…", true);
        navigator.geolocation.getCurrentPosition(async ({ coords }) => {
            showStatus("Finding weather near you…", true);
            const latitude = coords.latitude.toFixed(3);
            const longitude = coords.longitude.toFixed(3);
            const place = await resolvePlace(latitude, longitude);
            locationForm.elements.latitude.value = latitude;
            locationForm.elements.longitude.value = longitude;
            locationForm.elements.place.value = place;
            locationForm.requestSubmit();
        }, error => {
            locating = false;
            locate.disabled = false;
            const messages = {
                1: "Location permission was denied. Allow location in your browser or enter a city.",
                2: "Your device could not determine its location. Try again or enter a city.",
                3: "Location lookup timed out. Try again or enter a city."
            };
            showStatus(messages[error.code] || "Location lookup failed. Enter a city instead.", false);
        }, { timeout: 9000, maximumAge: 0 });
    });
}

const hourList = document.querySelector("#hour-list");
if (hourList) {
    const controls = document.querySelector(".scroll-controls");
    controls.hidden = false;
    for (const button of controls.querySelectorAll("button")) {
        button.addEventListener("click", () => {
            const reducedMotion = matchMedia("(prefers-reduced-motion: reduce)").matches;
            hourList.scrollBy({
                left: Number(button.dataset.scroll) * hourList.clientWidth * .8,
                behavior: reducedMotion ? "instant" : "smooth"
            });
        });
    }
    const refreshControls = () => {
        controls.firstElementChild.disabled = hourList.scrollLeft < 2;
        controls.lastElementChild.disabled = hourList.scrollLeft + hourList.clientWidth >= hourList.scrollWidth - 2;
    };
    hourList.addEventListener("scroll", refreshControls, { passive: true });
    new ResizeObserver(refreshControls).observe(hourList);
    refreshControls();
    initChart(hourList);
}

function initChart(hourList) {
    const cards = [...hourList.children];
    const points = cards.map(card => ({
        temperature: card.dataset.temp === "" ? null : Number(card.dataset.temp),
        time: card.dataset.time,
        label: card.dataset.label
    }));
    const valid = points.filter(point => point.temperature !== null).map(point => point.temperature);
    if (!valid.length) return;

    const chart = document.querySelector("#chart");
    const svg = document.querySelector("#temperature-graph");
    const value = document.querySelector("#chart-value");
    const slider = document.querySelector("#chart-hour");
    chart.hidden = false;
    slider.max = points.length - 1;
    let selected = points.findIndex(point => point.temperature !== null);
    let coordinates = [];
    let marker;
    let guide;

    function element(name, attributes, text) {
        const node = document.createElementNS("http://www.w3.org/2000/svg", name);
        for (const [key, attribute] of Object.entries(attributes)) node.setAttribute(key, attribute);
        if (text !== undefined) node.textContent = text;
        svg.append(node);
        return node;
    }

    function select(index, bringIntoView) {
        selected = index;
        const point = points[index];
        const { x, y } = coordinates[index];
        const temperature = point.temperature === null ? "temperature unavailable" : point.temperature + "°C";
        value.textContent = point.label + " · " + temperature;
        slider.value = index;
        slider.setAttribute("aria-valuetext", value.textContent);
        guide.setAttribute("x1", x);
        guide.setAttribute("x2", x);
        marker.setAttribute("visibility", y === null ? "hidden" : "visible");
        if (y !== null) {
            marker.setAttribute("cx", x);
            marker.setAttribute("cy", y);
        }
        for (const [i, card] of cards.entries()) card.classList.toggle("is-active", index === i);
        if (bringIntoView) {
            const cardBox = cards[index].getBoundingClientRect();
            const listBox = hourList.getBoundingClientRect();
            if (cardBox.left < listBox.left || cardBox.right > listBox.right) {
                hourList.scrollLeft += cardBox.left - listBox.left;
            }
        }
    }

    function draw() {
        const width = Math.max(220, Math.floor(chart.clientWidth));
        const height = 150;
        const left = 35;
        const right = width - 18;
        const bottom = height - 25;
        const top = 13;
        const low = Math.min(...valid) - 1;
        const high = Math.max(...valid) + 1;
        const yAt = temp => bottom - (temp - low) * (bottom - top) / (high - low);
        svg.replaceChildren();
        svg.setAttribute("viewBox", "0 0 " + width + " " + height);
        coordinates = points.map((point, index) => ({
            x: left + index * (right - left) / Math.max(1, points.length - 1),
            y: point.temperature === null ? null : yAt(point.temperature)
        }));

        for (const tick of new Set([low, Math.round((low + high) / 2), high])) {
            const y = yAt(tick);
            element("line", { x1: left, x2: right, y1: y, y2: y, class: "chart-grid" });
            element("text", { x: left - 6, y: y + 4, "text-anchor": "end", class: "chart-text" }, tick + "°");
        }
        const intervals = Math.min(points.length - 1, width < 400 ? 4 : 6);
        for (let i = 0; i <= intervals; i++) {
            const index = intervals ? Math.round(i * (points.length - 1) / intervals) : 0;
            element("text", { x: coordinates[index].x, y: height - 6, "text-anchor": "middle",
                class: "chart-text" }, points[index].time);
        }

        // Break the line where the provider has missing temperature values.
        let segment = [];
        for (const point of [...coordinates, { y: null }]) {
            if (point.y !== null) {
                segment.push(point);
            } else if (segment.length) {
                const first = segment[0];
                const last = segment.at(-1);
                const line = segment.map(({ x, y }) => x + " " + y).join(" L");
                element("path", { d: "M" + line + " L" + last.x + " " + bottom + " L" + first.x + " " + bottom + " Z", class: "chart-area" });
                element("path", { d: "M" + line, class: "chart-line" });
                if (segment.length === 1) element("circle", { cx: first.x, cy: first.y, r: 3, class: "chart-marker" });
                segment = [];
            }
        }
        guide = element("line", { y1: top, y2: bottom, class: "chart-guide" });
        marker = element("circle", { r: 5, class: "chart-marker" });
        select(selected, false);
    }

    slider.addEventListener("input", () => select(Number(slider.value), true));
    cards.forEach((card, index) => card.addEventListener("click", () => select(index, false)));
    for (const eventName of ["pointermove", "pointerdown"]) {
        svg.addEventListener(eventName, event => {
            const box = svg.getBoundingClientRect();
            const x = (event.clientX - box.left) * svg.viewBox.baseVal.width / box.width;
            const index = coordinates.reduce((best, point, i) =>
                Math.abs(point.x - x) < Math.abs(coordinates[best].x - x) ? i : best, 0);
            if (index !== selected) select(index, true);
        });
    }
    new ResizeObserver(draw).observe(chart);
    draw();
}
