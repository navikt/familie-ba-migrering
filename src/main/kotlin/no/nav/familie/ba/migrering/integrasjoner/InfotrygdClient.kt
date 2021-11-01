package no.nav.familie.ba.migrering.integrasjoner

import no.nav.familie.http.client.AbstractRestClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestOperations
import java.net.URI

private val logger = LoggerFactory.getLogger(InfotrygdClient::class.java)

@Component
class InfotrygdClient @Autowired constructor(@param:Value("\${FAMILIE_BA_INFOTRYGD_API_URL}") private val infotrygdApiUri: String,
                                       @Qualifier("azure") restOperations: RestOperations)
                                        : AbstractRestClient(restOperations, "migrering.infotrygd") {

    fun hentPersonerKlareForMigrering(migreringRequest: MigreringRequest): Set<String> {
        val uri = URI.create("$infotrygdApiUri/infotrygd/barnetrygd/migrering")
        return try {
            postForEntity(uri, migreringRequest)
        } catch (ex: Exception) {
            loggFeil(ex, uri)
            throw RuntimeException("Henting av personer for migrering feilet: ${ex.message}", ex)
        }
    }

    private fun loggFeil(ex: Exception, uri: URI) {
        val secureLogMessage = if (ex is HttpClientErrorException)
            "Http feil mot ${uri.path}: httpkode: ${ex.statusCode}, feilmelding ${ex.message}" else
            "Feil mot ${uri.path}; melding ${ex.message}"
        secureLogger.error(secureLogMessage, ex)
        logger.error("Feil mot ${uri.path}.")
    }
}

data class MigreringRequest(
    val page: Int,
    val size: Int,
    val valg: String,
    val undervalg: String,
    val maksAntallBarn: Int = 99
)
