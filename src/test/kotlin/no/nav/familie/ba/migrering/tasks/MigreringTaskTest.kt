package no.nav.familie.ba.migrering.tasks

import io.mockk.*
import no.nav.familie.ba.migrering.domain.MigreringStatus
import no.nav.familie.ba.migrering.domain.Migrertsak
import no.nav.familie.ba.migrering.domain.MigrertsakRepository
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
        every { sakClientMock.migrerPerson(any()) } returns ""
        val statusSlot = slot<Migrertsak>()
        every { migrertsakRepositoryMock.insert(capture(statusSlot)) } returns Migrertsak()

        val personIdent = "ooo"
        MigreringTask(sakClientMock, migrertsakRepositoryMock).doTask(MigreringTask.opprettTask(MigreringTaskDto(personIdent)))

        assertThat(statusSlot.captured.status).isEqualTo(MigreringStatus.SUKKSESS)
        assertThat(statusSlot.captured.personIdent).isEqualTo(personIdent)
    }

    @Test
    fun `Skal insert row til migeringsstatus med status == FEILET og aarsak for feil hvis sakClient kast et unntak`() {
        val aarsak = "en god aarsak"
        every { sakClientMock.migrerPerson(any()) } throws Exception(aarsak)
        val statusSlot = slot<Migrertsak>()
        every { migrertsakRepositoryMock.insert(capture(statusSlot)) } returns Migrertsak()

        val personIdent = "ooo"
        MigreringTask(sakClientMock, migrertsakRepositoryMock).doTask(MigreringTask.opprettTask(MigreringTaskDto(personIdent)))

        assertThat(statusSlot.captured.status).isEqualTo(MigreringStatus.FEILET)
        assertThat(statusSlot.captured.aarsak).isEqualTo(aarsak)
        assertThat(statusSlot.captured.personIdent).isEqualTo(personIdent)
    }
}
