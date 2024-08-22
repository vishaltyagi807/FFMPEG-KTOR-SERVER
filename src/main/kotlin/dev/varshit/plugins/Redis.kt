@file:OptIn(ExperimentalLettuceCoroutinesApi::class)

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

object Redis {
    private val redisClient: RedisClient = RedisClient.create(System.getenv("REDIS_HOST_URL"))
    private val redisConnection: StatefulRedisConnection<String, String> = redisClient.connect()
    private val redis: RedisCoroutinesCommands<String, String> = redisConnection.coroutines()

    fun addData(task: Task) {
        runBlocking {
            redis.lpush("video-transcoder", Json.encodeToString(Task.serializer(), task))
        }
    }

    fun configureRedis() {
        println("Configuring.....")
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                runBlocking {
                    println("New Loop")
                    try {
                        val task = redis.blpop(5, "video-transcoder")
                        if (task != null) {
                            println("new task")
                            TaskFactory.startTask(getTask(task.value))
                            println("Completed")
                        } else {
                            println("continue")
                        }
                    } catch (e: Exception) {
                        println("ERROR: ${e.message}")
                    }
                }
            }
        }
    }
}