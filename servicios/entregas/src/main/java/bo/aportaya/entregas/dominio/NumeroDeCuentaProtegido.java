package bo.aportaya.entregas.dominio;

import bo.aportaya.plataforma.dominio.CifradorSimetrico;

/**
 * Las tres formas protegidas de un numero de cuenta: el enmascarado que se muestra, el hash que deduplica y el cifrado
 * que se guarda para pagar. Salen SIEMPRE del mismo numero, aca y en el servidor (SEG-007): antes el cifrado podia
 * llegar del cliente y ser de otra cuenta, y entonces se mostraba una y se le pagaba a otra.
 */
public record NumeroDeCuentaProtegido(CuentaEnmascarada enmascarada, String cifrado) {

    public static NumeroDeCuentaProtegido de(String numeroEnClaro, String pimienta, int versionLlave) {
        return new NumeroDeCuentaProtegido(
                CuentaEnmascarada.de(numeroEnClaro, pimienta, versionLlave),
                CifradorSimetrico.cifrar(numeroEnClaro, pimienta));
    }
}
