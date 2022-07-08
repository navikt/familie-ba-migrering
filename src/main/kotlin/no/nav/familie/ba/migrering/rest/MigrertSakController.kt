package no.nav.familie.ba.migrering.rest

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.familie.ba.migrering.domain.MigreringStatus
import no.nav.familie.ba.migrering.domain.Migrertsak
import no.nav.familie.ba.migrering.domain.MigrertsakRepository
import no.nav.familie.ba.migrering.domain.TellFeilResponse
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.validation.Valid

@RestController
@RequestMapping("/api/data/migrertsak")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class MigrertSakController(
    private val migrertsakRepository: MigrertsakRepository
) {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @GetMapping("/")
    @Transactional(readOnly = true)
    fun hentAlleSaker(@RequestParam(required = false) status: List<MigreringStatus>?, @Schema(defaultValue = "0") @RequestParam(required = true) page: Int): MigrertSakResponse {
        val pageable = Pageable.ofSize(500).withPage(page)
        return if (status.isNullOrEmpty()) {
            MigrertSakResponse(migrertsakRepository.findAll(pageable).toList())
        } else {
            MigrertSakResponse(migrertsakRepository.findByStatusIn(status, pageable).toList())
        }
    }

    @GetMapping("/feiltype")
    @Transactional(readOnly = true)
    fun hentFeiledeSaker(@RequestParam(required = true) feiltype: MigreringsfeilType): MigrertSakResponse {
        return MigrertSakResponse(migrertsakRepository.findByStatusAndFeiltype(MigreringStatus.FEILET, feiltype.name).toList())
    }

    @PostMapping("/")
    @Transactional(readOnly = true)
    fun hentAlleSakerForPerson(@Valid @RequestBody body: PersondIdentRequest): MigrertSakResponse {
        return MigrertSakResponse(migrertsakRepository.findByStatusInAndPersonIdentOrderByMigreringsdato(MigreringStatus.values().toList(), body.personIdent))
    }

    @GetMapping("/tell-feilet")
    @Transactional(readOnly = true)
    fun tellAntallFeiledeMigrering(): List<TellFeilResponse> {
        return migrertsakRepository.tellFeiledeMigrerteSaker()
    }

    @GetMapping("/list-alle-feilet")
    @Transactional(readOnly = true)
    fun listFeiledMigreringer(): Map<String, Set<String>> {
        return migrertsakRepository.findByStatusIn(listOf(MigreringStatus.FEILET), Pageable.unpaged())
            .filter { it.feiltype != null }
            .groupBy { it.feiltype!! }
            .mapValues { it.value.map { it.personIdent }.toSet() }
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    fun hentMigrertSakerById(@PathVariable("id") id: UUID): Migrertsak? {
        return migrertsakRepository.findById(id).orElse(null)
    }

    @PutMapping("/{id}")
    @Transactional
    fun oppdaterMigrertSak(@PathVariable("id") id: UUID, @RequestBody @Valid migrertsak: Migrertsak): Migrertsak {
        return migrertsakRepository.save(migrertsak)
    }

    @DeleteMapping("/{id}")
    @Transactional
    fun slettMigrertSak(@PathVariable("id") id: UUID) {
        migrertsakRepository.deleteById(id)
    }

    @DeleteMapping("/feiltype/{feiltype}")
    @Transactional
    fun slettMigrertSakMedFeiltype(@PathVariable("feiltype") feiltype: MigreringsfeilType, @Schema(defaultValue = "true") @RequestParam(required = true) dryRun: Boolean) {
        migrertsakRepository.findByStatusAndFeiltype(MigreringStatus.FEILET, feiltype.name).forEach {
            if (dryRun) {
                secureLogger.info("dryRun er satt til false, så ignorerer sletting av $it")
            } else {
                secureLogger.info("Sletter fra migrertSak: $it")
                migrertsakRepository.deleteById(it.id)
            }
        }
    }
}

data class PersondIdentRequest(val personIdent: String)

class MigrertSakResponse(migrerteSaker: List<Migrertsak>) {

    val total: Int = migrerteSaker.size
    val data: List<Migrertsak> = migrerteSaker
}
