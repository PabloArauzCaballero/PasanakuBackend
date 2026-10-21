package bo.aportaya.aportaya_movil

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
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

    /// La imagen del QR del grupo (2026-10-20). Solo se comparten archivos de la carpeta
    /// `cache/compartir` de la app (ver `res/xml/archivos_compartidos.xml`): una ruta de
    /// afuera no sale. El permiso de lectura es temporal y solo para la app elegida.
    fun imagen(ruta: String, texto: String, porWhatsApp: Boolean): Boolean {
        val archivo = File(ruta)
        val carpeta = File(actividad.cacheDir, "compartir").canonicalFile
        if (!archivo.isFile || archivo.canonicalFile.parentFile != carpeta) return false
        val uri = FileProvider.getUriForFile(
            actividad,
            actividad.packageName + ".compartir",
            archivo,
        )
        fun intento(paquete: String?) = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, texto)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (paquete != null) setPackage(paquete)
        }
        if (porWhatsApp) {
            for (paquete in listOf("com.whatsapp", "com.whatsapp.w4b")) {
                if (abrir(intento(paquete))) return true
            }
        }
        return abrir(Intent.createChooser(intento(null), "Compartir el QR del grupo"))
    }

    private fun abrir(intento: Intent): Boolean = try {
        actividad.startActivity(intento)
        true
    } catch (sinApp: ActivityNotFoundException) {
        false
    }
}
