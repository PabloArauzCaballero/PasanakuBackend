package bo.aportaya.grupos.dominio;

import java.util.List;

/**
 * Lo que hace falta para rehacer un sorteo desde afuera.
 *
 * <p>El hash comprometido, la semilla revelada, las entropias que aportaron los
 * participantes, el metodo y el orden que salio. Con eso cualquiera recomputa el
 * resultado y comprueba que nadie lo eligio.
 *
 * <p>Los cupos viajan como numeros de cupo: el orden original (el que se baraja) es ascendente y el publicado
 * es el que salio. Las entropias incluyen la del snapshot congelado ({@code snapshot:<hash>}), que forma parte
 * de la preimagen del compromiso: sin ella el hash no se puede recomputar desde afuera.
 *
 * <p>Antes del revelado la semilla es nula, y ese es exactamente el punto: comprometer
 * y revelar existen para que nadie —nosotros incluidos— pueda elegir el resultado
 * despues de conocerlo.
 */
public record PaqueteDeSorteo(
        String hashComprometido,
        String semillaRevelada,
        String metodo,
        List<String> entropias,
        List<String> cuposEnOrdenOriginal,
        List<String> ordenPublicado) {}
