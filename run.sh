#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

# Include the workspace path in the label so copies of the project are independent.
label="edu.weathertravel.tomcat.$(printf '%s' "$PWD" | cksum | cut -d ' ' -f 1)"
mode="${1:-run}"

if [[ "$mode" == stop ]]; then
    if launchctl list "$label" >/dev/null 2>&1; then
        launchctl remove "$label"
    fi
    exit 0
fi

if [[ "$mode" != run && "$mode" != start ]]; then
    echo "Usage: scripts/run.sh [run|start|stop]" >&2
    exit 2
fi

if [[ -z "${DB_USER:-}" && -r .local/db.env ]]; then
    source .local/db.env
fi
: "${DB_USER:?Run scripts/setup-db.sh first or export DB_USER and DB_PASSWORD.}"
: "${DB_PASSWORD:?Set DB_PASSWORD before starting Tomcat.}"
if launchctl list "$label" >/dev/null 2>&1; then
    echo "Stop the running background instance with scripts/run.sh stop first." >&2
    exit 1
fi

CATALINA_HOME="$(brew --prefix tomcat@10)/libexec"
CATALINA_BASE="$PWD/.local/tomcat"
if [[ "$mode" == start ]]; then
    # A launchd job may be unable to read files on macOS's protected Desktop.
    mkdir -p .local
    if [[ -f .local/background-dir ]]; then
        CATALINA_BASE="$(cat .local/background-dir)"
    fi
    if [[ "$CATALINA_BASE" == "$PWD/.local/tomcat" || ! -d "$CATALINA_BASE" ]]; then
        CATALINA_BASE="$(mktemp -d "${TMPDIR:-/tmp}/weather-travel.XXXXXXXX")"
        printf '%s\n' "$CATALINA_BASE" > .local/background-dir
    fi
fi
export CATALINA_HOME CATALINA_BASE
mkdir -p "$CATALINA_BASE"/{conf,logs,temp,webapps,work}
if [[ ! -f "$CATALINA_BASE/conf/server.xml" ]]; then
    cp -R "$CATALINA_HOME/conf/." "$CATALINA_BASE/conf/"
    sed -i '' -e 's/<Server port="8005"/<Server port="-1"/' \
        -e 's/<Connector port="8080"/<Connector address="127.0.0.1" port="8080"/' \
        "$CATALINA_BASE/conf/server.xml"
fi

# Tomcat uses the JSP's timestamp to decide whether to recompile it.
"$(brew --prefix maven)/bin/mvn" -B -Dmaven.repo.local=.local/maven-repository \
    "-Dproject.build.outputTimestamp=$(date -u +%Y-%m-%dT%H:%M:%SZ)" package
cp target/weather-travel.war "$CATALINA_BASE/webapps/weather-travel.war"

if [[ "$mode" == run ]]; then
    exec "$CATALINA_HOME/bin/catalina.sh" run
fi

# launchd starts a separate process, so pass configuration through a private file.
(
    umask 077
    for key in CATALINA_HOME CATALINA_BASE JAVA_HOME JRE_HOME DB_URL DB_USER DB_PASSWORD WEATHER_MODE; do
        if [[ -n "${!key:-}" ]]; then
            printf 'export %s=%q\n' "$key" "${!key}"
        fi
    done > "$CATALINA_BASE/runtime.env"
)
log="$CATALINA_BASE/logs/catalina.out"
launchctl submit -l "$label" -o "$log" -e "$log" -- /bin/bash -c '
    source "$1" || exit 1
    exec "$CATALINA_HOME/bin/catalina.sh" run
' bash "$CATALINA_BASE/runtime.env"
printf 'Tomcat submitted to launchd. Log file %s\n' "$log"
