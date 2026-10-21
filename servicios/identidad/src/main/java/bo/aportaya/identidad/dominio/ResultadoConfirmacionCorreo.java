package bo.aportaya.identidad.dominio;

/** Resultado de comprobar el código de correo previo al alta. */
public record ResultadoConfirmacionCorreo(
        boolean verificada, boolean onboardingIncompleto, BorradorOnboardingDatos borrador) {}
