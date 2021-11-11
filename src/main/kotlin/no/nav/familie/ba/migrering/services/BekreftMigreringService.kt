package no.nav.familie.ba.migrering.services

import no.nav.familie.ba.migrering.domain.MigreringStatus
import no.nav.familie.ba.migrering.domain.Migrertsak
import no.nav.familie.ba.migrering.domain.MigrertsakRepository
import no.nav.familie.ba.migrering.integrasjoner.InfotrygdClient
import no.nav.familie.ba.migrering.integrasjoner.MigreringResponseDto
import no.nav.familie.kontrakter.felles.objectMapper
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class BekreftMigreringService(
    val infotrygdClient: InfotrygdClient,
    val migrertTaskRepository: MigrertsakRepository,
) {

    @Scheduled(cron = "0 0 12 * * ?", zone = "Europe/Oslo")
    fun bekreftMigrering() {
        val saker = migrertTaskRepository.findByStatus(MigreringStatus.MIGRERT_I_BA)
        LOG.info("Verifiser ${saker.size} migrerte saker")
        saker.forEach {
            bekreftMigrertSak(it)
        }
        val (sakerSuksess, sakerFeilet) = migrertTaskRepository.findAllById(saker.map { it.id })
            .partition { it.status == MigreringStatus.VERIFISERT }

        val resultat = "Verifisering av migrerte saker resultat: Antall vellykket: ${sakerSuksess.size}. Antall feilet: ${sakerFeilet.size}\n" +
                sakerFeilet.map { "MigrertSak(id=${it.id}, status=${it.status}, aarsak=${it.aarsak})\n" }

        when (sakerFeilet.size) {
            0 -> LOG.info(resultat)
            else -> LOG.error(resultat)
        }
    }

    fun bekreftMigrertSak(migrertsak: Migrertsak) {
        val migreringResponseDto =
            objectMapper.readValue(migrertsak.resultatFraBa?.jsonStr, MigreringResponseDto::class.java)
        if (migreringResponseDto.infotrygdStønadId == null) {
            migrertTaskRepository.update(migrertsak.copy(status = MigreringStatus.VERIFISERING_FEILET,
                                                         aarsak = "null infotrygdStønadId"))
            return
        }

        migrertTaskRepository.update(
            when (val opphørsgrunn = infotrygdClient.hentStønadFraId(migreringResponseDto.infotrygdStønadId).opphørsgrunn) {
                "5" -> migrertsak.copy(status = MigreringStatus.VERIFISERT)
                else -> migrertsak.copy(status = MigreringStatus.VERIFISERING_FEILET, aarsak = "opphørsgunn ($opphørsgrunn) is not 5 ")
            }
        )
    }

    companion object {
        val LOG = LoggerFactory.getLogger(BekreftMigreringService::class.java)
    }
}
