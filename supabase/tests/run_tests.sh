#!/usr/bin/env bash
# Runs every migration plus the RLS tests on a throwaway PostgreSQL database.
# Usage:  PGHOST=localhost PGUSER=postgres PGPASSWORD=postgres bash supabase/tests/run_tests.sh
# Exit code is 0 only when every test passes.
set -euo pipefail
cd "$(dirname "$0")/.."

DB="${TEST_DB:-campusconnect_test}"
psql -q -v ON_ERROR_STOP=1 -d postgres -c "drop database if exists ${DB}" -c "create database ${DB}"
run() { psql -q -v ON_ERROR_STOP=1 -d "${DB}" "$@"; }

run -f tests/stubs.sql
for f in migrations/*.sql; do
  echo "Applying ${f}"
  run -f "${f}"
done

echo "Running tests..."
OUT="$(psql -q -At -F '|' -v ON_ERROR_STOP=1 -d "${DB}" -f tests/rls_tests.sql)"
PASS=$(printf '%s\n' "${OUT}" | grep -c '^t|' || true)
FAIL=$(printf '%s\n' "${OUT}" | grep -c '^f|' || true)
printf '%s\n' "${OUT}" | grep '^f|' || true
echo "Passed: ${PASS}   Failed: ${FAIL}"
[ "${FAIL}" -eq 0 ] && [ "${PASS}" -gt 0 ]
