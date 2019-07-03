package circlet.plugins.pipelines.services.execution

import circlet.pipelines.engine.*
import circlet.pipelines.engine.api.*
import circlet.pipelines.engine.storage.*
import libraries.klogging.*
import runtime.*
import runtime.reactive.*

data class DummyContainer(val lifetimeSource: LifetimeSource)

class CircletIdeaJobExecutionProvider(private val lifetime: Lifetime, private val logCallback: (String) -> Unit) : JobExecutionProvider<CircletIdeaGraphStorageTransaction> {

    companion object : KLogging()

    private val runningJobs = mutableMapOf<Long, DummyContainer>()

    private var savedHandler: ((tx: CircletIdeaGraphStorageTransaction, job: AJobExecutionEntity<*>, newStatus: ExecutionStatus) -> Unit)? = null

    override fun scheduleExecution(tx: CircletIdeaGraphStorageTransaction, jobs: Iterable<AJobExecutionEntity<*>>) {
        jobs.forEach { job ->
            when (job) {
                is CircletIdeaAJobExecutionEntity -> {
                    val jobId = job.id
                    val image = job.meta.image
                    logCallback("prepare to run: image=$image, id=$jobId")
                    val jobLifetimeSource = lifetime.nested()

                    val dummyContainer = DummyContainer(jobLifetimeSource)
                    runningJobs[jobId] = dummyContainer
                    changeState(tx, job, ExecutionStatus.RUNNING)
                    var counter = 0

                    val timer = UiDispatch.dispatchInterval(1000) {
                        logCallback("run dummy container '$image'. counter = ${counter++}")
                        if (counter == 3) {
                            changeState(tx, job, generateFinalState(image))
                            jobLifetimeSource.terminate()
                        }
                    }
                    jobLifetimeSource.add {
                        runningJobs.remove(jobId)?.lifetimeSource?.terminate()
                        timer.cancel()
                    }

                }
                else -> error("unknown job $job")
            }
        }
    }

    override fun scheduleTermination(jobs: Iterable<AJobExecutionEntity<*>>) {
        TODO("scheduleTermination not implemented")
    }

    override fun subscribeIdempotently(handler: (tx: CircletIdeaGraphStorageTransaction, job: AJobExecutionEntity<*>, newStatus: ExecutionStatus) -> Unit) {
        if (savedHandler != null) {
            logger.warn { "subscribeIdempotently. savedHandler != null" }
        }
        this.savedHandler = handler
    }

    override fun onBeforeGraphStatusChanged(tx: CircletIdeaGraphStorageTransaction, entity: AGraphExecutionEntity, oldStatus: ExecutionStatus, newStatus: ExecutionStatus) {
        TODO("onBeforeGraphStatusChanged not implemented")
    }

    override fun onBeforeJobStatusChanged(tx: CircletIdeaGraphStorageTransaction, entity: AJobExecutionEntity<*>, oldStatus: ExecutionStatus, newStatus: ExecutionStatus) {
        TODO("onBeforeJobStatusChanged not implemented")
    }

    private fun changeState(tx: CircletIdeaGraphStorageTransaction, job: AJobExecutionEntity<*>, newStatus: ExecutionStatus) {
        job.status = newStatus
        savedHandler!!(tx, job, newStatus)
    }

    private fun generateFinalState(imageName: String) : ExecutionStatus {
        if (imageName.endsWith("_toFail")) {
            return ExecutionStatus.FAILED
        }
        return ExecutionStatus.SUCCEEDED
    }
}
