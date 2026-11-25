# Revocación de sesiones en TEST

**TESTED en local**, módulo `comun-web`. Pendiente verificar la integración con el broker y las réplicas en TEST.

- El lector no declara disponible la lista cuando el broker no devuelve particiones. Cuando se descubre un tema existente, la disponibilidad se recupera al alcanzar sus offsets. La configuración del consumidor impide crear automáticamente un tópico sin la política de retención prevista.
- Un registro ilegible se identifica por tema, partición y offset. El mensaje del decodificador puede llevar contenido del evento y ya no se incluye en el log.
- El runbook de TEST incorpora la preparación del tópico antes de activar los servicios y la comprobación de dos sesiones, incluyendo rechazo después de un reinicio.
- `:plataforma:comun-web:spotlessCheck :plataforma:comun-web:test` en modo offline con Java 21. JUnit informó 46 pruebas, 0 fallos. `python3 scripts/verificar_seguridad.py` terminó con `TODO OK · 5 aviso(s)`.
- La comprobación TCP al SSH de TEST recibió `Connection refused` desde este entorno. No hay evidencia de cambio del host ni de prueba funcional allí.
