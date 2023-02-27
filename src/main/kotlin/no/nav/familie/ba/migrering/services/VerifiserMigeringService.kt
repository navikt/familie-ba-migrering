package no.nav.familie.ba.migrering.services

import no.nav.familie.ba.migrering.domain.JsonWrapper
import no.nav.familie.ba.migrering.domain.MigreringStatus
import no.nav.familie.ba.migrering.domain.Migrertsak
import no.nav.familie.ba.migrering.domain.MigrertsakRepository
import no.nav.familie.ba.migrering.integrasjoner.InfotrygdClient
import no.nav.familie.ba.migrering.integrasjoner.InfotrygdFeedClient
import no.nav.familie.ba.migrering.integrasjoner.MigreringResponseDto
import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class VerifiserMigeringService(
    val opprettMigreringstaskService: OpprettTaskService,
    val migrertsakRepository: MigrertsakRepository,
    val infotrygdFeedClient: InfotrygdFeedClient,
    val infotrygdClient: InfotrygdClient,
) {

    private val logger = LoggerFactory.getLogger(VerifiserMigeringService::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun verifiserMigrering(personIdent: String, migreringsResponse: MigreringResponseDto) {
        val migrertsak = migrertsakRepository.insert(
            Migrertsak(
                id = UUID.randomUUID(),
                personIdent = personIdent,
                migreringsdato = LocalDateTime.now(),
                status = MigreringStatus.MIGRERT_I_BA,
                callId = MDC.get(MDCConstants.MDC_CALL_ID) ?: IdUtils.generateId(),
                resultatFraBa = JsonWrapper.of(migreringsResponse),
            ),
        )

        opprettMigreringstaskService.opprettVerifiserMigreringTask(migrertsak, migreringsResponse)
    }

    fun sjekkOmFeilytpeErMigrert(feiltype: String): Pair<List<String>, List<String>> {
        val personerMedFeiltype =
            migrertsakRepository.findByStatusAndFeiltype(MigreringStatus.FEILET, feiltype).map { it.personIdent }.toSet()

        return personerMedFeiltype.partition { personIdent -> sjekkOmPersonErMigrert(personIdent) }
    }

    fun sjekkOmPersonErMigrert(personIdent: String): Boolean {
        val (migrertSaker, ikkeMigrerteSaker) = migrertsakRepository.findByPersonIdentAndStatusNot(personIdent, MigreringStatus.ARKIVERT)
            .partition { it.status in listOf(MigreringStatus.MIGRERT_I_BA, MigreringStatus.VERIFISERT) }
        if (migrertSaker.isNotEmpty()) {
            secureLogger.info("Personen med personident $personIdent er allerede migrert")
            logger.info("Personen er allerede migrert")
            ikkeMigrerteSaker.forEach { migrertsak ->
                migrertsak.copy(status = MigreringStatus.MIGRERT_I_BA).also { migrertsakRepository.save(it) } // duplikater
            }
            return true
        }

        val harSendtVedtakshendelse = infotrygdFeedClient.hentOversiktOverVedtaksmeldingerSendtTilFeed(personIdent).isNotEmpty()
        val harIkkeAktiveStønader = infotrygdClient.hentAktivStønadForPerson(personIdent).isEmpty()
        return if (harSendtVedtakshendelse && harIkkeAktiveStønader) {
            logger.info("Fant person i vedtaksfeed og har ingen aktive stønader i infotrygd")
            secureLogger.info("Fant person i vedtaksfeed og har ingen aktive stønader i infotrygd: $personIdent")
            ikkeMigrerteSaker.forEach { migrertsak ->
                migrertsak.copy(status = MigreringStatus.MIGRERT_I_BA).also { migrertsakRepository.save(it) } // duplikater
            }
            true
        } else {
            false
        }
    }

    fun listÅpneSaker(ident: String): List<String> {
        return infotrygdClient.hentSaker(ident).filter { it.status != "FB" }
            .map { sak -> "${sak.saksnr}-${sak.saksblokk},${sak.valg}/${sak.undervalg},${sak.status},${sak.type}, ${sak.regDato},${sak.stønad?.iverksattFom}-${sak.stønad?.opphørtFom}-${sak.stønad?.opphørsgrunn} " }
    }
}
