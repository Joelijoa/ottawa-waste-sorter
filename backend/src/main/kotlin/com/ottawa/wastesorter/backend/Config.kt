package com.ottawa.wastesorter.backend

/**
 * Configuration lue dans les variables d'environnement.
 * Sur Cloud Run, GEMINI_API_KEY est injectée depuis Secret Manager (voir documentation/deploiement.md).
 */
data class AppConfig(
    val port: Int,
    val geminiApiKey: String?,
    val geminiModel: String,
    val maxUploadBytes: Long,
    val rateLimitPerMinute: Int,
    val minConfidence: Double,
) {
    companion object {
        fun fromEnv(env: Map<String, String> = System.getenv()): AppConfig = AppConfig(
            port = env["PORT"]?.toIntOrNull() ?: 8080,
            geminiApiKey = env["GEMINI_API_KEY"]?.takeIf { it.isNotBlank() },
            geminiModel = env["GEMINI_MODEL"]?.takeIf { it.isNotBlank() } ?: "gemini-2.5-flash",
            maxUploadBytes = env["MAX_UPLOAD_BYTES"]?.toLongOrNull() ?: (4L * 1024 * 1024),
            rateLimitPerMinute = env["RATE_LIMIT_PER_MINUTE"]?.toIntOrNull() ?: 20,
            minConfidence = env["MIN_CONFIDENCE"]?.toDoubleOrNull() ?: 0.6,
        )
    }
}
