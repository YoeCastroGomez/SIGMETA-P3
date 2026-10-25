-- 00_base.sql | SIGMETA
-- Crea la base vacia. Luego ejecutar 01 a 05 en orden (o solo sigmeta.sql).
-- Script integrado
-- Ejecutar sobre una base de pruebas: elimina y recrea la base sigmeta.
-- Las FK mantienen el nombre de la entidad referenciada.
-- CORRERLO SOLO UNA VEZ
DROP DATABASE IF EXISTS sigmeta;
-- una sola collation para tablas y parametros de los SP (evita "Illegal mix of collations")
CREATE DATABASE sigmeta DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sigmeta;
