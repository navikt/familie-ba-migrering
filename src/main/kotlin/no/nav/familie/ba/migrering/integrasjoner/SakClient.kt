package no.nav.familie.ba.migrering.integrasjoner

import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.getDataOrThrow
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import java.net.URI

private val logger = LoggerFactory.getLogger(SakClient::class.java)

@Component
class SakClient @Autowired constructor(
    @param:Value("\${BA_SAK_API_URL}") private val sakApiUri: String,
    @Qualifier("azure") restOperations: RestOperations
) : AbstractRestClient(restOperations, "migrering.sak") {

    fun migrerPerson(ident: String): Any? {
        val uri = URI.create("$sakApiUri/migrering")
        val response: Ressurs<Any> = postForEntity(uri, mapOf("ident" to ident))
        if (response.status == Ressurs.Status.SUKSESS && response.data == null) error("Ressurs har status suksess, men mangler data")
        return response.getDataOrThrow()
    }
}
