package dev.varshit.plugins

import dev.varshit.factory.TaskFactory
import dev.varshit.models.Task
import dev.varshit.models.VideoRequestBody
import io.github.jan.supabase.postgrest.from
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/supabase-status") {
            try {
                Supabase.client.from("video-transcoder").select {
                    limit(1)
                }
                call.respondText("SUCCESS")
            } catch (e: Exception) {
                call.respondText("ERROR: ${e.message}", status = HttpStatusCode.BadRequest)
            }
        }
        get("/status") {
            call.respondText("Status : ACTIVE")
        }
        post("/new-video") {
            val videoReq = call.receive<VideoRequestBody>()
            val task = Task(0, videoReq)
            TaskFactory.addTask(task)
            println("Quenched!...")
            call.respondText("Quenched!...")
        }
    }
}