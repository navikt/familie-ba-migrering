CREATE CAST (VARCHAR AS JSON) WITHOUT FUNCTION AS IMPLICIT;

CREATE TABLE IF NOT EXISTS migrertesaker(
    id UUID PRIMARY KEY,
    person_ident VARCHAR,
    migreringsdato TIMESTAMP(3) DEFAULT LOCALTIMESTAMP,
    status VARCHAR,
    aarsak VARCHAR,
    resultat_fra_ba JSON,
    sak_nummer VARCHAR
);