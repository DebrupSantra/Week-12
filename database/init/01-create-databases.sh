#!/bin/sh
set -eu

for database in product_db order_db payment_db user_db inventory_db notification_db; do
  psql \
    --username "$POSTGRES_USER" \
    --dbname "$POSTGRES_DB" \
    --set=db="$database" \
    --set=owner="$POSTGRES_USER" \
    --set=ON_ERROR_STOP=1 \
    --command='CREATE DATABASE :"db" OWNER :"owner"'
done
