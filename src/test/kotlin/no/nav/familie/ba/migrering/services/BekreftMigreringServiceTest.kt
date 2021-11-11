package no.nav.familie.ba.migrering.services

import io.mockk.*
import no.nav.familie.ba.migrering.domain.JsonWrapper
import no.nav.familie.ba.migrering.domain.MigreringStatus
import no.nav.familie.ba.migrering.domain.Migrertsak
import no.nav.familie.ba.migrering.domain.MigrertsakRepository
import no.nav.familie.ba.migrering.integrasjoner.InfotrygdClient
import no.nav.familie.ba.migrering.integrasjoner.MigreringResponseDto
import no.nav.familie.kontrakter.ba.infotrygd.Stønad
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BekreftMigreringServiceTest {

    @Test
    fun `Skal oppdatere status for verifiserte Migrertsaker`() {
        val infotrygdClientMock: InfotrygdClient = mockk()
        val migrertsakRepositoryMock: MigrertsakRepository = mockk()

        val migreringsakId = UUID.randomUUID()
        val stønadId: Long = 3
        val stønadIdSlot = slot<Long>()
        every { infotrygdClientMock.hentStønadFraId(capture(stønadIdSlot)) } returns Stønad(opphørsgrunn = "5")
        val statusForMigrertsakSlot = slot<MigreringStatus>()
        every { migrertsakRepositoryMock.findByStatus(capture(statusForMigrertsakSlot)) } returns listOf(
            Migrertsak(
                id = migreringsakId,
                resultatFraBa = JsonWrapper.of(
                    MigreringResponseDto(1, 2, stønadId, 4)
                )
            )
        )
        val migrertsakSlot = slot<Migrertsak>()
        every { migrertsakRepositoryMock.update(capture(migrertsakSlot)) } returns Migrertsak()
        every { migrertsakRepositoryMock.findAllById(any()) } returns listOf(Migrertsak(status = MigreringStatus.VERIFISERT))

        BekreftMigreringService(infotrygdClientMock, migrertsakRepositoryMock).bekreftMigrering()

        verify(exactly = 1) {
            migrertsakRepositoryMock.findByStatus(any())
        }
        verify(exactly = 1) {
            infotrygdClientMock.hentStønadFraId(any())
        }
        verify(exactly = 1) {
            migrertsakRepositoryMock.update(any())
        }

        assertThat(stønadIdSlot.captured).isEqualTo(stønadId)
        assertThat(statusForMigrertsakSlot.captured).isEqualTo(MigreringStatus.MIGRERT_I_BA)
        assertThat(migrertsakSlot.captured.status).isEqualTo(MigreringStatus.VERIFISERT)
        assertThat(migrertsakSlot.captured.id).isEqualTo(migreringsakId)
    }

    @Test
    fun `Skal sette status til VERIFISERING_FEILET hvis responseDto fra Infotrygd ikke har riktig opphørsgrunn`() {
        val infotrygdClientMock: InfotrygdClient = mockk()
        val migrertsakRepositoryMock: MigrertsakRepository = mockk()

        val suksessMigreringsakId = UUID.randomUUID()
        val feiletMigreringsakId = UUID.randomUUID()

        val suksessStønadId: Long = 3
        val feiletStønadId: Long = 13

        every { infotrygdClientMock.hentStønadFraId(suksessStønadId) } returns Stønad(opphørsgrunn = "5")
        every { infotrygdClientMock.hentStønadFraId(feiletStønadId) } returns Stønad(opphørsgrunn = "0")
        every { migrertsakRepositoryMock.findAllById(any()) } returns listOf(
            Migrertsak(status = MigreringStatus.VERIFISERT),
            Migrertsak(status = MigreringStatus.VERIFISERING_FEILET, aarsak = "aarsak")
        )

        every { migrertsakRepositoryMock.findByStatus(any()) } returns listOf(
            Migrertsak(
                id = suksessMigreringsakId,
                resultatFraBa = JsonWrapper.of(
                    MigreringResponseDto(1, 2, suksessStønadId, 4)
                )
            ),
            Migrertsak(
                id = feiletMigreringsakId,
                resultatFraBa = JsonWrapper.of(
                    MigreringResponseDto(1, 2, feiletStønadId, 4)
                )
            )
        )

        val migrertsakSlot: MutableList<Migrertsak> = mutableListOf()
        every { migrertsakRepositoryMock.update(capture(migrertsakSlot)) } returns Migrertsak()

        BekreftMigreringService(infotrygdClientMock, migrertsakRepositoryMock).bekreftMigrering()

        verify(exactly = 1) {
            migrertsakRepositoryMock.findByStatus(any())
        }
        verify(exactly = 2) {
            infotrygdClientMock.hentStønadFraId(any())
        }
        verify(exactly = 2) {
            migrertsakRepositoryMock.update(any())
        }

        assertThat(migrertsakSlot).hasSize(2)
        assertThat(migrertsakSlot.find { it.id == suksessMigreringsakId }!!.status).isEqualTo(MigreringStatus.VERIFISERT)
        assertThat(migrertsakSlot.find { it.id == feiletMigreringsakId }!!.status).isEqualTo(MigreringStatus.VERIFISERING_FEILET)
    }
}
