/// El texto del contrato de adhesión y del tarifario que se muestra en CU-05.
///
/// **De dónde sale.** Está escrito sobre la estructura que usan los contratos de
/// billetera móvil registrados en Bolivia —el de YASTA (Banco Unión) y el de Yape
/// (BCP) son los dos que se tomaron de referencia— y sobre el marco que los gobierna:
/// Ley N° 393 de Servicios Financieros, Ley N° 453 de los derechos de las usuarias y
/// los usuarios, Ley N° 164 (art. 56, datos personales en servicios TIC), el
/// Reglamento de Instrumentos Electrónicos de Pago del BCB, la Recopilación de Normas
/// para Servicios Financieros de ASFI y la Ley N° 708 de conciliación y arbitraje.
///
/// **El tarifario no está inventado**: los importes son los del tarifario `GENERAL`
/// v1 que siembra `seeders/minimos/04-tarifario.json`. Si ese tarifario cambia, este
/// texto cambia con él —y por eso la cláusula de comisiones dice que el que manda es
/// el publicado, no el que alguien recuerde—.
///
/// **Lo que todavía falta.** Un contrato de adhesión no rige por estar escrito: rige
/// cuando está registrado ante ASFI (RNSF, registro de contratos de adhesión). El
/// número y la fecha de ese registro los guarda `cumplimiento.contrato_adhesion`
/// (`numero_registro`, `fecha_registro`) y los tiene que publicar el servicio de
/// cumplimiento; hasta que exista ese endpoint, el pie dice la versión y no finge un
/// número de registro que no se obtuvo.
library;

/// La versión que se muestra. Va en el pie y tiene que coincidir con `codigo` y
/// `version` de la fila de `cumplimiento.contrato_adhesion` que se acepta.
const String versionDelContrato = 'CTO-BILLETERA v1';

const String textoDelContrato =
    '''CONTRATO DE ADHESIÓN AL SERVICIO DE BILLETERA DE DINERO ELECTRÓNICO «APORTAYA»

Leé este contrato completo antes de aceptarlo. Al aceptarlo quedás obligado por todo lo que dice, lo hayas leído o no. Si algo no se entiende, no lo aceptes: escribinos antes desde el canal de soporte de la app.


PRIMERA — PARTES

Por una parte APORTAYA, el proveedor del servicio, titular de la plataforma y responsable frente a vos por su funcionamiento. Por otra parte vos, la persona natural mayor de dieciocho (18) años que abre la cuenta a su propio nombre, en adelante «el Titular».

El Titular declara que abre la cuenta para sí mismo y no por cuenta ni en interés de otra persona. Abrir una billetera para que la use un tercero —prestarla, alquilarla, cederla— está prohibido y es causal de cierre inmediato.


SEGUNDA — DEFINICIONES

Billetera: el instrumento electrónico de pago con el que el Titular ordena operaciones desde su celular.

Dinero electrónico: el valor monetario en bolivianos (BOB) registrado a nombre del Titular, equivalente peso a peso a los fondos efectivamente recibidos.

Cuenta de custodia: la cuenta abierta en una entidad de intermediación financiera supervisada por ASFI donde se mantienen los fondos de todos los titulares, separada del patrimonio de AportaYa.

Grupo o pasanaku: la rueda de ahorro entre participantes que la plataforma organiza, regida por su propio contrato, distinto de este.

Credenciales: la contraseña, el código de un solo uso (OTP), el factor biométrico y cualquier otro dato que permita ordenar operaciones en nombre del Titular.

Tarifario vigente: el listado de comisiones publicado en la app y en el sitio web de AportaYa, transcrito al final de este contrato.


TERCERA — OBJETO

AportaYa presta al Titular el servicio de billetera de dinero electrónico: cargar saldo, mantenerlo, transferirlo, retirarlo y usarlo para aportar a un grupo de pasanaku y cobrar el turno que le corresponda, con el registro de cada movimiento y su comprobante.

Este contrato no cubre la participación en un grupo de pasanaku, que se rige por el contrato de participación que se acepta por separado al entrar a cada grupo.


CUARTA — NATURALEZA DEL SALDO

El dinero electrónico de la Billetera NO es un depósito. En consecuencia:

a) No genera intereses ni rendimiento de ninguna clase.
b) No está cubierto por el Fondo de Protección al Ahorrista.
c) No puede ser invertido, prestado ni afectado por AportaYa a ningún otro fin.

Los fondos se mantienen íntegramente en la cuenta de custodia, separados del patrimonio de AportaYa, y respaldan en todo momento el cien por ciento (100 %) del saldo registrado a favor de los titulares. AportaYa no tiene sobre ellos más facultad que ejecutar las órdenes del Titular.


QUINTA — APERTURA DE LA CUENTA Y VERIFICACIÓN DE IDENTIDAD

Para abrir la cuenta hacen falta: un celular con la app instalada, un número de línea boliviano a nombre del Titular o bajo su control, un correo electrónico, el documento de identidad vigente y la verificación de identidad que hace la app (fotografía del documento por sus dos caras, prueba de vida y cotejo).

El Titular declara que todo lo que informó es verdadero, exacto y actual, y se obliga a actualizarlo cuando cambie. AportaYa puede verificar esos datos ante el SEGIP, la ASFI, burós de información y las demás fuentes que la normativa le permita consultar, y el Titular lo autoriza expresamente por este acto.

Si los datos no coinciden con las fuentes consultadas, o si la verificación no se completa, la cuenta queda en estado pendiente: no opera hasta que se resuelva, y el Titular es informado de qué falta.


SEXTA — OPERACIONES PERMITIDAS

a) Cargar saldo desde los canales habilitados.
b) Retirar saldo a una cuenta bancaria propia o por los canales habilitados.
c) Transferir saldo a otra billetera AportaYa.
d) Aportar a un grupo de pasanaku del que el Titular participe.
e) Cobrar el turno que le corresponda en un grupo.
f) Consultar saldo, movimientos y comprobantes, y descargar su extracto.
g) Las demás que AportaYa habilite, previa comunicación al Titular y, cuando corresponda, previa autorización del regulador.


SÉPTIMA — LÍMITES OPERATIVOS

Las operaciones están sujetas a límites por monto y por cantidad, fijados según el nivel de debida diligencia de cada Titular conforme a la normativa del BCB y de ASFI. Los límites que rigen para la cuenta del Titular están publicados en la app, en su perfil, y pueden consultarse en todo momento.

Cuando una operación supere el umbral que fija la normativa, AportaYa está obligada a pedir la declaración del origen y del destino de los fondos y el respaldo documental correspondiente. Mientras esa declaración no se presente, la operación no se acredita.


OCTAVA — SEGURIDAD Y USO DE LAS CREDENCIALES

Las credenciales son personales e intransferibles. El Titular es responsable de guardarlas y de todas las operaciones que se ordenen con ellas.

El acceso exige doble factor de verificación. La contraseña se bloquea después de tres (3) intentos fallidos consecutivos y se desbloquea por el procedimiento de recuperación que la app indique.

El Titular se obliga a avisar a AportaYa de inmediato, por el canal de soporte, en caso de pérdida, robo o extravío del celular, o ante cualquier sospecha de que un tercero conoce sus credenciales. Desde el aviso, AportaYa bloquea la cuenta y las operaciones posteriores al bloqueo no son responsabilidad del Titular.

AportaYa nunca pide la contraseña ni el código de un solo uso por teléfono, correo, mensaje ni ningún otro medio. Quien lo pida está intentando estafar al Titular.


NOVENA — COMISIONES, IMPUESTOS Y FACTURACIÓN

El Titular autoriza expresamente a AportaYa a debitar de su saldo las comisiones del tarifario vigente, transcrito al final de este contrato. Los precios incluyen los impuestos que correspondan.

AportaYa no cobra cargos ni comisiones que no impliquen una contraprestación efectiva de servicios, ni cobra por servicios que el Titular no aceptó.

Por toda comisión cobrada, AportaYa emite la factura o nota fiscal a nombre del Titular y la pone a su disposición en la app.

Toda modificación del tarifario que aumente una comisión o incorpore una nueva se comunica al Titular con treinta (30) días calendario de anticipación, por la app y por el correo declarado. Dentro de ese plazo el Titular puede cerrar la cuenta sin costo. Las rebajas rigen desde su publicación.


DÉCIMA — OBLIGACIONES DEL TITULAR

a) Usar la Billetera personalmente y responder por las operaciones que ordene.
b) Mantener sus datos actualizados y avisar cualquier cambio.
c) Pagar las comisiones del tarifario vigente.
d) No usar la cuenta para actividades ilícitas, ni para operaciones por cuenta de terceros.
e) Entregar la información y el respaldo que la normativa de prevención de la legitimación de ganancias ilícitas y financiamiento del terrorismo exija.
f) Mantener la app instalada y actualizada, y el celular bajo su control.
g) Avisar de inmediato pérdida, robo o uso no autorizado.


DÉCIMA PRIMERA — OBLIGACIONES DE APORTAYA

a) Mantener los fondos en la cuenta de custodia, separados de su patrimonio, por el cien por ciento (100 %) del saldo registrado.
b) Ejecutar las órdenes del Titular en los plazos informados en la app.
c) Registrar cada movimiento con su comprobante y mantenerlos disponibles en la app, y permitir la descarga del extracto.
d) Informar de manera previa, clara, completa y oportuna las comisiones, los plazos y los riesgos del servicio.
e) Atender los reclamos en los plazos que fija la normativa.
f) Guardar reserva de la información del Titular, salvo requerimiento de autoridad competente.
g) Comunicar con anticipación toda modificación contractual o tarifaria.


DÉCIMA SEGUNDA — DERECHOS DEL TITULAR

Conforme a la Ley N° 393 y a la Ley N° 453, el Titular tiene derecho a recibir un trato equitativo y sin discriminación; a recibir información fidedigna, amplia, íntegra, clara, comprensible, oportuna y gratuita sobre el servicio y sus costos; a formular reclamos y obtener respuesta motivada; a la reserva de su información; a recibir el comprobante de cada operación; y a cerrar su cuenta cuando lo decida.


DÉCIMA TERCERA — TRATAMIENTO DE DATOS PERSONALES

AportaYa trata los datos personales del Titular —incluidos los datos biométricos obtenidos en la verificación de identidad— con las siguientes finalidades, y no otras:

a) Verificar la identidad y abrir la cuenta.
b) Prestar el servicio y ejecutar las operaciones.
c) Cumplir las obligaciones legales de prevención de la legitimación de ganancias ilícitas y de reporte a la Unidad de Investigaciones Financieras y a ASFI.
d) Prevenir el fraude y proteger la seguridad de las cuentas.
e) Atender reclamos y requerimientos de autoridad competente.

Los datos se conservan por el plazo que exige la normativa financiera y se guardan con medidas de seguridad técnicas y organizativas. No se venden ni se ceden a terceros con fines comerciales.

El envío de publicidad y de ofertas comerciales requiere un consentimiento aparte, que el Titular puede dar o retirar en cualquier momento desde su perfil, sin que eso afecte la prestación del servicio.

El Titular puede pedir el acceso, la corrección y la actualización de sus datos, y su eliminación cuando ya no exista obligación legal de conservarlos, conforme al artículo 56 de la Ley N° 164 y a los artículos 21 y 130 de la Constitución Política del Estado.


DÉCIMA CUARTA — VALOR PROBATORIO DE LAS OPERACIONES

Las operaciones ordenadas por medios electrónicos bajo este contrato tienen plena validez y fuerza probatoria, conforme al artículo 124 de la Ley N° 393 y a la Ley N° 164. Los registros electrónicos de AportaYa —con su fecha, hora y sello— hacen fe de las operaciones realizadas, salvo prueba en contrario.


DÉCIMA QUINTA — INTERRUPCIONES Y RESPONSABILIDAD

AportaYa responde por el funcionamiento del servicio y por los perjuicios que cause su culpa o negligencia. No responde por hechos que no le son imputables: caso fortuito, fuerza mayor, fallas de la red móvil o de internet del Titular, ni por el uso de las credenciales por un tercero antes de que el Titular haya dado el aviso de la cláusula octava.

Las interrupciones programadas por mantenimiento se avisan con anticipación por la app. Ante una interrupción no programada, AportaYa informa su causa y el plazo estimado de restablecimiento.


DÉCIMA SEXTA — PLAZO

Este contrato se pacta por un (1) año calendario desde su aceptación y se prorroga automáticamente por períodos iguales, salvo que alguna de las partes notifique lo contrario.


DÉCIMA SÉPTIMA — CIERRE DE LA CUENTA

El Titular puede cerrar su cuenta cuando quiera, desde la app, sin costo y sin expresar causa, siempre que no mantenga saldo comprometido ni obligaciones pendientes en un grupo de pasanaku activo.

AportaYa puede cerrar la cuenta, informando al Titular, cuando: los datos declarados resulten falsos, incompletos o no verificables; el Titular se niegue a entregar la información exigida por la normativa de prevención; se detecte uso de la cuenta por cuenta de terceros o para fines ilícitos; o lo ordene una autoridad competente.

En cualquier caso de cierre, el saldo disponible queda a disposición del Titular para su retiro. El saldo no es de AportaYa y el cierre no lo extingue.


DÉCIMA OCTAVA — RECLAMOS

El Titular puede reclamar desde la app, por el canal de soporte o por el punto de reclamo que AportaYa publique. AportaYa responde en el plazo que fija la normativa de ASFI para la atención de reclamos.

Si la respuesta no lo satisface, o si no recibe respuesta en plazo, el Titular puede acudir a la Defensoría del Consumidor Financiero de la Autoridad de Supervisión del Sistema Financiero (ASFI). Este contrato no limita ese derecho de ninguna manera.


DÉCIMA NOVENA — SOLUCIÓN DE CONTROVERSIAS

Agotada la vía del reclamo, las diferencias que no se resuelvan de común acuerdo dentro de los diez (10) días hábiles podrán someterse a conciliación y, en su defecto, a arbitraje, conforme a la Ley N° 708 de Conciliación y Arbitraje, ante el centro que las partes acuerden.

Acudir a la conciliación o al arbitraje es una opción del Titular, no una obligación: conserva íntegro su derecho de acudir a la vía judicial y a la Defensoría del Consumidor Financiero.


VIGÉSIMA — MODIFICACIONES

Toda modificación de este contrato se comunica al Titular con treinta (30) días calendario de anticipación, por la app y por el correo declarado, y se instrumenta por adenda. Dentro de ese plazo el Titular puede cerrar la cuenta sin costo. Las modificaciones que lo beneficien rigen desde su comunicación.


VIGÉSIMA PRIMERA — DOMICILIO Y NOTIFICACIONES

Las comunicaciones de AportaYa se dirigen al celular y al correo electrónico declarados por el Titular y registrados en la app; las del Titular, al canal de soporte. Es carga del Titular mantener esos datos actualizados.


VIGÉSIMA SEGUNDA — LEGISLACIÓN APLICABLE

Este contrato se rige por la legislación boliviana, en especial por la Ley N° 393 de Servicios Financieros, la Ley N° 453, la Ley N° 164, el Reglamento de Instrumentos Electrónicos de Pago del Banco Central de Bolivia y la Recopilación de Normas para Servicios Financieros de la ASFI, y por las demás normas que las sustituyan o complementen.


VIGÉSIMA TERCERA — ACEPTACIÓN

Al marcar las casillas y tocar «Aceptar y continuar», el Titular declara haber leído este contrato completo, haber comprendido sus cláusulas y aceptarlas. La aceptación queda registrada con su fecha, su hora y la versión del contrato aceptada, y el Titular puede consultarla y descargarla desde su perfil.

──────────

TARIFARIO VIGENTE

Los importes están en bolivianos (BOB) e incluyen impuestos.

Sin costo:
· abrir la cuenta
· mantener la cuenta
· cargar saldo
· retirar saldo
· transferir a otra billetera AportaYa
· entrar a un grupo de pasanaku
· aportar a un grupo de pasanaku
· consultar saldo y movimientos, y descargar el extracto

Comisión por cobro de turno: 0,30 % del monto de la bolsa, con un mínimo de Bs 10,00 y un máximo de Bs 50,00.

Se descuenta de la bolsa en el momento en que el turno se cobra, y figura como una línea aparte en el comprobante. El mínimo y el máximo rigen para cualquier monto de bolsa: una bolsa chica no queda consumida por la comisión, y una grande no paga un porcentaje sin techo.

Ejemplos, para que el porcentaje se entienda antes de cobrarlo:
· bolsa de Bs 2.000 → 0,30 % = Bs 6,00 → se cobra Bs 10,00 (el mínimo)
· bolsa de Bs 8.000 → 0,30 % = Bs 24,00 → se cobra Bs 24,00
· bolsa de Bs 30.000 → 0,30 % = Bs 90,00 → se cobra Bs 50,00 (el máximo)

Ninguna otra operación tiene costo. Si en la app aparece un cobro que no está en esta lista, es un error: reclamalo y se devuelve.

El tarifario que rige es el publicado en la app y en el sitio web de AportaYa. Todo aumento o concepto nuevo se avisa con treinta (30) días calendario de anticipación.


──────────

Versión del contrato: $versionDelContrato

Este contrato de adhesión y su tarifario se registran ante la Autoridad de Supervisión del Sistema Financiero. El número y la fecha de registro se publican junto a esta versión en el sitio web de AportaYa y en tu perfil dentro de la app.
''';
