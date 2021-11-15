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
import org.springframework.format.annotation.DateTimeFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Optional
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VerifiserMigreringTaskTest {
    val migrertsakRepositoryMock: MigrertsakRepository = mockk()
    val infotrygdClientMock: InfotrygdClient = mockk()

    @Test
    fun `skal oppdatere migrertsak med status VERIFISERT når stønad fra infotrygd har opphørsgrunn 5 og opphørtFom lik virkningFom i BA`() {
        every { infotrygdClientMock.hentStønadFraId(any()) } returns Stønad(
            opphørsgrunn = "5", opphørtFom = YearMonth.now().format(
                DateTimeFormatter.ofPattern("MMyyyy")
            )
        )
        every { migrertsakRepositoryMock.findById(any()) } returns Optional.of(
            Migrertsak(
                personIdent = "12345678910", resultatFraBa = JsonWrapper.of(
                    MigreringResponseDto(1, 2, 3, virkningFom = YearMonth.now())
                )
            )
        )
        val statusSlotUpdate = slot<Migrertsak>()
        every { migrertsakRepositoryMock.update(capture(statusSlotUpdate)) } returns Migrertsak()

        VerifiserMigreringTask(infotrygdClientMock, migrertsakRepositoryMock).doTask(
            VerifiserMigreringTask.opprettTaskMedTriggerTid(UUID.randomUUID().toString())
        )
        assertThat(statusSlotUpdate.captured.status).isEqualTo(MigreringStatus.VERIFISERT)
    }

    @Test
    fun `skal feile hvis stønadId mangler eller stønad IKKE har opphørsgrunn 5`() {
        every { infotrygdClientMock.hentStønadFraId(any()) } returns Stønad(opphørsgrunn = "5")
        every { migrertsakRepositoryMock.findById(any()) } returns Optional.of(
            Migrertsak(personIdent = "12345678910", resultatFraBa = JsonWrapper.of(MigreringResponseDto(1, 2, null)))
        )
        val statusSlotUpdate = slot<Migrertsak>()
        every { migrertsakRepositoryMock.update(capture(statusSlotUpdate)) } returns Migrertsak()

        assertThatThrownBy {
            VerifiserMigreringTask(infotrygdClientMock, migrertsakRepositoryMock).doTask(
                VerifiserMigreringTask.opprettTaskMedTriggerTid(UUID.randomUUID().toString())
            )
        }.hasMessageContaining("infotrygdStønadId")

        every { infotrygdClientMock.hentStønadFraId(any()) } returns Stønad(opphørsgrunn = "0")
        every { migrertsakRepositoryMock.findById(any()) } returns Optional.of(
            Migrertsak(personIdent = "12345678910", resultatFraBa = JsonWrapper.of(MigreringResponseDto(1, 2, 3)))
        )

        assertThatThrownBy {
            VerifiserMigreringTask(infotrygdClientMock, migrertsakRepositoryMock).doTask(
                VerifiserMigreringTask.opprettTaskMedTriggerTid(UUID.randomUUID().toString())
            )
        }.hasMessageContaining("Opphørsgrunn")
    }

    @Test
    fun `skal feile hvis stønad opphørtFom fra Infotrygd er ulik virkningFom i BA`() {
        every { infotrygdClientMock.hentStønadFraId(any()) } returns Stønad(opphørsgrunn = "5", opphørtFom = "000000")
        every { migrertsakRepositoryMock.findById(any()) } returns Optional.of(
            Migrertsak(personIdent = "12345678910", resultatFraBa = JsonWrapper.of(MigreringResponseDto(1, 2, 3)))
        )
        val statusSlotUpdate = slot<Migrertsak>()
        every { migrertsakRepositoryMock.update(capture(statusSlotUpdate)) } returns Migrertsak()

        assertThatThrownBy {
            VerifiserMigreringTask(infotrygdClientMock, migrertsakRepositoryMock).doTask(
                VerifiserMigreringTask.opprettTaskMedTriggerTid(UUID.randomUUID().toString())
            )
        }.hasMessageContainingAll("OpphørtFom", "virkningFom")
    }
}
