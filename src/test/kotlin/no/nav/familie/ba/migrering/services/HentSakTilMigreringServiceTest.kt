package no.nav.familie.ba.migrering.services

import io.mockk.every
import io.mockk.mockk
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

    @Test
    fun `Skal opprett task for hver person returns fra InfotrygdClient`() {
        val personIdenter = arrayOf("123", "223")
        every { infotrygdClientMock.hentPersonerKlareForMigrering(any()) } returns personIdenter.toSet()
        val tasker = mutableListOf<Task>()
        every { taskRepositoryMock.save(capture(tasker)) } returns Task(type = "", payload = "")
        HentSakTilMigreringService(infotrygdClientMock, taskRepositoryMock, true).hentSakTilMigrering()

        assertThat(tasker).hasSize(2)
        assertThat(
            personIdenter.all { personIdent ->
                tasker.find { it.payload.contains(personIdent) } != null
            }
        ).isTrue
    }
}
