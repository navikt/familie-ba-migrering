package no.nav.familie.ba.migrering

import no.nav.familie.ba.migrering.services.HentSakTilMigreringService
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

/**
 * Egen scheduler for dev som kjører oftere enn preprod/prod
 */
@Service
@Profile("dev")
class HentSakTilMigreringDevService(
    val hentSakTilMigreringService: HentSakTilMigreringService,
) {

    @Scheduled(fixedRate = 60 * 10000)
    fun hentSakTilMigreringScheduler() {
        hentSakTilMigreringService.migrer(5)
    }
}
