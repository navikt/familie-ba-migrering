package no.nav.familie.ba.migrering

import no.nav.familie.ba.migrering.config.ApplicationConfig
import org.springframework.boot.SpringApplication
import org.springframework.context.annotation.Import

/**
 * Start opp docker-compose up for å starte database og wiremock
 *
 * Kjør main i [no.nav.familie.ba.migrering.testdata.GenererTestdataWiremock] for å
 * generere testdata.
 **/
@Import(ApplicationConfig::class)
class DevLauncher

fun main(args: Array<String>) {
    System.setProperty("spring.profiles.active", "dev")
    val springApp = SpringApplication(DevLauncher::class.java)
    springApp.setAdditionalProfiles("dev")
    springApp.run(*args)
}
