package com.github.lonepheasantwarrior.talkify.service.provider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class PcmSpeedProcessorTest {

    private val sampleRate = 24000

    /** 生成 [seconds] 秒带包络的正弦 PCM16LE（单声道），模拟语音的周期性 */
    private fun tone(seconds: Double): ByteArray {
        val n = (sampleRate * seconds).toInt()
        val out = ByteArray(n * 2)
        for (i in 0 until n) {
            val env = 0.5 * (1 + sin(2 * PI * 3 * i / sampleRate))
            val v = (8000 * env * sin(2 * PI * 220 * i / sampleRate)).toInt().toShort()
            out[2 * i] = (v.toInt() and 0xff).toByte()
            out[2 * i + 1] = (v.toInt() shr 8).toByte()
        }
        return out
    }

    /** 以不按帧对齐的 [piece] 字节分片喂入（模拟 SSE 分片），返回输出总字节数 */
    private fun runStream(speed: Float, input: ByteArray, piece: Int = 1001): Int {
        val p = PcmSpeedProcessor(sampleRate, 1, speed)
        var total = 0
        var off = 0
        while (off < input.size) {
            val end = minOf(input.size, off + piece)
            val out = p.process(input.copyOfRange(off, end))
            assertEquals("output must be frame aligned", 0, out.size % 2)
            total += out.size
            off = end
        }
        total += p.flush().size
        return total
    }

    @Test
    fun `speed 2x halves duration`() {
        val input = tone(2.0)
        val ratio = input.size.toDouble() / runStream(2.0f, input)
        assertEquals(2.0, ratio, 0.15)
    }

    @Test
    fun `speed 1_5x and 0_8x scale duration`() {
        val input = tone(2.0)
        assertEquals(1.5, input.size.toDouble() / runStream(1.5f, input), 0.12)
        assertEquals(0.8, input.size.toDouble() / runStream(0.8f, input), 0.08)
    }

    @Test
    fun `effective speed multiplies user setting with android rate`() {
        assertEquals(1.0f, PcmSpeedProcessor.effectiveSpeed("", 100f), 1e-4f)
        assertEquals(2.0f, PcmSpeedProcessor.effectiveSpeed("2.0", 100f), 1e-4f)
        // 阅读 App 滑块 150 × 设置 1.2 = 1.8
        assertEquals(1.8f, PcmSpeedProcessor.effectiveSpeed("1.2", 150f), 1e-4f)
        // 非法/越界输入
        assertEquals(1.0f, PcmSpeedProcessor.effectiveSpeed("abc", 100f), 1e-4f)
        assertEquals(1.0f, PcmSpeedProcessor.effectiveSpeed("-1", 100f), 1e-4f)
        assertEquals(PcmSpeedProcessor.MAX_SPEED, PcmSpeedProcessor.effectiveSpeed("3.0", 200f), 1e-4f)
        assertEquals(PcmSpeedProcessor.MIN_SPEED, PcmSpeedProcessor.effectiveSpeed("0.8", 10f), 1e-4f)
    }

    @Test
    fun `identity detection`() {
        assertTrue(PcmSpeedProcessor.isIdentity(1.0f))
        assertTrue(PcmSpeedProcessor.isIdentity(1.01f))
        assertFalse(PcmSpeedProcessor.isIdentity(1.2f))
    }
}
