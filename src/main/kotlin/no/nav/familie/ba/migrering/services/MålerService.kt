package no.nav.familie.ba.migrering.services

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tags
import no.nav.familie.ba.migrering.domain.MigreringStatus
import no.nav.familie.ba.migrering.domain.MigrertsakRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class MålerService(private val migrerertsakRepository: MigrertsakRepository) {

    val antallLøpendeSakerGauge = MultiGauge.builder("migrert").register(Metrics.globalRegistry)
    private val logger = LoggerFactory.getLogger(MålerService::class.java)


    @Scheduled(fixedDelay = 1 * 60 * 60 * 1000, initialDelay = 3 * 50 * 1000)
    fun antallLøpendeSaker() {
        logger.info("Oppdaterer metrikker")
        val rows = mutableListOf<MultiGauge.Row<Number>>()

        val totalMigrerte =
            migrerertsakRepository.countByStatusIn(listOf(MigreringStatus.MIGRERT_I_BA, MigreringStatus.VERIFISERT))
        val antallSomIkkeErVerifisertInfotrygd = migrerertsakRepository.countByStatusIn(listOf(MigreringStatus.MIGRERT_I_BA))
        val antallFeil = migrerertsakRepository.countByStatusIn(listOf(MigreringStatus.FEILET))

        rows.addAll(
            listOf(
                MultiGauge.Row.of(
                    Tags.of("migrert", "total"),
                    totalMigrerte
                ),
                MultiGauge.Row.of(
                    Tags.of("migrert", "ikkeverifisert"),
                    antallSomIkkeErVerifisertInfotrygd
                ),
                MultiGauge.Row.of(
                    Tags.of("migrert", "feil"),
                    antallFeil
                )
            )

        )
        antallLøpendeSakerGauge.register(rows, true)
    }
}