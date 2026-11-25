# Revocación de sesiones en TEST

H1 · Mantener la validación de sesiones cerrada durante la carga inicial de Kafka.

- H1.S1 · Corregir la disponibilidad de la lista.
  - H1.S1.M1 · Distinguir un tema ausente de uno existente sin registros. CA: ausente implica lista no disponible; existente y vacío permite declararla al día. DoD: prueba unitaria de ambos estados y del redescubrimiento. Estado: HECHO.
  - H1.S1.M2 · Evitar que un error de decodificación copie datos del mensaje a los logs. CA: nombre del tema, partición y offset identifican el incidente. DoD: prueba existente de lectura tras un mensaje ilegible. Estado: HECHO.
- H1.S2 · Preparar la comprobación en el entorno real.
  - H1.S2.M1 · Documentar el arranque con el tema provisionado y la prueba con dos sesiones. CA: un operador puede diferenciar un fallo general de un rechazo por revocación. DoD: runbook versionado con SHA, estado, tiempo y reinicio. Estado: HECHO.
