#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE digitrans_gateway;
    CREATE DATABASE digitrans_erp;
    CREATE DATABASE digitrans_crm;
    CREATE DATABASE digitrans_supply;
EOSQL
