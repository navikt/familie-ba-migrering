package no.nav.familie.ba.migrering.config

import no.nav.familie.sikkerhet.ClientTokenValidationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Configuration
class RestSecurityFilterConfig {

    @Bean
    fun requestFilter() = object : OncePerRequestFilter() {
        override fun doFilterInternal(
            request: HttpServletRequest,
            response: HttpServletResponse,
            filterChain: FilterChain
        ) {
            val clientTokenValidationFilter = ClientTokenValidationFilter(true, true)
            clientTokenValidationFilter.doFilter(request, response, filterChain)
        }

        override fun shouldNotFilter(request: HttpServletRequest) =
            request.requestURI.contains("/internal") ||
                    request.requestURI.startsWith("/swagger") ||
                    request.requestURI.startsWith("/v3") // i bruk av swagger
    }
}
