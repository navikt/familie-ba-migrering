package no.nav.familie.ba.migrering

import no.nav.familie.ba.migrering.services.HentSakTilMigreringService
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
@Profile("dev")
/**
 * Egen scheduler for dev som kjører oftere enn preprod/prod
 */
class HentSakTilMigreringDevService(
    val hentSakTilMigreringService: HentSakTilMigreringService,
) {

    @Scheduled(fixedRate = 60 * 1000)
    fun hentSakTilMigreringScheduler() {
        hentSakTilMigreringService.migrer(10)
    }
}
