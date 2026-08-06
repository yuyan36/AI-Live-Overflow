package com.ailiveoverflow.emotion

class EmotionEngine {
    private var heat = 0.0
    private var valence = 0.5
    private var arousal = 0.5

    fun setHeat(value: Double) {
        heat = value.coerceIn(0.0, 100.0)
    }

    fun boostHeat(amount: Double) {
        heat = (heat + amount).coerceIn(0.0, 100.0)
    }

    fun decayHeat() {
        heat = (heat - 1.0).coerceAtLeast(0.0)
    }

    fun getHeat(): Double = heat

    fun getHeatLevel(): String = when {
        heat > 70 -> "high"
        heat > 40 -> "medium"
        heat > 10 -> "low"
        else -> "none"
    }

    fun setValence(v: Double) { valence = v.coerceIn(0.0, 1.0) }
    fun setArousal(a: Double) { arousal = a.coerceIn(0.0, 1.0) }
    fun getValence(): Double = valence
    fun getArousal(): Double = arousal

    fun getDominantExpression(): String {
        return when {
            valence > 0.7 && arousal > 0.6 -> "happy"
            valence < 0.3 && arousal > 0.6 -> "angry"
            valence < 0.3 && arousal < 0.4 -> "sad"
            arousal > 0.8 -> "excited"
            arousal < 0.3 -> "sleepy"
            heat > 50 -> "blush"
            else -> "idle"
        }
    }
}