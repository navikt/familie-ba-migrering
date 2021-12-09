package no.nav.familie.ba.migrering.config

import no.nav.familie.sikkerhet.ClientTokenValidationFilter
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Configuration
class RestSecurityFilterConfig(@Value("\${rolle.teamfamilie.forvalter}")
                               val forvalterRolleTeamfamilie: String) {

    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @Bean
    fun requestFilter() = object : OncePerRequestFilter() {
        override fun doFilterInternal(
            request: HttpServletRequest,
            response: HttpServletResponse,
            filterChain: FilterChain
        ) {
            val grupper = hentGrupper()
            if (!grupper.contains(forvalterRolleTeamfamilie) && !environment.activeProfiles.contains("dev")) {
                secureLogger.info("Ugyldig rolle for url=${request.requestURI} grupper=$grupper, forvalterRolleTeamfamilie=$forvalterRolleTeamfamilie")
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Handling krever teamfamilie forvalter rolle")
            } else {
                try {
                    val clientTokenValidationFilter = ClientTokenValidationFilter(true, true)
                    clientTokenValidationFilter.doFilter(request, response, filterChain)
                } catch (e: Exception) {
                    secureLogger.warn("Uventet feil i doFilter for url=${request.requestURI} grupper=$grupper", e)
                    throw e
                }
            }
        }

        override fun shouldNotFilter(request: HttpServletRequest): Boolean {
            val shouldNotFilter = request.requestURI.contains("/internal") ||
                    request.requestURI.startsWith("/api/task") ||
                    request.requestURI.startsWith("/swagger") ||
                    request.requestURI.startsWith("/v3")
            logger.info("Should not filter returns $shouldNotFilter for ${request.requestURI}")
            return shouldNotFilter
        } // i bruk av swagger
    }

    private fun hentGrupper(): List<String> {
        return Result.runCatching { SpringTokenValidationContextHolder().tokenValidationContext }
            .fold(
                onSuccess = {
                    @Suppress("UNCHECKED_CAST")
                    it.getClaims("azuread")?.get("groups") as List<String>? ?: emptyList()
                },
                onFailure = { emptyList() }
            )
    }
}
