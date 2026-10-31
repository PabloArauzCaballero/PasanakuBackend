package bo.aportaya.transparencia.aplicacion;

import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.transparencia.dominio.EtapasDeReputacion;
import bo.aportaya.transparencia.infraestructura.EtapaRepositorio;
import bo.aportaya.transparencia.infraestructura.ModeloRepositorio;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El catalogo de etapas del modelo de scoring vigente, de la mas baja a la mas alta
 * (etapas de reputacion, 2026-10-30).
 *
 * <p>Viaja entero y sin paginar: es un catalogo cerrado de pocas filas fijas, no una coleccion
 * que crece (regla 96.6 admite el caso; el contrato fija {@code maxItems: 50}).
 *
 * <p>Cada etapa lleva su desbloqueo con el {@code estado}: la app muestra el {@code PROPUESTO}
 * como «por confirmar», y solo el {@code VIGENTE} limita algo (regla 97.5.4). Quien aplica el
 * limite es grupos, al crear y al entrar; aca solo se publica el catalogo.
 *
 * <p>Lista vacia si no hay modelo vigente: sin modelo no hay escala, y sin escala no hay
 * etapas. No es un error del que pregunta.
 */
@Service
public class ListarEtapas {

    private final Datos datos;
    private final ModeloRepositorio modelos;
    private final EtapaRepositorio etapas;
    private final Reloj reloj;

    public ListarEtapas(Datos datos, ModeloRepositorio modelos, EtapaRepositorio etapas, Reloj reloj) {
        this.datos = datos;
        this.modelos = modelos;
        this.etapas = etapas;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public List<EtapasDeReputacion.Etapa> ejecutar(ContextoSesion ctx) {
        return datos.conContexto(
                ctx, dsl -> modelos.modeloVigente(dsl, reloj.ahora().atOffset(ZoneOffset.UTC))
                        .map(modelo -> etapas.etapasDelModelo(dsl, modelo.id()))
                        .orElse(List.of()));
    }
}
