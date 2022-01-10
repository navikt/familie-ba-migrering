CREATE TABLE migrertesaker_logg (
    id              UUID NOT NULL,
    person_ident    VARCHAR,
    migreringsdato  TIMESTAMP(3) DEFAULT LOCALTIMESTAMP,
    status          VARCHAR,
    aarsak          VARCHAR,
    call_id         VARCHAR,
    feiltype        VARCHAR
);