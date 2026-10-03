package com.helboy.nemotalk.model

data class NvidiaModel(
    val id: String,
    val displayName: String,
    val description: String,
    val isRecommended: Boolean = false
) {
    companion object {
        val ALL_MODELS = listOf(
            NvidiaModel(
                id = "nvidia/nemotron-4-340b-instruct",
                displayName = "Nemotron 340B (پرچمدار)",
                description = "مدل پرچمدار و رسمی NeMo ان‌ویدیا — فعال در تمام حساب‌های رسمی build.nvidia.com",
                isRecommended = true
            ),
            NvidiaModel(
                id = "nvidia/nemotron-3-super-120b-a12b",
                displayName = "Nemotron Super 120B",
                description = "مدل فوق‌سریع ۱۲۰ میلیارد پارامتری ان‌ویدیا با کمترین تاخیر برای مکالمه صوتی",
                isRecommended = true
            ),
            NvidiaModel(
                id = "meta/llama-3.3-70b-instruct",
                displayName = "Llama 3.3 70B",
                description = "مدل محبوب ۷۰ میلیاردی با درک استثنایی زبان فارسی و استدلال روان",
                isRecommended = false
            ),
            NvidiaModel(
                id = "meta/llama-3.2-11b-vision-instruct",
                displayName = "Llama 3.2 11B",
                description = "مدل سبک و سریع یازده میلیاردی با پاسخ‌های آنی",
                isRecommended = false
            ),
            NvidiaModel(
                id = "google/gemma-3-12b-it",
                displayName = "Google Gemma 3 12B",
                description = "مدل نسل سوم گوگل میزبانی‌شده روی کلاود ان‌ویدیا",
                isRecommended = false
            ),
            NvidiaModel(
                id = "openai/gpt-oss-20b",
                displayName = "GPT-OSS 20B",
                description = "مدل اوپن‌سورس بهینه‌سازی‌شده روی کلاود ان‌ویدیا",
                isRecommended = false
            ),
            NvidiaModel(
                id = "nvidia/llama-3.1-nemotron-70b-instruct",
                displayName = "Nemotron 70B",
                description = "مدل کلاسیک نموترون ان‌ویدیا",
                isRecommended = false
            )
        )

        fun findById(id: String): NvidiaModel {
            return ALL_MODELS.find { it.id == id } ?: NvidiaModel(
                id = id,
                displayName = id.substringAfterLast("/"),
                description = "مدل سفارشی ان‌ویدیا",
                isRecommended = false
            )
        }
    }
}
