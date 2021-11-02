package no.nav.familie.ba.migrering.tasks

import no.nav.familie.ba.migrering.domain.MigreringStatus
import no.nav.familie.ba.migrering.domain.Migrertsak
import no.nav.familie.ba.migrering.domain.MigrertsakJDBCRepository
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
    val migrertsakJDBCRepository: MigrertsakJDBCRepository,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val personIdent = objectMapper.readValue(task.payload, MigreringTaskDto::class.java).personIdent
        secureLogger.info("Migrerer sak for person $personIdent")
        val migrertsak = Migrertsak(
            id = UUID.randomUUID(),
            personIdent = personIdent,
            migreringsdato = LocalDateTime.now(),
            status = MigreringStatus.UKJENT,
            sakNummer = "",
        )
        migrertsakJDBCRepository.lagre(migrertsak)

        var resultatFraBa = ""
        var status = MigreringStatus.MIGRERT_I_BA
        var aarsak: String = ""
        try {
            resultatFraBa = objectMapper.writeValueAsString(sakClient.migrerPerson(personIdent))
        } catch (e: Exception) {
            status = MigreringStatus.FEILET
            aarsak = e.message!!
        }

        migrertsakJDBCRepository.oppdaterStatusÅrsakOgResultat(uuid = migrertsak.id,
                                                               status = status.name,
                                                               aarsak = aarsak,
                                                               resultat = resultatFraBa )

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
