package no.nav.familie.ba.migrering.services

import no.nav.familie.ba.migrering.domain.Migrertsak
import no.nav.familie.ba.migrering.integrasjoner.MigreringResponseDto
import no.nav.familie.ba.migrering.tasks.MigreringTask
import no.nav.familie.ba.migrering.tasks.MigreringTaskDto
import no.nav.familie.ba.migrering.tasks.VerifiserMigreringTask
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.Properties
import java.util.UUID

@Service
class OpprettTaskService(
    val taskRepository: TaskRepository
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun opprettMigreringtask(personident: String) {
        try {
            MDC.put(MDCConstants.MDC_CALL_ID, UUID.randomUUID().toString())
            taskRepository.save(MigreringTask.opprettTask(MigreringTaskDto(personident)))
            secureLogger.info("Oppretter MigreringTask for $personident")
        } finally {
            MDC.clear()
        }
    }


    fun opprettVerifiserMigreringTask(migrertsak: Migrertsak, migrerinstResponseDto: MigreringResponseDto) {
        val properties = Properties().apply {
            put("personIdent", migrertsak.personIdent)
            put("fagsakId", migrerinstResponseDto.fagsakId.toString())
            put("behandlingId", migrerinstResponseDto.behandlingId.toString())
            put("callId", migrertsak.callId)
        }

        taskRepository.save(
            VerifiserMigreringTask.opprettTaskMedTriggerTid(
                migrertsak.id.toString(),
                LocalDate.now().plusDays(1).atTime(7, 30),
                properties
            )
        )
    }


    companion object {

        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
