CREATE TABLE IF NOT EXISTS migrertesaker(
    id UUID PRIMARY KEY,
    person_ident VARCHAR,
    migreringsdato TIMESTAMP(3) DEFAULT LOCALTIMESTAMP,
    status VARCHAR,
    aarsak VARCHAR,
    resultat_fra_ba json,
    sak_nummer VARCHAR
)