# Recorridos Patrol de la app

Los siete archivos `*_test.dart` usan Patrol 3.20.0 y `patrol_cli` 3.11.0. En CI
corren sobre macOS 15, Xcode 26.3 y un simulador iOS 26.2. El workflow arranca
Prism en el puerto 4010 y compila la app con `API=http://127.0.0.1:4010/api/v1`.
El `pubspec.yaml` declara los identificadores nativos para que Patrol pueda abrir
la app instalada. Para ejecutar los recorridos se usa `patrol test --target
integration_test/<archivo>_test.dart`; `patrolTest` inicializa su propio binding.

La corrida [36028937945](https://github.com/PabloArauzCaballero/PasanakuBackend/actions/runs/36028937945)
confirmó que Prism y el runner nativo arrancan, descubren los casos y los ejecutan.
Los recorridos todavía no están en verde: `alta_y_billetera_test.dart` busca la
pestaña «Perfil» al abrir la app, cuando la ruta inicial actual es `/portada` para
quien no tiene sesión. `deep_link_test.dart` también espera la antigua pantalla
«Recargar saldo» al inicio; la variante con la app cerrada intentó abrir un bundle
ID vacío antes de declarar la configuración de Patrol. Además, el destino
`/pasanaku/unirse/:codigo` y el esquema iOS `aportaya` siguen sin implementación;
el contrato actual tampoco ofrece cómo resolver el token de una invitación en el
grupo correspondiente. No se debe presentar esos recorridos como aprobados.

En esta Mac, la ejecución Android local con `patrol_cli` 3.11.0 se bloqueó por
JDK 26 (`Unsupported class file major version 70`). Para Android se necesita un
JDK 17 o 21 específico de la herramienta, sin cambiar el JDK global. La prueba
en un teléfono físico de gama baja requerida por F12.1 tampoco se ha ejecutado.
