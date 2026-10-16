# Mensaje de verificación en sesión activa

Fecha: 2026-10-15  
Rama: `codex/alta-atlas-sesion-premium-2026-10-11`  
Estado: escrito y verificado funcionalmente en tests focalizados; regresión global parcial.

## Resultado y alcance

Una persona con sesión activa que todavía no tiene `BILLETERA_VER` debe llegar a una pantalla
que consulte el estado KYC propio y explique que su cuenta está en proceso de verificación,
no ver «No tenés acceso a esto». El permiso sigue validándose exclusivamente en backend.

IN: metadata local del JWT solo para navegación; puerta de rutas; presentación autenticada del
estado KYC; pruebas y evidencia visual.  
OUT: cambiar roles/permisos, aprobar cuentas, modificar backend/DB, desplegar, publicar o tocar
las capturas anteriores de referencia.

## Causa y controles

La cuenta observada anteriormente tenía expediente `EN_REVISION` y el token no tenía permiso
efectivo de billetera; el endpoint protegido devuelve 403 de forma correcta. Se reutiliza el
endpoint existente de consulta del estado propio. Un permiso decodificado del JWT solo redirige
la UI; no se verifica ni concede acceso y no se usa para autorizar operaciones. La pantalla
consulta al servicio de identidad y el backend conserva todos sus controles.

La ruta nueva es `/sesion/verificacion` y no agrega identificadores a la URL. Si el estado KYC
resulta aprobado pero el token sigue sin permiso, se explica que hay que cerrar sesión y volver
a ingresar; no se concede permiso en cliente. Si la consulta falla, permanece el estado de error
con reintento existente.

## Archivos del cambio

- `apps/movil/lib/proveedores/sesion.dart`: deriva ID y presencia de `BILLETERA_VER` solo como
  metadatos de navegación; actualiza tras abrir, hidratar, renovar y cerrar sesión.
- `apps/movil/lib/navegacion/acceso_a_rutas.dart` y `rutas.dart`: redirige sesiones sin permiso
  explícito a la pantalla de estado y conserva accesos normales para cuentas habilitadas.
- `apps/movil/lib/pantallas/identidad/pantalla_estado_de_verificacion.dart` y
  `textos_de_verificacion.dart`: texto directo «Tu cuenta está en proceso de verificación»,
  sesión conservada, botón para actualizar estado y salida segura de la cuenta.
- `apps/movil/test/unidad/acceso_a_rutas_test.dart`,
  `apps/movil/test/unidad/sesion_observable_test.dart`,
  `apps/movil/test/identidad/pantalla_estado_verificacion_sesion_test.dart`,
  `apps/movil/test/goldens/h10_verificacion_visual_test.dart`: cobertura de redirección,
  reclamos ausentes/presentes, recorrido real del GoRouter en widget test y seis capturas nuevas.

## Skills y reglas aplicadas

`skills-router`, `outcome-first`, `context-thrift`, `factual-discovery`,
`anti-hallucination-guard`, `scope-discipline`, `native-code-patterns`,
`progress-reporting`, `root-cause-debugging`, `rationalization-guard`,
`evidence-and-verification`, `finish-your-turn`, `mobile-ux-design`, `flutter-development`,
`flutter-state-architecture`, `flutter-testing`, `flutter-theming`, `visual-proof`,
`authn-identity`, `auth-session-pentest`, `kyc-identity-verification`,
`data-privacy-financial`, `error-handling-contract`, `frontend-ux-states`; reglas 90, 95 y 80.

## Evidencia

Comando focalizado ejecutado después del último cambio:

```text
flutter test test/unidad/acceso_a_rutas_test.dart test/unidad/sesion_observable_test.dart test/identidad/pantalla_estado_verificacion_sesion_test.dart test/goldens/h10_verificacion_visual_test.dart
00:04 +50: All tests passed!
```

Demuestra el caso de login con sesión sin permiso, la consulta KYC simulada en el test, el texto
correcto y que «No tenés acceso a esto» no se renderiza en el flujo cubierto. La API se sustituyó
por un fake: no es una prueba contra el entorno TEST.

Análisis estático posterior al último cambio:

```text
flutter analyze lib/proveedores/sesion.dart lib/navegacion/acceso_a_rutas.dart lib/navegacion/rutas.dart lib/pantallas/identidad/pantalla_estado_de_verificacion.dart lib/pantallas/identidad/textos_de_verificacion.dart
No issues found! (ran in 6.4s)
```

Prueba visual posterior al último cambio: `flutter test --update-goldens
test/goldens/h10_verificacion_visual_test.dart` → `All tests passed!` (30 casos). Se
inspeccionaron las seis capturas en claro/oscuro a 360, 412 y 600 dp; título y detalle no se
cortan, botones quedan visibles, contraste legible y sin desbordes. Capturas en
`evidencia/h10-visual/h10-verificacion-sesion-en-revision-{claro,oscuro}-{360,412,600}-130.png`.
No se usaron datos reales.

Regresión amplia ejecutada:

```text
flutter test test/unidad test/identidad test/goldens/h8_verificacion_oscura_test.dart test/goldens/h10_verificacion_visual_test.dart
00:24 +437 -3: Some tests failed.
Failing tests: h4_ci_icono_visual_test.dart (diff 40.65%); motivo_de_salida_en_ingreso_test.dart (dos diffs golden, 1.38% y otro).
```

Los archivos de esos tres tests no forman parte del diff. La causa no se ha demostrado con una
corrida sobre baseline limpio; por eso no se clasifican como entorno ni se actualizan sus
goldens. Impiden declarar regresión global verde.

## Continuación de verificación (2026-10-15)

- SDK local disponible: Flutter 3.47.5; CI del repositorio fija Flutter 3.44.8 y no se encontró
  una instalación local 3.44.8. Los tres fallos visuales se reprodujeron individualmente bajo
  3.47.5. Se inspeccionaron las imágenes master/test: muestran diferencias reales de UI; no se
  actualizaron ni clasificaron como baseline sin comparación con 3.44.8.
- APK debug construido apuntando exclusivamente al host TEST mediante `API` y
  `HOSTS_PERMITIDOS`. Artefacto local: `apps/movil/build/app/outputs/flutter-apk/app-debug.apk`,
  261.493.812 bytes, SHA-256
  `53F9B3297089F2EDC569EB966D0C878E33D6F4DD67645F41415E0B3560FAAF18`.
- El AVD Pixel 2 no arranca: su imagen Android 36 carece de `kernel-ranchu`. Samsung A06-L
  (Android 33) arrancó con 1 GiB. La app se instaló y llegó al login; en el primer intento el botón
  `Ingresar` quedó deshabilitado y no se envió login. Con 768 MiB Android mostró `System UI isn't
  responding`; al repetir con 1 GiB el sistema volvió a mostrar el mismo diálogo antes de completar
  los campos. El AVD se apagó inmediatamente; no hubo petición de autenticación ni cambio/borrado
  de cuenta.
- Pico observado: 14,91 GiB usados de 15,32 GiB visibles (0,41 GiB libres); no se cruzó el tope
  solicitado de 15,1 GiB usados. Ambos AVD quedaron apagados y `adb devices` quedó vacío.

## No cubierto / riesgos

- Falta validar en una versión de Flutter fijada por el proyecto si los tres golden difieren del
  baseline, y corregir solo si el fallo existe también allí y se confirma como regresión real.
- Falta login funcional contra TEST y comprobar en dispositivo que el estado `EN_REVISION` muestra
  el mensaje de verificación. La prueba widget/contrato continúa usando API fake.
- No se hizo despliegue ni publicación.
- Riesgo residual aceptado: un cliente puede alterar sus metadatos JWT locales, pero eso solo
  cambia la pantalla inicial; toda API privada continúa autorizándose en el servidor.

## Pasos exactos para retomar

1. Mantener apagado el AVD Pixel 2 hasta reparar/reinstalar su imagen Android 36 con kernel; no
   usar Samsung A06-L con menos de 1 GiB, pues Android dejó de responder.
2. Instalar Flutter 3.44.8 (versión fijada por `.github/workflows/ci.yml`) y desde
   `PasanakuBackend/apps/movil` ejecutar, serialmente, los tres tests individuales indicados en
   `Regresión amplia ejecutada`; no usar `--update-goldens`.
3. Antes de otro AVD, confirmar memoria disponible. Un AVD consumió suficiente como para dejar
   solo 0,41 GiB libres; parar servicios Gradle antes de arrancarlo y apagarlo con `adb emu kill`
   tras la prueba.
4. Para reconstruir el APK debug TEST, desde `PasanakuBackend/apps/movil` ejecutar:

   ```text
   flutter build apk --debug --dart-define=API=https://api.aportaya.161.97.85.216.sslip.io/api/v1 --dart-define=HOSTS_PERMITIDOS=api.aportaya.161.97.85.216.sslip.io
   ```

5. Repetir el flujo UI solo cuando el botón de ingreso se habilite con los campos y el AVD mantenga
   el límite de RAM. No aprobar KYC, alterar roles ni eliminar la cuenta. Confirmar la pantalla
   `Tu cuenta está en proceso de verificación` y revisar que no aparezca `No tenés acceso a esto`.
