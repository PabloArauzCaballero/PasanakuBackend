# Estado del entorno D3

- **Abajo.** No quedan contenedores `aportaya-d3-*`, ni redes `aportaya-d3-*`, ni procesos de Gradle de este carril.
  Docker Desktop NO se apago (ya corria antes; hay contenedores de otros trabajos que no son de este carril).
- Puertos usados cuando esta arriba: 5444 (PostgreSQL), 6444 (PgBouncer), 18080 (gateway). Todos en 127.0.0.1.
- Si el agente E lo necesita: ver ENTORNO-E2E.md. **Arriba consume ~2,4 GB** (gateway + 8 servicios + infra);
  bajarlo con `bash despliegue/ensayos/bajar_entorno.sh` apenas termine.
- Imagenes locales `aportaya/*:test` (17, ~9 GB en disco) quedan para no reconstruir; borrarlas con
  `docker rmi $(docker images -q 'aportaya/*:test')` si hace falta espacio.
