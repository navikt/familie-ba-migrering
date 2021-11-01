package no.nav.familie.ba.migrering.services

import no.nav.familie.ba.migrering.integrasjoner.InfotrygdClient
import no.nav.familie.ba.migrering.integrasjoner.MigreringRequest
import no.nav.familie.ba.migrering.tasks.MigreringTask
import no.nav.familie.ba.migrering.tasks.MigreringTaskDto
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class HentSakTilMigreringService(val infotrygdClient: InfotrygdClient) {

    @Scheduled(cron = "0 8 * * *")
    fun hentSakTilMigrering() {
        val personerForMigrering = infotrygdClient.hentPersonerKlareForMigrering(
            MigreringRequest(
                page = 1,
                size = 2,
                valg = "OR",
                undervalg = "OS",
                maksAntallBarn = 3,
            )
        )

        Log.info("Fant ${personerForMigrering.size} personer for migrering")

        personerForMigrering.forEach {
            MigreringTask.opprettTask(MigreringTaskDto(it))
        }
    }

    companion object {
        val Log = LoggerFactory.getLogger(HentSakTilMigreringService::class.java)
    }
}
