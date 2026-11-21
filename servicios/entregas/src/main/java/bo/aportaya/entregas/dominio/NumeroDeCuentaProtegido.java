package bo.aportaya.entregas.dominio;

import bo.aportaya.plataforma.dominio.CifradorSimetrico;

/**
 * Las tres formas protegidas de un numero de cuenta: el enmascarado que se muestra, el hash que deduplica y el cifrado
 * que se guarda para pagar. Salen SIEMPRE del mismo numero, aca y en el servidor (SEG-007): antes el cifrado podia
 * llegar del cliente y ser de otra cuenta, y entonces se mostraba una y se le pagaba a otra.
 */
public record NumeroDeCuentaProtegido(CuentaEnmascarada enmascarada, String cifrado) {

    /**
     * @param pimienta el pepper del hash que deduplica
     * @param llaveDeCifrado la llave del cifrado que se guarda; propia y distinta de la pimienta (F1.L2.M07)
     */
    public static NumeroDeCuentaProtegido de(
            String numeroEnClaro, String pimienta, int versionLlave, String llaveDeCifrado) {
        return new NumeroDeCuentaProtegido(
                CuentaEnmascarada.de(numeroEnClaro, pimienta, versionLlave),
                CifradorSimetrico.cifrar(numeroEnClaro, llaveDeCifrado));
    }
}
