package bo.aportaya.aportaya_movil

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

/// Comparte la invitación desde las apps del teléfono (2026-10-18): el servidor no
/// tiene SMS ni WhatsApp conectados, así que el mensaje sale del WhatsApp o del SMS de
/// quien invita. Solo intents del sistema; ningún SDK de terceros. `true` si se abrió.
class CompartirMensaje(private val actividad: Activity) {

    fun porWhatsApp(telefono: String, texto: String): Boolean {
        val digitos = telefono.filter { it.isDigit() }
        val enlace = Uri.parse(
            "https://wa.me/$digitos?text=" + URLEncoder.encode(texto, "UTF-8"),
        )
        // Primero WhatsApp o WhatsApp Business; si no están, wa.me abre en el navegador.
        for (paquete in listOf("com.whatsapp", "com.whatsapp.w4b", null)) {
            val intento = Intent(Intent.ACTION_VIEW, enlace)
            if (paquete != null) intento.setPackage(paquete)
            if (abrir(intento)) return true
        }
        return false
    }

    fun porSms(telefono: String, texto: String): Boolean {
        val intento = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(telefono)))
        intento.putExtra("sms_body", texto)
        return abrir(intento)
    }

    fun conOtraApp(texto: String): Boolean {
        val intento = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, texto)
        }
        return abrir(Intent.createChooser(intento, "Compartir invitación"))
    }

    private fun abrir(intento: Intent): Boolean = try {
        actividad.startActivity(intento)
        true
    } catch (sinApp: ActivityNotFoundException) {
        false
    }
}
