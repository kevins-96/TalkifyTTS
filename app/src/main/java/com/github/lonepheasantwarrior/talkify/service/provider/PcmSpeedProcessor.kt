package com.github.lonepheasantwarrior.talkify.service.provider

import com.github.lonepheasantwarrior.talkify.service.provider.sonic.Sonic
import kotlin.math.abs

/**
 * 本地变速不变调（time-stretch）处理器：PCM16LE 进，PCM16LE 出。
 *
 * 供"模型不提供语速参数、且不可靠执行语速指令"的供应商使用（如小米 MiMo：
 * 实测 user 指令"两倍语速"仅得到 0.96x～1.14x）。底层为 Sonic 算法（Apache-2.0）。
 *
 * 一个实例只服务一段连续音频流（如一个文本块的 SSE 响应），非线程安全；
 * 流结束时必须调用 [flush] 取出尾部残留样本。
 *
 * @param sampleRate 采样率（Hz）
 * @param channelCount 声道数
 * @param speed 速度倍数，>1 加快、<1 放慢，调用方应先经 [clampSpeed] 规整
 */
class PcmSpeedProcessor(
    sampleRate: Int,
    private val channelCount: Int,
    speed: Float,
) {
    private val sonic = Sonic(sampleRate, channelCount).apply { this.speed = speed }
    private val frameBytes = 2 * channelCount

    /** 上次输入不足一帧的尾字节（SSE 分片不保证按帧边界切） */
    private var carry = ByteArray(0)

    /** 输入一段 PCM，返回当前可取出的已变速 PCM（可能为空） */
    fun process(input: ByteArray): ByteArray {
        if (input.isEmpty()) return EMPTY
        val merged = if (carry.isEmpty()) input else carry + input
        val usable = merged.size - merged.size % frameBytes
        carry = if (usable == merged.size) EMPTY else merged.copyOfRange(usable, merged.size)
        if (usable > 0) sonic.writeBytesToStream(merged, usable)
        return drain()
    }

    /** 流结束：冲刷 Sonic 内部缓冲并返回剩余输出（不足一帧的尾字节丢弃） */
    fun flush(): ByteArray {
        carry = EMPTY
        sonic.flushStream()
        return drain()
    }

    private fun drain(): ByteArray {
        val available = sonic.samplesAvailable() * frameBytes
        if (available <= 0) return EMPTY
        val out = ByteArray(available)
        val n = sonic.readBytesFromStream(out, available)
        return if (n == available) out else out.copyOf(n)
    }

    companion object {
        const val MIN_SPEED = 0.5f
        const val MAX_SPEED = 3.0f
        private val EMPTY = ByteArray(0)

        /** 与 1.0 的差小于此值视为原速，跳过处理（避免无谓的算法失真） */
        private const val IDENTITY_EPSILON = 0.02f

        fun clampSpeed(speed: Float): Float =
            if (speed.isNaN()) 1.0f else speed.coerceIn(MIN_SPEED, MAX_SPEED)

        fun isIdentity(speed: Float): Boolean = abs(speed - 1.0f) < IDENTITY_EPSILON

        /**
         * 有效倍数 = 用户设置倍数 × 系统语速倍数。
         *
         * @param userMultiplier 设置页"语速倍数"原文（空白/非法按 1.0）
         * @param androidSpeechRate Android TTS 语速 [0,200]，100 为原速；
         *        应用内预览恒为 100（[SynthesisParams] 默认值）
         */
        fun effectiveSpeed(userMultiplier: String, androidSpeechRate: Float): Float {
            val user = userMultiplier.trim().toFloatOrNull()?.takeIf { it > 0f } ?: 1.0f
            val system = if (androidSpeechRate > 0f) androidSpeechRate / 100f else 1.0f
            return clampSpeed(user * system)
        }
    }
}
