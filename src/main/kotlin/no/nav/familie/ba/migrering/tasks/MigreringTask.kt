package no.nav.familie.ba.migrering.tasks

import no.nav.familie.ba.migrering.domain.JsonWrapper
import no.nav.familie.ba.migrering.domain.MigreringStatus
import no.nav.familie.ba.migrering.domain.Migrertsak
import no.nav.familie.ba.migrering.domain.MigrertsakRepository
import no.nav.familie.ba.migrering.integrasjoner.KanIkkeMigrereException
import no.nav.familie.ba.migrering.integrasjoner.SakClient
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = MigreringTask.TASK_STEP_TYPE,
    beskrivelse = "Migrering sak fra Infotrygd",
    maxAntallFeil = 3
)
class MigreringTask(
    val sakClient: SakClient,
    val migrertsakRepository: MigrertsakRepository,
    val taskRepository: TaskRepository
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val payload = objectMapper.readValue(task.payload, MigreringTaskDto::class.java)

        secureLogger.info("Migrerer sak for person ${payload.personIdent}")

        var migrertsak =
            migrertsakRepository.findByStatusAndPersonIdent(MigreringStatus.UKJENT, payload.personIdent).singleOrNull()

        if (migrertsak == null) {
            val sakId = UUID.randomUUID()
            migrertsak = migrertsakRepository.insert(
                Migrertsak(
                    id = sakId,
                    personIdent = payload.personIdent,
                    migreringsdato = LocalDateTime.now(),
                    status = MigreringStatus.UKJENT,
                    callId = task.callId
                )
            )
        }

        try {
            val responseBa = sakClient.migrerPerson(payload.personIdent)
            migrertsakRepository.update(
                Migrertsak(
                    id = migrertsak.id,
                    personIdent = payload.personIdent,
                    status = MigreringStatus.MIGRERT_I_BA,
                    resultatFraBa = JsonWrapper.of(responseBa),
                    callId = task.callId
                )
            )

            val properties = Properties().apply {
                put("personIdent", payload.personIdent)
                put("fagsakId", responseBa.fagsakId.toString())
                put("behandlingId", responseBa.behandlingId.toString())
                put("callId", task.callId)
            }
            taskRepository.save(
                VerifiserMigreringTask.opprettTaskMedTriggerTid(
                    migrertsak.id.toString(),
                    LocalDate.now().plusDays(1).atTime(11, 0),
                    properties
                )
            )
        } catch (e: Exception) {
            var feiltype: String = "UKJENT"
            if (e is KanIkkeMigrereException) {
                feiltype = e.feiltype
            }
            migrertsakRepository.update(
                Migrertsak(
                    id = migrertsak.id,
                    personIdent = payload.personIdent,
                    status = MigreringStatus.FEILET,
                    resultatFraBa = null,
                    feiltype = feiltype,
                    aarsak = e.message,
                    callId = task.callId
                )
            )
            task.metadata.put("feiltype", feiltype)

            secureLogger.info(
                "Migrering av sak for person ${payload.personIdent} feilet med feiltype=$feiltype. Starter migrering av annen person",
                e
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
                properties = Properties().apply {
                    put("personIdent", migreringTaskDto.personIdent)
                }
            )
        }
    }
}

data class MigreringTaskDto(val personIdent: String)
