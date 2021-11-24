package no.nav.familie.ba.migrering.tasks

import io.mockk.*
import no.nav.familie.ba.migrering.domain.MigreringStatus
import no.nav.familie.ba.migrering.domain.Migrertsak
import no.nav.familie.ba.migrering.domain.MigrertsakRepository
import no.nav.familie.ba.migrering.integrasjoner.MigreringResponseDto
import no.nav.familie.ba.migrering.integrasjoner.SakClient
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MigreringTaskTest {
    val migrertsakRepositoryMock: MigrertsakRepository = mockk()
    val sakClientMock: SakClient = mockk()
    val taskRepository: TaskRepository = mockk()

    @Test
    fun `Skal insert row til migeringsstatus med status == SUKKESS hvis sakClient ikke kast et unntak`() {
        every { sakClientMock.migrerPerson(any()) } returns MigreringResponseDto(1, 2)
        val statusSlotInsert = slot<Migrertsak>()
        val statusSlotUpdate = slot<Migrertsak>()
        every { migrertsakRepositoryMock.findByStatusAndPersonIdent(MigreringStatus.UKJENT, "ooo") } returns emptyList()
        every { migrertsakRepositoryMock.insert(capture(statusSlotInsert)) } returns Migrertsak()
        every { migrertsakRepositoryMock.update(capture(statusSlotUpdate)) } returns Migrertsak()
        every { taskRepository.save(any()) } returns VerifiserMigreringTask.opprettTaskMedTriggerTid("1")

        val personIdent = "ooo"
        MigreringTask(sakClientMock, migrertsakRepositoryMock, taskRepository).doTask(
            MigreringTask.opprettTask(
                MigreringTaskDto(
                    personIdent = personIdent
                )
            )
        )

        assertThat(statusSlotInsert.captured.status).isEqualTo(MigreringStatus.UKJENT)
        assertThat(statusSlotInsert.captured.personIdent).isEqualTo(personIdent)

        assertThat(statusSlotUpdate.captured.status).isEqualTo(MigreringStatus.MIGRERT_I_BA)
        assertThat(statusSlotUpdate.captured.personIdent).isEqualTo(personIdent)
    }

    @Test
    fun `Skal insert row til migeringsstatus med status == FEILET og aarsak for feil hvis sakClient kast et unntak`() {
        val aarsak = "en god aarsak"
        every { sakClientMock.migrerPerson(any()) } throws Exception(aarsak)
        val statusSlotInsert = slot<Migrertsak>()
        val statusSlotUpdate = slot<Migrertsak>()

        every { migrertsakRepositoryMock.findByStatusAndPersonIdent(MigreringStatus.UKJENT, "ooo") } returns emptyList()
        every { migrertsakRepositoryMock.insert(capture(statusSlotInsert)) } returns Migrertsak()
        every { migrertsakRepositoryMock.update(capture(statusSlotUpdate)) } returns Migrertsak()

        val personIdent = "ooo"
        MigreringTask(sakClientMock, migrertsakRepositoryMock, taskRepository).doTask(
            MigreringTask.opprettTask(
                MigreringTaskDto(
                    personIdent = personIdent
                )
            )
        )

        assertThat(statusSlotInsert.captured.status).isEqualTo(MigreringStatus.UKJENT)
        assertThat(statusSlotInsert.captured.personIdent).isEqualTo(personIdent)

        assertThat(statusSlotUpdate.captured.status).isEqualTo(MigreringStatus.FEILET)
        assertThat(statusSlotUpdate.captured.aarsak).isEqualTo(aarsak)
        assertThat(statusSlotUpdate.captured.personIdent).isEqualTo(personIdent)
    }


    @Test
    fun `Skal gjenbruke rad med status == UKJENT ved migrering`() {
        val aarsak = "en god aarsak"
        every { sakClientMock.migrerPerson(any()) } throws Exception(aarsak)
        val statusSlotUpdate = slot<Migrertsak>()

        val uuidGammelSak = UUID.randomUUID()
        every { migrertsakRepositoryMock.findByStatusAndPersonIdent(MigreringStatus.UKJENT, "ooo") } returns listOf(Migrertsak(id = uuidGammelSak))
        every { migrertsakRepositoryMock.update(capture(statusSlotUpdate)) } returns Migrertsak()

        val personIdent = "ooo"
        MigreringTask(sakClientMock, migrertsakRepositoryMock, taskRepository).doTask(
            MigreringTask.opprettTask(
                MigreringTaskDto(
                    personIdent = personIdent
                )
            )
        )
        assertThat(statusSlotUpdate.captured.id).isEqualTo(uuidGammelSak)
        assertThat(statusSlotUpdate.captured.status).isEqualTo(MigreringStatus.FEILET)
        assertThat(statusSlotUpdate.captured.aarsak).isEqualTo(aarsak)
        assertThat(statusSlotUpdate.captured.personIdent).isEqualTo(personIdent)
    }
}
