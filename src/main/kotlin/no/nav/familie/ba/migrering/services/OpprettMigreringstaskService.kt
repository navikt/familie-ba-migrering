package no.nav.familie.ba.migrering.services

import no.nav.familie.ba.migrering.tasks.MigreringTask
import no.nav.familie.ba.migrering.tasks.MigreringTaskDto
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class OpprettMigreringstaskService(
    val taskRepository: TaskRepository
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun opprettMigreringtask(personident: String) {
        taskRepository.save(MigreringTask.opprettTask(MigreringTaskDto(personident)))
        secureLogger.info("Oppretter MigreringTask for $personident")
    }


    companion object {

        private val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
