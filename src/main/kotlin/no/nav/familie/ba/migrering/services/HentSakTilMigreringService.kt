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

    @Scheduled(cron = "0 0 13 * * MON-FRI", zone = "Europe/Oslo")
    fun hentSakTilMigreringScheduler() {
        migrer()
    }

    fun migrer(antallPersoner: Int = this.antallPersoner) : String {
        if (!migreringAktivert) {
            Log.info("Migrering deaktivert, stopper videre jobbing")
            return "Migrering deaktivert, stopper videre jobbing"
        }

        var antallPersonerMigrert = 0
        var startSide = 4
        while (antallPersonerMigrert < antallPersoner) {
            val personerForMigrering = infotrygdClient.hentPersonerKlareForMigrering(
                MigreringRequest(
                    page = startSide,
                    size = ANTALL_PERSONER_SOM_HENTES_FRA_INFOTRYGD,
                    valg = "OR",
                    undervalg = "OS",
                    maksAntallBarn = 1,
                )
            )
            Log.info("Fant ${personerForMigrering.size} personer for migrering på side $startSide")
            if (personerForMigrering.isEmpty()) break

            antallPersonerMigrert = oppretteEllerSkipMigrering(personerForMigrering, antallPersonerMigrert, antallPersoner)
            if (antallPersonerMigrert < antallPersoner) {
                startSide++
            }
        }



        return "Migrerte $antallPersoner"
    }

    private fun oppretteEllerSkipMigrering(personerForMigrering: Set<String>, antallAlleredeMigret: Int, antallPersonerSomSkalMigreres: Int): Int {
        var antallPersonerMigrert = antallAlleredeMigret
        for (person in personerForMigrering) {
            if (!existByPersonIdentAndStatusIn(
                    person,
                    listOf(
                        MigreringStatus.MIGRERT_I_BA,
                        MigreringStatus.FEILET,
                        MigreringStatus.VERIFISERT,
                        MigreringStatus.UKJENT
                    )
                )
            ) {
                taskRepository.save(MigreringTask.opprettTask(MigreringTaskDto(person)))
                secureLogger.info("Oppretter migrering for person $person")
                antallPersonerMigrert++
            } else secureLogger.info("Personen $person er allerede forsøkt migrert")

            if (antallPersonerMigrert == antallPersonerSomSkalMigreres)
                return antallPersonerMigrert
        }
        return antallPersonerMigrert
    }

    private fun existByPersonIdentAndStatusIn(ident: String, status: List<MigreringStatus>): Boolean {
        return status.any { migrertsakRepository.findByStatusAndPersonIdent(it, ident).isNotEmpty() }
    }

    companion object {
        val Log = LoggerFactory.getLogger(HentSakTilMigreringService::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
        val ANTALL_PERSONER_SOM_HENTES_FRA_INFOTRYGD = 300
    }
}
