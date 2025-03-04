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
            try {
                println("New Request...")
                val videoReq = call.receive<VideoRequestBody>()
                val task = Task(0, videoReq)
                val res = TaskFactory.addTask(task)
                call.respondText(res)
            } catch (e: Exception) {
                call.respondText("ERROR: ${e.message}", status = HttpStatusCode.BadRequest)
            }
        }
        delete("/cancel-current-task") {
            try {
                TaskFactory.cancelCurrentTask()
                call.respondText("SUCCESS")
            } catch (e: Exception) {
                call.respondText("ERROR: ${e.message}", status = HttpStatusCode.BadRequest)
            }
        }
        delete("/delete-all-tasks") {
            try {
                TaskFactory.deleteAllTasks()
                call.respondText("SUCCESS")
            } catch (e: Exception) {
                call.respondText("ERROR: ${e.message}", status = HttpStatusCode.BadRequest)
            }
        }
        get("/get-all-tasks") {
            try {
                call.respond(TaskFactory.getAllTasks())
            } catch (e: Exception) {
                call.respondText("ERROR: ${e.message}", status = HttpStatusCode.BadRequest)
            }
        }
    }
}