package bo.aportaya.identidad.infraestructura;

import bo.aportaya.identidad.dominio.AlcanceDeCuenta;
import java.time.Duration;

/**
 * El correo del enlace de un solo uso del operador (P-1, P-4).
 *
 * <p>El enlace lo arma el servidor con caracteres seguros (base64 de URL); aun asi se escapa antes de
 * ponerlo en el HTML. El texto dice que hacer y que NO hacer: nadie del equipo pide este enlace.
 */
public final class PlantillaCorreoDeInvitacion {

    private PlantillaCorreoDeInvitacion() {}

    public static String asunto(AlcanceDeCuenta alcance) {
        return switch (alcance) {
            case ALTA -> "Tu invitacion al backoffice de AportaYa";
            case CLAVE -> "Restablece tu clave del backoffice de AportaYa";
            case FACTOR -> "Reinscribe tu autenticador del backoffice de AportaYa";
        };
    }

    public static String html(String enlace, AlcanceDeCuenta alcance, Duration vigencia) {
        String instruccion =
                switch (alcance) {
                    case ALTA -> "Elige tu clave y enrola tu aplicacion autenticadora para entrar por primera vez.";
                    case CLAVE -> "Una persona de seguridad aprobo tu pedido. Elige tu clave nueva.";
                    case FACTOR ->
                        "Una persona de seguridad aprobo tu pedido. Vas a necesitar tu clave actual para"
                                + " enrolar tu aplicacion autenticadora nueva.";
                };
        long horas = Math.max(1, vigencia.toHours());
        String destino = escapar(enlace);
        return """
                <!doctype html>
                <html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width"></head>
                <body style="margin:0;background:#eef6f3;font-family:Arial,sans-serif;color:#173b35">
                  <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="padding:34px 12px">
                    <tr><td align="center">
                      <table role="presentation" width="520" cellspacing="0" cellpadding="0" style="width:520px;max-width:100%;background:#ffffff;border-radius:24px">
                        <tr><td style="padding:34px 30px 8px;text-align:center">
                          <h1 style="margin:0 0 12px;font-size:26px;line-height:1.2;color:#123c35">{{ASUNTO}}</h1>
                          <p style="margin:0;color:#5d746f;font-size:16px;line-height:1.6">{{INSTRUCCION}}</p>
                        </td></tr>
                        <tr><td align="center" style="padding:22px 30px">
                          <a href="{{ENLACE}}" style="display:inline-block;padding:14px 26px;border-radius:12px;background:#08745e;color:#ffffff;font-size:16px;font-weight:700;text-decoration:none">Continuar</a>
                        </td></tr>
                        <tr><td style="padding:6px 30px 30px">
                          <div style="padding:16px 18px;border-radius:14px;background:#f5f9f8;border:1px solid #e4eeeb;color:#5d746f;font-size:14px;line-height:1.55">
                            <strong style="color:#173b35">El enlace sirve una sola vez y vence en {{HORAS}} horas.</strong><br>
                            Nadie de AportaYa te lo va a pedir por llamada, chat ni mensaje. Si no esperabas este correo, ignoralo y avisa a seguridad.
                          </div>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body></html>
                """
                .replace("{{ASUNTO}}", escapar(asunto(alcance)))
                .replace("{{INSTRUCCION}}", escapar(instruccion))
                .replace("{{ENLACE}}", destino)
                .replace("{{HORAS}}", Long.toString(horas));
    }

    static String escapar(String texto) {
        return texto.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
