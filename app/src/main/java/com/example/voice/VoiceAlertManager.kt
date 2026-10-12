package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import com.example.core.util.CurrencyUtils
import java.util.Locale

class VoiceAlertManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
        }
    }

    fun speakReminder(
        merchantName: String,
        amountPaise: Long,
        daysRemaining: Int,
        languageCode: String = "en",
        speechRate: Float = 1.0f
    ) {
        if (!isInitialized || tts == null) return

        val locale = getLocaleForCode(languageCode)
        tts?.language = locale
        tts?.setSpeechRate(speechRate)

        val rupees = amountPaise / 100
        val spokenText = generateSpokenText(merchantName, rupees, daysRemaining, languageCode)

        tts?.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, "reminder_tts_${System.currentTimeMillis()}")
    }

    fun testVoice(languageCode: String, speechRate: Float = 1.0f) {
        speakReminder(
            merchantName = "AutoPay Alert",
            amountPaise = 49900L,
            daysRemaining = 2,
            languageCode = languageCode,
            speechRate = speechRate
        )
    }

    private fun generateSpokenText(merchant: String, rupees: Long, days: Int, lang: String): String {
        val daysText = if (days <= 0) "today" else "in $days days"
        return when (lang) {
            "hi" -> "भुगतान सूचना: $merchant के लिए $rupees रुपये का भुगतान अपेक्षित है।"
            "ta" -> "கட்டண அறிவிப்பு: $merchant பயன்பாட்டிற்கு $rupees ரூபாய் கட்டணம் செலுத்தப்பட வேண்டும்."
            "te" -> "చెల్లింపు హెచ్చరిక: $merchant కోసం $rupees రూపాయల చెల్లింపు రావాల్సి ఉంది."
            "kn" -> "ಪಾವತಿ ಎಚ್ಚರಿಕೆ: $merchant ಗಾಗಿ $rupees ರೂಪಾಯಿಗಳ ಪಾವತಿ ನಿರೀಕ್ಷಿಸಲಾಗಿದೆ."
            "mr" -> "पेमेंट सूचना: $merchant साठी $rupees रुपयांचे पेमेंट अपेक्षित आहे."
            else -> "Payment alert. Your payment of $rupees rupees for $merchant is due $daysText."
        }
    }

    private fun getLocaleForCode(code: String): Locale {
        return when (code) {
            "hi" -> Locale.forLanguageTag("hi-IN")
            "ta" -> Locale.forLanguageTag("ta-IN")
            "te" -> Locale.forLanguageTag("te-IN")
            "kn" -> Locale.forLanguageTag("kn-IN")
            "mr" -> Locale.forLanguageTag("mr-IN")
            else -> Locale.ENGLISH
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
