package bo.aportaya.cumplimiento.aplicacion;

import bo.aportaya.cumplimiento.dominio.ContratoPublicado;
import bo.aportaya.cumplimiento.infraestructura.ContratoRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CU-05 · Los contratos de adhesion que rigen hoy.
 *
 * <p><b>Existe por un hueco concreto.</b> {@code aceptaContratos} de CU-01 son UUID, y
 * los ids de {@code cumplimiento.contrato_adhesion} son {@code gen_random_uuid()}: uno
 * distinto por entorno. Sin una forma de preguntarlos, la app no tenia como armar el
 * alta y mandaba codigos inventados —{@code 'ADHESION'}, {@code 'TARIFARIO'}—, que el
 * servidor rechazaba al deserializar. El alta entera no llegaba al backend por esto.
 *
 * <p><b>Es de solo lectura y no emite evento.</b> Consultar que contrato rige no es un
 * acto del que haya que dejar rastro; el rastro es la aceptacion, y esa la escribe
 * {@link CU05AceptarContrato} con su evidencia sellada.
 *
 * <p>Corre con el contexto que le pasen —desde la ruta publica, el del sistema—, porque
 * se consulta durante el alta, cuando todavia no hay usuario. No devuelve dato de
 * nadie: solo lo que ya esta publicado en el sitio web.
 */
@Service
public class CU05ConsultarContratosVigentes {

    private final Datos datos;
    private final ContratoRepositorio contratos;

    public CU05ConsultarContratosVigentes(Datos datos, ContratoRepositorio contratos) {
        this.datos = datos;
        this.contratos = contratos;
    }

    /** Solo lectura, pero transaccional igual: {@link Datos} exige transaccion viva. */
    @Transactional(readOnly = true)
    public List<ContratoPublicado> ejecutar(ContextoSesion ctx) {
        return datos.conContexto(ctx, contratos::vigentes);
    }
}
