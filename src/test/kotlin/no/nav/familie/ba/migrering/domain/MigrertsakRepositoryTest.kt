package no.nav.familie.ba.migrering.domain

import no.nav.familie.ba.migrering.DevLauncher
import no.nav.familie.ba.migrering.database.DbContainerInitializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.jupiter.api.Disabled
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
    lateinit var migrertsakJDBCRepository: MigrertsakJDBCRepository

    @Test
    fun `save() skal lagre MigrertSak til database`() {
        val migrertsak = Migrertsak(
            id = UUID.randomUUID(),
            sakNummer = "",
            aarsak = null,
            resultatFraBa = "{\"test\": \"test\"}",
            migreringsdato = LocalDateTime.now(),
            personIdent = "1234",
            status = MigreringStatus.MIGRERT_I_BA
        )

        migrertsakJDBCRepository.lagre(migrertsak)
    }

    @Test
    fun `find() skal hent MigrertSak fra database`() {
        val migrertsak = Migrertsak(
            id = UUID.randomUUID(),
            sakNummer = "",
            aarsak = null,
            resultatFraBa = "{\"test\": \"test\"}",
            migreringsdato = LocalDateTime.now(),
            personIdent = "1234", status = MigreringStatus.MIGRERT_I_BA
        )

        migrertsakJDBCRepository.lagre(migrertsak)

        val sak = migrertsakJDBCRepository.findByID(migrertsak.id)
        assertThat(sak!!.resultatFraBa).isEqualTo(migrertsak.resultatFraBa)
    }

    @Test
    fun `resultatFraBa skal lagres som json`() {
        val targetSak = migrertsakJDBCRepository.lagre(
            Migrertsak(
                id = UUID.randomUUID(),
                sakNummer = "",
                aarsak = null,
                resultatFraBa = "{\"test\": \"ooo\"}",
                migreringsdato = LocalDateTime.now(),
                personIdent = "1234", status = MigreringStatus.MIGRERT_I_BA
            )
        )

        migrertsakJDBCRepository.lagre(
            Migrertsak(
                id = UUID.randomUUID(),
                sakNummer = "",
                aarsak = null,
                resultatFraBa = "{\"test\": \"xxx\"}",
                migreringsdato = LocalDateTime.now(),
                personIdent = "1234", status = MigreringStatus.MIGRERT_I_BA
            )
        )

        //val saker = migrertsakJDBCRepository.findByID()
        //assertThat(saker).hasSize(1)
        //assertThat(saker[0].id).isEqualTo(targetSak.id)
    }
}
