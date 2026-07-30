-- V3__booking.sql fue editado después de aplicado en los entornos locales
-- (agregó tempo_extra al CREATE TABLE), por lo que Flyway nunca la re-ejecutó.
-- Nunca editar migraciones ya aplicadas: esta migración repara el drift.
ALTER TABLE appointments
    ADD COLUMN tempo_extra BOOLEAN NOT NULL DEFAULT FALSE;
