package no.nav.familie.ba.migrering.services

import no.nav.familie.ba.migrering.services.Kategori.UTVIDET
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class HentSakTilMigreringScheduler(
    val hentSakTilMigreringService: HentSakTilMigreringService,
    @Value("\${migrering.antallPersoner.ordinær}") val antallPersoner: Int,
    @Value("\${migrering.antallPersoner.utvidet}") val antallPersonerUtvidetBa: Int,
) {

    @Scheduled(cron = "0 0 13 * * MON-FRI", zone = "Europe/Oslo")
    fun hentSakTilMigreringScheduler() {
        Log.info("Trigger migrering av $antallPersoner")
        hentSakTilMigreringService.migrer(antallPersoner)
        hentSakTilMigreringService.migrer(antallPersonerUtvidetBa, kategori = UTVIDET)
    }


    companion object {
        val Log = LoggerFactory.getLogger(HentSakTilMigreringScheduler::class.java)
    }
}
