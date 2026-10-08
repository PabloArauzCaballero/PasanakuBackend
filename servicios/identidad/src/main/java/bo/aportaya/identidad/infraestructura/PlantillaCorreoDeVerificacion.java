package bo.aportaya.identidad.infraestructura;

import java.time.Duration;

/** HTML autocontenido del OTP. La animacion tiene una presentacion estatica segura. */
public final class PlantillaCorreoDeVerificacion {

    private PlantillaCorreoDeVerificacion() {}

    public static String asunto() {
        return "Tu codigo de verificacion de AportaYa";
    }

    public static String html(String codigo, Duration vigencia) {
        if (codigo == null || !codigo.matches("\\d{6}")) {
            throw new IllegalArgumentException("El codigo de correo debe tener seis digitos");
        }
        StringBuilder digitos = new StringBuilder();
        for (int i = 0; i < codigo.length(); i++) {
            digitos.append(
                            "<td class=\"digit\" style=\"width:48px;height:62px;background:#ffffff;border:1px solid #dce7e4;border-radius:14px;text-align:center;font:700 30px/62px Arial,sans-serif;color:#123c35;box-shadow:0 8px 24px rgba(18,60,53,.08);animation:flip .65s ease both;animation-delay:")
                    .append(i * 90)
                    .append("ms\">")
                    .append(codigo.charAt(i))
                    .append("</td>");
            if (i + 1 < codigo.length()) {
                digitos.append("<td style=\"width:8px\"></td>");
            }
        }
        long minutos = Math.max(1, vigencia.toMinutes());
        return """
                <!doctype html>
                <html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width">
                <style>
                @keyframes flip{0%{opacity:0;transform:translateY(-14px) rotateX(75deg)}60%{opacity:1;transform:translateY(3px) rotateX(0)}100%{transform:translateY(0)}}
                @keyframes orbit{to{transform:rotate(360deg)}}
                @media(max-width:520px){.card{width:92%!important}.digit{width:40px!important;height:56px!important;line-height:56px!important;font-size:25px!important}}
                </style></head>
                <body style="margin:0;background:#eef6f3;font-family:Arial,sans-serif;color:#173b35">
                  <div style="display:none;max-height:0;overflow:hidden">Tu codigo de AportaYa ya esta listo. Caduca pronto.</div>
                  <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background:linear-gradient(145deg,#e8f7f2,#f7fbfa);padding:34px 12px">
                    <tr><td align="center">
                      <table role="presentation" class="card" width="520" cellspacing="0" cellpadding="0" style="width:520px;max-width:100%;background:#ffffff;border-radius:28px;overflow:hidden;box-shadow:0 20px 55px rgba(18,60,53,.14)">
                        <tr><td style="height:9px;background:linear-gradient(90deg,#11a683,#65d4b8,#f6b94a)"></td></tr>
                        <tr><td style="padding:38px 34px 12px;text-align:center">
                          <div style="display:inline-block;padding:9px 14px;border-radius:999px;background:#e7f7f2;color:#08745e;font-size:12px;font-weight:700;letter-spacing:1.4px;text-transform:uppercase">Verificacion segura</div>
                          <h1 style="margin:22px 0 10px;font-size:30px;line-height:1.15;color:#123c35">Una combinacion, tu cuenta</h1>
                          <p style="margin:0 auto;max-width:400px;color:#5d746f;font-size:16px;line-height:1.6">Escribe estos seis digitos en AportaYa para confirmar que este correo te pertenece.</p>
                        </td></tr>
                        <tr><td align="center" style="padding:24px 12px">
                          <table role="presentation" cellspacing="0" cellpadding="0"><tr>{{DIGITOS}}</tr></table>
                          <p style="margin:18px 0 0;color:#08745e;font-size:13px;font-weight:700">La combinacion cambia en cada solicitud</p>
                        </td></tr>
                        <tr><td style="padding:8px 34px 34px">
                          <div style="padding:18px 20px;border-radius:18px;background:#f5f9f8;border:1px solid #e4eeeb;color:#5d746f;font-size:14px;line-height:1.55">
                            <strong style="color:#173b35">Disponible durante {{MINUTOS}} minutos.</strong><br>
                            Nunca compartas este codigo. El equipo de AportaYa no te lo pedira por llamada, chat ni mensaje.
                          </div>
                        </td></tr>
                        <tr><td style="padding:22px 34px;background:#123c35;color:#cce5df;text-align:center;font-size:12px;line-height:1.6">
                          Recibiste este correo porque se inicio un registro con esta direccion.<br>Si no fuiste tu, puedes ignorarlo con seguridad.
                        </td></tr>
                      </table>
                      <p style="margin:20px 0 0;color:#79908b;font-size:11px">AportaYa · confianza que se mueve contigo</p>
                    </td></tr>
                  </table>
                </body></html>
                """
                .replace("{{DIGITOS}}", digitos)
                .replace("{{MINUTOS}}", Long.toString(minutos));
    }
}
