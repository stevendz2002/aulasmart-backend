-- V3 había estrechado el catálogo de recursos de sincronización al único
-- recurso del producto en su momento (AULAS). La sincronización de la
-- ocupación académica por clases (EVENTOS, calendario público de SIGA en
-- publicomanejoespacios) reintroduce el segundo recurso verificado.
BEGIN;

ALTER TABLE sincronizacion_siga DROP CONSTRAINT IF EXISTS sincronizacion_recurso_ck;

ALTER TABLE sincronizacion_siga
    ADD CONSTRAINT sincronizacion_recurso_ck CHECK (recurso IN ('AULAS', 'EVENTOS'));

COMMIT;
