package no.nav.familie.ba.migrering.rest

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import no.nav.familie.ba.migrering.integrasjoner.MigreringResponseDto
import no.nav.familie.ba.migrering.services.HentSakTilMigreringService
import no.nav.familie.ba.migrering.services.Kategori
import no.nav.familie.ba.migrering.services.VerifiserMigeringService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/migrer")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class MigreringController(
    private val hentSakTilMigreringService: HentSakTilMigreringService,
    private val verifiserMigeringService: VerifiserMigeringService,
) {

    @PostMapping("/")
    fun migrerFraInfotrygd(
        @Valid @RequestBody
        startMigreringRequest: StartMigreringRequest,
    ): String {
        return hentSakTilMigreringService.migrer(startMigreringRequest.antallPersoner, kategori = startMigreringRequest.kategori)
    }

    @PostMapping("/rekjor")
    fun migrerIdenter(
        @Valid @RequestBody
        identer: Set<String>,
    ): String {
        return hentSakTilMigreringService.rekjørMigreringer(identer)
    }

    @PostMapping("/rekjor/{feiltype}")
    fun migrer(
        @Valid @PathVariable
        feiltype: String,
    ): String {
        return hentSakTilMigreringService.rekjørMigreringerMedFeiltype(feiltype)
    }

    @PostMapping("/alle-sakstyper")
    fun migrerFraInfotrygdAlleSakstyper(): String {
        hentSakTilMigreringService.migrer(antallPersoner = 2000, kategori = Kategori.ORDINÆR)
        hentSakTilMigreringService.migrer(antallPersoner = 2000, kategori = Kategori.ORDINÆR_DELT_BOSTED)
        hentSakTilMigreringService.migrer(antallPersoner = 2000, kategori = Kategori.ORDINÆR_EØS_PRIMÆRLAND)
        hentSakTilMigreringService.migrer(antallPersoner = 2000, kategori = Kategori.UTVIDET)
        hentSakTilMigreringService.migrer(antallPersoner = 2000, kategori = Kategori.UTVIDET_DELT_BOSTED)
        hentSakTilMigreringService.migrer(antallPersoner = 2000, kategori = Kategori.UTVIDET_EØS_PRIMÆRLAND)
        return "OK"
    }

    @PostMapping("/valider/")
    fun validerOmPersonErMigrert(
        @Valid @RequestBody
        body: PersondIdentRequest,
    ): Boolean {
        return verifiserMigeringService.sjekkOmPersonErMigrert(body.personIdent)
    }

    @PostMapping("/valider/{feiltype}")
    fun validerOmErMigrert(
        @Valid @PathVariable
        feiltype: String,
    ): Map<String, List<String>> {
        val (migrert, fortsattÅpne) = verifiserMigeringService.sjekkOmFeilytpeErMigrert(feiltype)

        return mapOf(
            "migrert" to migrert,
            "fortsattÅpne" to fortsattÅpne,
        )
    }

    @PostMapping("/migrert-av-saksbehandler")
    fun migrer(
        @Valid @RequestBody
        request: MigrertAvSaksbehandlerRequest,
    ): Ressurs<String> {
        verifiserMigeringService.verifiserMigrering(request.personIdent, request.migreringsResponse)
        return Ressurs.success("OK")
    }

    @PostMapping("/sak")
    @Transactional(readOnly = true)
    fun visÅpneSakerFor(
        @Valid @RequestBody
        identer: Set<String>,
    ): List<Pair<String, List<String>>> {
        return identer.map { Pair(it, verifiserMigeringService.listÅpneSaker(it)) }
    }

    data class StartMigreringRequest(
        @Min(1)
        @Max(20)
        val antallPersoner: Int,
        val kategori: Kategori = Kategori.ORDINÆR,
    )

    data class MigrertAvSaksbehandlerRequest(val personIdent: String, val migreringsResponse: MigreringResponseDto)
}
