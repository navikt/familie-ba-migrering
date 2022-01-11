@file:Suppress("unused")

package no.nav.familie.ba.migrering.rest

/**
 * Enum med feil som kan oppstå under migrering. Samme liste er i ba-sak
 **/
enum class MigreringsfeilType(val beskrivelse: String) {
    AKTIV_BEHANDLING("Det finnes allerede en aktiv behandling på personen som ikke er migrering"),
    ALLEREDE_MIGRERT("Personen er allerede migrert"),
    BEREGNET_BELØP_FOR_UTBETALING_ULIKT_BELØP_FRA_INFOTRYGD("Beregnet beløp var ulikt beløp fra Infotryg"),
    DIFF_BARN_INFOTRYGD_OG_PDL("Kan ikke migrere fordi barn fra PDL ikke samsvarer med løpende barnetrygdbarn fra Infotrygd"),
    FAGSAK_AVSLUTTET_UTEN_MIGRERING("Personen er allerede migrert"),
    FLERE_DELYTELSER_I_INFOTRYGD("Finnes flere delytelser på sak"),
    FLERE_LØPENDE_SAKER_INFOTRYGD("Fant mer enn én aktiv sak på bruker i infotrygd"),
    IKKE_GYLDIG_KJØREDATO("Ikke gyldig kjøredato"),
    IKKE_STØTTET_SAKSTYPE("Kan kun migrere ordinære saker (OR, OS)"),
    INGEN_BARN_MED_LØPENDE_STØNAD_I_INFOTRYGD("Fant ingen barn med løpende stønad på sak"),
    INGEN_LØPENDE_SAK_INFOTRYGD("Personen har ikke løpende sak i infotrygd"),
    IVERKSETT_BEHANDLING_UTEN_VEDTAK("Fant ikke aktivt vedtak på behandling"),
    KAN_IKKE_OPPRETTE_BEHANDLING("Kan ikke opprette behandling"),
    MANGLER_ANDEL_TILKJENT_YTELSE("Fant ingen andeler tilkjent ytelse på behandlingen"),
    MANGLER_FØRSTE_UTBETALINGSPERIODE("Tilkjent ytelse er null"),
    MANGLER_VILKÅRSVURDERING("Fant ikke vilkårsvurdering."),
    MIGRERING_ALLEREDE_PÅBEGYNT("Migrering allerede påbegynt"),
    UGYLDIG_ANTALL_DELYTELSER_I_INFOTRYGD("Kan kun migrere ordinære saker med nøyaktig ett utbetalingsbeløp"),
    UKJENT("Ukjent migreringsfeil"),
    ÅPEN_SAK_INFOTRYGD("Bruker har åpen behandling i Infotrygd"),
}