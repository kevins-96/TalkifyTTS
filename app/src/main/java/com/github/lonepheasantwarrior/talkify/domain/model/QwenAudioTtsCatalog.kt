package com.github.lonepheasantwarrior.talkify.domain.model

/**
 * 阿里云百炼 Qwen-Audio-TTS（qwen-audio-3.x-tts-*）模型与音色目录。
 *
 * 与 qwen3-tts-flash 是两套接口、两套音色：音色"必须属于当前模型支持的音色列表，
 * 不能跨模型混用"（官方文档，混用返回 InvalidParameter）。此处只收录官方音色列表中
 * 明确列出的常用中文/多语种音色，完整列表见：
 * https://help.aliyun.com/zh/model-studio/qwen-audio-tts-voice-list
 *
 * 纯 Kotlin、无 Android 依赖，供 Provider（请求兜底音色）与音色仓储（设置页列表）共用。
 */
object QwenAudioTtsCatalog {

    const val MODEL_PREFIX = "qwen-audio-"

    data class Voice(val id: String, val displayName: String, val models: Set<String>)

    private const val M31_PLUS = "qwen-audio-3.1-tts-plus"
    private const val M31_FLASH = "qwen-audio-3.1-tts-flash"
    private const val M30_PLUS = "qwen-audio-3.0-tts-plus"
    private const val M30_FLASH = "qwen-audio-3.0-tts-flash"

    val voices: List<Voice> = listOf(
        // 3.1：plus 与 flash 共有的多语种/方言音色（支持 instruction）
        Voice("longanhuan_v3.1", "龙安欢", setOf(M31_PLUS, M31_FLASH)),
        Voice("longanlingxin_v3.1", "龙安灵心", setOf(M31_PLUS, M31_FLASH)),
        Voice("longanfengyue_v3.1", "龙安风悦", setOf(M31_PLUS, M31_FLASH)),
        Voice("xunanchuan_v3.1", "许南川", setOf(M31_PLUS, M31_FLASH)),
        // 3.1 flash 专有
        Voice("yuxiaoyun_v3.1", "雨小芸", setOf(M31_FLASH)),
        Voice("longanyang_v3.1", "龙安洋", setOf(M31_FLASH)),
        Voice("loongbella_v3.1", "Bella", setOf(M31_FLASH)),
        Voice("Emily_v3.1", "Emily（英文）", setOf(M31_FLASH)),
        Voice("Abby_v3.1", "Abby（英文）", setOf(M31_FLASH)),
        Voice("David_v3.1", "David（英文）", setOf(M31_FLASH)),
        // 3.0 plus
        Voice("longanlingxin", "龙安灵心", setOf(M30_PLUS)),
        Voice("longanlufeng", "龙安鲁风", setOf(M30_PLUS)),
        // 3.0 flash
        Voice("longanhuan_v3.6", "龙安欢", setOf(M30_FLASH)),
        Voice("longanfengyue", "龙安风悦", setOf(M30_FLASH)),
        Voice("longanyuanfei", "龙安远飞", setOf(M30_FLASH)),
        Voice("longanlingxi", "龙安灵犀", setOf(M30_FLASH)),
        Voice("longanxiaoxin", "龙安小欣", setOf(M30_FLASH)),
        Voice("longjielidou_v3.6", "龙杰力豆", setOf(M30_FLASH)),
        Voice("longpaopao_v3.6", "龙泡泡", setOf(M30_FLASH)),
        Voice("longhuohuo_v3.6", "龙火火", setOf(M30_FLASH)),
        Voice("longchuanshu_v3.6", "龙川叔", setOf(M30_FLASH)),
    )

    fun isQwenAudioModel(modelId: String): Boolean =
        modelId.trim().startsWith(MODEL_PREFIX, ignoreCase = true)

    /** 指定模型可用的音色；未收录的模型（如新快照）回落为 3.1 flash 列表 */
    fun voicesFor(modelId: String): List<Voice> {
        val model = modelId.trim().lowercase()
        val exact = voices.filter { model in it.models }
        if (exact.isNotEmpty()) return exact
        val family = when {
            model.startsWith("qwen-audio-3.0-tts-plus") -> M30_PLUS
            model.startsWith("qwen-audio-3.0-tts-flash") -> M30_FLASH
            model.startsWith("qwen-audio-3.1-tts-plus") -> M31_PLUS
            else -> M31_FLASH
        }
        return voices.filter { family in it.models }
    }

    /**
     * 解析请求音色：配置音色属于该模型时原样使用，否则（含从 qwen3-tts-flash
     * 切换模型后残留的 Cherry 等）回落为该模型首个音色，避免 InvalidParameter。
     */
    fun resolveVoice(modelId: String, configuredVoice: String): String {
        val candidates = voicesFor(modelId)
        val voice = configuredVoice.trim()
        if (voice.isNotEmpty() && (candidates.any { it.id == voice } || voice.startsWith(MODEL_PREFIX))) {
            // qwen-audio-3.x-tts-{plus|flash}-xxx 形式为官方小语种/复刻基础音色，原样透传
            return voice
        }
        return candidates.first().id
    }
}
