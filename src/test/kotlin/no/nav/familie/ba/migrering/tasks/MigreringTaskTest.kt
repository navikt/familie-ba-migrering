package no.nav.familie.ba.migrering.tasks

import io.mockk.*
import no.nav.familie.ba.migrering.domain.MigreringStatus
import no.nav.familie.ba.migrering.domain.Migrertsak
import no.nav.familie.ba.migrering.domain.MigrertsakJDBCRepository
import no.nav.familie.ba.migrering.domain.MigrertsakRepository
import no.nav.familie.ba.migrering.integrasjoner.SakClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MigreringTaskTest {
    val migrertsakJDBCRepositoryMock: MigrertsakJDBCRepository = mockk()
    val sakClientMock: SakClient = mockk()


    @Disabled
    @Test
    fun `Skal insert row til migeringsstatus med status == SUKKESS hvis sakClient ikke kast et unntak`() {
        every { sakClientMock.migrerPerson(any()) } returns ""
        val statusSlotInsert = slot<Migrertsak>()
        val statusSlotUpdate = slot<Migrertsak>()
        every { migrertsakJDBCRepositoryMock.lagre(capture(statusSlotInsert)) } returns Migrertsak()
        //every { migrertsakJDBCRepositoryMock.oppdaterStatusÅrsakOgResultat(capture(statusSlotUpdate)) } returns Migrertsak()

        val personIdent = "ooo"
        MigreringTask(sakClientMock, migrertsakJDBCRepositoryMock).doTask(MigreringTask.opprettTask(MigreringTaskDto(personIdent)))

        assertThat(statusSlotInsert.captured.status).isEqualTo(MigreringStatus.UKJENT)
        assertThat(statusSlotInsert.captured.personIdent).isEqualTo(personIdent)

    }

    @Disabled
    @Test
    fun `Skal insert row til migeringsstatus med status == FEILET og aarsak for feil hvis sakClient kast et unntak`() {
        val aarsak = "en god aarsak"
        every { sakClientMock.migrerPerson(any()) } throws Exception(aarsak)
        val statusSlotInsert = slot<Migrertsak>()
        val statusSlotUpdate = slot<UUID>()

        every { migrertsakJDBCRepositoryMock.lagre(capture(statusSlotInsert)) } returns Migrertsak()
        //every { migrertsakJDBCRepositoryMock.oppdaterStatusÅrsakOgResultat(capture(statusSlotUpdate)) } returns Migrertsak()

        val personIdent = "ooo"
        MigreringTask(sakClientMock, migrertsakJDBCRepositoryMock).doTask(MigreringTask.opprettTask(MigreringTaskDto(personIdent)))

        assertThat(statusSlotInsert.captured.status).isEqualTo(MigreringStatus.UKJENT)
        assertThat(statusSlotInsert.captured.personIdent).isEqualTo(personIdent)


    }
}
