package dev.varshit

import dev.varshit.plugins.configureRouting
import dev.varshit.plugins.configureSerialization
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 69, module = Application::module).start(wait = true)
}

fun Application.module() {
    println("Started....")
    configureSerialization()
    configureRouting()
}
