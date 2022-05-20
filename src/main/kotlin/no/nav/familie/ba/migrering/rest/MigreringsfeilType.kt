@file:Suppress("unused")

package no.nav.familie.ba.migrering.rest

/**
 * Enum med feil som kan oppstå under migrering. Samme liste er i ba-sak
 **/
enum class MigreringsfeilType(val beskrivelse: String) {
    AKTIV_BEHANDLING("Det finnes allerede en aktiv behandling på personen som ikke er migrering"),
    ALLEREDE_MIGRERT("Personen er allerede migrert"),
    BEREGNET_BELØP_FOR_UTBETALING_ULIKT_BELØP_FRA_INFOTRYGD("Beregnet beløp var ulikt beløp fra Infotrygd"),
    BEREGNET_DELT_BOSTED_BELØP_ULIKT_BELØP_FRA_INFOTRYGD("Beløp beregnet for delt bosted var ulikt beløp fra Infotrygd"),
    DIFF_BARN_INFOTRYGD_OG_BA_SAK("Antall barn på tilkjent ytelse samsvarer ikke med antall barn på stønaden fra infotrygd"),
    DIFF_BARN_INFOTRYGD_OG_PDL("Kan ikke migrere fordi barn fra PDL ikke samsvarer med løpende barnetrygdbarn fra Infotrygd"),
    FAGSAK_AVSLUTTET_UTEN_MIGRERING("Personen er allerede migrert"),
    FLERE_DELYTELSER_I_INFOTRYGD("Finnes flere delytelser på sak"),
    FLERE_LØPENDE_SAKER_INFOTRYGD("Fant mer enn én aktiv sak på bruker i infotrygd"),
    HAR_BARN_OVER_18_PÅ_INFOTRYGDSAK("Infotrygdsak har barn over 18"),
    HISTORISK_IDENT_REGNET_SOM_EKSTRA_BARN_I_INFOTRYGD("Listen med barn fra Infotrygd har identer tilhørende samme barn"),
    IDENT_IKKE_LENGER_AKTIV("Ident ikke lenger aktiv"),
    IKKE_GYLDIG_KJØREDATO("Ikke gyldig kjøredato"),
    IKKE_STØTTET_GRADERING("Personen har ikke støttet gradering"),
    IKKE_STØTTET_SAKSTYPE("Kan kun migrere OR OS/MD eller UT EF"),
    INGEN_BARN_MED_LØPENDE_STØNAD_I_INFOTRYGD("Fant ingen barn med løpende stønad på sak"),
    INGEN_LØPENDE_SAK_INFOTRYGD("Personen har ikke løpende sak i infotrygd"),
    INSTITUSJON("Midlertidig ignoerert fordi det er en institusjon"),
    IVERKSETT_BEHANDLING_UTEN_VEDTAK("Fant ikke aktivt vedtak på behandling"),
    KAN_IKKE_OPPRETTE_BEHANDLING("Kan ikke opprette behandling"),
    KUN_ETT_MIGRERINGFORSØK_PER_DAG("Migrering allerede påbegynt i dag. Vent minst en dag før man prøver igjen"),
    MANGLER_ANDEL_TILKJENT_YTELSE("Fant ingen andeler tilkjent ytelse på behandlingen"),
    MANGLER_FØRSTE_UTBETALINGSPERIODE("Tilkjent ytelse er null"),
    MANGLER_VILKÅRSVURDERING("Fant ikke vilkårsvurdering."),
    MER_ENN_ETT_BARN_PÅ_SAK_AV_TYPE_UT_MD("Migrering av sakstype UT-MD er begrenset til saker med ett barn"),
    MIGRERING_ALLEREDE_PÅBEGYNT("Migrering allerede påbegynt"),
    OPPGITT_ANTALL_BARN_ULIKT_ANTALL_BARNIDENTER("Antall barnidenter samsvarer ikke med stønad.antallBarn"),
    SMÅBARNSTILLEGG_BA_SAK_IKKE_INFOTRYGD("Uoverensstemmelse angående småbarnstillegg"),
    SMÅBARNSTILLEGG_INFOTRYGD_IKKE_BA_SAK("Uoverensstemmelse angående småbarnstillegg"),
    UGYLDIG_ANTALL_DELYTELSER_I_INFOTRYGD("Kan kun migrere ordinære saker med nøyaktig ett utbetalingsbeløp"),
    UKJENT("Ukjent migreringsfeil"),
    ÅPEN_SAK_INFOTRYGD("Bruker har åpen behandling i Infotrygd"),
    ÅPEN_SAK_TIL_BESLUTNING_I_INFOTRYGD("Bruker har avventende vedtak til beslutning i Infotrygd")

}
