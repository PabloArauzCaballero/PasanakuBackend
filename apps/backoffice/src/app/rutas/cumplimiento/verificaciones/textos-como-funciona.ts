/**
 * «Cómo funciona», la pestaña de la ficha que explica el negocio y el sistema detrás de un expediente de identidad.
 * Prosa en bloques (`bloquesDeTexto`): un renglón es un párrafo; `- ` y `1. ` arman listas. Cada afirmación se apoya en
 * el contrato `identidad.yaml` o en el servicio `identidad` (las rutas van al final de cada sección).
 */
export type SeccionExplicada = { id: string; titulo: string; cuerpo: string; fuente?: string }

export const COMO_FUNCIONA: readonly SeccionExplicada[] = [
  {
    id: 'para-que',
    titulo: 'Para qué existe esta revisión (negocio)',
    cuerpo: `AportaYa mueve dinero de personas que confían entre sí: aportes a un grupo, entregas, retiros. Antes de que alguien aporte o reciba, la plataforma tiene que saber quién es. Eso es la verificación de identidad.
Mientras su expediente espera, la cuenta de esa persona no opera: queda «en revisión». Por eso cada expediente en la cola es alguien esperando, y la cola vacía es una buena noticia.
Es la base del resto de Cumplimiento: las alertas de monitoreo, los casos de investigación y los reportes a la UIF se apoyan en saber quién es cada persona.`,
    fuente: 'ConsultarMiPerfil (estado de la cuenta: falta verificar, en revisión u opera)',
  },
  {
    id: 'recorrido',
    titulo: 'El recorrido de un expediente',
    cuerpo: `1. La persona hace el alta en la app: nombre, teléfono, correo, tipo de documento, número, extensión del departamento y fecha de vencimiento.
2. Toma cinco fotos: anverso y reverso del documento, una selfie de frente y los dos perfiles.
3. El servidor abre la verificación en estado «En revisión» y la cuenta queda esperando.
4. El expediente aparece en esta cola, ordenado por antigüedad: primero quien más espera.
5. Una persona de Cumplimiento abre la ficha, mira las fotos y decide: aprobar o rechazar con un motivo.
6. Aprobado: la identidad queda verificada y la cuenta pasa a operar. Rechazado: la persona puede consultar el motivo con su sesión.`,
    fuente: 'identidad.yaml · listarVerificaciones, resolverVerificacion, EstadoDeVerificacion',
  },
  {
    id: 'reglas',
    titulo: 'Las reglas que hace cumplir el servidor',
    cuerpo: `La pantalla apaga los botones para ayudar, pero no es la barrera: si alguien la esquiva, el servidor rechaza igual con estos códigos.
- AP-CU01-08 · expediente ya resuelto: una decisión no se cambia desde el backoffice.
- AP-CU01-09 · fotos incompletas: no se aprueba sin las cinco caras. Aprobar sin mirar el documento entero es no revisar.
- AP-CU01-10 · documento sin vencimiento: sin fecha no hay forma de comprobar que el documento siga vigente.
- AP-CU01-11 · documento vencido: la vigencia la calcula el servidor con la fecha de hoy en La Paz, no el reloj de quien mira.
- Rechazar exige motivo, de hasta 160 caracteres: a quien le rechazan la cuenta tiene derecho a saber por qué.
- Cada decisión viaja con una clave de idempotencia: un doble clic o un reintento de la red no la registra dos veces.`,
    fuente: 'identidad.yaml · resolverVerificacion (422 ReglaDeNegocio), DecisionDeVerificacion',
  },
  {
    id: 'quien-decide',
    titulo: 'Por qué decide una persona y no un motor',
    cuerpo: `La decisión es manual a propósito. Un motor que rechaza sin que nadie mire deja a alguien sin cuenta y sin explicación, y el día que se equivoca nadie sabe por qué.
Cada decisión queda con el nombre de quien la tomó (revisada_por) y, si es un rechazo, con su motivo (motivo_rechazo). Automatizar después es fácil; auditar hacia atrás una decisión sin dueño, no.`,
    fuente: 'CU02RevisarExpediente (servicio identidad)',
  },
  {
    id: 'privacidad',
    titulo: 'Datos personales: qué se ve y qué no',
    cuerpo: `- El número del documento nunca llega al backoffice: solo el tipo y el departamento de expedición. Se coteja mirando el anverso.
- Las fotos no se cargan con la lista: se piden al pulsar «Ver las fotos», una por una, y cada lectura queda registrada con el nombre de quien miró.
- Las fotos se reciben como binario desde el servicio dueño, que valida sesión y permiso antes de leer un byte. Al salir de la ficha se borran de la memoria del navegador.
- Descargar la cola en CSV o JSON exige un permiso aparte: el archivo lleva nombres, teléfonos y correos sin enmascarar.
- Ocultar un botón no protege nada: el permiso de resolver (VERIFICACION_RESOLVER) lo vuelve a exigir el servidor en cada pedido.`,
    fuente: 'identidad.yaml · ExpedienteEnRevision.documento, verContenidoDeFoto; ADR-034',
  },
  {
    id: 'sistema',
    titulo: 'Qué pasa por detrás (sistema)',
    cuerpo: `- La cola la sirve el servicio identidad: GET /identidad/verificaciones. Sin filtro trae los cuatro estados; «Por decidir» junta Pendiente y En revisión en esta pantalla, porque el servidor filtra por un solo estado.
- Los datos salen de tres tablas del esquema de identidad: usuario (nombre, teléfono, correo), documento_identidad (tipo, extensión, vencimiento, anverso y reverso) y verificacion_kyc (estado, fechas, selfie, perfiles, motivo).
- Una foto cuenta como cargada solo si su archivo está en la carpeta del expediente (identidad/<cuenta>/). Las filas antiguas con un archivo de relleno no cuentan como foto.
- Las fotos viven en un almacén interno que no tiene dirección pública: por eso las sirve el servicio (GET …/fotos/{cara}/contenido) y no un enlace directo.
- La decisión es POST /identidad/verificaciones/{id}/decision con APROBAR o RECHAZAR y el motivo. Devuelve el expediente ya resuelto; la cola se recarga sola.`,
    fuente: 'FilaDeExpediente, RevisionRepositorio, CU02RevisarExpediente',
  },
]
