package com.github.lonepheasantwarrior.talkify.service.provider.impl

import com.github.lonepheasantwarrior.talkify.domain.model.AliyunBailianConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AliyunQwenAudioRequestTest {

    @Test
    fun `rate combines setting and system rate and clamps to api range`() {
        val c = AliyunBailianConfig(speechRate = "1.5")
        assertEquals(1.5f, AliyunBailianProvider.buildQwenAudioRequestFields(c, 100f).first, 1e-4f)
        assertEquals(2.0f, AliyunBailianProvider.buildQwenAudioRequestFields(c, 200f).first, 1e-4f)
        assertEquals(1.0f, AliyunBailianProvider.buildQwenAudioRequestFields(AliyunBailianConfig(), 100f).first, 1e-4f)
        assertEquals(0.5f, AliyunBailianProvider.buildQwenAudioRequestFields(AliyunBailianConfig(), 20f).first, 1e-4f)
    }

    @Test
    fun `instruction is trimmed and omitted when blank`() {
        assertNull(AliyunBailianProvider.buildQwenAudioRequestFields(AliyunBailianConfig(), 100f).second)
        assertNull(AliyunBailianProvider.buildQwenAudioRequestFields(AliyunBailianConfig(styleInstruction = "  "), 100f).second)
        assertEquals(
            "请讲上海话。",
            AliyunBailianProvider.buildQwenAudioRequestFields(AliyunBailianConfig(styleInstruction = " 请讲上海话。 "), 100f).second
        )
    }
}
