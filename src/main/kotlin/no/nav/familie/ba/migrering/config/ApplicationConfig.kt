package no.nav.familie.ba.migrering.config

import no.nav.familie.http.config.RestTemplateAzure
import no.nav.familie.log.filter.LogFilter
import no.nav.security.token.support.client.core.http.OAuth2HttpClient
import no.nav.security.token.support.client.spring.oauth2.DefaultOAuth2HttpClient
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.boot.web.servlet.server.ServletWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.Duration
import java.time.temporal.ChronoUnit

@SpringBootConfiguration
@ConfigurationPropertiesScan("no.nav.familie")
@ComponentScan("no.nav.familie.ba.migrering", "no.nav.familie.prosessering")
@EnableJwtTokenValidation(ignore = ["org.springframework", "org.springdoc"])
@EnableOAuth2Client
@Import(RestTemplateAzure::class)
@EnableScheduling //brukes av prosessering(Tasks)
class ApplicationConfig {

    @Bean
    fun servletWebServerFactory(): ServletWebServerFactory {
        val serverFactory = JettyServletWebServerFactory()
        serverFactory.port = 8098
        return serverFactory
    }

    @Bean
    fun logFilter(): FilterRegistrationBean<LogFilter> {
        log.info("Registering LogFilter filter")
        val filterRegistration: FilterRegistrationBean<LogFilter> = FilterRegistrationBean()
        filterRegistration.filter = LogFilter()
        filterRegistration.order = 1
        return filterRegistration
    }

    /**
     * Overskrever OAuth2HttpClient som settes opp i token-support som ikke kan få med objectMapper fra felles
     * pga .setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE)
     * og [OAuth2AccessTokenResponse] som burde settes med setters, då feltnavn heter noe annet enn feltet i json
     */
    @Bean
    @Primary
    fun oAuth2HttpClient(): OAuth2HttpClient {
        return DefaultOAuth2HttpClient(
            RestTemplateBuilder()
                .setConnectTimeout(Duration.of(2, ChronoUnit.SECONDS))
                .setReadTimeout(Duration.of(4, ChronoUnit.SECONDS))
        )
    }

    companion object {

        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
