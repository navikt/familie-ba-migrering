package no.nav.familie.ba.migrering.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.OncePerRequestFilter

@Configuration
class RestSecurityFilterConfig(
    @Value("\${rolle.teamfamilie.forvalter}")
    val forvalterRolleTeamfamilie: String,
) {

    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    @Bean
    fun requestFilter() = object : OncePerRequestFilter() {
        override fun doFilterInternal(
            request: HttpServletRequest,
            response: HttpServletResponse,
            filterChain: FilterChain,
        ) {
            val grupper = hentGrupper()
            if ((!tilgangSomApp() && !grupper.contains(forvalterRolleTeamfamilie)) && !environment.activeProfiles.contains("dev")) {
                secureLogger.info("Ugyldig rolle for url=${request.requestURI} grupper=$grupper, forvalterRolleTeamfamilie=$forvalterRolleTeamfamilie")
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Handling krever teamfamilie forvalter rolle")
            } else {
                try {
                    filterChain.doFilter(request, response)
                } catch (e: Exception) {
                    secureLogger.warn("Uventet feil i doFilter for url=${request.requestURI} grupper=$grupper", e)
                    throw e
                }
            }
        }

        override fun shouldNotFilter(request: HttpServletRequest): Boolean {
            return request.requestURI.contains("/internal") ||
                request.requestURI.startsWith("/api/task") ||
                request.requestURI.startsWith("/swagger") ||
                request.requestURI.startsWith("/v3")
        } // i bruk av swagger
    }

    private fun hentGrupper(): List<String> {
        return Result.runCatching { SpringTokenValidationContextHolder().tokenValidationContext }
            .fold(
                onSuccess = {
                    @Suppress("UNCHECKED_CAST")
                    it.getClaims("azuread")?.get("groups") as List<String>? ?: emptyList()
                },
                onFailure = { emptyList() },
            )
    }

    private fun tilgangSomApp(): Boolean {
        return Result.runCatching { SpringTokenValidationContextHolder().tokenValidationContext }
            .fold(
                onSuccess = {
                    @Suppress("UNCHECKED_CAST")
                    val roller = it.getClaims("azuread")?.get("roles") as List<String>?
                    if (roller.isNullOrEmpty()) {
                        false
                    } else {
                        roller.contains("access_as_application")
                    }
                },
                onFailure = { false },
            )
    }
}
