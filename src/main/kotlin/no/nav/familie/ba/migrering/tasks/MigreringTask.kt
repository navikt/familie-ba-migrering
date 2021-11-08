package no.nav.familie.ba.migrering.tasks

import no.nav.familie.ba.migrering.domain.JsonWrapper
import no.nav.familie.ba.migrering.domain.MigreringStatus
import no.nav.familie.ba.migrering.domain.Migrertsak
import no.nav.familie.ba.migrering.domain.MigrertsakRepository
import no.nav.familie.ba.migrering.integrasjoner.SakClient
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
@TaskStepBeskrivelse(
    taskStepType = MigreringTask.TASK_STEP_TYPE,
    beskrivelse = "Migrering sak fra Infotrygd",
    maxAntallFeil = 3
)
class MigreringTask(
    val sakClient: SakClient,
    val migrertsakRepository: MigrertsakRepository,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val payload = objectMapper.readValue(task.payload, MigreringTaskDto::class.java)

        secureLogger.info("Migrerer sak for person $payload.personIdent")
        val sakId = UUID.randomUUID()
        migrertsakRepository.insert(
            Migrertsak(
                id = sakId,
                personIdent = payload.personIdent,
                migreringsdato = LocalDateTime.now(),
                status = MigreringStatus.UKJENT,
            )
        )

        try {
            val responseBa = sakClient.migrerPerson(payload.personIdent)
            migrertsakRepository.update(
                Migrertsak(
                    id = sakId,
                    personIdent = payload.personIdent,
                    status = MigreringStatus.MIGRERT_I_BA,
                    resultatFraBa = JsonWrapper.of(responseBa),
                )
            )
        } catch (e: Exception) {
            migrertsakRepository.update(
                Migrertsak(
                    id = sakId,
                    personIdent = payload.personIdent,
                    status = MigreringStatus.FEILET,
                    resultatFraBa = null,
                    aarsak = e.message,
                )
            )
        }
    }

    companion object {

        const val TASK_STEP_TYPE = "MigreringTask"
        private val logger = LoggerFactory.getLogger(MigreringTask::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")

        fun opprettTask(migreringTaskDto: MigreringTaskDto): Task {
            return Task(
                type = TASK_STEP_TYPE,
                payload = objectMapper.writeValueAsString(
                    migreringTaskDto
                ),
            )
        }
    }
}

data class MigreringTaskDto(val personIdent: String)
