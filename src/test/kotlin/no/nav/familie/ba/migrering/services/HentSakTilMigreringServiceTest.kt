package no.nav.familie.ba.migrering.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.migrering.domain.MigreringStatus
import no.nav.familie.ba.migrering.domain.Migrertsak
import no.nav.familie.ba.migrering.domain.MigrertsakRepository
import no.nav.familie.ba.migrering.integrasjoner.InfotrygdClient
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HentSakTilMigreringServiceTest {
    val taskRepositoryMock: TaskRepository = mockk()
    val infotrygdClientMock: InfotrygdClient = mockk()
    val migertsakRepository: MigrertsakRepository = mockk()

    @Test
    fun `Skal opprett task for hver person returns fra InfotrygdClient`() {
        val personIdenter = arrayOf("123", "223")
        every { infotrygdClientMock.hentPersonerKlareForMigrering(any()) } returns personIdenter.toSet()
        val tasker = mutableListOf<Task>()
        every { taskRepositoryMock.save(capture(tasker)) } returns Task(type = "", payload = "")
        every { migertsakRepository.findByStatusAndPersonIdent(any(), any()) } returns emptyList()
        HentSakTilMigreringService(
            infotrygdClientMock,
            taskRepositoryMock,
            migertsakRepository,
            true,
            20,
        ).hentSakTilMigrering()

        assertThat(tasker).hasSize(2)
        assertThat(
            personIdenter.all { personIdent ->
                tasker.find { it.payload.contains(personIdent) } != null
            }
        ).isTrue
    }

    @Test
    fun `Skal ikke migrer hvis det er en migrert sak for den personen i repository`() {
        val personIdent = "123"
        every { infotrygdClientMock.hentPersonerKlareForMigrering(any()) } returns setOf(personIdent)
        every { migertsakRepository.findByStatusAndPersonIdent(MigreringStatus.MIGRERT_I_BA, personIdent) } returns listOf(Migrertsak())
        every { taskRepositoryMock.save(any()) } returns Task(type = "", payload = "")

        HentSakTilMigreringService(
            infotrygdClientMock,
            taskRepositoryMock,
            migertsakRepository,
            true,
            20,
        ).hentSakTilMigrering()

        verify(exactly = 0) { taskRepositoryMock.save(any()) }
    }
}
