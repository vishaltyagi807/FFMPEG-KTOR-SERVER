package dev.varshit.factory

import dev.varshit.models.Task
import dev.varshit.plugins.Redis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.serialization.json.Json

object TaskFactory {

    private val factory = FFMPEGFactory()

    fun startTask(task: Task, scope: CoroutineScope): Job {
        return factory.processTask(task, scope)
    }

    suspend fun addTask(task: Task): String = Redis.addData(task)

    suspend fun deleteAllTasks() = Redis.deleteAllTasks()

    suspend fun getAllTasks(): List<Task> = Redis.getAllTasks()

    fun cancelCurrentTask() = factory.cancelCurrentTask()

    fun getTask(value: String): Task {
        return Json.decodeFromString(Task.serializer(), value)
    }

}