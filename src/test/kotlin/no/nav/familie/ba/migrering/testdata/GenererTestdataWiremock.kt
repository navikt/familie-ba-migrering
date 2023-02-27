package no.nav.familie.ba.migrering.testdata

import no.nav.familie.ba.migrering.integrasjoner.MigreringResponseDto
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.boot.web.client.RestTemplateBuilder
import java.time.YearMonth

class GenererTestdataWiremock

fun main(args: Array<String>) {
    val restTemplate = RestTemplateBuilder().build()

    val personer = (1..100).map {
        // generate random number
        (100_000..900_000).random().toString()
    }.toSet()

    val personAsJson = objectMapper.writeValueAsString(personer).replace("\"", "\\\"")

    // mock infotrygd i
    val infotrygdMigreringResponse = INFOTRYGD_WIREMOCK.replace("BODY", personAsJson)
    println(
        "Oppretter infotrygd-testdata i wiremock " +
            restTemplate.postForEntity(
                "http://localhost:8300/__admin/mappings/new",
                infotrygdMigreringResponse,
                String::class.java,
            ),
    )

    // mock sak i wiremock
    for (i in 0..99) {
        val modulo = i % 8 // hver 8 skal feile
        if (modulo == 0) {
            Ressurs.failure<String>("feil").copy(data = "FEIL_FRA_TEST")
            objectMapper.writeValueAsString(Ressurs.failure<String>("feil").copy(data = "FEIL_FRA_TEST")).replace("\"", "\\\"")
                .also {
                    val request = SAK_ERROR_WIREMOCK.replace("BODY", it).replace("IDENT", personer.elementAt(i))
                    println(
                        "Oppretter wiremock for sak som skal feile for person  ${personer.elementAt(i)} " + restTemplate.postForEntity(
                            "http://localhost:8300/__admin/mappings/new",
                            request,
                            String::class.java,
                        ),
                    )
                }
        } else {
            objectMapper.writeValueAsString(
                Ressurs.Companion.success(
                    MigreringResponseDto(
                        1000,
                        9000,
                        virkningFom = YearMonth.now(),
                    ),
                ),
            ).replace("\"", "\\\"")
                .also {
                    val request = SAK_WIREMOCK.replace("BODY", it).replace("IDENT", personer.elementAt(i))
                    println(
                        "Oppretter wiremock for sak som skal returner ok for person  ${personer.elementAt(i)} " + restTemplate.postForEntity(
                            "http://localhost:8300/__admin/mappings/new",
                            request,
                            String::class.java,
                        ),
                    )
                }
        }
    }
}

const val INFOTRYGD_WIREMOCK = """
    {
    "request": {
        "method": "POST",
        "url": "/infotrygd/infotrygd/barnetrygd/migrering"
    },
    "response": {
        "status": 200,
        "body": "BODY",
        "headers": {
            "Content-Type": "application/json"
        }
    }
}
"""

const val SAK_WIREMOCK = """
    {
    "request": {
        "method": "POST",
        "url": "/basak/migrering",
        "bodyPatterns" : [ {
            "matchesJsonPath" : "${'$'}[?(@.ident == 'IDENT')]"
        } ]
    },
    "response": {
        "status": 200,
        "body": "BODY",
        "headers": {
            "Content-Type": "application/json"
        }
    }
}
"""

const val SAK_ERROR_WIREMOCK = """
    {
    "request": {
        "method": "POST",
        "url": "/basak/migrering",
        "bodyPatterns" : [ {
            "matchesJsonPath" : "${'$'}[?(@.ident == 'IDENT')]"
        } ]
    },
    "response": {
        "status": 500,
        "body": "BODY",
        "headers": {
            "Content-Type": "application/json"
        }
    }
}
"""
