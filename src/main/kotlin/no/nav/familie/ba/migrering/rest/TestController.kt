package no.nav.familie.ba.migrering.rest

import no.nav.familie.ba.migrering.services.BekreftMigreringService
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/test")
@Unprotected
@Profile("preprod")
class TestController(val bekreftMigreringService: BekreftMigreringService) {

    @GetMapping(path = ["/triggerBekreft"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun triggerBekreft() {
        bekreftMigreringService.bekreftMigrering()
    }
}
