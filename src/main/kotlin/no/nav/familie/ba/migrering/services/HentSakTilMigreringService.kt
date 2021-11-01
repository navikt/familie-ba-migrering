package no.nav.familie.ba.migrering.services

import no.nav.familie.ba.migrering.integrasjoner.InfotrygdClient
import no.nav.familie.ba.migrering.integrasjoner.MigreringRequest
import no.nav.familie.ba.migrering.tasks.MigreringTask
import no.nav.familie.ba.migrering.tasks.MigreringTaskDto
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class HentSakTilMigreringService(val infotrygdClient: InfotrygdClient, val taskRepository: TaskRepository) {

    @Scheduled(cron = "0 8 * * *")
    fun hentSakTilMigrering() {
        val personerForMigrering = infotrygdClient.hentPersonerKlareForMigrering(
            MigreringRequest(
                page = 1,
                size = MAX_PERSON_FOR_MIGRERING,
                valg = "OR",
                undervalg = "OS",
                maksAntallBarn = 1,
            )
        )

        if (personerForMigrering.size > MAX_PERSON_FOR_MIGRERING) {
            Log.error("For manger personer (${personerForMigrering.size}) avbryter migrering")
            return
        }

        Log.info("Fant ${personerForMigrering.size} personer for migrering")

        personerForMigrering.forEach {
            taskRepository.save(MigreringTask.opprettTask(MigreringTaskDto(it)))
        }
    }

    companion object {
        val Log = LoggerFactory.getLogger(HentSakTilMigreringService::class.java)
        val MAX_PERSON_FOR_MIGRERING = 10
    }
}
