package no.nav.familie.ba.migrering.rest

import no.nav.familie.ba.migrering.domain.MigreringStatus
import no.nav.familie.ba.migrering.domain.Migrertsak
import no.nav.familie.ba.migrering.domain.MigrertsakRepository
import no.nav.familie.ba.migrering.domain.TellFeilResponse
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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

    @GetMapping("/")
    @Transactional(readOnly = true)
    fun hentAlleSaker(@RequestParam(required = false) status: List<MigreringStatus>): MigrertSakResponse {
        return if (status.isEmpty()) {
            MigrertSakResponse(migrertsakRepository.findAll().toList())
        } else {
            MigrertSakResponse(migrertsakRepository.findByStatusIn(status))
        }
    }

    @GetMapping("/tell-feilet")
    @Transactional(readOnly = true)
    fun tellAntallFeiledeMigrering(): List<TellFeilResponse> {
        return migrertsakRepository.tellFeiledeMigrerteSaker()

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


}

class MigrertSakResponse(migrerteSaker: List<Migrertsak>) {

    val total: Int = migrerteSaker.size
    val data: List<Migrertsak> = migrerteSaker
}
