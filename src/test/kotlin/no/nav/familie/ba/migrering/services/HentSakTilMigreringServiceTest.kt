package no.nav.familie.ba.migrering.services

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.migrering.domain.MigreringStatus
import no.nav.familie.ba.migrering.domain.Migrertsak
import no.nav.familie.ba.migrering.domain.MigrertsakRepository
import no.nav.familie.ba.migrering.integrasjoner.InfotrygdClient
import no.nav.familie.ba.migrering.integrasjoner.MigreringRequest
import no.nav.familie.ba.migrering.services.HentSakTilMigreringService.Companion.ANTALL_PERSONER_SOM_HENTES_FRA_INFOTRYGD
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HentSakTilMigreringServiceTest {

    val taskRepositoryMock: TaskRepository = mockk()
    val infotrygdClientMock: InfotrygdClient = mockk()
    val migertsakRepository: MigrertsakRepository = mockk()

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @Test
    fun `Skal opprett task for hver person returns fra InfotrygdClient`() {
        val personIdenter = arrayOf("123", "223")
        every {
            infotrygdClientMock.hentPersonerKlareForMigrering(
                MigreringRequest(
                    4,
                    ANTALL_PERSONER_SOM_HENTES_FRA_INFOTRYGD,
                    "OR",
                    "OS",
                    1
                )
            )
        } returns personIdenter.toSet()
        every {
            infotrygdClientMock.hentPersonerKlareForMigrering(
                MigreringRequest(
                    5,
                    ANTALL_PERSONER_SOM_HENTES_FRA_INFOTRYGD,
                    "OR",
                    "OS",
                    1
                )
            )
        } returns emptySet()
        val tasker = mutableListOf<Task>()
        every { taskRepositoryMock.save(capture(tasker)) } returns Task(type = "", payload = "")
        every { migertsakRepository.findByStatusAndPersonIdent(any(), any()) } returns emptyList()
        HentSakTilMigreringService(
            infotrygdClientMock,
            taskRepositoryMock,
            migertsakRepository,
            true,
            20,
        ).hentSakTilMigreringScheduler()

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
        every {
            infotrygdClientMock.hentPersonerKlareForMigrering(
                MigreringRequest(
                    4,
                    ANTALL_PERSONER_SOM_HENTES_FRA_INFOTRYGD,
                    "OR",
                    "OS",
                    1
                )
            )
        } returns setOf(personIdent)
        every {
            infotrygdClientMock.hentPersonerKlareForMigrering(
                MigreringRequest(
                    5,
                    ANTALL_PERSONER_SOM_HENTES_FRA_INFOTRYGD,
                    "OR",
                    "OS",
                    1
                )
            )
        } returns emptySet()
        every { migertsakRepository.findByStatusAndPersonIdent(MigreringStatus.MIGRERT_I_BA, personIdent) } returns listOf(
            Migrertsak()
        )
        every { taskRepositoryMock.save(any()) } returns Task(type = "", payload = "")

        println(
            HentSakTilMigreringService(
                infotrygdClientMock,
                taskRepositoryMock,
                migertsakRepository,
                true,
                1,
            ).migrer()
        )

        verify(exactly = 0) { taskRepositoryMock.save(any()) }
    }

    @Test
    fun `Skal kjøre migrering til maks antall er nådd`() {
        every {
            infotrygdClientMock.hentPersonerKlareForMigrering(
                MigreringRequest(
                    4,
                    ANTALL_PERSONER_SOM_HENTES_FRA_INFOTRYGD,
                    "OR",
                    "OS",
                    1
                )
            )
        } returns arrayOf("1", "2", "3").toSet()
        every {
            infotrygdClientMock.hentPersonerKlareForMigrering(
                MigreringRequest(
                    5,
                    ANTALL_PERSONER_SOM_HENTES_FRA_INFOTRYGD,
                    "OR",
                    "OS",
                    1
                )
            )
        } returns arrayOf("4", "5", "6", "7", "8").toSet()
        every {
            infotrygdClientMock.hentPersonerKlareForMigrering(
                MigreringRequest(
                    6,
                    ANTALL_PERSONER_SOM_HENTES_FRA_INFOTRYGD,
                    "OR",
                    "OS",
                    1
                )
            )
        } returns arrayOf("9", "10", "11", "12", "13").toSet()
        val tasker = mutableListOf<Task>()
        every { taskRepositoryMock.save(capture(tasker)) } returns Task(type = "", payload = "")
        every { migertsakRepository.findByStatusAndPersonIdent(any(), any()) } returns emptyList()
        every { migertsakRepository.findByStatusAndPersonIdent(MigreringStatus.MIGRERT_I_BA, "4") } returns listOf(Migrertsak())
        every { migertsakRepository.findByStatusAndPersonIdent(MigreringStatus.MIGRERT_I_BA, "9") } returns listOf(Migrertsak())
        HentSakTilMigreringService(
            infotrygdClientMock,
            taskRepositoryMock,
            migertsakRepository,
            true,
            10,
        ).hentSakTilMigreringScheduler()

        assertThat(tasker).hasSize(10)

        tasker.forEach { task ->
            println(task.payload)
        }

        assertThat(
            listOf("1", "2", "3", "5", "6", "7", "8", "10", "11", "12").all { personIdent ->
                tasker.find { it.payload.contains(personIdent) } != null
            }
        ).isTrue

    }
}
