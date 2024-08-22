package dev.varshit.factory

import dev.varshit.models.Task
import dev.varshit.plugins.Redis
import kotlinx.serialization.json.Json

object TaskFactory {

    suspend fun startTask(task: Task) {
        val factory = FFMPEGFactory()
        factory.processTask(task)
    }

    fun addTask(task: Task) {
        Redis.addData(task)
    }

    fun getTask(value: String): Task {
        return Json.decodeFromString(Task.serializer(), value)
    }

}