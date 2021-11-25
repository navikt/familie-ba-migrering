package no.nav.familie.ba.migrering.services

import no.nav.familie.ba.migrering.domain.MigreringStatus
import no.nav.familie.ba.migrering.domain.MigrertsakRepository
import no.nav.familie.ba.migrering.integrasjoner.InfotrygdClient
import no.nav.familie.ba.migrering.integrasjoner.MigreringRequest
import no.nav.familie.ba.migrering.tasks.MigreringTask
import no.nav.familie.ba.migrering.tasks.MigreringTaskDto
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class HentSakTilMigreringService(
    val infotrygdClient: InfotrygdClient,
    val taskRepository: TaskRepository,
    val migrertsakRepository: MigrertsakRepository,
    @Value("\${migrering.aktivert:false}") val migreringAktivert: Boolean,
    @Value("\${migrering.antallPersoner}") val antallPersoner: Int,
) {

    @Scheduled(cron = "0 0 9 * * ?", zone = "Europe/Oslo")
    fun hentSakTilMigrering() {
        if (!migreringAktivert) {
            Log.info("Migrering deaktivert, stopper videre jobbing")
            return
        }
        val personerForMigrering = infotrygdClient.hentPersonerKlareForMigrering(
            MigreringRequest(
                page = 4,
                size = ANTALL_PERSONER_SOM_HENTES_FRA_INFOTRYGD,
                valg = "OR",
                undervalg = "OS",
                maksAntallBarn = 1,
            )
        )

        if (personerForMigrering.size > ANTALL_PERSONER_SOM_HENTES_FRA_INFOTRYGD) {
            Log.error("For manger personer (${personerForMigrering.size}) avbryter migrering")
            return
        }

        Log.info("Fant ${personerForMigrering.size} personer for migrering")

        var antallPersonerMigrert = 0
        for (person in personerForMigrering) {
            if (!existByPersonIdentAndStatusIn(person, listOf(MigreringStatus.MIGRERT_I_BA, MigreringStatus.FEILET, MigreringStatus.VERIFISERT))) {
                taskRepository.save(MigreringTask.opprettTask(MigreringTaskDto(person)))
                antallPersonerMigrert++
            }
            if (antallPersonerMigrert == antallPersoner)
                break
        }
    }

    private fun existByPersonIdentAndStatusIn(ident: String, status: List<MigreringStatus>): Boolean {
        return status.any { migrertsakRepository.findByStatusAndPersonIdent(it, ident).isNotEmpty() }
    }

    companion object {
        val Log = LoggerFactory.getLogger(HentSakTilMigreringService::class.java)
        val ANTALL_PERSONER_SOM_HENTES_FRA_INFOTRYGD = 200
    }
}
