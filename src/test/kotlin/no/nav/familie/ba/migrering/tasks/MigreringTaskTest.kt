package no.nav.familie.ba.migrering.tasks

import io.mockk.*
import no.nav.familie.ba.migrering.domain.MigreringStatus
import no.nav.familie.ba.migrering.domain.Migrertsak
import no.nav.familie.ba.migrering.domain.MigrertsakRepository
import no.nav.familie.ba.migrering.integrasjoner.MigreringResponseDto
import no.nav.familie.ba.migrering.integrasjoner.SakClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MigreringTaskTest {
    val migrertsakRepositoryMock: MigrertsakRepository = mockk()
    val sakClientMock: SakClient = mockk()

    @Test
    fun `Skal insert row til migeringsstatus med status == SUKKESS hvis sakClient ikke kast et unntak`() {
        val sakNummer: Long = 3
        every { sakClientMock.migrerPerson(any()) } returns MigreringResponseDto(1, 2, 0, sakNummer)
        val statusSlotInsert = slot<Migrertsak>()
        val statusSlotUpdate = slot<Migrertsak>()
        every { migrertsakRepositoryMock.insert(capture(statusSlotInsert)) } returns Migrertsak()
        every { migrertsakRepositoryMock.update(capture(statusSlotUpdate)) } returns Migrertsak()

        val personIdent = "ooo"
        MigreringTask(sakClientMock, migrertsakRepositoryMock).doTask(
            MigreringTask.opprettTask(
                MigreringTaskDto(
                    personIdent = personIdent
                )
            )
        )

        assertThat(statusSlotInsert.captured.status).isEqualTo(MigreringStatus.UKJENT)
        assertThat(statusSlotInsert.captured.personIdent).isEqualTo(personIdent)
        assertThat(statusSlotInsert.captured.sakNummer).isEmpty()

        assertThat(statusSlotUpdate.captured.status).isEqualTo(MigreringStatus.MIGRERT_I_BA)
        assertThat(statusSlotUpdate.captured.personIdent).isEqualTo(personIdent)
        assertThat(statusSlotUpdate.captured.sakNummer).isEqualTo(sakNummer.toString())
    }

    @Test
    fun `Skal insert row til migeringsstatus med status == FEILET og aarsak for feil hvis sakClient kast et unntak`() {
        val aarsak = "en god aarsak"
        every { sakClientMock.migrerPerson(any()) } throws Exception(aarsak)
        val statusSlotInsert = slot<Migrertsak>()
        val statusSlotUpdate = slot<Migrertsak>()

        every { migrertsakRepositoryMock.insert(capture(statusSlotInsert)) } returns Migrertsak()
        every { migrertsakRepositoryMock.update(capture(statusSlotUpdate)) } returns Migrertsak()

        val personIdent = "ooo"
        MigreringTask(sakClientMock, migrertsakRepositoryMock).doTask(
            MigreringTask.opprettTask(
                MigreringTaskDto(
                    personIdent = personIdent
                )
            )
        )

        assertThat(statusSlotInsert.captured.status).isEqualTo(MigreringStatus.UKJENT)
        assertThat(statusSlotInsert.captured.personIdent).isEqualTo(personIdent)
        assertThat(statusSlotInsert.captured.sakNummer).isEmpty()

        assertThat(statusSlotUpdate.captured.status).isEqualTo(MigreringStatus.FEILET)
        assertThat(statusSlotUpdate.captured.aarsak).isEqualTo(aarsak)
        assertThat(statusSlotUpdate.captured.personIdent).isEqualTo(personIdent)
        assertThat(statusSlotUpdate.captured.sakNummer).isEmpty()
    }
}
