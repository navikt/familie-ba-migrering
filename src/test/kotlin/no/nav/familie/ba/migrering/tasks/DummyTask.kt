package no.nav.familie.ba.migrering.tasks

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service


@Service
@TaskStepBeskrivelse(taskStepType = "dummy", beskrivelse = "Teste task")
class DummyTask: AsyncTaskStep {

    override fun doTask(task: Task) {
        println("Kjører $task")
    }


}