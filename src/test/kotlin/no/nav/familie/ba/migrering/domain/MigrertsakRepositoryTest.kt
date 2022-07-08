package no.nav.familie.ba.migrering.domain

import no.nav.familie.ba.migrering.DevLauncher
import no.nav.familie.ba.migrering.database.DatabaseCleanUpService
import no.nav.familie.ba.migrering.database.DbContainerInitializer
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("dev")
@SpringBootTest(
    classes = [DevLauncher::class],
    properties = [
        "no.nav.security.jwt.issuer.azuread.discoveryUrl: http://localhost:\${mock-oauth2-server.port}/azuread/.well-known/openid-configuration"
    ]
)

@EnableMockOAuth2Server
class MigrertsakRepositoryTest {

    @Autowired
    lateinit var migrertsakRepository: MigrertsakRepository

    @Autowired
    lateinit var migrertsakRepositoryForJsonQuery: MigrertsakRepositoryForJsonQuery

    @Autowired
    lateinit var databaseCleanUpService: DatabaseCleanUpService

    data class BaResultat(
        val test: String = "ok",
    )

    @BeforeEach
    fun truncateTables() {
        databaseCleanUpService.truncate()
    }

    @Test
    fun `save() skal lagre MigrertSak til database`() {
        val resultatFraBa = JsonWrapper.of(BaResultat())

        val migrertsak = Migrertsak(
            id = UUID.randomUUID(),
            callId = "",
            aarsak = null,
            resultatFraBa = resultatFraBa,
            migreringsdato = LocalDateTime.now(),
            personIdent = "1234", status = MigreringStatus.MIGRERT_I_BA
        )

        migrertsakRepository.insert(migrertsak)
    }

    @Test
    fun `find() skal hent MigrertSak fra database`() {
        val resultatFraBa = JsonWrapper.of(BaResultat())

        val migrertsak = Migrertsak(
            id = UUID.randomUUID(),
            callId = "",
            aarsak = null,
            resultatFraBa = resultatFraBa,
            migreringsdato = LocalDateTime.now(),
            personIdent = "1234", status = MigreringStatus.MIGRERT_I_BA
        )

        migrertsakRepository.insert(migrertsak)

        val sak = migrertsakRepository.findById(migrertsak.id)
        assertThat(sak.get().resultatFraBa).isEqualTo(migrertsak.resultatFraBa)
    }

    @Test
    fun `resultatFraBa skal lagres som json`() {
        val resultatFraBa = JsonWrapper.of(BaResultat())

        val targetSak = migrertsakRepository.insert(
            Migrertsak(
                id = UUID.randomUUID(),
                callId = "",
                aarsak = null,
                resultatFraBa = resultatFraBa,
                migreringsdato = LocalDateTime.now(),
                personIdent = "1234", status = MigreringStatus.MIGRERT_I_BA
            )
        )

        migrertsakRepository.insert(
            Migrertsak(
                id = UUID.randomUUID(),
                callId = "",
                aarsak = null,
                resultatFraBa = null,
                migreringsdato = LocalDateTime.now(),
                personIdent = "1234", status = MigreringStatus.MIGRERT_I_BA
            )
        )

        val saker = migrertsakRepositoryForJsonQuery.finnMedBaResultat("test", "ok")
        assertThat(saker).hasSize(1)
        assertThat(saker[0].id).isEqualTo(targetSak.id)
    }
}
