-- One-time local setup. Run as a PostgreSQL superuser:
--   psql -U postgres -f docs/create-databases.sql
-- Database-per-service: each microservice owns its own database.
CREATE USER sky WITH PASSWORD 'sky';
CREATE DATABASE order_db OWNER sky;
CREATE DATABASE inventory_db OWNER sky;
