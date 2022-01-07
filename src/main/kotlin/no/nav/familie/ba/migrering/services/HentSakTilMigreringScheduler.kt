package no.nav.familie.ba.migrering.services

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class HentSakTilMigreringScheduler(
    val hentSakTilMigreringService: HentSakTilMigreringService,
    @Value("\${migrering.antallPersoner}") val antallPersoner: Int,
) {

    @Scheduled(cron = "0 0 13 * * MON-FRI", zone = "Europe/Oslo")
    fun hentSakTilMigreringScheduler() {
        Log.info("Trigger migrering av $antallPersoner")
        hentSakTilMigreringService.migrer(antallPersoner)
    }


    companion object {
        val Log = LoggerFactory.getLogger(HentSakTilMigreringScheduler::class.java)
    }
}
