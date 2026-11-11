package com.github.lonepheasantwarrior.talkify.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QwenAudioTtsCatalogTest {

    @Test
    fun `model detection`() {
        assertTrue(QwenAudioTtsCatalog.isQwenAudioModel("qwen-audio-3.1-tts-flash"))
        assertTrue(QwenAudioTtsCatalog.isQwenAudioModel(" Qwen-Audio-3.0-tts-plus "))
        assertFalse(QwenAudioTtsCatalog.isQwenAudioModel("qwen3-tts-flash"))
        assertFalse(QwenAudioTtsCatalog.isQwenAudioModel(""))
    }

    @Test
    fun `voices are filtered by model`() {
        val plus30 = QwenAudioTtsCatalog.voicesFor("qwen-audio-3.0-tts-plus").map { it.id }
        assertEquals(listOf("longanlingxin", "longanlufeng"), plus30)
        assertTrue("longanhuan_v3.6" in QwenAudioTtsCatalog.voicesFor("qwen-audio-3.0-tts-flash").map { it.id })
        // 未收录的新快照回落到同族列表
        assertTrue(QwenAudioTtsCatalog.voicesFor("qwen-audio-3.1-tts-flash-2026-12-01").isNotEmpty())
    }

    @Test
    fun `qwen3 voice left over after model switch falls back`() {
        assertEquals("longanhuan_v3.1", QwenAudioTtsCatalog.resolveVoice("qwen-audio-3.1-tts-flash", "Cherry"))
        assertEquals("longanhuan_v3.1", QwenAudioTtsCatalog.resolveVoice("qwen-audio-3.1-tts-flash", ""))
        assertEquals("Abby_v3.1", QwenAudioTtsCatalog.resolveVoice("qwen-audio-3.1-tts-flash", "Abby_v3.1"))
        // 跨模型音色不透传
        assertEquals("longanlingxin", QwenAudioTtsCatalog.resolveVoice("qwen-audio-3.0-tts-plus", "Abby_v3.1"))
        // 官方小语种/复刻基础音色原样透传
        assertEquals(
            "qwen-audio-3.1-tts-flash-zora_bg",
            QwenAudioTtsCatalog.resolveVoice("qwen-audio-3.1-tts-flash", "qwen-audio-3.1-tts-flash-zora_bg")
        )
    }
}
