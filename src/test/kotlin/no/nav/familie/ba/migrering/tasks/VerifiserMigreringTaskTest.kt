package no.nav.familie.ba.migrering.tasks

import io.mockk.*
import no.nav.familie.ba.migrering.domain.JsonWrapper
import no.nav.familie.ba.migrering.domain.MigreringStatus
import no.nav.familie.ba.migrering.domain.Migrertsak
import no.nav.familie.ba.migrering.domain.MigrertsakRepository
import no.nav.familie.ba.migrering.integrasjoner.InfotrygdClient
import no.nav.familie.ba.migrering.integrasjoner.MigreringResponseDto
import no.nav.familie.kontrakter.ba.infotrygd.Stønad
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VerifiserMigreringTaskTest {
    val migrertsakRepositoryMock: MigrertsakRepository = mockk()
    val infotrygdClientMock: InfotrygdClient = mockk()

    @Test
    fun `skal oppdatere migrertsak med status VERIFISERT når stønad fra infotrygd har opphørsgrunn 5 og opphørtFom lik virkningFom i BA`() {
        every { infotrygdClientMock.hentStønad(any()) } returns Stønad(
            opphørsgrunn = "5", opphørtFom = YearMonth.now().format(
                DateTimeFormatter.ofPattern("MMyyyy")
            )
        )
        every { migrertsakRepositoryMock.findById(any()) } returns Optional.of(
            Migrertsak(
                personIdent = "12345678910",
                resultatFraBa = JsonWrapper.of(mockMigreringResponse.copy(virkningFom = YearMonth.now()))
            )
        )
        val statusSlotUpdate = slot<Migrertsak>()
        every { migrertsakRepositoryMock.update(capture(statusSlotUpdate)) } returns Migrertsak()

        VerifiserMigreringTask(infotrygdClientMock, migrertsakRepositoryMock).doTask(
            VerifiserMigreringTask.opprettTaskMedTriggerTid(UUID.randomUUID().toString(), properties = Properties())
        )
        assertThat(statusSlotUpdate.captured.status).isEqualTo(MigreringStatus.VERIFISERT)
    }

    @Test
    fun `skal feile hvis stønad id-data mangler eller stønad IKKE har opphørsgrunn 5`() {
        every { infotrygdClientMock.hentStønad(any()) } returns Stønad(opphørsgrunn = "5")
        every { migrertsakRepositoryMock.findById(any()) } returns Optional.of(
            Migrertsak(personIdent = "12345678910", resultatFraBa = JsonWrapper.of(mockMigreringResponse.copy(infotrygdTkNr = null)))
        )
        val statusSlotUpdate = slot<Migrertsak>()
        every { migrertsakRepositoryMock.update(capture(statusSlotUpdate)) } returns Migrertsak()

        assertThatThrownBy {
            VerifiserMigreringTask(infotrygdClientMock, migrertsakRepositoryMock).doTask(
                VerifiserMigreringTask.opprettTaskMedTriggerTid(UUID.randomUUID().toString(), properties = Properties())
            )
        }.hasMessageContaining("tkNr, iverksattFom, virkningFom og/eller region fra responseDto var null")

        every { infotrygdClientMock.hentStønad(any()) } returns Stønad(opphørsgrunn = "0")
        every { migrertsakRepositoryMock.findById(any()) } returns Optional.of(
            Migrertsak(personIdent = "12345678910", resultatFraBa = JsonWrapper.of(mockMigreringResponse))
        )

        assertThatThrownBy {
            VerifiserMigreringTask(infotrygdClientMock, migrertsakRepositoryMock).doTask(
                VerifiserMigreringTask.opprettTaskMedTriggerTid(UUID.randomUUID().toString(), properties = Properties())
            )
        }.hasMessageContaining("Opphørsgrunn")
    }

    @Test
    fun `skal feile hvis stønad opphørtFom fra Infotrygd er ulik virkningFom i BA`() {
        every { infotrygdClientMock.hentStønad(any()) } returns Stønad(opphørsgrunn = "5", opphørtFom = "000000")
        every { migrertsakRepositoryMock.findById(any()) } returns Optional.of(
            Migrertsak(personIdent = "12345678910", resultatFraBa = JsonWrapper.of(mockMigreringResponse))
        )
        val statusSlotUpdate = slot<Migrertsak>()
        every { migrertsakRepositoryMock.update(capture(statusSlotUpdate)) } returns Migrertsak()

        assertThatThrownBy {
            VerifiserMigreringTask(infotrygdClientMock, migrertsakRepositoryMock).doTask(
                VerifiserMigreringTask.opprettTaskMedTriggerTid(UUID.randomUUID().toString(), properties = Properties())
            )
        }.hasMessageContainingAll("OpphørtFom", "virkningFom")
    }

    companion object {

        private val mockMigreringResponse = MigreringResponseDto(
            fagsakId = 1,
            behandlingId = 2,
            infotrygdTkNr = "infotrygdTkNr",
            infotrygdIverksattFom = "infotrygdIverksattFom",
            infotrygdVirkningFom = "infotrygdVirkningFom",
            infotrygdRegion = "infotrygdRegion",
        )
    }
}
