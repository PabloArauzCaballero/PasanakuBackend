package bo.aportaya.grupos;

import static org.mockito.Mockito.mock;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

/**
 * Paga UNA vez por JVM, al abrir la sesion de JUnit, el arranque de Mockito (ByteBuddy, Objenesis y el
 * mock maker inline): 0,7-0,8 s de CPU medidos aunque el primer doble sea de {@code Runnable}.
 *
 * <p>Sin esto, ese costo caia dentro del {@code @BeforeEach} de la primera clase que mockea, cronometrado con
 * el limite de 5 s de los atomos. Con la CPU del runner repartida entre modulos, {@code
 * AdmisionDelGrupoSustitucionTest.armar()} superaba los 5 s y fallaba de forma intermitente. El arranque de la
 * herramienta no es trabajo de la prueba, igual que el del contenedor (ver {@code beforeall.method.default} en
 * {@code aportaya.base.gradle.kts}); el limite de cada prueba no cambia.
 */
public final class MockitoAlAbrirLaSesion implements LauncherSessionListener {
    @Override
    public void launcherSessionOpened(LauncherSession sesion) {
        mock(Runnable.class);
    }
}
