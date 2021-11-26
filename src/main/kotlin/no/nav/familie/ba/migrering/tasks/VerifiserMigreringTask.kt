package no.nav.familie.ba.migrering.tasks

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.migrering.domain.MigreringStatus
import no.nav.familie.ba.migrering.domain.Migrertsak
import no.nav.familie.ba.migrering.domain.MigrertsakRepository
import no.nav.familie.ba.migrering.integrasjoner.InfotrygdClient
import no.nav.familie.ba.migrering.integrasjoner.MigreringResponseDto
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.PropertiesWrapper
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = VerifiserMigreringTask.TASK_STEP_TYPE,
    beskrivelse = "Verifisering av migrering fra Infotrygd",
    maxAntallFeil = 3,
    triggerTidVedFeilISekunder = 60
)
class VerifiserMigreringTask(
    val infotrygdClient: InfotrygdClient,
    val migrertsakRepository: MigrertsakRepository,
) : AsyncTaskStep {

    private val verifiserteMigreringerCounter: Counter =Metrics.counter("verifiserteMigreringer")

    override fun doTask(task: Task) {
        val migrertSak = migrertsakRepository.findById(UUID.fromString(task.payload)).get()
        logger.info("Verifiserer Migrertsak(id=${migrertSak.id}")
        secureLogger.info("Verifiserer $migrertSak")
        bekreftMigrertSak(migrertSak)
    }

    fun bekreftMigrertSak(migrertsak: Migrertsak) {
        val resultatFraBa =
            objectMapper.readValue(migrertsak.resultatFraBa?.jsonStr, MigreringResponseDto::class.java)
        if (resultatFraBa.infotrygdStønadId == null) {
            secureLogger.error("Migrert sak mangler infotrygdStønadId:\n${migrertsak}")
            error("Verifisering feilet: infotrygdStønadId = null")
        }
        val infotrygdStønad = infotrygdClient.hentStønadFraId(resultatFraBa.infotrygdStønadId)
        when (infotrygdStønad.opphørsgrunn) {
            "5" -> {
                val virkningFomIBa = resultatFraBa.virkningFom?.format(DateTimeFormatter.ofPattern("MMyyyy"))
                if (infotrygdStønad.opphørtFom == virkningFomIBa) {
                    migrertsakRepository.update(migrertsak.copy(status = MigreringStatus.VERIFISERT))
                    verifiserteMigreringerCounter.increment()
                }
                else {
                    secureLogger.error("OpphørtFom i Infotrygd var ulik virkningFom i BA:\n$infotrygdStønad\n$migrertsak")
                    error("OpphørtFom i Infotrygd var ulik virkningFom i BA (${infotrygdStønad.opphørtFom} =/= $virkningFomIBa)")
                }
            }
            else -> {
                secureLogger.error("Migrert sak har ikke blitt oppdatert med opphørsgrunn 5 i Infotrygd:\n$infotrygdStønad")
                error("Opphørsgrunn i Infotrygd var ${infotrygdStønad.opphørsgrunn}, og ikke 5")
            }
        }
    }

    companion object {

        const val TASK_STEP_TYPE = "VerifiserMigreringTask"
        private val logger = LoggerFactory.getLogger(VerifiserMigreringTask::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")

        fun opprettTaskMedTriggerTid(
            migrertsakId: String,
            triggerTid: LocalDateTime = LocalDateTime.now(),
            properties: Properties
        ): Task {
            return Task(
                type = TASK_STEP_TYPE,
                payload = migrertsakId,
                triggerTid = triggerTid,
                metadataWrapper = PropertiesWrapper(properties),
            )
        }
    }
}

