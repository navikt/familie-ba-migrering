package no.nav.familie.ba.migrering.tasks

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.migrering.domain.MigreringStatus
import no.nav.familie.ba.migrering.domain.Migrertsak
import no.nav.familie.ba.migrering.domain.MigrertsakLogg
import no.nav.familie.ba.migrering.domain.MigrertsakLoggRepository
import no.nav.familie.ba.migrering.domain.MigrertsakRepository
import no.nav.familie.ba.migrering.integrasjoner.InfotrygdClient
import no.nav.familie.ba.migrering.integrasjoner.MigreringResponseDto
import no.nav.familie.ba.migrering.integrasjoner.SakClient
import no.nav.familie.ba.migrering.rest.MigreringsfeilType.ÅPEN_SAK_TIL_BESLUTNING_I_INFOTRYGD
import no.nav.familie.ba.migrering.services.OpprettTaskService
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.Properties
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MigreringTaskTest {

    private val migrertsakRepositoryMock: MigrertsakRepository = mockk()
    private val migrertsakLoggRepositoryMock: MigrertsakLoggRepository = mockk()
    private val sakClientMock: SakClient = mockk()
    private val infotrygdClient: InfotrygdClient = mockk(relaxed = true)
    private val taskService: TaskService = mockk()

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }
    @Test
    fun `Skal insert row til migeringsstatus med status == SUKKESS hvis sakClient ikke kast et unntak`() {
        every { sakClientMock.migrerPerson(any()) } returns MigreringResponseDto(1, 2)
        val statusSlotInsert = slot<Migrertsak>()
        val statusSlotUpdate = slot<Migrertsak>()
        val slotTask = slot<Task>()
        every { migrertsakRepositoryMock.findByStatusInAndPersonIdentOrderByMigreringsdato(any(), "ooo") } returns emptyList()
        every { migrertsakRepositoryMock.insert(capture(statusSlotInsert)) } returns Migrertsak(personIdent = "ooo")
        every { migrertsakRepositoryMock.update(capture(statusSlotUpdate)) } returns Migrertsak()
        every { taskService.save(capture(slotTask)) } returns VerifiserMigreringTask.opprettTaskMedTriggerTid(
            "1",
            properties = Properties()
        )

        val personIdent = "ooo"
        MigreringTask(
            sakClientMock,
            infotrygdClient,
            migrertsakRepositoryMock,
            migrertsakLoggRepositoryMock,
            taskService,
            OpprettTaskService(taskService)
        ).doTask(
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

        assertThat(slotTask.captured.type).isEqualTo(VerifiserMigreringTask.TASK_STEP_TYPE)

        assertThat(slotTask.captured.metadata["personIdent"]).isEqualTo("ooo")
        assertThat(slotTask.captured.metadata["fagsakId"]).isEqualTo("1")
        assertThat(slotTask.captured.metadata["behandlingId"]).isEqualTo("2")
    }

    @Test
    fun `Skal insert row til migeringsstatus med status == FEILET og aarsak for feil hvis sakClient kast et unntak`() {
        val aarsak = "en god aarsak"
        every { sakClientMock.migrerPerson(any()) } throws Exception(aarsak)
        val statusSlotInsert = slot<Migrertsak>()
        val statusSlotUpdate = slot<Migrertsak>()

        every { migrertsakRepositoryMock.findByStatusInAndPersonIdentOrderByMigreringsdato(any(), "ooo") } returns emptyList()
        every { migrertsakRepositoryMock.insert(capture(statusSlotInsert)) } returns Migrertsak()
        every { migrertsakRepositoryMock.update(capture(statusSlotUpdate)) } returns Migrertsak()
        every { migrertsakLoggRepositoryMock.insert(any()) } returns MigrertsakLogg.tilMigrertsakLogg(Migrertsak(UUID.randomUUID()))

        val personIdent = "ooo"
        MigreringTask(
            sakClientMock,
            infotrygdClient,
            migrertsakRepositoryMock,
            migrertsakLoggRepositoryMock,
            taskService,
            OpprettTaskService(taskService)
        ).doTask(
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
    fun `Skal trigge en ny migrering med fire and forge og feil inne i den påvirker ikke gjeldende task`() {
        val aarsak = "en god aarsak"
        every { sakClientMock.migrerPerson(any()) } throws Exception(aarsak)
        val statusSlotInsert = slot<Migrertsak>()
        val statusSlotUpdate = slot<Migrertsak>()

        every {
            migrertsakRepositoryMock.findByStatusInAndPersonIdentOrderByMigreringsdato(
                listOf(
                    MigreringStatus.UKJENT,
                    MigreringStatus.FEILET
                ),
                "ooo"
            )
        } returns emptyList()
        every { migrertsakRepositoryMock.insert(capture(statusSlotInsert)) } returns Migrertsak()
        every { migrertsakRepositoryMock.update(capture(statusSlotUpdate)) } returns Migrertsak()

        val personIdent = "ooo"
        MigreringTask(
            sakClientMock,
            infotrygdClient,
            migrertsakRepositoryMock,
            migrertsakLoggRepositoryMock,
            taskService,
            OpprettTaskService(taskService)
        ).doTask(
            MigreringTask.opprettTask(
                MigreringTaskDto(
                    personIdent = personIdent
                )
            )
        ) // do task skal ikke kaste feil fordi feilen skjer i egen context

        assertThat(statusSlotUpdate.captured.status).isEqualTo(MigreringStatus.FEILET)
    }

    @Test
    fun `Skal gjenbruke rad med status == UKJENT ved migrering`() {
        val aarsak = "en god aarsak"
        every { sakClientMock.migrerPerson(any()) } throws Exception(aarsak)
        val statusSlotUpdate = slot<Migrertsak>()
        val taskSlot = slot<Task>()

        val uuidGammelSak = UUID.randomUUID()
        every {
            migrertsakRepositoryMock.findByStatusInAndPersonIdentOrderByMigreringsdato(
                listOf(MigreringStatus.UKJENT, MigreringStatus.FEILET), "ooo"
            )
        } returns listOf(Migrertsak(id = uuidGammelSak))
        every { migrertsakRepositoryMock.update(capture(statusSlotUpdate)) } returns Migrertsak()

        val personIdent = "ooo"
        val task = MigreringTask.opprettTask(
            MigreringTaskDto(
                personIdent = personIdent
            )
        )
        every { taskService.save(capture(taskSlot)) } returns task
        every { migrertsakLoggRepositoryMock.insert(any()) } returns MigrertsakLogg.tilMigrertsakLogg(Migrertsak(id = uuidGammelSak))

        MigreringTask(
            sakClientMock,
            infotrygdClient,
            migrertsakRepositoryMock,
            migrertsakLoggRepositoryMock,
            taskService,
            OpprettTaskService(taskService)
        ).doTask(
            task
        )
        assertThat(statusSlotUpdate.captured.id).isEqualTo(uuidGammelSak)
        assertThat(statusSlotUpdate.captured.status).isEqualTo(MigreringStatus.FEILET)
        assertThat(statusSlotUpdate.captured.aarsak).isEqualTo(aarsak)
        assertThat(statusSlotUpdate.captured.personIdent).isEqualTo(personIdent)
    }

    @Test
    fun `Skal logge forrige kjøring til database`() {
        val aarsak = "en god aarsak"
        every { sakClientMock.migrerPerson(any()) } throws Exception(aarsak)
        val personIdent = "ooo"

        val migrertSak = Migrertsak(
            id = UUID.randomUUID(),
            personIdent = personIdent,
            status = MigreringStatus.UKJENT,
            feiltype = "TEST",
            aarsak = "Noe galt"
        )
        every {
            migrertsakRepositoryMock.findByStatusInAndPersonIdentOrderByMigreringsdato(
                listOf(MigreringStatus.UKJENT, MigreringStatus.FEILET), "ooo"
            )
        } returns listOf(migrertSak)
        every { migrertsakRepositoryMock.update(any()) } returns migrertSak

        val task = MigreringTask.opprettTask(
            MigreringTaskDto(
                personIdent = personIdent
            )
        )
        every { taskService.save(any()) } returns task
        val migrertsakLoggSlot = slot<MigrertsakLogg>()
        every { migrertsakLoggRepositoryMock.insert(capture(migrertsakLoggSlot)) } returns MigrertsakLogg.tilMigrertsakLogg(migrertSak)

        MigreringTask(
            sakClientMock,
            infotrygdClient,
            migrertsakRepositoryMock,
            migrertsakLoggRepositoryMock,
            taskService,
            OpprettTaskService(taskService)
        ).doTask(
            task
        )

        assertThat(migrertsakLoggSlot.captured.id).isEqualTo(migrertSak.id)
        assertThat(migrertsakLoggSlot.captured.personIdent).isEqualTo(migrertSak.personIdent)
        assertThat(migrertsakLoggSlot.captured.status).isEqualTo(migrertSak.status)
        assertThat(migrertsakLoggSlot.captured.feiltype).isEqualTo(migrertSak.feiltype)
        assertThat(migrertsakLoggSlot.captured.aarsak).isEqualTo(migrertSak.aarsak)
        assertThat(migrertsakLoggSlot.captured.migreringsdato).isEqualTo(migrertSak.migreringsdato)
    }

    @Test
    fun `Skal sette migeringsstatus til FEILET når bruker har åpen sak i Infotrygd, og sette feiltype og aarsak fra tilhørende enum`() {
        every { infotrygdClient.harÅpenSak(any()) } returns true
        val statusSlotInsert = slot<Migrertsak>()
        val statusSlotUpdate = slot<Migrertsak>()

        every { migrertsakRepositoryMock.findByStatusInAndPersonIdentOrderByMigreringsdato(any(), "ooo") } returns emptyList()
        every { migrertsakRepositoryMock.insert(capture(statusSlotInsert)) } returns Migrertsak()
        every { migrertsakRepositoryMock.update(capture(statusSlotUpdate)) } returns Migrertsak()
        every { migrertsakLoggRepositoryMock.insert(any()) } returns MigrertsakLogg.tilMigrertsakLogg(Migrertsak(UUID.randomUUID()))

        val personIdent = "ooo"
        MigreringTask(
            sakClientMock,
            infotrygdClient,
            migrertsakRepositoryMock,
            migrertsakLoggRepositoryMock,
            taskService,
            OpprettTaskService(taskService)
        ).doTask(
            MigreringTask.opprettTask(
                MigreringTaskDto(
                    personIdent = personIdent
                )
            )
        )

        assertThat(statusSlotInsert.captured.status).isEqualTo(MigreringStatus.UKJENT)
        assertThat(statusSlotInsert.captured.personIdent).isEqualTo(personIdent)

        assertThat(statusSlotUpdate.captured.status).isEqualTo(MigreringStatus.FEILET)
        assertThat(statusSlotUpdate.captured.feiltype).isEqualTo(ÅPEN_SAK_TIL_BESLUTNING_I_INFOTRYGD.name)
        assertThat(statusSlotUpdate.captured.aarsak).isEqualTo(ÅPEN_SAK_TIL_BESLUTNING_I_INFOTRYGD.beskrivelse)
    }
}
