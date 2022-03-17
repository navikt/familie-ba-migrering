package no.nav.familie.ba.migrering.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

@SpringBootTest
@ActiveProfiles("dev")
@SpringJUnitConfig(classes = [HentSakTilMigreringScheduler::class, TestConfig::class])
class HentSakTilMigreringSchedulerTest {

    @Autowired
    lateinit var hentSakTilMigreringScheduler: HentSakTilMigreringScheduler

    @Autowired
    lateinit var hentSakTilMigreringService: HentSakTilMigreringService

    @Test
    fun `Skal migrere antallet ordinære saker definert for gjeldende profil`() {
        val slot = slot<Kategori>()

        every { hentSakTilMigreringService.migrer(any(), any(), capture(slot)) } returns ""

        hentSakTilMigreringScheduler.hentOrdinærSakTilMigreringScheduler()

        verify(exactly = 1) {
            hentSakTilMigreringService.migrer(3, any(), Kategori.ORDINÆR)
        }
    }

    @Test
    fun `Skal migrere antallet utvidete saker definert for gjeldende profil`() {
        val slot = slot<Kategori>()

        every { hentSakTilMigreringService.migrer(any(), any(), capture(slot)) } returns ""

        hentSakTilMigreringScheduler.hentUtvidetSakerTilMigreringScheduler()

        verify(exactly = 1) {
            hentSakTilMigreringService.migrer(3, any(), Kategori.UTVIDET)
        }
    }
}

@Configuration
class TestConfig {
    @Bean
    fun hentSakTilMigreringService(): HentSakTilMigreringService = mockk()
}