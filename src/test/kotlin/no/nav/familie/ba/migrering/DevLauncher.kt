package no.nav.familie.ba.migrering

import no.nav.familie.ba.migrering.config.ApplicationConfig
import org.springframework.boot.SpringApplication
import org.springframework.context.annotation.Import


@Import(ApplicationConfig::class)
class DevLauncher


fun main(args: Array<String>) {
    System.setProperty("spring.profiles.active", "dev")
    val springApp = SpringApplication(DevLauncher::class.java)
    springApp.setAdditionalProfiles("dev")
    springApp.run(*args)
}