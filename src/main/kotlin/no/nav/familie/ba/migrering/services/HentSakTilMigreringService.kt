package no.nav.familie.ba.migrering.services

import no.nav.familie.ba.migrering.domain.MigreringStatus
import no.nav.familie.ba.migrering.domain.MigrertsakRepository
import no.nav.familie.ba.migrering.integrasjoner.InfotrygdClient
import no.nav.familie.ba.migrering.integrasjoner.MigreringRequest
import no.nav.familie.ba.migrering.skalKjøreMigering
import no.nav.familie.ba.migrering.tasks.MigreringTask
import no.nav.familie.ba.migrering.tasks.MigreringTaskDto
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class HentSakTilMigreringService(
    val infotrygdClient: InfotrygdClient,
    val taskRepository: TaskRepository,
    val migrertsakRepository: MigrertsakRepository,
    @Value("\${migrering.aktivert:false}") val migreringAktivert: Boolean
) {

    fun migrer(antallPersoner: Int, migreringsDato: LocalDate = LocalDate.now()): String { //migreringsDato skal kun brukes fra tester
        if (!skalKjøreMigering(migreringAktivert, migreringsDato)) {
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
            secureLogger.info("Resultat fra infotrygd: $personerForMigrering")
            if (personerForMigrering.isEmpty()) break

            antallPersonerMigrert = oppretteEllerSkipMigrering(personerForMigrering, antallPersonerMigrert, antallPersoner)
            if (antallPersonerMigrert < antallPersoner) {
                startSide = startSide.inc()
            }
        }

        return "Migrerte $antallPersoner"
    }


    fun rekjørMigreringer(identer: Set<String>): String {
        if (!migreringAktivert) {
            Log.info("Migrering deaktivert, stopper videre jobbing")
            return "Migrering deaktivert, stopper videre jobbing"
        }
        var antallRekjøringer = 0
        identer.forEach { peronident ->
            //sjekk infotrygd
            if (migrertsakRepository.findByStatusAndPersonIdent(MigreringStatus.FEILET, peronident).isNotEmpty()) {
                taskRepository.save(MigreringTask.opprettTask(MigreringTaskDto(peronident)))
                secureLogger.info("Oppretter MigreringTask for person $peronident")
                antallRekjøringer++
            }
        }
        return "Rekjørt $antallRekjøringer migreringer"
    }

    fun rekjørMigreringerMedFeiltype(feiltype: String): String {
        if (!migreringAktivert) {
            Log.info("Migrering deaktivert, stopper videre jobbing")
            return "Migrering deaktivert, stopper videre jobbing"
        }

        val migrertsakMedFeiltype =
            migrertsakRepository.findByStatusAndFeiltype(MigreringStatus.FEILET, feiltype).map { it.personIdent }.toSet()
        migrertsakMedFeiltype.forEach {
            taskRepository.save(MigreringTask.opprettTask(MigreringTaskDto(it)))
            secureLogger.info("Oppretter MigreringTask for person $it")
        }
        return "Rekjørt ${migrertsakMedFeiltype.size} med feiltype=$feiltype"
    }

    private fun oppretteEllerSkipMigrering(
        personerForMigrering: Set<String>,
        antallAlleredeMigret: Int,
        antallPersonerSomSkalMigreres: Int
    ): Int {
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
                secureLogger.info("Oppretter MigreringTask for person $person")
                antallPersonerMigrert++
            } else secureLogger.info("Skipper oppretting av MigreringTask for $person har treff i MigrertSak")

            if (antallPersonerMigrert == antallPersonerSomSkalMigreres)
                return antallPersonerMigrert
        }
        return antallPersonerMigrert
    }

    private fun existByPersonIdentAndStatusIn(ident: String, status: List<MigreringStatus>): Boolean {
        return status.any { migrertsakRepository.findByStatusAndPersonIdent(it, ident).isNotEmpty() } //TODO
    }


    companion object {

        val Log = LoggerFactory.getLogger(HentSakTilMigreringService::class.java)
        private val secureLogger = LoggerFactory.getLogger("secureLogger")
        const val ANTALL_PERSONER_SOM_HENTES_FRA_INFOTRYGD = 300
    }
}
