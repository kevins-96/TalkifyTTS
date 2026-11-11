package com.github.lonepheasantwarrior.talkify.infrastructure.provider.repo

import android.content.Context
import com.github.lonepheasantwarrior.talkify.R
import com.github.lonepheasantwarrior.talkify.domain.model.AliyunBailianConfig
import com.github.lonepheasantwarrior.talkify.domain.model.ProviderIds
import com.github.lonepheasantwarrior.talkify.domain.model.QwenAudioTtsCatalog
import com.github.lonepheasantwarrior.talkify.domain.model.TtsProvider
import com.github.lonepheasantwarrior.talkify.domain.repository.VoiceInfo
import com.github.lonepheasantwarrior.talkify.domain.repository.VoiceRepository
import com.github.lonepheasantwarrior.talkify.infrastructure.xml.VoiceXmlEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 阿里云百炼音色仓储：按当前配置的模型返回音色列表。
 *
 * - qwen3-tts-flash 等（默认）：res/xml/bailian_qwen3_tts_voices.xml
 * - qwen-audio-3.x-tts-*：[QwenAudioTtsCatalog]（两套音色不能混用）
 *
 * 注意：设置抽屉在打开时加载音色，修改模型 ID 后需保存并重新打开抽屉才会刷新列表。
 */
class AliyunBailianVoiceRepository(
    private val context: Context
) : VoiceRepository {

    private val qwen3Repository = object : BaseXmlVoiceRepository(
        context = context,
        xmlResId = R.xml.bailian_qwen3_tts_voices,
        expectedProviderId = ProviderIds.AliyunBailian.providerId
    ) {
        override fun VoiceXmlEntry.toVoiceInfo(): VoiceInfo =
            VoiceInfo(voiceId = id, displayName = displayName)
    }

    override suspend fun getVoicesForProvider(provider: TtsProvider): List<VoiceInfo> {
        if (provider.id != ProviderIds.AliyunBailian.providerId) return emptyList()
        val modelId = withContext(Dispatchers.IO) {
            (AliyunBailianConfigRepository(context)
                .getConfig(provider.id) as? AliyunBailianConfig)?.modelId.orEmpty()
        }
        if (!QwenAudioTtsCatalog.isQwenAudioModel(modelId)) {
            return qwen3Repository.getVoicesForProvider(provider)
        }
        return QwenAudioTtsCatalog.voicesFor(modelId).map {
            VoiceInfo(voiceId = it.id, displayName = "${it.displayName}（${it.id}）")
        }
    }
}
