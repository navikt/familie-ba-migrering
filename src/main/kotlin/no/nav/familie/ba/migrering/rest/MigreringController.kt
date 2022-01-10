package no.nav.familie.ba.migrering.rest

import no.nav.familie.ba.migrering.services.HentSakTilMigreringService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid
import javax.validation.constraints.Max
import javax.validation.constraints.Min

@RestController
@RequestMapping("/api/migrer")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class MigreringController(
    private val hentSakTilMigreringService: HentSakTilMigreringService,
) {


    @PostMapping("/")
    fun migrerFraInfotrygd(@Valid @RequestBody startMigreringRequest: StartMigreringRequest): String {
        return hentSakTilMigreringService.migrer(startMigreringRequest.antallPersoner)
    }


    @PostMapping("/rekjor")
    fun migrerIdenter(@Valid @RequestBody identer: Set<String>): String {
        return hentSakTilMigreringService.rekjørMigreringer(identer)
    }

    @PostMapping("/rekjor/{feiltype}")
    fun migrer(@Valid @PathVariable feiltype: String): String {
        return hentSakTilMigreringService.rekjørMigreringerMedFeiltype(feiltype)
    }

    data class StartMigreringRequest(@Min(1) @Max(20) val antallPersoner: Int)


}
