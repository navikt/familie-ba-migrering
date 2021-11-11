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
        LOG.info("Verifiser ${saker.size} migrertsak")
        var sakerSuksess: Int = 0
        saker.forEach {
            sakerSuksess += if (bekreftMigrertSak(it)) 1 else 0
        }
        LOG.info("Verifisering er klar. Verifiserte $sakerSuksess saker")
    }

    fun bekreftMigrertSak(migrertsak: Migrertsak): Boolean {
        val migreringResponseDto =
            objectMapper.readValue(migrertsak.resultatFraBa?.jsonStr, MigreringResponseDto::class.java)
        if (migreringResponseDto.infotrygdStønadId == null) {
            LOG.error("Verifisering feilet med Migrertsak(id = ${migrertsak.id}): null infotrygdStønadId")
            migrertTaskRepository.update(migrertsak.copy(status = MigreringStatus.VERIFISERING_FEILET))
            return false
        }

        return migrertTaskRepository.update(
            migrertsak.copy(
                status = when (infotrygdClient.hentStønadFraId(migreringResponseDto.infotrygdStønadId).opphørsgrunn) {
                    "5" -> MigreringStatus.VERIFISERT
                    else -> {
                        LOG.error("Verifisering feilet med Migrertsak(id= ${migrertsak.id}): opphørsgunn is not 5 ")
                        MigreringStatus.VERIFISERING_FEILET
                    }
                }
            )
        ).status == MigreringStatus.VERIFISERT
    }

    companion object {
        val LOG = LoggerFactory.getLogger(BekreftMigreringService::class.java)
    }
}
