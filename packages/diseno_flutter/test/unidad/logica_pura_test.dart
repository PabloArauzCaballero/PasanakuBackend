import 'package:aportaya_diseno/atomos/campo_monto.dart';
import 'package:aportaya_diseno/atomos/cuenta_enmascarada.dart';
import 'package:aportaya_diseno/atomos/fecha.dart';
import 'package:aportaya_diseno/moleculas/reloj_de_plazo.dart';
import 'package:aportaya_diseno/organismos/calendario_de_cuotas.dart';
import 'package:aportaya_diseno/organismos/cuota.dart';
import 'package:aportaya_diseno/organismos/estado_de_cuota.dart';
import 'package:aportaya_diseno/organismos/lista_de_movimientos.dart';
import 'package:flutter_test/flutter_test.dart';

/// Los átomos con aritmética o reglas: se prueban en milisegundos, sin widget.
void main() {
  group('CampoMonto.aCadenaDelContrato', () {
    test(
      'acepta lo que escribe una persona y devuelve la cadena del contrato',
      () {
        expect(CampoMonto.aCadenaDelContrato('1.240,50'), '1240.50');
        expect(CampoMonto.aCadenaDelContrato('12'), '12.00');
        expect(CampoMonto.aCadenaDelContrato('12,5'), '12.50');
        expect(CampoMonto.aCadenaDelContrato('0,05'), '0.05');
      },
    );
    test('rechaza lo que no es un importe', () {
      expect(CampoMonto.aCadenaDelContrato('abc'), isNull);
      expect(CampoMonto.aCadenaDelContrato('1,2,3'), isNull);
      expect(CampoMonto.aCadenaDelContrato('1,234'), isNull);
      expect(CampoMonto.aCadenaDelContrato(''), isNull);
    });
  });

  test('CuentaEnmascarada muestra solo los últimos cuatro', () {
    expect(CuentaEnmascarada.enmascarar('1234567890'), '•••• 7890');
    expect(CuentaEnmascarada.enmascarar('12 34 56 78'), '•••• 5678');
  });

  test('Fecha lleva la zona horaria de La Paz, explícita', () {
    expect(
      Fecha.formatear('2026-09-09T14:30:00Z'),
      '9 sep 2026, 10:30 (La Paz)',
    );
    expect(
      Fecha.formatear('2026-01-01T03:00:00Z', conHora: false),
      '31 dic 2025',
    );
  });

  test('RelojDePlazo dice lo que falta sin recalcular el vencimiento', () {
    final ahora = DateTime(2026, 9, 9, 10);
    expect(
      RelojDePlazo.restante(DateTime(2026, 9, 12, 10), ahora),
      'Faltan 3 días',
    );
    expect(
      RelojDePlazo.restante(DateTime(2026, 9, 9, 15), ahora),
      'Faltan 5 h',
    );
    expect(
      RelojDePlazo.restante(DateTime(2026, 9, 9, 10, 20), ahora),
      'Vence hoy',
    );
    expect(RelojDePlazo.restante(DateTime(2026, 9, 8), ahora), 'Vencido');
  });

  group('ListaDeMovimientos.neto', () {
    test('suma cadenas del contrato sin pasar por double', () {
      expect(ListaDeMovimientos.neto(['500.00', '-250.00', '10.00']), '260.00');
      expect(ListaDeMovimientos.neto(['0.10', '0.20']), '0.30');
      expect(
        ListaDeMovimientos.neto(['-1000000000000000.01', '0.01']),
        '-1000000000000000.00',
      );
    });
  });

  group('CalendarioDeCuotas', () {
    test('el total exigible no suma lo futuro ni lo pagado', () {
      const cuotas = <Cuota>[
        (
          fechaIso: '2026-09-05',
          grupo: 'A',
          monto: '250.00',
          estado: EstadoDeCuota.pagada,
        ),
        (
          fechaIso: '2026-09-10',
          grupo: 'B',
          monto: '200.00',
          estado: EstadoDeCuota.pendiente,
        ),
        (
          fechaIso: '2026-09-10',
          grupo: 'A',
          monto: '250.00',
          estado: EstadoDeCuota.vencida,
        ),
        (
          fechaIso: '2026-09-25',
          grupo: 'A',
          monto: '250.00',
          estado: EstadoDeCuota.futura,
        ),
      ];
      expect(CalendarioDeCuotas.totalExigible(cuotas), '450.00');
    });
    test('la más urgente manda el color del día', () {
      expect(
        CalendarioDeCuotas.masUrgente([
          EstadoDeCuota.pagada,
          EstadoDeCuota.vencida,
        ]),
        EstadoDeCuota.vencida,
      );
      expect(
        CalendarioDeCuotas.masUrgente([
          EstadoDeCuota.futura,
          EstadoDeCuota.pendiente,
        ]),
        EstadoDeCuota.pendiente,
      );
      expect(
        CalendarioDeCuotas.masUrgente([EstadoDeCuota.futura]),
        EstadoDeCuota.futura,
      );
    });
  });
}
