package no.nav.familie.ba.migrering.integrasjoner

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.getDataOrThrow
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime

private val logger = LoggerFactory.getLogger(InfotrygdFeedClient::class.java)

@Component
class InfotrygdFeedClient @Autowired constructor(
    @param:Value("\${FAMILIE_BA_INFOTRYGD_FEED_API_URL}") private val infotrygdFeedApiUri: String,
    @Qualifier("azure") restOperations: RestOperations
) : AbstractRestClient(restOperations, "migrering.infotrygd.feed") {

    fun hentOversiktOverVedtaksmeldingerSendtTilFeed(personIdent: String): List<FeedOpprettetDto> {
        val uri = URI.create("$infotrygdFeedApiUri/barnetrygd/v1/feed/BA_Vedtak_v1/opprettet")
        try {
            val response: Ressurs<List<FeedOpprettetDto>> = postForEntity(uri, personIdent)
            return response.getDataOrThrow().also { logger.info("Vedtaksmeldinger sendt for person: $it") }
        } catch (e: Exception) {
            val secureLogMessage = if (e is HttpStatusCodeException)
                "Http feil mot ${uri.path}: httpkode: ${e.statusCode}, feilmelding ${e.getResponseBodyAsString()}" else
                "Feil mot ${uri.path}; melding ${e.message}"
            secureLogger.error(secureLogMessage, e)
            logger.error("Feil mot ${uri.path}.")
            throw RuntimeException("Feil mot ${uri.path}: ${e.message}", e)
        }
    }
}

data class FeedOpprettetDto(
    val opprettetDato: LocalDateTime,
    val datoStartNyBa: LocalDate?
)