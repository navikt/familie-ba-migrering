package no.nav.familie.ba.migrering.rest

import no.nav.familie.ba.migrering.integrasjoner.MigreringResponseDto
import no.nav.familie.ba.migrering.services.HentSakTilMigreringService
import no.nav.familie.ba.migrering.services.Kategori
import no.nav.familie.ba.migrering.services.VerifiserMigeringService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.internal.TaskMaintenanceService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.transaction.annotation.Transactional
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
    private val verifiserMigeringService: VerifiserMigeringService,
    private val taskMaintenanceService: TaskMaintenanceService
) {


    @PostMapping("/")
    fun migrerFraInfotrygd(@Valid @RequestBody startMigreringRequest: StartMigreringRequest): String {
        return hentSakTilMigreringService.migrer(startMigreringRequest.antallPersoner, kategori = startMigreringRequest.kategori)
    }


    @PostMapping("/rekjor")
    fun migrerIdenter(@Valid @RequestBody identer: Set<String>): String {
        return hentSakTilMigreringService.rekjørMigreringer(identer)
    }

    @PostMapping("/rekjor/{feiltype}")
    fun migrer(@Valid @PathVariable feiltype: String): String {
        return hentSakTilMigreringService.rekjørMigreringerMedFeiltype(feiltype)
    }

    @PostMapping("/valider/")
    fun validerOmPersonErMigrert(@Valid @RequestBody body: PersondIdentRequest): Boolean {
       return verifiserMigeringService.sjekkOmPersonErMigrert(body.personIdent)
    }

    @PostMapping("/valider/{feiltype}")
    fun validerOmErMigrert(@Valid @PathVariable feiltype: String): Map<String, List<String>> {
        val (migrert, fortsattÅpne) =  verifiserMigeringService.sjekkOmFeilytpeErMigrert(feiltype)


        return mapOf(
                "migrert" to migrert,
                "fortsattÅpne" to fortsattÅpne
        )
    }

    @PostMapping("/migrert-av-saksbehandler")
    fun migrer(@Valid @RequestBody request: MigrertAvSaksbehandlerRequest): Ressurs<String> {
        verifiserMigeringService.verifiserMigrering(request.personIdent, request.migreringsResponse)
        return Ressurs.success("OK")
    }

    @PostMapping("/sak")
    @Transactional(readOnly = true)
    fun visÅpneSakerFor(@Valid @RequestBody identer: Set<String>): List<Pair<String, List<String>>>{
        return identer.map{Pair(it, verifiserMigeringService.listÅpneSaker(it))}
    }

    @PostMapping("/slett-gamle-tasker")
    @Transactional
    fun slettGamletasker(): String {
        taskMaintenanceService.slettTasksKlarForSletting()
        return "OK"
    }


    data class StartMigreringRequest(@Min(1) @Max(20) val antallPersoner: Int, val kategori: Kategori = Kategori.ORDINÆR)

    data class MigrertAvSaksbehandlerRequest(val personIdent: String, val migreringsResponse: MigreringResponseDto)
}
