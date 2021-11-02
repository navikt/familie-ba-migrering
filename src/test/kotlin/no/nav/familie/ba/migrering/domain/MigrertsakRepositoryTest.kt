package no.nav.familie.ba.migrering.domain

import no.nav.familie.ba.migrering.DevLauncher
import no.nav.familie.ba.migrering.database.DbContainerInitializer
import org.junit.jupiter.api.Tag
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
@SpringBootTest(classes = [DevLauncher::class])
class MigrertsakRepositoryTest {

    @Autowired
    lateinit var migrertsakRepository: MigrertsakRepository

    @Test
    fun `save() skal lagre MigrertSak til database`() {
        val migrertsak = Migrertsak(id = UUID.randomUUID(),
                                    sakNummer = "",
                                    aarsak = null,
                                    resultatFraBa = "{test: \"test\"}",
                                    migreringsdato = LocalDateTime.now(),
                                    personIdent = "1234", status = MigreringStatus.MIGRERT_I_BA)

        migrertsakRepository.insert(migrertsak)


    }
}