package no.nav.familie.ba.migrering.services

import no.nav.familie.ba.migrering.domain.MigreringStatus
import no.nav.familie.ba.migrering.domain.MigrertsakRepository
import no.nav.familie.ba.migrering.integrasjoner.InfotrygdClient
import no.nav.familie.ba.migrering.integrasjoner.MigreringRequest
import no.nav.familie.ba.migrering.skalKjøreMigering
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class HentSakTilMigreringService(
    val infotrygdClient: InfotrygdClient,
    val opprettMigreringstaskService: OpprettMigreringstaskService,
    val migrertsakRepository: MigrertsakRepository,
    @Value("\${migrering.aktivert:false}") val migreringAktivert: Boolean
) {

    fun migrer(
        antallPersoner: Int,
        migreringsDato: LocalDate = LocalDate.now()
    ): String { //migreringsDato skal kun brukes fra tester
        if (!skalKjøreMigering(migreringAktivert, migreringsDato)) {
            Log.info(MIGRERING_DEAKTIVERT_MELDING)
            return MIGRERING_DEAKTIVERT_MELDING
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
            Log.info(MIGRERING_DEAKTIVERT_MELDING)
            return MIGRERING_DEAKTIVERT_MELDING
        }
        var antallRekjøringer = 0
        identer.forEach { peronident ->
            if (migrertsakRepository.findByStatusAndPersonIdent(MigreringStatus.FEILET, peronident).isNotEmpty()) {
                opprettMigreringstaskService.opprettMigreringtask(peronident)
                antallRekjøringer++
            }
        }
        return "Rekjørt $antallRekjøringer migreringer"
    }

    fun rekjørMigreringerMedFeiltype(feiltype: String): String {
        if (!migreringAktivert) {
            Log.info(MIGRERING_DEAKTIVERT_MELDING)
            return MIGRERING_DEAKTIVERT_MELDING
        }

        val migrertsakMedFeiltype =
            migrertsakRepository.findByStatusAndFeiltype(MigreringStatus.FEILET, feiltype).map { it.personIdent }.toSet()
        migrertsakMedFeiltype.forEach {
            opprettMigreringstaskService.opprettMigreringtask(it)
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
                opprettMigreringstaskService.opprettMigreringtask(person)
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
        const val MIGRERING_DEAKTIVERT_MELDING = "Migrering deaktivert, stopper videre jobbing"

    }
}
