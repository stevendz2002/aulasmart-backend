CREATE TABLE sesion_autenticacion (
    id UUID PRIMARY KEY,
    correo VARCHAR(254) NOT NULL,
    expira_en TIMESTAMPTZ NOT NULL,
    revocada BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_sesion_expira ON sesion_autenticacion(expira_en);
CREATE TABLE token_renovacion (
    hash VARCHAR(64) PRIMARY KEY,
    sesion_id UUID NOT NULL REFERENCES sesion_autenticacion(id) ON DELETE CASCADE,
    usado BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_token_sesion ON token_renovacion(sesion_id);
