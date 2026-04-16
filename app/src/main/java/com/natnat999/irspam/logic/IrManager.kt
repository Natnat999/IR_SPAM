package com.natnat999.irspam.logic

import android.content.Context
import android.hardware.ConsumerIrManager
import android.util.Log

class IrManager(context: Context) {
    private val irManager: ConsumerIrManager? = 
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    fun hasIrEmitter(): Boolean {
        return irManager?.hasIrEmitter() ?: false
    }

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

    fun transmitProntoHex(prontoHex: String) {
        try {
            val parts = prontoHex.trim().split("\\s+".toRegex())
            if (parts.size < 4) return

            val frequencyCode = parts[1].toInt(16)
            val frequency = (1000000 / (frequencyCode * 0.241246)).toInt()

            val pattern = parts.subList(4, parts.size).map { 
                val value = it.toInt(16)
                (value * (1000000.0 / frequency)).toInt()
            }.toIntArray()

            transmit(frequency, pattern)
        } catch (e: Exception) {
            Log.e("IrManager", "Format ProntoHex invalide : ${e.message}")
        }
    }
}
