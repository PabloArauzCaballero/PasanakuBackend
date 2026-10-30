package bo.aportaya.transparencia.aplicacion;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.transparencia.dominio.ArenasDeReputacion;
import bo.aportaya.transparencia.infraestructura.ArenaRepositorio;
import bo.aportaya.transparencia.infraestructura.ModeloRepositorio;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El catalogo de arenas del modelo de scoring vigente, de la mas baja a la mas alta
 * (arenas de reputacion, 2026-10-30).
 *
 * <p>Viaja entero y sin paginar: es un catalogo cerrado de pocas filas fijas, no una coleccion
 * que crece (regla 96.6 admite el caso; el contrato fija {@code maxItems: 50}).
 *
 * <p>Cada arena lleva su desbloqueo con el {@code estado}: la app muestra el {@code PROPUESTO}
 * como «por confirmar», y solo el {@code VIGENTE} limita algo (regla 97.5.4). Quien aplica el
 * limite es grupos, al crear y al entrar; aca solo se publica el catalogo.
 *
 * <p>Lista vacia si no hay modelo vigente: sin modelo no hay escala, y sin escala no hay
 * arenas. No es un error del que pregunta.
 */
@Service
public class ListarArenas {

    private final Datos datos;
    private final ModeloRepositorio modelos;
    private final ArenaRepositorio arenas;
    private final Reloj reloj;

    public ListarArenas(Datos datos, ModeloRepositorio modelos, ArenaRepositorio arenas, Reloj reloj) {
        this.datos = datos;
        this.modelos = modelos;
        this.arenas = arenas;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<ArenasDeReputacion.Arena> ejecutar(ContextoSesion ctx) {
        return datos.conContexto(
                ctx, dsl -> modelos.modeloVigente(dsl, reloj.ahora().atOffset(ZoneOffset.UTC))
                        .map(modelo -> arenas.arenasDelModelo(dsl, modelo.id()))
                        .orElse(List.of()));
    }
}
