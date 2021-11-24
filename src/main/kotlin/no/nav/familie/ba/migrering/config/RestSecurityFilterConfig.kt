package no.nav.familie.ba.migrering.config

import no.nav.familie.sikkerhet.ClientTokenValidationFilter
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
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

    @Bean
    fun requestFilter() = object : OncePerRequestFilter() {
        override fun doFilterInternal(
            request: HttpServletRequest,
            response: HttpServletResponse,
            filterChain: FilterChain
        ) {
            val clientTokenValidationFilter = ClientTokenValidationFilter(true, true)
            clientTokenValidationFilter.doFilter(request, response, filterChain)
            if (!hentGrupper().contains(forvalterRolleTeamfamilie) && !environment.activeProfiles.contains("dev")) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Handling krever teamfamilie forvalter rolle")
            }
        }

        override fun shouldNotFilter(request: HttpServletRequest) =
            request.requestURI.contains("/internal") ||
                    request.requestURI.startsWith("/swagger") ||
                    request.requestURI.startsWith("/v3") // i bruk av swagger
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
