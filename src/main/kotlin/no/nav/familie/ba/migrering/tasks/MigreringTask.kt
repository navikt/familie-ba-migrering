package no.nav.familie.ba.migrering.tasks

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.migrering.domain.JsonWrapper
import no.nav.familie.ba.migrering.domain.MigreringStatus
import no.nav.familie.ba.migrering.domain.Migrertsak
import no.nav.familie.ba.migrering.domain.MigrertsakLogg
import no.nav.familie.ba.migrering.domain.MigrertsakLoggRepository
import no.nav.familie.ba.migrering.domain.MigrertsakRepository
import no.nav.familie.ba.migrering.integrasjoner.InfotrygdClient
import no.nav.familie.ba.migrering.integrasjoner.KanIkkeMigrereException
import no.nav.familie.ba.migrering.integrasjoner.SakClient
import no.nav.familie.ba.migrering.rest.MigreringsfeilType
import no.nav.familie.ba.migrering.services.OpprettTaskService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
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
    val infotrygdClient: InfotrygdClient,
    val migrertsakRepository: MigrertsakRepository,
    val migrertsakLoggRepository: MigrertsakLoggRepository,
    val taskRepository: TaskRepository,
    val opprettTaskService: OpprettTaskService
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val payload = objectMapper.readValue(task.payload, MigreringTaskDto::class.java)

        secureLogger.info("Migrerer sak for person ${payload.personIdent}")

        var migrertsak = migrertsakRepository.findByStatusInAndPersonIdentOrderByMigreringsdato(listOf(MigreringStatus.UKJENT, MigreringStatus.FEILET), payload.personIdent).lastOrNull()

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
        } else {
            migrertsakLoggRepository.insert(MigrertsakLogg.tilMigrertsakLogg(migrertsak))
        }

        try {
            if (infotrygdClient.harÅpenSak(payload.personIdent))
                kastOgTellMigreringsFeil(MigreringsfeilType.ÅPEN_SAK_TIL_BESLUTNING_I_INFOTRYGD)
            else if (infotrygdClient.hentSaker(payload.personIdent).any { it.status != "FB" })
                kastOgTellMigreringsFeil(MigreringsfeilType.ÅPEN_SAK_INFOTRYGD)
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
            opprettTaskService.opprettVerifiserMigreringTask(migrertsak, responseBa)
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
                "Migrering av sak for person ${payload.personIdent} feilet med feiltype=$feiltype.",
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

val migreringsFeilCounter = mutableMapOf<String, Counter>()
fun kastOgTellMigreringsFeil(
    feiltype: MigreringsfeilType
): Nothing =
    throw KanIkkeMigrereException(feiltype.name, feiltype.beskrivelse, null).also {
        if (migreringsFeilCounter[feiltype.name] == null) {
            migreringsFeilCounter[feiltype.name] = Metrics.counter("migrering.feil", "type", feiltype.name)
        }

        migreringsFeilCounter[feiltype.name]?.increment()
    }
