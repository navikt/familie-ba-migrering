package no.nav.familie.ba.migrering.services

import no.nav.familie.ba.migrering.rest.MigreringsfeilType
import no.nav.familie.ba.migrering.services.Kategori.ORDINÆR_DELT_BOSTED
import no.nav.familie.ba.migrering.services.Kategori.UTVIDET
import no.nav.familie.ba.migrering.services.Kategori.UTVIDET_DELT_BOSTED
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class HentSakTilMigreringScheduler(
    val hentSakTilMigreringService: HentSakTilMigreringService,
    @Value("\${migrering.antallPersoner.ordinær}") val antallPersoneOrdinærr: Int,
    @Value("\${migrering.antallPersoner.utvidet}") val antallPersonerUtvidet: Int,
    @Value("\${migrering.antallPersoner.ordinær.deltbosted:0}") val antallPersonerOrdinærDeltBosted: Int,
    @Value("\${migrering.antallPersoner.utvidet.deltbosted:0}") val antallPersonerUtvidetDeltBosted: Int,
) {

    @Scheduled(cron = "0 0 13 * * MON-FRI", zone = "Europe/Oslo")
    fun hentOrdinærSakTilMigreringScheduler() {
        Log.info("Trigger migrering av $antallPersoneOrdinærr ordinære saker")
        hentSakTilMigreringService.migrer(antallPersoneOrdinærr)
    }

    @Scheduled(cron = "0 50 12 * * MON-FRI", zone = "Europe/Oslo")
    fun hentUtvidetSakerTilMigreringScheduler() {
        Log.info("Trigger migrering av $antallPersonerUtvidet utvidete saker")
        hentSakTilMigreringService.migrer(antallPersonerUtvidet, kategori = UTVIDET)
    }

    @Scheduled(cron = "0 40 12 * * MON-FRI", zone = "Europe/Oslo")
    fun hentDeltBostedScheduler() {
        Log.info("Trigger migrering av $antallPersonerOrdinærDeltBosted ordinære saker med delt bosted")
        if (antallPersonerOrdinærDeltBosted > 0) {
            hentSakTilMigreringService.migrer(antallPersonerOrdinærDeltBosted, kategori = ORDINÆR_DELT_BOSTED)
        }
        Log.info("Trigger migrering av $antallPersonerUtvidetDeltBosted utvidete saker med delt bosted")
        if (antallPersonerUtvidetDeltBosted > 0) {
            hentSakTilMigreringService.migrer(antallPersonerUtvidetDeltBosted, kategori = UTVIDET_DELT_BOSTED)
        }
    }

    @Scheduled(cron = "0 0 17 * * FRI", zone = "Europe/Oslo")
    fun rekjørMigreringerMedFeiltypeÅpenSak() {
        Log.info("Trigger automatisk rekjøring av migreringer som feilet pga. åpen sak i Infotrygd")
        hentSakTilMigreringService.rekjørMigreringerMedFeiltype(MigreringsfeilType.ÅPEN_SAK_INFOTRYGD.name)
        hentSakTilMigreringService.rekjørMigreringerMedFeiltype(MigreringsfeilType.ÅPEN_SAK_TIL_BESLUTNING_I_INFOTRYGD.name)
    }

    @Scheduled(cron = "0 0 17 * * MON-FRI", zone = "Europe/Oslo")
    fun rekjørMigreringerMedFeiltypeUKJENT() {
        Log.info("Trigger automatisk rekjøring av migreringer som feilet ukjente feil")
        hentSakTilMigreringService.rekjørMigreringerMedFeiltype(MigreringsfeilType.UKJENT.name)
    }

    companion object {
        val Log = LoggerFactory.getLogger(HentSakTilMigreringScheduler::class.java)
    }
}
