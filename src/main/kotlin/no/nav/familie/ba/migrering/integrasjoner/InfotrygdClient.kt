package no.nav.familie.ba.migrering.integrasjoner

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.ba.infotrygd.Stønad
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
class InfotrygdClient @Autowired constructor(
    @param:Value("\${FAMILIE_BA_INFOTRYGD_API_URL}") private val infotrygdApiUri: String,
    @Qualifier("azure") restOperations: RestOperations
) : AbstractRestClient(restOperations, "migrering.infotrygd") {

    fun hentPersonerKlareForMigrering(migreringRequest: MigreringRequest): Set<String> {
        val uri = URI.create("$infotrygdApiUri/infotrygd/barnetrygd/migrering")
        return try {
            postForEntity(uri, migreringRequest)
        } catch (ex: Exception) {
            loggFeil(ex, uri)
            throw RuntimeException("Henting av personer for migrering feilet: ${ex.message}", ex)
        }
    }

    fun hentStønad(stønadRequest: StønadRequest): Stønad {
        val uri = URI.create("$infotrygdApiUri/infotrygd/barnetrygd/stonad/sok")

        return try {
            postForEntity(uri, stønadRequest)
        } catch (ex: Exception) {
            loggFeil(ex, uri)
            throw RuntimeException("Henting av stønad for migrert person feilet: ${ex.message}", ex)
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
    val maksAntallBarn: Int = 99,
    val minimumAlder: Int = 7,
)

class StønadRequest(
    val personIdent: String,
    val tknr: String,
    val iverksattFom: String,
    val virkningFom: String,
    val region: String
)
