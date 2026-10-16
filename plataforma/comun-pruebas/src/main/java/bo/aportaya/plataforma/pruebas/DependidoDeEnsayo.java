package bo.aportaya.plataforma.pruebas;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Un servicio dependido de mentira, que se puede romper a proposito: la pieza para ejercitar de
 * verdad la caida de un dependido (regla 98.8.4) sin Docker y sin otro servicio.
 *
 * <p>Escucha en loopback y en un puerto libre. Responde lo que se le diga ({@link #responder}),
 * puede tardar ({@link #tardar}), y puede CAERSE ({@link #caer}): el puerto deja de aceptar y
 * el cliente recibe «connection refused» como con un contenedor apagado. Cuenta cuantas
 * peticiones le llegaron realmente, que es lo que distingue un cortacircuitos abierto («no
 * llamo») de uno que solo falla rapido («llamo y falla»).
 *
 * <p>Es un doble declarado: prueba el cliente, no al servicio real. La integracion contra el
 * servicio de verdad sigue haciendo falta (regla 65).
 */
public final class DependidoDeEnsayo implements AutoCloseable {

    private final AtomicInteger peticiones = new AtomicInteger();
    private volatile int estado = 200;
    private volatile String cuerpo = "{}";
    private volatile long demoraMs;
    private HttpServer servidor;
    private final int puerto;

    public DependidoDeEnsayo() {
        this.servidor = arrancar(0);
        this.puerto = servidor.getAddress().getPort();
    }

    public String urlBase() {
        return "http://127.0.0.1:" + puerto;
    }

    /** Lo que responde mientras este vivo. */
    public DependidoDeEnsayo responder(int estadoHttp, String cuerpoJson) {
        this.estado = estadoHttp;
        this.cuerpo = cuerpoJson;
        return this;
    }

    /** Cuanto tarda en responder: para probar el tiempo maximo del cliente. */
    public DependidoDeEnsayo tardar(long milisegundos) {
        this.demoraMs = milisegundos;
        return this;
    }

    /** Se apaga: el puerto deja de aceptar conexiones. */
    public synchronized void caer() {
        if (servidor != null) {
            servidor.stop(0);
            servidor = null;
        }
    }

    /** Vuelve a levantar en el MISMO puerto: lo que hace un orquestador al reiniciar el contenedor. */
    public synchronized void levantar() {
        if (servidor == null) {
            servidor = arrancar(puerto);
        }
    }

    /** Peticiones que llegaron de verdad al servidor (las que un cortacircuitos abierto no deja pasar). */
    public int peticionesRecibidas() {
        return peticiones.get();
    }

    private HttpServer arrancar(int puertoDeseado) {
        try {
            HttpServer nuevo =
                    HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), puertoDeseado), 0);
            nuevo.createContext("/", intercambio -> {
                peticiones.incrementAndGet();
                try {
                    if (demoraMs > 0) {
                        Thread.sleep(demoraMs);
                    }
                    byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
                    intercambio.getResponseHeaders().add("Content-Type", "application/json");
                    intercambio.sendResponseHeaders(estado, bytes.length == 0 ? -1 : bytes.length);
                    if (bytes.length > 0) {
                        intercambio.getResponseBody().write(bytes);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    intercambio.close();
                }
            });
            nuevo.start();
            return nuevo;
        } catch (IOException e) {
            throw new IllegalStateException("no pude levantar el dependido de ensayo", e);
        }
    }

    @Override
    public void close() {
        caer();
    }
}
