import 'package:flutter/material.dart';

import '../atomos/avatar.dart';
import '../atomos/barra_de_puntos.dart';
import '../atomos/boton.dart';
import '../atomos/boton_flotante.dart';
import '../atomos/boton_icono.dart';
import '../atomos/boton_variante.dart';
import '../atomos/campo.dart';
import '../atomos/campo_busqueda.dart';
import '../atomos/campo_codigo.dart';
import '../atomos/campo_contrasena.dart';
import '../atomos/campo_monto.dart';
import '../atomos/campo_o_t_p.dart';
import '../atomos/casilla.dart';
import '../atomos/chip_elegible.dart';
import '../atomos/chip_estado.dart';
import '../atomos/codigo_q_r.dart';
import '../atomos/cuenta_enmascarada.dart';
import '../atomos/esqueleto.dart';
import '../atomos/estrellas_calificacion.dart';
import '../atomos/fecha.dart';
import '../atomos/girador.dart';
import '../atomos/grupo_radio.dart';
import '../atomos/interruptor.dart';
import '../atomos/marca.dart';
import '../atomos/monto.dart';
import '../atomos/numero_que_sube.dart';
import '../atomos/progreso.dart';
import '../atomos/punto.dart';
import '../atomos/rueda.dart';
import '../atomos/selector_segmentado.dart';
import '../atomos/tono.dart';
import '../errores.dart';
import '../moleculas/acciones_rapidas.dart';
import '../moleculas/acordeon.dart';
import '../moleculas/alerta.dart';
import '../moleculas/barra_de_pasos.dart';
import '../moleculas/chips_de_filtro.dart';
import '../moleculas/desglose_de_cobro.dart';
import '../moleculas/escalera_de_etapas.dart';
import '../moleculas/fila_de_cotejo.dart';
import '../moleculas/fila_de_movimiento.dart';
import '../moleculas/item_de_notificacion.dart';
import '../moleculas/item_pasanaku.dart';
import '../moleculas/medidor_de_rango.dart';
import '../moleculas/opcion_con_costo.dart';
import '../moleculas/pestanas.dart';
import '../moleculas/reloj_de_plazo.dart';
import '../moleculas/resumen_de_periodo.dart';
import '../moleculas/riel_de_turnos.dart';
import '../moleculas/tarjeta_de_racha.dart';
import '../moleculas/tarjeta_k_p_i.dart';
import '../moleculas/tarjeta_saldo.dart';
import '../moleculas/tipo_de_movimiento.dart';
import '../moviles/barra_pestanas.dart';
import '../moviles/hoja_de_confirmacion.dart';
import '../moviles/notificacion_emergente.dart';
import '../moviles/pantalla_de_onboarding.dart';
import '../moviles/teclado_numerico.dart';
import '../organismos/banner_de_pauta.dart';
import '../organismos/calendario_de_cuotas.dart';
import '../organismos/estado_de_cuota.dart';
import '../organismos/estado_error.dart';
import '../organismos/estado_vacio.dart';
import '../organismos/lista_de_movimientos.dart';
import '../organismos/lista_de_requisitos.dart';
import '../organismos/motivo_vacio.dart';
import '../organismos/panel_bienvenida.dart';
import '../organismos/panel_de_factores.dart';
import '../organismos/panel_sorteo.dart';
import '../organismos/tarjeta_de_oferta.dart';
import '../organismos/tarjeta_de_solicitud.dart';
import '../organismos/vale.dart';
import '../tokens/tokens.dart';

/// **Las muestras del catálogo**: cada pieza en sus variantes, con datos obviamente
/// ficticios. Las consumen Widgetbook (en dispositivo), los goldens y la prueba de
/// accesibilidad, así el catálogo y lo que se prueba son la misma cosa.
///
/// Es el criterio de aceptación visual contra `docs/Views/AportaYa-Maqueta.html`.
final DateTime hoyDeMuestra = DateTime(2026, 9, 9, 10);

typedef Muestra = ({String nombre, Widget Function() widget});

final List<({String grupo, List<Muestra> muestras})> catalogo = [
  (
    grupo: 'Átomos · botones',
    muestras: [
      (
        nombre: 'Primario',
        widget: () => Boton(
          texto: 'Confirmar aporte de Bs 250',
          variante: BotonVariante.primario,
          onPressed: () {},
        ),
      ),
      (
        nombre: 'Secundario',
        widget: () => Boton(texto: 'Ver detalle', onPressed: () {}),
      ),
      (
        nombre: 'Fantasma',
        widget: () => Boton(
          texto: 'Cancelar',
          variante: BotonVariante.fantasma,
          onPressed: () {},
        ),
      ),
      (
        nombre: 'Peligro',
        widget: () => Boton(
          texto: 'Cerrar mi cuenta',
          variante: BotonVariante.peligro,
          onPressed: () {},
        ),
      ),
      (
        nombre: 'Enlace',
        widget: () => Boton(
          texto: 'Sobre este aviso',
          variante: BotonVariante.enlace,
          onPressed: () {},
        ),
      ),
      (
        nombre: 'Deshabilitado',
        widget: () => const Boton(
          texto: 'Confirmar',
          variante: BotonVariante.primario,
          onPressed: null,
        ),
      ),
      (
        nombre: 'Cargando',
        widget: () => Boton(
          texto: 'Enviando',
          variante: BotonVariante.primario,
          cargando: true,
          onPressed: () {},
        ),
      ),
      (
        nombre: 'Ícono',
        widget: () => BotonIcono(
          icono: Icons.qr_code_scanner,
          etiqueta: 'Escanear QR',
          onPressed: () {},
        ),
      ),
      (
        nombre: 'Flotante',
        widget: () => BotonFlotante(
          icono: Icons.add,
          etiqueta: 'Recargar',
          onPressed: () {},
        ),
      ),
    ],
  ),
  (
    grupo: 'Átomos · campos',
    muestras: [
      (
        nombre: 'Texto',
        widget: () =>
            const Campo(etiqueta: 'Nombre', ayuda: 'Como figura en tu carnet'),
      ),
      (
        nombre: 'Error',
        widget: () => const Campo(
          etiqueta: 'Celular',
          error: 'Revisá el número: le falta un dígito',
          valorInicial: '7000000',
        ),
      ),
      (
        nombre: 'Éxito',
        widget: () => const Campo(etiqueta: 'Correo', exito: true),
      ),
      (
        nombre: 'Deshabilitado',
        widget: () => const Campo(etiqueta: 'Documento', habilitado: false),
      ),
      (
        nombre: 'Monto',
        widget: () =>
            CampoMonto(etiqueta: 'Monto a recargar', onChanged: (_) {}),
      ),
      (
        nombre: 'Contraseña',
        widget: () =>
            const CampoContrasena(etiqueta: 'Contraseña', fortaleza: 0.6),
      ),
      (
        nombre: 'Búsqueda',
        widget: () => CampoBusqueda(controlador: TextEditingController()),
      ),
      (nombre: 'OTP', widget: () => CampoOTP(onCompleto: (_) {})),
      (
        nombre: 'OTP con error',
        widget: () => CampoOTP(
          onCompleto: (_) {},
          error: 'El código no coincide. Te quedan 2 intentos.',
        ),
      ),
      (
        nombre: 'Código de invitación',
        widget: () =>
            CampoCodigo(controlador: TextEditingController(text: 'K7P2Q')),
      ),
    ],
  ),
  (
    grupo: 'Átomos · selección',
    muestras: [
      (
        nombre: 'Casilla',
        widget: () => Casilla(
          etiqueta: 'Acepto el contrato de adhesión',
          detalle: 'Versión v3, con hash',
          valor: true,
          onChanged: (_) {},
        ),
      ),
      (
        nombre: 'Interruptor',
        widget: () => Interruptor(
          etiqueta: 'Publicidad segmentada',
          detalle: 'Apagarla no cambia ninguna condición de tu cuenta',
          valor: false,
          onChanged: (_) {},
        ),
      ),
      (
        nombre: 'Radio',
        widget: () => GrupoRadio<int>(
          opciones: const {
            1: 'Pagar en dos partes',
            2: 'Plan de regularización',
          },
          valor: 1,
          onChanged: (_) {},
        ),
      ),
      (
        nombre: 'Segmentado',
        widget: () => SelectorSegmentado<String>(
          opciones: const {'lista': 'Lista', 'cal': 'Calendario'},
          valor: 'lista',
          onChanged: (_) {},
        ),
      ),
    ],
  ),
  (
    grupo: 'Átomos · indicadores',
    muestras: [
      (
        nombre: 'Chips de estado',
        widget: () => const Wrap(
          spacing: Espacio.s2,
          runSpacing: Espacio.s2,
          children: [
            ChipEstado(texto: 'Al día', tono: Tono.ok),
            ChipEstado(texto: 'Vence hoy', tono: Tono.aviso),
            ChipEstado(texto: 'Vencida', tono: Tono.error),
            ChipEstado(texto: 'En revisión', tono: Tono.info),
            ChipEstado(texto: 'Borrador', tono: Tono.neutro),
          ],
        ),
      ),
      (
        nombre: 'Chip elegible',
        widget: () => Wrap(
          spacing: Espacio.s2,
          children: [
            ChipElegible(
              texto: 'Por pagar',
              icono: Icons.schedule,
              seleccionado: true,
              onTap: () {},
            ),
            ChipElegible(
              texto: 'Vencidas',
              icono: Icons.warning_amber,
              onTap: () {},
            ),
            ChipElegible(texto: 'La Ramada', onCerrar: () {}),
          ],
        ),
      ),
      (
        nombre: 'Avatar',
        widget: () => const Row(
          children: [
            Avatar(nombre: 'Rosa Aduviri', tamano: Espacio.s5),
            SizedBox(width: Espacio.s2),
            Avatar(nombre: 'Rosa Aduviri', tamano: Espacio.s6),
            SizedBox(width: Espacio.s2),
            Avatar(nombre: 'Rosa Aduviri'),
            SizedBox(width: Espacio.s2),
            Avatar(nombre: 'Rosa Aduviri', tamano: Espacio.s7 + Espacio.s2),
          ],
        ),
      ),
      (
        nombre: 'Girador',
        widget: () => const Girador(etiqueta: 'Cargando tu saldo'),
      ),
      (
        nombre: 'Progreso',
        widget: () => const Column(
          children: [
            Progreso(valor: 0.6, etiqueta: 'Avance del ciclo'),
            SizedBox(height: Espacio.s3),
            Progreso(
              valor: 0.3,
              etiqueta: 'Fondo consumido',
              anillo: true,
              tono: Tono.aviso,
            ),
          ],
        ),
      ),
      (nombre: 'Esqueleto', widget: () => const Esqueleto()),
      (
        nombre: 'Punto',
        widget: () => const Row(
          children: [
            Punto(),
            SizedBox(width: Espacio.s3),
            Punto(cuenta: 3),
          ],
        ),
      ),
      (
        nombre: 'QR',
        widget: () => const CodigoQR(
          contenido: 'aportaya://unirse/K7P2Q',
          etiqueta: 'Código QR de la invitación',
          tamano: Espacio.s7 * 3,
        ),
      ),
      (
        nombre: 'Cuenta enmascarada',
        widget: () => const CuentaEnmascarada(
          numero: '1234567890',
          banco: 'Banco de ejemplo',
        ),
      ),
      (nombre: 'Fecha', widget: () => const Fecha(iso: '2026-09-09T14:30:00Z')),
      (
        nombre: 'Estrellas',
        widget: () => EstrellasCalificacion(valor: 4, onChanged: (_) {}),
      ),
      (
        nombre: 'Puntos del tour',
        widget: () => const BarraDePuntos(total: 4, actual: 1),
      ),
      (
        nombre: 'Monto',
        widget: () => const Monto(
          monto: '1240.00',
          moneda: 'BOB',
          etiqueta: 'Saldo disponible',
        ),
      ),
      (nombre: 'Marca', widget: () => const Marca(tamano: 72)),
      (
        nombre: 'Puntaje que sube',
        widget: () => const NumeroQueSube(valor: 720, etiqueta: 'Tu puntaje'),
      ),
      (
        nombre: 'Rueda · en una lista',
        widget: () => const Rueda(turnos: 10, cobrados: 3, miTurno: 5),
      ),
      (
        nombre: 'Rueda · en la pantalla del grupo',
        widget: () => const Rueda(
          turnos: 10,
          cobrados: 3,
          miTurno: 5,
          turnoActual: 3,
          diametro: 200,
        ),
      ),
      (
        nombre: 'Rueda · completa',
        widget: () => const Rueda(turnos: 8, cobrados: 8, miTurno: 1),
      ),
    ],
  ),
  (
    grupo: 'Moléculas',
    muestras: [
      (
        nombre: 'Tarjeta de saldo',
        widget: () => TarjetaSaldo(
          disponible: '1240.00',
          retenido: '150.00',
          enPasanakus: '2400.00',
          moneda: 'BOB',
          accionPrincipal: Boton(
            texto: 'Recargar',
            variante: BotonVariante.primario,
            onPressed: () {},
          ),
          accionSecundaria: Boton(
            texto: 'Retirar',
            variante: BotonVariante.sobreVerde,
            onPressed: () {},
          ),
        ),
      ),
      (
        nombre: 'Tarjeta KPI',
        widget: () => const TarjetaKPI(
          titulo: 'Tasa de morosidad',
          valor: Text('7,4 %'),
          variacion: '+1,2 puntos vs. mes anterior',
          provisorio: true,
          tono: Tono.error,
        ),
      ),
      (nombre: 'Racha', widget: () => const TarjetaDeRacha(meses: 6, hito: 12)),
      (
        nombre: 'Racha rota',
        widget: () => TarjetaDeRacha(
          meses: 6,
          hito: 12,
          rota: true,
          salida: Boton(
            texto: 'Poner al día la cuota',
            variante: BotonVariante.primario,
            onPressed: () {},
          ),
        ),
      ),
      (
        nombre: 'Ítem de pasanaku',
        widget: () => ItemPasanaku(
          nombre: 'La Ramada',
          rol: 'Organiza Rosa A.',
          avance: 0.5,
          estado: 'Al día',
          onTap: () {},
        ),
      ),
      (
        nombre: 'Fila de movimiento',
        widget: () => const Column(
          children: [
            FilaDeMovimiento(
              tipo: TipoDeMovimiento.aporte,
              concepto: 'Aporte · La Ramada',
              monto: '-250.00',
              moneda: 'BOB',
            ),
            FilaDeMovimiento(
              tipo: TipoDeMovimiento.recargaQR,
              concepto: 'Recarga por QR',
              monto: '500.00',
              moneda: 'BOB',
            ),
            FilaDeMovimiento(
              tipo: TipoDeMovimiento.comision,
              concepto: 'Comisión de entrega',
              monto: '-10.00',
              moneda: 'BOB',
              pendiente: true,
            ),
          ],
        ),
      ),
      (
        nombre: 'Resumen de período',
        widget: () => const ResumenDePeriodo(
          titulo: 'Septiembre 2026',
          entro: '1500.00',
          salio: '760.00',
          moneda: 'BOB',
          saldoInicial: '500.00',
          saldoFinal: '1240.00',
        ),
      ),
      (
        nombre: 'Chips de filtro',
        widget: () => ChipsDeFiltro<String>(
          opciones: const [
            (clave: 'todo', texto: 'Todo', icono: Icons.all_inclusive),
            (clave: 'aportes', texto: 'Aportes', icono: Icons.groups_outlined),
            (clave: 'recargas', texto: 'Recargas', icono: Icons.qr_code_2),
            (clave: 'retiros', texto: 'Retiros', icono: Icons.outbox_outlined),
            (
              clave: 'entregas',
              texto: 'Entregas',
              icono: Icons.card_giftcard_outlined,
            ),
          ],
          valor: 'aportes',
          onChanged: (_) {},
        ),
      ),
      (
        nombre: 'Alerta',
        widget: () => const Column(
          children: [
            Alerta(
              titulo: 'Tu cuenta tiene una restricción',
              detalle:
                  'Bs 400 retenidos por el oficio 123/2026. Consultá en soporte.',
              tono: Tono.aviso,
            ),
            SizedBox(height: Espacio.s2),
            Alerta(titulo: 'Listo, tu aporte quedó guardado', tono: Tono.ok),
          ],
        ),
      ),
      (
        nombre: 'Ítem de notificación',
        widget: () => ItemDeNotificacion(
          titulo: 'Te toca en 2 turnos',
          detalle: 'La Ramada · te avisamos cuando se acredite',
          cuando: 'hace 2 h',
          tono: Tono.ok,
          onTap: () {},
        ),
      ),
      (
        nombre: 'Acciones rápidas',
        widget: () => AccionesRapidas(
          acciones: [
            (texto: 'Recargar', icono: Icons.add, onTap: () {}),
            (texto: 'Retirar', icono: Icons.outbox_outlined, onTap: () {}),
            (
              texto: 'Vales',
              icono: Icons.confirmation_number_outlined,
              onTap: () {},
            ),
            (texto: 'Aportes', icono: Icons.groups_outlined, onTap: () {}),
          ],
        ),
      ),
      (
        nombre: 'Barra de pasos',
        widget: () => const BarraDePasos(
          total: 8,
          actual: 3,
          nombre: 'Anverso del carnet',
        ),
      ),
      (
        nombre: 'Reloj de plazo',
        widget: () => RelojDePlazo(
          etiqueta: 'Plazo para responder',
          venceIso: '2026-09-11T23:59:00Z',
          ahora: hoyDeMuestra,
          norma: '5 días hábiles · RNSF Libro 4 Título I',
        ),
      ),
      (
        nombre: 'Fila de cotejo',
        widget: () => Column(
          children: [
            const FilaDeCotejo(
              campo: 'Nombre',
              declarado: 'Ana Quispe',
              leido: 'Ana Quispe',
            ),
            const SizedBox(height: Espacio.s2),
            FilaDeCotejo(
              campo: 'Fecha de nacimiento',
              declarado: '12/04/1995',
              leido: '21/04/1995',
              onCorregir: () {},
            ),
          ],
        ),
      ),
      (
        nombre: 'Opción con costo',
        widget: () => OpcionConCosto(
          titulo: 'Pagar en dos partes',
          queHace:
              'Mitad ahora, mitad en 15 días; el fondo cubre la diferencia.',
          costo: 'Sin recargo si cumplís las dos fechas',
          onElegir: () {},
          tono: Tono.ok,
        ),
      ),
      (
        nombre: 'Escalera de etapas',
        widget: () => const EscaleraDeEtapas(
          etapas: [
            (
              nombre: 'Preventiva',
              dias: '−3 a 0',
              canales: 'Push · WhatsApp · en la app',
              topeSemanal: 1,
            ),
            (
              nombre: 'Temprana',
              dias: '1 a 7',
              canales: 'Push · WhatsApp · SMS',
              topeSemanal: 3,
            ),
            (
              nombre: 'Administrativa',
              dias: '8 a 30',
              canales: 'WhatsApp · SMS · llamada',
              topeSemanal: 3,
            ),
          ],
          actual: 1,
        ),
      ),
      (
        nombre: 'Medidor de rango',
        widget: () => const MedidorDeRango(
          etiqueta: 'Valor de ceder tu turno',
          minimo: '40.00',
          maximo: '120.00',
          moneda: 'BOB',
          estimado: '85.00',
        ),
      ),
      (
        nombre: 'Desglose de cobro',
        widget: () => const DesgloseDeCobro(
          lineas: [
            (concepto: 'Bolsa del turno', monto: '3000.00'),
            (concepto: 'Comisión de entrega', monto: '-10.00'),
            (concepto: 'Descuento por nivel (25 %)', monto: '2.50'),
          ],
          neto: '2992.50',
          moneda: 'BOB',
          nota:
              'Con una bolsa chica manda el piso de Bs 10 y el descuento vale poco.',
        ),
      ),
      (
        nombre: 'Riel de turnos',
        widget: () => const RielDeTurnos(total: 10, actual: 3, mio: 6),
      ),
      (
        nombre: 'Pestañas',
        widget: () => Pestanas(
          titulos: const ['Lista', 'Calendario'],
          actual: 0,
          onChanged: (_) {},
        ),
      ),
      (
        nombre: 'Acordeón',
        widget: () => const Acordeon(
          titulo: 'Reglamento del grupo',
          abierto: true,
          child: Text(
            'Aporte de Bs 250 cada mes, el día 10. Lo aprobó el grupo por votación.',
          ),
        ),
      ),
    ],
  ),
  (
    grupo: 'Organismos',
    muestras: [
      (
        nombre: 'Estado vacío',
        widget: () => const EstadoVacio(
          mensaje:
              'Todavía no tenés movimientos. Cuando recargues saldo van a aparecer acá.',
        ),
      ),
      (
        nombre: 'Estado vacío por filtro',
        widget: () => const EstadoVacio(
          mensaje: 'Ningún movimiento coincide con el filtro. Probá con otro.',
          motivo: MotivoVacio.porFiltro,
        ),
      ),
      (
        nombre: 'Estado vacío por permiso',
        widget: () => const EstadoVacio(
          mensaje: 'No tenés acceso a esto.',
          motivo: MotivoVacio.porPermiso,
        ),
      ),
      (
        nombre: 'Estado error',
        widget: () => EstadoError(error: _ErrorDeMuestra(), reintentar: () {}),
      ),
      (
        nombre: 'Estado sin conexión',
        widget: () => EstadoError(error: _SinRedDeMuestra(), reintentar: () {}),
      ),
      (
        nombre: 'Lista de movimientos',
        widget: () => const SizedBox(
          height: Espacio.s7 * 6,
          child: ListaDeMovimientos(
            moneda: 'BOB',
            movimientos: [
              (
                fechaIso: '2026-09-09T10:00:00Z',
                tipo: TipoDeMovimiento.aporte,
                concepto: 'Aporte · La Ramada',
                monto: '-250.00',
                saldoCorrido: '1240.00',
                pendiente: false,
              ),
              (
                fechaIso: '2026-09-09T08:00:00Z',
                tipo: TipoDeMovimiento.recargaQR,
                concepto: 'Recarga por QR',
                monto: '500.00',
                saldoCorrido: '1490.00',
                pendiente: false,
              ),
              (
                fechaIso: '2026-09-08T18:00:00Z',
                tipo: TipoDeMovimiento.bono,
                concepto: 'Bono de bienvenida',
                monto: '10.00',
                saldoCorrido: '990.00',
                pendiente: false,
              ),
            ],
          ),
        ),
      ),
      (
        nombre: 'Lista de requisitos',
        widget: () => const ListaDeRequisitos(
          requisitos: [
            (
              codigo: 'HAB_APRENDIZ_ANTIGUEDAD',
              nombre: 'Antigüedad',
              umbral: '3 meses',
              tuValor: '5 meses',
              cumplido: true,
            ),
            (
              codigo: 'HAB_APRENDIZ_REPUTACION',
              nombre: 'Reputación',
              umbral: '600',
              tuValor: '540',
              cumplido: false,
            ),
            (
              codigo: 'HAB_APRENDIZ_CAPACITACION',
              nombre: 'Capacitación',
              umbral: '1 módulo',
              tuValor: '0',
              cumplido: false,
            ),
          ],
        ),
      ),
      (
        nombre: 'Panel de factores',
        widget: () => const PanelDeFactores(
          veredicto: 'La permuta no pasa la validación',
          aprobado: false,
          motivo: 'Lo dejaría cobrando la bolsa con 6 cuotas por aportar.',
          factores: [
            (
              nombre: 'Cuotas por aportar',
              valor: '6',
              umbral: '≤ 4',
              pasa: false,
            ),
            (nombre: 'Historial', valor: '2 ciclos', umbral: '≥ 1', pasa: true),
            (nombre: 'Mora abierta', valor: 'no', umbral: 'no', pasa: true),
            (
              nombre: 'Riesgo del grupo',
              valor: 'bajo',
              umbral: 'medio',
              pasa: true,
            ),
            (nombre: 'Compensación', valor: '4 %', umbral: '≤ 5 %', pasa: true),
          ],
        ),
      ),
      (
        nombre: 'Banner de pauta',
        widget: () => BannerDePauta(
          anunciante: 'Ferretería Ejemplo',
          titulo: '8 % menos en herramientas eléctricas',
          detalle: 'Con tu vale de AportaYa, hasta el 30 de septiembre',
          onSobreEsteAviso: () {},
        ),
      ),
      (
        nombre: 'Panel de bienvenida',
        widget: () => PanelBienvenida(
          nombre: 'Ana',
          bono: '10.00',
          moneda: 'BOB',
          pasos: [
            (texto: 'Recargá tu saldo', hecho: false, onTap: () {}),
            (texto: 'Entrá a un grupo con código', hecho: false, onTap: () {}),
            (texto: 'Activá la huella', hecho: true, onTap: () {}),
          ],
        ),
      ),
      (
        nombre: 'Tarjeta de solicitud',
        widget: () => TarjetaDeSolicitud(
          nombre: 'Juan Mamani',
          venceIso: '2026-09-10T12:00:00Z',
          aFavor: const ['2 pasanakus completos', '14 aportes en fecha'],
          enContra: const ['Es su primer grupo de este monto'],
          onAceptar: () {},
          onRechazar: (_) {},
          venceHoy: true,
        ),
      ),
      (
        nombre: 'Tarjeta de oferta',
        widget: () => TarjetaDeOferta(
          quien: 'Marta C.',
          turnoQueCede: 3,
          turnoQuePide: 7,
          compensacion: '85.00',
          moneda: 'BOB',
          estado: 'PUBLICADA',
          onAceptar: () {},
        ),
      ),
      (
        nombre: 'Vale',
        widget: () => const Vale(
          comercio: 'Ferretería Ejemplo',
          beneficio: '8 % de descuento',
          estado: 'Disponible',
          origen: 'Por completar tu primer ciclo',
          condiciones: [
            'Válido en las tres sucursales',
            'No acumulable',
            'Uno por persona',
          ],
          codigoCorto: 'H4K9-2P',
        ),
      ),
      (
        nombre: 'Panel del sorteo',
        widget: () => PanelSorteo(
          pasos: const [
            (
              nombre: 'Compromiso publicado',
              hash: 'sha256:9f3a…c21e',
              cuando: '1 sep 2026, 09:00',
            ),
            (
              nombre: 'Lista de cupos sellada',
              hash: 'sha256:1b7d…88aa',
              cuando: '1 sep 2026, 09:00',
            ),
            (
              nombre: 'Semilla de fuente externa',
              hash: 'sha256:44c0…0f19',
              cuando: '1 sep 2026, 12:00',
            ),
            (
              nombre: 'Ejecución',
              hash: 'sha256:e2e2…7bd4',
              cuando: '1 sep 2026, 12:01',
            ),
            (
              nombre: 'Notificación',
              hash: 'sha256:0a0a…9c31',
              cuando: '1 sep 2026, 12:01',
            ),
          ],
          coincide: true,
          onReproducir: () {},
          onVerificacionPublica: () {},
        ),
      ),
      (
        nombre: 'Calendario de cuotas',
        widget: () => CalendarioDeCuotas(
          mes: DateTime(2026, 9),
          hoy: hoyDeMuestra,
          moneda: 'BOB',
          onMes: (_) {},
          posicion: 'Mes 6 de 14, hasta que se cierre el último pasanaku',
          cuotas: const [
            (
              fechaIso: '2026-09-05',
              grupo: 'La Ramada',
              monto: '250.00',
              estado: EstadoDeCuota.pagada,
            ),
            (
              fechaIso: '2026-09-10',
              grupo: 'Compañeras del taller',
              monto: '200.00',
              estado: EstadoDeCuota.pendiente,
            ),
            (
              fechaIso: '2026-09-10',
              grupo: 'La Ramada',
              monto: '250.00',
              estado: EstadoDeCuota.vencida,
            ),
            (
              fechaIso: '2026-09-25',
              grupo: 'La Ramada',
              monto: '250.00',
              estado: EstadoDeCuota.futura,
            ),
          ],
        ),
      ),
    ],
  ),
  (
    grupo: 'Móviles',
    muestras: [
      (
        nombre: 'Tab bar',
        widget: () => BarraPestanas(
          destinos: const [
            (
              texto: 'Inicio',
              icono: Icons.home_outlined,
              iconoActivo: Icons.home,
              novedades: 0,
            ),
            (
              texto: 'Grupos',
              icono: Icons.groups_outlined,
              iconoActivo: Icons.groups,
              novedades: 2,
            ),
            (
              texto: 'Movimientos',
              icono: Icons.receipt_long_outlined,
              iconoActivo: Icons.receipt_long,
              novedades: 0,
            ),
            (
              texto: 'Perfil',
              icono: Icons.person_outline,
              iconoActivo: Icons.person,
              novedades: 0,
            ),
          ],
          actual: 0,
          onChanged: (_) {},
        ),
      ),
      (
        nombre: 'Hoja de confirmación',
        widget: () => HojaDeConfirmacion(
          titulo: 'Confirmar aporte de Bs 250',
          detalle: const Text(
            'La Ramada · período de septiembre · se debita de tu saldo disponible.',
          ),
          textoConfirmar: 'Confirmar aporte de Bs 250',
          onConfirmar: () {},
          onCancelar: () {},
        ),
      ),
      (
        nombre: 'Teclado numérico',
        widget: () => TecladoNumerico(onTecla: (_) {}, onBorrar: () {}),
      ),
      (
        nombre: 'Onboarding',
        widget: () => SizedBox(
          height: Espacio.s7 * 14,
          child: PantallaDeOnboarding(
            ilustracion: const Icon(Icons.groups, size: Espacio.s7 * 2),
            titulo: 'El pasanaku de siempre, sin el cuaderno',
            texto:
                'Los mismos turnos, sin que nadie tenga que juntar ni guardar.',
            pie: Column(
              children: [
                const BarraDePuntos(total: 4, actual: 0),
                const SizedBox(height: Espacio.s4),
                Boton(
                  texto: 'Siguiente',
                  variante: BotonVariante.primario,
                  onPressed: () {},
                  expandido: true,
                ),
              ],
            ),
          ),
        ),
      ),
      (
        nombre: 'Notificación emergente',
        widget: () => NotificacionEmergente(
          titulo: 'Recibiste Bs 250',
          detalle: 'Entró una recarga por QR',
          enEspera: 2,
          onAbrir: () {},
          onCerrar: () {},
        ),
      ),
    ],
  ),
];

class _ErrorDeMuestra implements Exception, ErrorPresentable {
  @override
  String get mensaje =>
      'No pudimos cobrar. Revisá tu saldo e intentá de nuevo.';
  @override
  String? get trazaId => 'traza-de-muestra';
  @override
  bool get sinConexion => false;
}

class _SinRedDeMuestra implements Exception, ErrorPresentable {
  @override
  String get mensaje =>
      'No hay conexión. Te mostramos lo último que vimos; para operar hace falta señal.';
  @override
  String? get trazaId => null;
  @override
  bool get sinConexion => true;
}
