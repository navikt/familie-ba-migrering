package no.nav.familie.ba.migrering.integrasjoner

import com.fasterxml.jackson.databind.JsonNode
import no.nav.familie.ba.migrering.rest.MigreringsfeilType
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.http.client.RessursException
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.kontrakter.felles.objectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.YearMonth

private val logger = LoggerFactory.getLogger(SakClient::class.java)

@Component
class SakClient @Autowired constructor(
    @param:Value("\${BA_SAK_API_URL}") private val sakApiUri: String,
    @Qualifier("azure") restOperations: RestOperations
) : AbstractRestClient(restOperations, "migrering.sak") {

    fun migrerPerson(ident: String): MigreringResponseDto {
        val uri = URI.create("$sakApiUri/migrering")
        try {
            val response: Ressurs<MigreringResponseDto> = postForEntity(uri, mapOf("ident" to ident))
            secureLogger.info("Response fra ba-sak ved migrering $response")
            if (response.status == Ressurs.Status.SUKSESS && response.data == null) error("Ressurs har status suksess, men mangler data")
            return response.getDataOrThrow()
        } catch (e: HttpStatusCodeException) {
            val ressurs = e.getResponseBodyAsString()
            secureLogger.info("Feilressurs fra sak : $ressurs")

            if (!ressurs.isNullOrBlank()) {
                val actualObj: JsonNode = objectMapper.readTree(ressurs)
                val data = actualObj.get("data").asText("UKJENT")
                throw KanIkkeMigrereException(feiltype = data.toString(), melding = ressurs, e)
            }
            throw e
        } catch (e: RessursException) {
            if (e.cause is KanIkkeMigrereException) {
                secureLogger.info("Kaster videre cause", e.cause)
                throw e
            } else {
                val feiltype = try {
                    MigreringsfeilType.valueOf(e.ressurs.data as String).name
                } catch (e: Exception) {
                    "UKJENT"
                }

                throw KanIkkeMigrereException(feiltype = feiltype, melding = objectMapper.writeValueAsString(e.ressurs), e)
            }
        }
    }
}

data class MigreringResponseDto(
    val fagsakId: Long,
    val behandlingId: Long,
    val infotrygdStønadId: Long? = null,
    val infotrygdSakId: Long? = null,
    val virkningFom: YearMonth? = null,
    val infotrygdTkNr: String? = null,
    val infotrygdVirkningFom: String? = null,
    val infotrygdIverksattFom: String? = null,
    val infotrygdRegion: String? = null,
)
