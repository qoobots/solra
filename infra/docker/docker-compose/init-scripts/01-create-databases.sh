#!/bin/bash
# Postgres init — create per-service databases for local dev
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
  CREATE DATABASE solra_auth;
  CREATE DATABASE solra_saf;
  CREATE DATABASE solra_avt;
  CREATE DATABASE solra_spc;
  CREATE DATABASE solra_soc;
  CREATE DATABASE solra_grw;
  CREATE DATABASE solra_not;
  CREATE DATABASE solra_crt;
  CREATE DATABASE solra_mon;
EOSQL

echo "Created solra_* databases successfully."
