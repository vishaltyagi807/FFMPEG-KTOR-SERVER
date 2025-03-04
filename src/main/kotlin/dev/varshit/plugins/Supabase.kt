package dev.varshit.plugins

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.minimalSettings
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object Supabase {
    val client = createSupabaseClient(
        supabaseUrl = System.getenv("SUPABASE_URL") ?: "http://localhost:8000",
        supabaseKey = System.getenv("SUPABASE_KEY") ?: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.ewogICJyb2xlIjogInNlcnZpY2Vfcm9sZSIsCiAgImlzcyI6ICJzdXBhYmFzZSIsCiAgImlhdCI6IDE3NDAxNjI2MDAsCiAgImV4cCI6IDE4OTc5MjkwMDAKfQ.ZdM-QuLhogkkyk9_eGfHCWxGEH49ta22UsLDyUqLvkE"
    ) {
        install(Auth) {
            minimalSettings()
        }
        install(Postgrest)
        install(Storage)
    }
}