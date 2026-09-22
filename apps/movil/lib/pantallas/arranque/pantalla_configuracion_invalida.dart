import 'package:flutter/material.dart';

/// Lo que ve quien abre la app cuando `resultadoGateway.valida` (ver
/// `dominio/cliente.dart` y `dominio/configuracion.dart`) es `false`: `main.dart`
/// construye esto en vez de `AppAportaYa`, así que ningún provider que dependa de
/// `dioProvider` llega a crearse y no sale ninguna petición.
///
/// Nunca muestra la URL rechazada — no ayuda a quien instaló la app (no puede
/// arreglar el build desde acá) y sí ayuda a quien mira por encima del hombro.
class PantallaConfiguracionInvalida extends StatelessWidget {
  const PantallaConfiguracionInvalida({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      home: Scaffold(
        body: SafeArea(
          child: Center(
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Semantics(
                liveRegion: true,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.error_outline, size: 48),
                    const SizedBox(height: 16),
                    Text(
                      'AportaYa no puede arrancar',
                      style: Theme.of(context).textTheme.titleLarge,
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: 8),
                    Text(
                      'Esta instalación no tiene una configuración válida. '
                      'No se hizo ninguna petición a ningún servidor.',
                      style: Theme.of(context).textTheme.bodyMedium,
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: 8),
                    Text(
                      'Si sos parte del equipo, avisá a la persona de guardia de '
                      'infraestructura con la hora y la versión instalada.',
                      style: Theme.of(context).textTheme.bodySmall,
                      textAlign: TextAlign.center,
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
