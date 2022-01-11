package no.nav.familie.ba.migrering.rest

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.migrering.domain.MigreringStatus
import no.nav.familie.ba.migrering.domain.Migrertsak
import no.nav.familie.ba.migrering.domain.MigrertsakRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class MigrertSakControllerTest {

    private val migrertsakRepository = mockk<MigrertsakRepository>()
    lateinit var migrertSakController: MigrertSakController

    @BeforeEach
    fun setUp() {
        migrertSakController = MigrertSakController(migrertsakRepository)
    }

    @Test
    fun listFeiledMigreringer() {
        every { migrertsakRepository.findByStatusIn(listOf(MigreringStatus.FEILET)) } returns listOf(
            Migrertsak(
                UUID.randomUUID(),
                personIdent = "1",
                status = MigreringStatus.FEILET,
                feiltype = "FEILTYPE_1"
            ),
            Migrertsak(
                UUID.randomUUID(),
                personIdent = "2",
                status = MigreringStatus.FEILET,
                feiltype = "FEILTYPE_1"
            ),
            Migrertsak(
                UUID.randomUUID(),
                personIdent = "3",
                status = MigreringStatus.FEILET,
                feiltype = "FEILTYPE_2"
            ),
            Migrertsak(
                UUID.randomUUID(),
                personIdent = "1",
                status = MigreringStatus.FEILET,
                feiltype = "FEILTYPE_1"
            ),
            Migrertsak(
                UUID.randomUUID(),
                personIdent = "4",
                status = MigreringStatus.FEILET,
                feiltype = null
            ),
        )
        val listeMedFeilede = migrertSakController.listFeiledMigreringer()
        assertThat(listeMedFeilede.size).isEqualTo(2)
        assertThat(listeMedFeilede["FEILTYPE_1"]).containsExactly("1", "2")
        assertThat(listeMedFeilede["FEILTYPE_1"]?.size).isEqualTo(2)
        assertThat(listeMedFeilede["FEILTYPE_2"]).containsExactly("3")
        assertThat(listeMedFeilede["FEILTYPE_2"]?.size).isEqualTo(1)
    }
}

