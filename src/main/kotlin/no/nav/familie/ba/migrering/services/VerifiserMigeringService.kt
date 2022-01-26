package no.nav.familie.ba.migrering.services

import no.nav.familie.ba.migrering.domain.JsonWrapper
import no.nav.familie.ba.migrering.domain.MigreringStatus
import no.nav.familie.ba.migrering.domain.Migrertsak
import no.nav.familie.ba.migrering.domain.MigrertsakRepository
import no.nav.familie.ba.migrering.integrasjoner.MigreringResponseDto
import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID


@Service
class VerifiserMigeringService(
    val opprettMigreringstaskService: OpprettTaskService,
    val migrertsakRepository: MigrertsakRepository
) {

    fun verifiserMigrering(personIdent: String, migreringsResponse: MigreringResponseDto) {
        val migrertsak = migrertsakRepository.insert(
            Migrertsak(
                id = UUID.randomUUID(),
                personIdent = personIdent,
                migreringsdato = LocalDateTime.now(),
                status = MigreringStatus.MIGRERT_I_BA,
                callId = MDC.get(MDCConstants.MDC_CALL_ID) ?: IdUtils.generateId(),
                resultatFraBa = JsonWrapper.of(migreringsResponse),
            )
        )

        opprettMigreringstaskService.opprettVerifiserMigreringTask(migrertsak, migreringsResponse)
    }
}
