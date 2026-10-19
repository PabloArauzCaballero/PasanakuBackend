import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:aportaya_diseno/moviles/alcance_haptica.dart';

import 'dominio/cierre_de_sesion.dart';
import 'pantallas/identidad/capa_de_bloqueo.dart';
import 'pantallas/soporte/capa_de_apertura.dart';
import 'pantallas/soporte/capa_de_aviso_de_contrato.dart';
import 'pantallas/soporte/capa_de_tutorial.dart';
import 'proveedores/haptica.dart';
import 'package:aportaya_diseno/moviles/anclas_de_tutorial.dart';
import 'package:aportaya_diseno/moviles/como_en_un_telefono.dart';
import 'package:aportaya_diseno/tema.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';

const localeDeAportaYa = Locale('es', 'BO');
const localesDeAportaYa = <Locale>[localeDeAportaYa];
const delegadosDeLocalizacionDeAportaYa = <LocalizationsDelegate<dynamic>>[
  GlobalMaterialLocalizations.delegate,
  GlobalWidgetsLocalizations.delegate,
  GlobalCupertinoLocalizations.delegate,
];

class AppAportaYaReal extends StatelessWidget {
  const AppAportaYaReal({
    super.key,
    required this.enrutador,
    required this.ref,
  });

  final GoRouter enrutador;
  final WidgetRef ref;

  @override
  Widget build(BuildContext context) {
    return AlcanceHaptica(
      haptica: ref.read(hapticaProvider),
      child: MaterialApp.router(
        title: 'Aporteo',
        locale: localeDeAportaYa,
        supportedLocales: localesDeAportaYa,
        localizationsDelegates: delegadosDeLocalizacionDeAportaYa,
        theme: temaDesde(Tokens.claro, Brightness.light),
        darkTheme: temaDesde(Tokens.oscuro, Brightness.dark),
        themeMode: ThemeMode.system,
        routerConfig: enrutador,
        debugShowCheckedModeBanner: false,
        // El registro de anclas y la capa del tutorial envuelven a TODA la app: un
        // recorrido cruza pantallas, así que no puede vivir dentro de una. Con el
        // tutorial apagado, las dos piezas no dibujan ni escuchan nada.
        builder: (context, child) => CapaDeBloqueo(
          hijo: ComoEnUnTelefono(
            child: ProveedorDeAnclas(
              hijo: CapaDeTutorial(
                enrutador: enrutador,
                hijo: CapaDeApertura(
                  enrutador: enrutador,
                  child: CapaDeAvisoDeContrato(child: child),
                ),
              ),
            ),
          ),
          onIngresarDeNuevo: () => ref
              .read(cierreDeSesionProvider)
              .ejecutar(motivo: null, esperarAlServidor: false)
              .ignore(),
        ),
      ),
    );
  }
}
