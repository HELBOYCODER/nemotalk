package com.helboy.nemotalk.model

/**
 * Vision-capable model definitions from NVIDIA NIM catalog.
 * All models below support image input via OpenAI-compatible `messages[0].content` arrays.
 */
data class VisionModel(val id: String, val displayName: String)

val VISION_MODELS: List<VisionModel> = listOf(
    VisionModel("meta/llama-3.2-90b-vision-instruct", "Llama 3.2 90B Vision"),
    VisionModel("meta/llama-3.2-11b-vision-instruct", "Llama 3.2 11B Vision"),
    VisionModel("microsoft/phi-3.5-vision-128k-instruct", "Phi 3.5 Vision"),
    VisionModel("nvidia/neva-22b", "NVIDIA Neva 22B")
)

fun isVisionCapable(modelId: String): Boolean {
    return VISION_MODELS.any { it.id == modelId }
}

/**
 * Decide which vision model to fall back to when the primary reasoning model
 * cannot process images.
 */
fun visionFallbackFor(modelId: String): String {
    if (isVisionCapable(modelId)) return modelId
    return "meta/llama-3.2-90b-vision-instruct"
}
