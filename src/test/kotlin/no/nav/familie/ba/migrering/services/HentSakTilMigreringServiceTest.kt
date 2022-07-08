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
import no.nav.familie.ba.migrering.integrasjoner.MigreringResponse
import no.nav.familie.ba.migrering.services.HentSakTilMigreringService.Companion.ANTALL_PERSONER_SOM_HENTES_FRA_INFOTRYGD
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HentSakTilMigreringServiceTest {

    private val taskRepositoryMock: TaskRepository = mockk()
    private val infotrygdClientMock: InfotrygdClient = mockk()
    private val migertsakRepository: MigrertsakRepository = mockk()
    private lateinit var service: HentSakTilMigreringService

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        service = HentSakTilMigreringService(infotrygdClientMock, OpprettTaskService(taskRepositoryMock), migertsakRepository, true)
    }

    @Test
    fun `Skal opprett task for hver person returns fra InfotrygdClient`() {
        val personIdenter = arrayOf("123", "223")
        every {
            infotrygdClientMock.hentPersonerKlareForMigrering(
                MigreringRequest(
                    0,
                    ANTALL_PERSONER_SOM_HENTES_FRA_INFOTRYGD,
                    "OR",
                    "OS",
                    HentSakTilMigreringService.MAX_ANTALL_BARN,
                    HentSakTilMigreringService.MINIMUM_ALDER,
                )
            )
        } returns MigreringResponse(personIdenter.toSet(), 2)
        every {
            infotrygdClientMock.hentPersonerKlareForMigrering(
                MigreringRequest(
                    1,
                    ANTALL_PERSONER_SOM_HENTES_FRA_INFOTRYGD,
                    "OR",
                    "OS",
                    HentSakTilMigreringService.MAX_ANTALL_BARN,
                    HentSakTilMigreringService.MINIMUM_ALDER,
                )
            )
        } returns MigreringResponse(emptySet(), 2)
        val tasker = mutableListOf<Task>()
        every { taskRepositoryMock.save(capture(tasker)) } returns Task(type = "", payload = "")
        every { migertsakRepository.findByPersonIdentAndStatusNot(any(), MigreringStatus.ARKIVERT) } returns emptyList()
        service.migrer(10, GYLDIG_MIGRERINGSKJØRETIDSPUNKT)

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
                    0,
                    ANTALL_PERSONER_SOM_HENTES_FRA_INFOTRYGD,
                    "OR",
                    "OS",
                    HentSakTilMigreringService.MAX_ANTALL_BARN,
                    HentSakTilMigreringService.MINIMUM_ALDER,
                )
            )
        } returns MigreringResponse(setOf(personIdent), 2)
        every {
            infotrygdClientMock.hentPersonerKlareForMigrering(
                MigreringRequest(
                    1,
                    ANTALL_PERSONER_SOM_HENTES_FRA_INFOTRYGD,
                    "OR",
                    "OS",
                    HentSakTilMigreringService.MAX_ANTALL_BARN,
                    HentSakTilMigreringService.MINIMUM_ALDER,
                )
            )
        } returns MigreringResponse(emptySet(), 2)
        every { migertsakRepository.findByPersonIdentAndStatusNot(personIdent, MigreringStatus.ARKIVERT) } returns listOf(
            Migrertsak()
        )
        every { taskRepositoryMock.save(any()) } returns Task(type = "", payload = "")

        println(
            service.migrer(10, GYLDIG_MIGRERINGSKJØRETIDSPUNKT)
        )

        verify(exactly = 0) { taskRepositoryMock.save(any()) }
    }

    @Test
    fun `Skal kjøre migrering til maks antall er nådd`() {

        every {
            infotrygdClientMock.hentPersonerKlareForMigrering(
                MigreringRequest(
                    0,
                    ANTALL_PERSONER_SOM_HENTES_FRA_INFOTRYGD,
                    "OR",
                    "OS",
                    HentSakTilMigreringService.MAX_ANTALL_BARN,
                    HentSakTilMigreringService.MINIMUM_ALDER,
                )
            )
        } returns MigreringResponse(arrayOf("1", "2", "3").toSet(), 3)
        every {
            infotrygdClientMock.hentPersonerKlareForMigrering(
                MigreringRequest(
                    1,
                    ANTALL_PERSONER_SOM_HENTES_FRA_INFOTRYGD,
                    "OR",
                    "OS",
                    HentSakTilMigreringService.MAX_ANTALL_BARN,
                    HentSakTilMigreringService.MINIMUM_ALDER,
                )
            )
        } returns MigreringResponse(arrayOf("4", "5", "6", "7", "8").toSet(), 3)
        every {
            infotrygdClientMock.hentPersonerKlareForMigrering(
                MigreringRequest(
                    2,
                    ANTALL_PERSONER_SOM_HENTES_FRA_INFOTRYGD,
                    "OR",
                    "OS",
                    HentSakTilMigreringService.MAX_ANTALL_BARN,
                    HentSakTilMigreringService.MINIMUM_ALDER,
                )
            )
        } returns MigreringResponse(arrayOf("9", "10", "11", "12", "13").toSet(), 3)
        val tasker = mutableListOf<Task>()
        every { taskRepositoryMock.save(capture(tasker)) } returns Task(type = "", payload = "")
        every { migertsakRepository.findByPersonIdentAndStatusNot(any(), MigreringStatus.ARKIVERT) } returns emptyList()
        every { migertsakRepository.findByPersonIdentAndStatusNot("4", MigreringStatus.ARKIVERT) } returns listOf(Migrertsak())
        every { migertsakRepository.findByPersonIdentAndStatusNot("9", MigreringStatus.ARKIVERT) } returns listOf(Migrertsak())
        service.migrer(10, GYLDIG_MIGRERINGSKJØRETIDSPUNKT)

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

    @Test
    fun `Rekjør migrering på feiltype - ikke opprett task hvis det er ingen migrertsak å rekjøre`() {
        every { migertsakRepository.findByStatusAndFeiltype(MigreringStatus.FEILET, "TESTFEIL") } returns emptyList()
        service.rekjørMigreringerMedFeiltype("TESTFEIL").also {
            assertThat(it).isEqualTo("Rekjørt 0 med feiltype=TESTFEIL")
        }

        verify(exactly = 0) { taskRepositoryMock.save(any()) }
    }

    @Test
    fun `Rekjør migrering på feiltype - oppretter 2 tasker når det er 2 med feiltype`() {
        every { migertsakRepository.findByStatusAndFeiltype(MigreringStatus.FEILET, "TESTFEIL") } returns listOf(
            Migrertsak(
                personIdent = "1"
            ),
            Migrertsak(personIdent = "2")
        )
        every { taskRepositoryMock.save(any()) } returns Task(type = "", payload = "")
        service.rekjørMigreringerMedFeiltype("TESTFEIL").also {
            assertThat(it).isEqualTo("Rekjørt 2 med feiltype=TESTFEIL")
        }

        verify(exactly = 2) { taskRepositoryMock.save(any()) }
    }

    @Test
    fun `Rekjør migrering på feiltype - oppretter 1 tasker når det er 2 med feiltype med samme feiltype og personident`() {
        every { migertsakRepository.findByStatusAndFeiltype(MigreringStatus.FEILET, "TESTFEIL") } returns listOf(
            Migrertsak(
                personIdent = "1"
            ),
            Migrertsak(personIdent = "1")
        )
        every { taskRepositoryMock.save(any()) } returns Task(type = "", payload = "")
        service.rekjørMigreringerMedFeiltype("TESTFEIL").also {
            assertThat(it).isEqualTo("Rekjørt 1 med feiltype=TESTFEIL")
        }

        verify(exactly = 1) { taskRepositoryMock.save(any()) }
    }

    @Test
    fun `Rekjør migrering på liste identer - ikke opprett task hvis input er empty`() {
        service.rekjørMigreringer(emptySet()).also {
            assertThat(it).isEqualTo("Rekjørt 0 migreringer")
        }

        verify(exactly = 0) { taskRepositoryMock.save(any()) }
    }

    @Test
    fun `Rekjør migrering på liste identer - ikke opprett task hvis input ikke har migrertsak`() {
        every { migertsakRepository.findByStatusAndPersonIdent(MigreringStatus.FEILET, "1") } returns emptyList()
        service.rekjørMigreringer(setOf("1")).also {
            assertThat(it).isEqualTo("Rekjørt 0 migreringer")
        }

        verify(exactly = 0) { taskRepositoryMock.save(any()) }
    }

    @Test
    fun `Rekjør migrering på liste identer - oppretter 2 tasker når det er 2 med feiltype`() {
        every { migertsakRepository.findByStatusAndPersonIdent(MigreringStatus.FEILET, "1") } returns listOf(Migrertsak())
        every { migertsakRepository.findByStatusAndPersonIdent(MigreringStatus.FEILET, "2") } returns listOf(Migrertsak())
        every { taskRepositoryMock.save(any()) } returns Task(type = "", payload = "")

        service.rekjørMigreringer(setOf("1", "2")).also {
            assertThat(it).isEqualTo("Rekjørt 2 migreringer")
        }

        verify(exactly = 2) { taskRepositoryMock.save(any()) }
    }

    @Test
    fun `Rekjør migrering på liste identer - oppretter 1 tasker når det er 2 med feiltype med samme feiltype og personident`() {
        every { migertsakRepository.findByStatusAndPersonIdent(MigreringStatus.FEILET, "1") } returns listOf(
            Migrertsak(
                personIdent = "1"
            ),
            Migrertsak(personIdent = "1")
        )
        every { taskRepositoryMock.save(any()) } returns Task(type = "", payload = "")
        service.rekjørMigreringer(setOf("1")).also {
            assertThat(it).isEqualTo("Rekjørt 1 migreringer")
        }

        verify(exactly = 1) { taskRepositoryMock.save(any()) }
    }

    companion object {
        private val GYLDIG_MIGRERINGSKJØRETIDSPUNKT: LocalDate = LocalDate.of(2022, 1, 1)
    }
}
