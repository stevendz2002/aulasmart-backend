-- Cambio de esquema: rechazo automático sin atribuirlo a una persona.
ALTER TABLE solicitud_aula DROP CONSTRAINT solicitud_revision_ck;
ALTER TABLE solicitud_aula ADD CONSTRAINT solicitud_revision_ck CHECK (
 (estado = 'PENDIENTE' AND revisada_por IS NULL AND revisada_en IS NULL)
 OR (estado = 'APROBADA' AND revisada_por IS NOT NULL AND revisada_en IS NOT NULL)
 OR (estado = 'RECHAZADA' AND revisada_en IS NOT NULL)
);
