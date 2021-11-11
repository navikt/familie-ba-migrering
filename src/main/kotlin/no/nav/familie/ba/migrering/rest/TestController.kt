package no.nav.familie.ba.migrering.rest

import no.nav.familie.ba.migrering.domain.MigreringStatus
import no.nav.familie.ba.migrering.domain.MigrertsakRepository
import no.nav.familie.ba.migrering.services.BekreftMigreringService
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/test")
@Unprotected
@Profile("preprod")
class TestController(
    val bekreftMigreringService: BekreftMigreringService,
    val migrertsakRepository: MigrertsakRepository
) {

    @GetMapping(path = ["/triggerBekreft"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun triggerBekreft(): String? {
        LOG.info("trigger BekreftMigreringService")
        try {
            bekreftMigreringService.bekreftMigrering()
            return "Ok"
        } catch (e: Exception) {
            return e.message
        }
    }

    @GetMapping(path = ["/revertBekreft"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun triggerRevertBekreft(): String? {
        LOG.info("trigger revert of verification")
        var revertertSak = 0
        try {
            (
                migrertsakRepository.findByStatus(status = MigreringStatus.VERIFISERT) + migrertsakRepository.findByStatus(
                    status = MigreringStatus.VERIFISERING_FEILET
                )
                ).forEach {
                migrertsakRepository.update(it.copy(status = MigreringStatus.MIGRERT_I_BA))
                revertertSak++
            }
            return "$revertertSak saker revertert ok"
        } catch (e: Exception) {
            return "Error ${e.message}, og $revertertSak saker revertert"
        }
    }

    companion object{
        val LOG = LoggerFactory.getLogger(TestController::class.java)
    }
}
