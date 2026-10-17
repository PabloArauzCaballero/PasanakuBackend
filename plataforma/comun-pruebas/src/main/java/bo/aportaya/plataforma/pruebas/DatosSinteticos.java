package bo.aportaya.plataforma.pruebas;

import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import net.datafaker.Faker;

/**
 * Datos de persona y montos SINTETICOS para los fixtures, con Datafaker y semilla fija (tableros, H2.S3).
 *
 * <p>Determinista: la misma semilla da la misma secuencia, asi un test que falla vuelve a fallar con los mismos datos.
 * Nada de esto es real (regla 97.6): los correos van a un dominio de prueba y los telefonos a un rango que ningun seed
 * usa. Los montos salen en {@link Dinero}, con dos decimales exactos: nunca un {@code double} (regla 91.1.1).
 */
public final class DatosSinteticos {

    /** La semilla de todos los fixtures que no piden otra. */
    public static final long SEMILLA = 20261016L;

    private final Faker faker;
    private final Random azar;
    private final AtomicInteger secuencia = new AtomicInteger();

    private DatosSinteticos(long semilla) {
        this.azar = new Random(semilla);
        this.faker = new Faker(Locale.forLanguageTag("es"), new Random(semilla));
    }

    public static DatosSinteticos conSemilla(long semilla) {
        return new DatosSinteticos(semilla);
    }

    public static DatosSinteticos porDefecto() {
        return new DatosSinteticos(SEMILLA);
    }

    public String nombres() {
        return faker.name().firstName();
    }

    public String apellidos() {
        return faker.name().lastName() + " " + faker.name().lastName();
    }

    /** Unico por instancia: lleva el numero de secuencia. */
    public String correo() {
        String base = (faker.name().firstName() + faker.name().lastName())
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z]", "");
        return (base.isEmpty() ? "persona" : base) + "." + secuencia.incrementAndGet() + "@sintetico.pasanaku.test";
    }

    /** E.164 boliviano en el rango +59178, que ningun seed usa. Unico por instancia. */
    public String telefono() {
        return "+59178" + String.format(Locale.ROOT, "%06d", secuencia.incrementAndGet());
    }

    /** Un carnet de 7 digitos que no empieza en cero. */
    public String documento() {
        return String.valueOf(1_000_000 + azar.nextInt(9_000_000));
    }

    public LocalDate nacimiento() {
        return faker.timeAndDate().birthday(21, 65);
    }

    /** Un importe entre {@code minimo} y {@code maximo}, con dos decimales exactos. */
    public Dinero monto(String minimo, String maximo, Moneda moneda) {
        BigDecimal desde = new BigDecimal(minimo);
        BigDecimal rango = new BigDecimal(maximo).subtract(desde);
        long centavos = rango.movePointRight(2).longValueExact();
        BigDecimal elegido = desde.add(
                BigDecimal.valueOf(Math.floorMod(azar.nextLong(), centavos + 1)).movePointLeft(2));
        return Dinero.de(elegido.setScale(2, RoundingMode.UNNECESSARY), moneda);
    }

    /** Una fecha dentro de los ultimos {@code dias}, contando desde {@code hoy}. */
    public LocalDate fechaEnLosUltimos(int dias, LocalDate hoy) {
        return hoy.minusDays(azar.nextInt(dias + 1));
    }
}
