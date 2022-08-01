package no.nav.familie.ba.migrering.tasks

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.migrering.domain.MigreringStatus
import no.nav.familie.ba.migrering.domain.Migrertsak
import no.nav.familie.ba.migrering.domain.MigrertsakRepository
import no.nav.familie.ba.migrering.integrasjoner.InfotrygdClient
import no.nav.familie.ba.migrering.integrasjoner.InfotrygdFeedClient
import no.nav.familie.ba.migrering.integrasjoner.MigreringResponseDto
import no.nav.familie.ba.migrering.integrasjoner.StønadRequest
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.PropertiesWrapper
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = VerifiserMigreringTask.TASK_STEP_TYPE,
    beskrivelse = "Verifisering av migrering fra Infotrygd",
    maxAntallFeil = 3,
    triggerTidVedFeilISekunder = 60
)
class VerifiserMigreringTask(
    val infotrygdClient: InfotrygdClient,
    val infotrygdFeedClient: InfotrygdFeedClient,
    val migrertsakRepository: MigrertsakRepository,
) : AsyncTaskStep {

    private val verifiserteMigreringerCounter: Counter = Metrics.counter("verifiserteMigreringer")

    override fun doTask(task: Task) {
        val migrertSak = migrertsakRepository.findById(UUID.fromString(task.payload)).get()
        logger.info("Verifiserer Migrertsak(id=${migrertSak.id}")
        secureLogger.info("Verifiserer $migrertSak")
        bekreftMigrertSak(migrertSak)
    }

    fun bekreftMigrertSak(migrertsak: Migrertsak) {
        val resultatFraBa =
            objectMapper.readValue(migrertsak.resultatFraBa?.jsonStr, MigreringResponseDto::class.java)
        checkNotNull(
            resultatFraBa.infotrygdTkNr,
            resultatFraBa.infotrygdIverksattFom,
            resultatFraBa.infotrygdVirkningFom,
            resultatFraBa.infotrygdRegion
        ) {
            secureLogger.error("Migrert sak mangler infotrygdstønad id-nøkler:\n$migrertsak")
            "Verifisering feilet: tkNr, iverksattFom, virkningFom og/eller region fra responseDto var null"
        }
        val infotrygdStønad = infotrygdClient.hentStønad(
            StønadRequest(
                migrertsak.personIdent,
                resultatFraBa.infotrygdTkNr!!,
                resultatFraBa.infotrygdIverksattFom!!,
                resultatFraBa.infotrygdVirkningFom!!,
                resultatFraBa.infotrygdRegion!!
            )
        )
        when (infotrygdStønad.opphørsgrunn) {
            "5" -> {
                val virkningFomIBa = resultatFraBa.virkningFom?.format(DateTimeFormatter.ofPattern("MMyyyy"))
                if (infotrygdStønad.opphørtFom == virkningFomIBa) {
                    migrertsakRepository.update(migrertsak.copy(status = MigreringStatus.VERIFISERT))
                    verifiserteMigreringerCounter.increment()
                } else {
                    secureLogger.error("OpphørtFom i Infotrygd var ulik virkningFom i BA:\n$infotrygdStønad\n$migrertsak")
                    error("OpphørtFom i Infotrygd var ulik virkningFom i BA (${infotrygdStønad.opphørtFom} =/= $virkningFomIBa)")
                }
            }
            else -> {
                secureLogger.error("Migrert sak har ikke blitt oppdatert med opphørsgrunn 5 i Infotrygd:\n$infotrygdStønad")
                if (erVedtaksmeldingSendt(migrertsak, resultatFraBa.virkningFom)) {
                    secureLogger.warn("Opphørsgrunn ${infotrygdStønad.opphørsgrunn} for ${migrertsak.personIdent}")
                    error("Opphørsgrunn i Infotrygd var ${infotrygdStønad.opphørsgrunn} og ikke 5, selv om vedtaksmelding er sendt")
                } else {
                    error("Det har IKKE blitt sendt vedtaksmelding med denne personidenten til infotrygd-feed etter migrering")
                }
            }
        }
    }

    private fun erVedtaksmeldingSendt(
        migrertsak: Migrertsak,
        virkningFom: YearMonth?
    ) = infotrygdFeedClient.hentOversiktOverVedtaksmeldingerSendtTilFeed(migrertsak.personIdent).any {
        it.opprettetDato.isAfter(migrertsak.migreringsdato) && YearMonth.from(it.datoStartNyBa!!) == virkningFom
    }

    private fun checkNotNull(vararg values: Any?, lazyMessage: () -> Any) {
        values.forEach { checkNotNull(it, lazyMessage) }
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
