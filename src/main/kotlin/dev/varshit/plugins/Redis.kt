@file:OptIn(ExperimentalLettuceCoroutinesApi::class, DelicateCoroutinesApi::class)

package dev.varshit.plugins

import dev.varshit.factory.TaskFactory
import dev.varshit.factory.TaskFactory.getTask
import dev.varshit.models.Task
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicBoolean

object Redis {
    private val redisClient: RedisClient = RedisClient.create(System.getenv("REDIS_HOST_URL"))
    private val redisConnection: StatefulRedisConnection<String, String> = redisClient.connect()
    private val redis: RedisCoroutinesCommands<String, String> = redisConnection.coroutines()
    private var workerRunning = AtomicBoolean(false)
    private const val CHANNEL_NAME = "worker"

    suspend fun deleteAllTasks() {
        var task = redis.lpop("video-transcoder")
        while (task != null) {
            task = redis.lpop("video-transcoder")
        }
    }

    suspend fun getAllTasks(): List<Task> {
        return redis.lrange("video-transcoder", 0, -1).map { Json.decodeFromString<Task>(it) }
    }

    private suspend fun isTaskAlreadyInQueue(task: Task): Boolean {
        val res = redis.lrange("video-transcoder", 0, -1)
        val tasks = res.map { Json.decodeFromString<Task>(it) }
        val index = tasks.indexOfFirst { it.request.record?.id == task.request.record?.id }
        return index != -1
    }

    suspend fun addData(task: Task): String {
        if (isTaskAlreadyInQueue(task)) {
            println("Task already exists in queue: $task")
            return "Task already exists in queue: ${task.request.record?.id}"
        }
        redis.lpush("video-transcoder", Json.encodeToString(Task.serializer(), task))
        redis.publish(CHANNEL_NAME, "new_task")
        println("Queued task : ${task.request.record?.id}")
        startWork()
        return "Queued!..."
    }

    private fun startWork() {
        if (!workerRunning.compareAndSet(false, true)) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                println("Worker started...")
                var task = redis.blpop(2, "video-transcoder")
                while (task != null) {
                    println("New task")
                    TaskFactory.startTask(getTask(task.value), this).join()
                    println("Completed")
                    task = redis.blpop(2, "video-transcoder")
                }
                workerRunning.set(false)
                println("All task Completed...")
            } catch (e: Exception) {
                println("ERROR: ${e.message}")
            }
        }

    }
}