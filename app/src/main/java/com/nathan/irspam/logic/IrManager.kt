package com.nathan.irspam.logic

import android.content.Context
import android.hardware.ConsumerIrManager
import android.util.Log

class IrManager(context: Context) {
    private val irManager: ConsumerIrManager? = 
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    fun hasIrEmitter(): Boolean {
        return irManager?.hasIrEmitter() ?: false
    }

    /**
     * Transmet un signal IR à partir d'un pattern de durées (microsecondes)
     */
    fun transmit(carrierFrequency: Int, pattern: IntArray) {
        if (hasIrEmitter()) {
            try {
                irManager?.transmit(carrierFrequency, pattern)
                Log.d("IrManager", "Transmission de $carrierFrequency Hz")
            } catch (e: Exception) {
                Log.e("IrManager", "Erreur lors de la transmission: ${e.message}")
            }
        }
    }

    /**
     * Convertit un code ProntoHex en pattern utilisable par Android
     * Exemple de format : 0000 006D 0022 0002 0157 00AC ...
     */
    fun transmitProntoHex(prontoHex: String) {
        val parts = prontoHex.trim().split("\\s+".toRegex())
        if (parts.size < 4) return

        // La fréquence porteuse est la 2ème partie (ex: 006D)
        val frequencyCode = parts[1].toInt(16)
        val frequency = (1000000 / (frequencyCode * 0.241246)).toInt()

        // Les durées commencent après les 4 premières parties
        val pattern = parts.subList(4, parts.size).map { 
            // Conversion Hex -> Décimal -> Durée en microsecondes via la fréquence
            val value = it.toInt(16)
            (value * (1000000.0 / frequency)).toInt()
        }.toIntArray()

        transmit(frequency, pattern)
    }
}
