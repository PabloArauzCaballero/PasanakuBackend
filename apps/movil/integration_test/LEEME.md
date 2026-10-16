# Recorridos de la app

En el simulador iOS de GitHub CI, `arranque_y_registro_test.dart` comprueba el
arranque sin sesión, el tour y la entrada al formulario de alta.
`deep_link_test.dart` comprueba `aportaya://unirse/<tokenId>.<secreto>` como URI
de inicio y como URL nativa con la app abierta. Patrol monta el árbol Flutter
de prueba antes de abrir enlaces nativos; no prueba un arranque del proceso desde
una app terminada. Las
pruebas widget cubren la ruta sin pestañas y el doble envío de recarga con una
sola petición.

El enlace permite al organizador copiarlo, al destinatario autenticarse con el
teléfono invitado, ver el monto y el reglamento, y aceptarlo una vez. Identidad
valida el secreto, teléfono, estado y nivel KYC; grupos verifica restricciones,
reputación y cupo, y crea participante, cupo y firma en una sola transacción.

Los otros guiones Patrol de F12.1 todavía necesitan un entorno de ensayo con API
real y un Android físico: Scalar no ejecuta los efectos financieros completos ni
provee una cámara real. No se consideran aprobados por las pruebas de iOS.

En esta Mac, la ejecución Android local con `patrol_cli` 3.11.0 se bloqueó por
JDK 26 (`Unsupported class file major version 70`). Para Android se necesita un
JDK 17 o 21 específico de la herramienta, sin cambiar el JDK global.
