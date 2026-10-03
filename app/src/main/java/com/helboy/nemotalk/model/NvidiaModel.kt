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
                id = "nvidia/llama-3.1-nemotron-70b-instruct",
                displayName = "Nemotron 70B (پیش‌فرض)",
                description = "بهترین مدل گفتگوی چندمنظوره با استدلال قدرتمند و بهینه‌سازی NeMo",
                isRecommended = true
            ),
            NvidiaModel(
                id = "nvidia/nemotron-4-340b-instruct",
                displayName = "Nemotron 340B (قدرتمندترین)",
                description = "مدل عظیم ۳۴۰ میلیارد پارامتری ان‌ویدیا برای استدلال بسیار پیچیده",
                isRecommended = false
            ),
            NvidiaModel(
                id = "nvidia/nemotron-mini-4b-instruct",
                displayName = "Nemotron Mini 4B (سریع‌ترین)",
                description = "مدل سبک و فوق‌العاده سریع با حداقل تاخیر برای مکالمه صوتی بلادرنگ",
                isRecommended = false
            ),
            NvidiaModel(
                id = "meta/llama-3.3-70b-instruct",
                displayName = "Llama 3.3 70B",
                description = "مدل پرچمدار متا با تسلط عالی بر زبان فارسی و چندزبانه",
                isRecommended = false
            ),
            NvidiaModel(
                id = "deepseek-ai/deepseek-r1",
                displayName = "DeepSeek R1",
                description = "مدل تفکر عمیق و ریاضیات استنتاجی میزبانی‌شده روی کلاود ان‌ویدیا",
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
