#!/usr/bin/env bash
# Builds the scratch database the generator reads: every migration of database/migrations applied in order, then the
# catalog (columns, constraints, unique indexes) exported to target/catalog.json. Uses the standard PG* environment
# variables (PGHOST, PGPORT, PGUSER, PGPASSWORD) of a role allowed to create databases.
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
migrations="$here/../../../database/migrations"
db="${MYSMS_MODEL_DB:-mysms_model}"
mkdir -p "$here/target"
psql -X -q -d postgres -c "DROP DATABASE IF EXISTS $db" -c "CREATE DATABASE $db"
(cd "$migrations" && ls */*.sql | sort -t/ -k2) | while read -r f; do
  psql -X -q -v ON_ERROR_STOP=1 -d "$db" -f "$migrations/$f" > /dev/null
done
psql -X -At -d "$db" -f "$here/introspect.sql" > "$here/target/catalog.json"
echo "catalog: $(python3 -c "import json,sys; print(len(json.load(open(sys.argv[1]))))" "$here/target/catalog.json") tables"
