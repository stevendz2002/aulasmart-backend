# Entregable 1: modelo entidad-relación

## 1. Alcance y decisiones

El modelo cubre cuentas y roles propios de AulaSmart, catálogos sincronizados desde SIGA, aulas, ocupaciones académicas, solicitudes internas y control de sincronización. No incluye reservas oficiales de SIGA ni equipamiento, porque no pertenecen al alcance verificado.

`CONVERSACION` y `MENSAJE` no se incluyen: la persistencia del historial de AulaBot no es necesaria para las consultas ni para los cinco entregables. Podrán agregarse después si se convierte en un requisito explícito.

Los códigos `codigo_siga` conservan los identificadores externos confirmados en aulas, edificios, tipos y dependencias. Las claves `id_*` son internas y evitan acoplar las relaciones locales a códigos externos.

`aula.activa` es un atributo **local**, no un campo observado en SIGA. `horario_academico.id_externo` es nullable de forma provisional porque el endpoint de eventos no devolvió objetos que permitan confirmar un identificador. Las fechas de ocupaciones y solicitudes usan `TIMESTAMPTZ` para conservar instantes completos y evitar perder horas o zona horaria.

## 2. Diagrama ER único

```mermaid
erDiagram
    USUARIO ||--o{ USUARIO_ROL : posee
    ROL ||--o{ USUARIO_ROL : asigna
    EDIFICIO ||--o{ AULA : ubica
    TIPO_AULA ||--o{ AULA : clasifica
    DEPENDENCIA ||--o{ AULA : administra
    AULA ||--o{ HORARIO_ACADEMICO : tiene
    USUARIO ||--o{ SOLICITUD_AULA : registra
    USUARIO ||--o{ SOLICITUD_AULA : revisa
    AULA ||--o{ SOLICITUD_AULA : recibe

    USUARIO {
        BIGINT id_usuario PK
        VARCHAR nombre
        VARCHAR correo UK
        VARCHAR password_hash
        BOOLEAN activo
        TIMESTAMPTZ creado_en
    }
    ROL {
        SMALLINT id_rol PK
        VARCHAR nombre UK
        VARCHAR descripcion
    }
    USUARIO_ROL {
        BIGINT id_usuario PK,FK
        SMALLINT id_rol PK,FK
    }
    EDIFICIO {
        BIGINT id_edificio PK
        BIGINT codigo_siga UK
        VARCHAR nombre
    }
    TIPO_AULA {
        BIGINT id_tipo_aula PK
        BIGINT codigo_siga UK
        VARCHAR nombre
    }
    DEPENDENCIA {
        BIGINT id_dependencia PK
        BIGINT codigo_siga UK
        VARCHAR nombre
    }
    AULA {
        BIGINT id_aula PK
        BIGINT codigo_siga UK
        VARCHAR nombre
        BIGINT id_edificio FK
        INTEGER capacidad
        BIGINT id_tipo_aula FK
        BIGINT id_dependencia FK
        BOOLEAN activa
    }
    HORARIO_ACADEMICO {
        BIGINT id_horario PK
        BIGINT id_aula FK
        VARCHAR id_externo
        TIMESTAMPTZ fecha_inicio
        TIMESTAMPTZ fecha_fin
        VARCHAR fuente
    }
    SOLICITUD_AULA {
        BIGINT id_solicitud PK
        BIGINT id_usuario FK
        BIGINT id_aula FK
        TIMESTAMPTZ fecha_inicio
        TIMESTAMPTZ fecha_fin
        VARCHAR estado
        VARCHAR motivo
        TIMESTAMPTZ creada_en
        BIGINT revisada_por FK
        TIMESTAMPTZ revisada_en
    }
    SINCRONIZACION_SIGA {
        BIGINT id_sincronizacion PK
        VARCHAR recurso
        TIMESTAMPTZ iniciada_en
        TIMESTAMPTZ finalizada_en
        VARCHAR estado
        INTEGER registros_procesados
        DATE ventana_inicio
        DATE ventana_fin
        VARCHAR detalle_error
    }
```

`SINCRONIZACION_SIGA` es una entidad de control independiente: registra ejecuciones de sincronización y no representa una relación de negocio con cada aula o evento. La unicidad de `HORARIO_ACADEMICO.id_externo` es compuesta con `fuente` y solo se aplica cuando el identificador no es nulo; no se afirma que el valor sea único por sí solo.

## 3. Cardinalidades y reglas

- Un usuario puede tener varios roles y un rol puede pertenecer a varios usuarios. La clave compuesta de `USUARIO_ROL` impide asignaciones duplicadas.
- Cada aula pertenece exactamente a un edificio, un tipo y una dependencia; cada catálogo puede clasificar muchas aulas.
- Un aula puede tener muchos horarios académicos y muchas solicitudes. Un horario importado nunca se convierte en una solicitud: son conceptos y ciclos de vida distintos.
- Una solicitud tiene un docente solicitante. Si está aprobada o rechazada, debe registrar revisor y fecha; si está pendiente, ambos deben ser nulos.
- Los correos se consideran únicos sin distinguir mayúsculas de minúsculas. Las contraseñas se almacenan como hash, nunca como texto plano.
- `capacidad > 0`, `fecha_fin > fecha_inicio` y los estados permitidos se validan también en la base de datos.

## 4. Solapamientos y disponibilidad

Dos intervalos `[inicio, fin)` se solapan cuando:

`inicio_existente < fin_consultado AND fin_existente > inicio_consultado`.

El esquema usa una restricción de exclusión PostgreSQL para impedir dos solicitudes **aprobadas** solapadas en la misma aula. La aprobación también debe comprobar, dentro de una transacción y con control de concurrencia, los horarios académicos; una restricción entre dos tablas no puede sustituir esa regla de aplicación.

Para responder disponibilidad, el backend debe exigir aulas activas, ausencia de solapamientos y evidencia de sincronizaciones exitosas y vigentes. Si faltan datos o la ventana sincronizada no cubre la consulta, debe responder que no puede confirmarse la disponibilidad, no afirmar que el aula está libre.

La aprobación en AulaSmart no registra ni implica una reserva oficial en SIGA.

## 5. Implementación

- Esquema reproducible: `aulasmart-backend/src/main/resources/db/migration/V1__initial_schema.sql`, dejado también disponible en el contenedor vía `compose.yaml` para su aplicación directa con `psql`.
- Datos exclusivamente sintéticos: `database/seeds/001_datos_prueba.sql`.
- Verificación de relaciones y reglas: `database/validation/001_validaciones.sql`.
- Evidencia de campos externos y aspectos pendientes: `docs/evidencias/01-fuentes-siga.md`.
