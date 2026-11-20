#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

umask 077
mkdir -p .local
if [[ ! -f .local/db.env ]]; then
    printf 'export DB_USER=weather_app\nexport DB_PASSWORD=%s\n' "$(openssl rand -hex 24)" > .local/db.env
fi
source .local/db.env

# Arguments such as -p apply to the administrator connection.
{
    cat db/schema.sql
    printf "CREATE USER IF NOT EXISTS 'weather_app'@'127.0.0.1' IDENTIFIED BY '%s';\n" "$DB_PASSWORD"
    printf "GRANT SELECT, INSERT ON weather_travel.search_history TO 'weather_app'@'127.0.0.1';\n"
} | "$(brew --prefix mysql)/bin/mysql" -u root "$@"
