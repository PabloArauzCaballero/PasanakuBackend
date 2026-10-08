# aportaya_cliente_grupos.api.DefaultApi

## Load the API package
```dart
import 'package:aportaya_cliente_grupos/api.dart';
```

All URIs are relative to */api/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**calcularPlazoHabil**](DefaultApi.md#calcularplazohabil) | **GET** /grupos/calendario/calcular | Fecha límite en días hábiles, con los días que se saltearon
[**comprometerSorteo**](DefaultApi.md#comprometersorteo) | **POST** /grupos/{grupoId}/sorteo | Publicar el compromiso del sorteo
[**consultarActividad**](DefaultApi.md#consultaractividad) | **GET** /grupos/participantes/{usuarioId}/actividad | Si la persona participa hoy de algun grupo activo
[**consultarPaqueteDelSorteo**](DefaultApi.md#consultarpaquetedelsorteo) | **GET** /grupos/sorteos/{sorteoId}/paquete | El paquete publicado de un sorteo, para poder recomputarlo
[**crearGrupo**](DefaultApi.md#creargrupo) | **POST** /grupos | Crear el grupo y disparar el congelamiento del tarifario
[**invitarAlGrupo**](DefaultApi.md#invitaralgrupo) | **POST** /grupos/{grupoId}/invitaciones | Invitar a un contacto, con token de un solo uso
[**postularAlGrupo**](DefaultApi.md#postularalgrupo) | **POST** /grupos/{grupoId}/postulaciones | Postular a un grupo, con el motivo legible del puntaje
[**proponerAcuerdo**](DefaultApi.md#proponeracuerdo) | **POST** /acuerdos | Proponer un acuerdo y abrir su votación
[**resolverAlias**](DefaultApi.md#resolveralias) | **GET** /grupos/participantes/alias/{alias} | A que persona apunta un alias
[**revelarSorteo**](DefaultApi.md#revelarsorteo) | **POST** /grupos/{grupoId}/sorteo/revelacion | Revelar la semilla y crear los turnos
[**solicitarPermuta**](DefaultApi.md#solicitarpermuta) | **POST** /turnos/permutas | Solicitar la permuta de dos turnos
[**solicitarRetiro**](DefaultApi.md#solicitarretiro) | **POST** /grupos/{grupoId}/retiros | Solicitar el retiro y ver la posición exacta antes de confirmar
[**traspasarCupo**](DefaultApi.md#traspasarcupo) | **POST** /grupos/{grupoId}/cupos/{cupoId}/traspasos | Traspasar un cupo a otro participante
[**votarAcuerdo**](DefaultApi.md#votaracuerdo) | **POST** /acuerdos/{acuerdoId}/votos | Emitir el voto, con el peso de los cupos


# **calcularPlazoHabil**
> SalidaPlazoHabil calcularPlazoHabil(desde, dias, alcance, referenciaId)

Fecha límite en días hábiles, con los días que se saltearon

El resultado se GUARDA donde se use y no se recalcula: si despues se declara un feriado, ese plazo no se mueve. Devuelve tambien que dias se saltearon, porque un plazo que no se puede explicar no se puede defender. Nota de ruta: el caso de uso dice `/calendario`, que no esta entre los prefijos reservados de `grupos` (/grupos, /turnos, /acuerdos). Va bajo /grupos/calendario para no invadir un prefijo sin dueno.

### Example
```dart
import 'package:aportaya_cliente_grupos/api.dart';

final api = AportayaClienteGrupos().getDefaultApi();
final DateTime desde = 2013-10-20; // DateTime | 
final int dias = 56; // int | 
final String alcance = alcance_example; // String | 
final String referenciaId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 

try {
    final response = api.calcularPlazoHabil(desde, dias, alcance, referenciaId);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->calcularPlazoHabil: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **desde** | **DateTime**|  | 
 **dias** | **int**|  | 
 **alcance** | **String**|  | 
 **referenciaId** | **String**|  | [optional] 

### Return type

[**SalidaPlazoHabil**](SalidaPlazoHabil.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **comprometerSorteo**
> CompromisoDeSorteo comprometerSorteo(grupoId, idempotencyKey, entradaCompromiso)

Publicar el compromiso del sorteo

Publica SOLO el hash de la semilla. Sin esa separacion, quien ejecuta el sorteo podria probar semillas hasta que salga el orden que le conviene: el compromiso es lo que convierte «confia en nosotros» en «verificalo vos».

### Example
```dart
import 'package:aportaya_cliente_grupos/api.dart';

final api = AportayaClienteGrupos().getDefaultApi();
final String grupoId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaCompromiso entradaCompromiso = ; // EntradaCompromiso | 

try {
    final response = api.comprometerSorteo(grupoId, idempotencyKey, entradaCompromiso);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->comprometerSorteo: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **grupoId** | **String**|  | 
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaCompromiso** | [**EntradaCompromiso**](EntradaCompromiso.md)|  | [optional] 

### Return type

[**CompromisoDeSorteo**](CompromisoDeSorteo.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **consultarActividad**
> ActividadEnGrupos consultarActividad(usuarioId)

Si la persona participa hoy de algun grupo activo

La pregunta `nucleo-financiero` antes de cerrar una billetera (CU-16): cerrar la cuenta de alguien que todavia debe aportar a un pasanaku deja al grupo sin de donde cobrarle.

### Example
```dart
import 'package:aportaya_cliente_grupos/api.dart';

final api = AportayaClienteGrupos().getDefaultApi();
final String usuarioId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 

try {
    final response = api.consultarActividad(usuarioId);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->consultarActividad: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **usuarioId** | **String**|  | 

### Return type

[**ActividadEnGrupos**](ActividadEnGrupos.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **consultarPaqueteDelSorteo**
> PaqueteDelSorteo consultarPaqueteDelSorteo(sorteoId)

El paquete publicado de un sorteo, para poder recomputarlo

La pregunta `transparencia` para CU-61. Trae lo que hace falta para rehacer el sorteo desde afuera: el hash comprometido, la semilla revelada, las entropias, el metodo y el orden publicado. Antes del revelado la semilla viaja nula, y ese es el punto: comprometer y revelar existen para que nadie —nosotros incluidos— pueda elegir el resultado despues de conocerlo.

### Example
```dart
import 'package:aportaya_cliente_grupos/api.dart';

final api = AportayaClienteGrupos().getDefaultApi();
final String sorteoId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 

try {
    final response = api.consultarPaqueteDelSorteo(sorteoId);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->consultarPaqueteDelSorteo: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **sorteoId** | **String**|  | 

### Return type

[**PaqueteDelSorteo**](PaqueteDelSorteo.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **crearGrupo**
> SalidaGrupo crearGrupo(idempotencyKey, entradaGrupo)

Crear el grupo y disparar el congelamiento del tarifario

El grupo nace BORRADOR y emite `grupos.grupo_creado`; `tarifas` congela el precio y `nucleo-financiero` abre la cuenta del grupo al consumirlo. Pasa a ABIERTO_A_INSCRIPCION cuando los dos respondieron. Que nazca en borrador no es un rodeo: un grupo sin precio congelado no puede recibir a nadie, porque el precio es justamente lo que cada participante acepta al firmar el reglamento. Dejarlo abierto antes seria pedirle a alguien que se comprometa a un costo que todavia no existe. La cuenta del grupo tiene por titular al GRUPO y nunca al organizador (R-GRP-04): si fuera del organizador, la plata del grupo estaria a nombre de una persona.

### Example
```dart
import 'package:aportaya_cliente_grupos/api.dart';

final api = AportayaClienteGrupos().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaGrupo entradaGrupo = ; // EntradaGrupo | 

try {
    final response = api.crearGrupo(idempotencyKey, entradaGrupo);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->crearGrupo: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaGrupo** | [**EntradaGrupo**](EntradaGrupo.md)|  | 

### Return type

[**SalidaGrupo**](SalidaGrupo.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **invitarAlGrupo**
> SalidaInvitacion invitarAlGrupo(grupoId, idempotencyKey, entradaInvitacion)

Invitar a un contacto, con token de un solo uso

Si el destinatario esta suprimido NO se envia nada y se responde como si hubiera salido bien: decir «esa persona pidio no recibir mensajes» ya cuenta algo de ella a quien no tiene por que saberlo. El mensaje dice quien invita y a que grupo, con el monto y la periodicidad — nadie deberia aceptar un compromiso de dinero sin saber cual— y no revela datos de los otros integrantes. El token lo emite `identidad`: aca se recibe su identificador.

### Example
```dart
import 'package:aportaya_cliente_grupos/api.dart';

final api = AportayaClienteGrupos().getDefaultApi();
final String grupoId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaInvitacion entradaInvitacion = ; // EntradaInvitacion | 

try {
    final response = api.invitarAlGrupo(grupoId, idempotencyKey, entradaInvitacion);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->invitarAlGrupo: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **grupoId** | **String**|  | 
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaInvitacion** | [**EntradaInvitacion**](EntradaInvitacion.md)|  | 

### Return type

[**SalidaInvitacion**](SalidaInvitacion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **postularAlGrupo**
> SalidaPostulacion postularAlGrupo(grupoId, idempotencyKey, entradaPostulacion)

Postular a un grupo, con el motivo legible del puntaje

El postulante ve POR QUE se le propuso ese grupo —monto compatible, gente de su zona, riesgo parecido— y nunca los datos de los demas. Un puntaje sin explicacion es un numero que nadie puede discutir, y un emparejamiento que nadie puede discutir es uno que se acepta por resignacion. Los pesos del criterio son datos CON VIGENCIA, no constantes: hay que poder explicar con que pesos se armo un grupo de hace seis meses.

### Example
```dart
import 'package:aportaya_cliente_grupos/api.dart';

final api = AportayaClienteGrupos().getDefaultApi();
final String grupoId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaPostulacion entradaPostulacion = ; // EntradaPostulacion | 

try {
    final response = api.postularAlGrupo(grupoId, idempotencyKey, entradaPostulacion);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->postularAlGrupo: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **grupoId** | **String**|  | 
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaPostulacion** | [**EntradaPostulacion**](EntradaPostulacion.md)|  | 

### Return type

[**SalidaPostulacion**](SalidaPostulacion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **proponerAcuerdo**
> SalidaAcuerdo proponerAcuerdo(idempotencyKey, entradaAcuerdo)

Proponer un acuerdo y abrir su votación

Ningun efecto colectivo se ejecuta sin acuerdo aprobado, y el efecto va en la MISMA transaccion que la aprobacion: si el efecto falla, el acuerdo no queda aprobado sin su efecto — que seria un grupo convencido de que decidio algo que no ocurrio.

### Example
```dart
import 'package:aportaya_cliente_grupos/api.dart';

final api = AportayaClienteGrupos().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaAcuerdo entradaAcuerdo = ; // EntradaAcuerdo | 

try {
    final response = api.proponerAcuerdo(idempotencyKey, entradaAcuerdo);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->proponerAcuerdo: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaAcuerdo** | [**EntradaAcuerdo**](EntradaAcuerdo.md)|  | 

### Return type

[**SalidaAcuerdo**](SalidaAcuerdo.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **resolverAlias**
> AliasResuelto resolverAlias(alias)

A que persona apunta un alias

La pregunta `nucleo-financiero` para transferir a un alias (CU-12). El alias vive en `grupos.participante` y ese esquema no se lee desde afuera (invariante 11). Devuelve el identificador de la persona y nada mas — ni el nombre, ni el telefono, ni la cuenta. La cuenta la resuelve quien la posee: `grupos` no conoce `nucleo_financiero.cuenta_billetera`, y esta ruta no es un directorio.

### Example
```dart
import 'package:aportaya_cliente_grupos/api.dart';

final api = AportayaClienteGrupos().getDefaultApi();
final String alias = alias_example; // String | 

try {
    final response = api.resolverAlias(alias);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->resolverAlias: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **alias** | **String**|  | 

### Return type

[**AliasResuelto**](AliasResuelto.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **revelarSorteo**
> RevelacionDeSorteo revelarSorteo(grupoId, idempotencyKey, entradaRevelacion)

Revelar la semilla y crear los turnos

Verifica que SHA256(semilla || entropias) sea el hash publicado y recien entonces crea los turnos, todo en una transaccion: o hay turnos completos o no hay ninguno. Si el hash no cierra, el sorteo queda ANULADO — jamas se publica un resultado que no verifica.

### Example
```dart
import 'package:aportaya_cliente_grupos/api.dart';

final api = AportayaClienteGrupos().getDefaultApi();
final String grupoId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaRevelacion entradaRevelacion = ; // EntradaRevelacion | 

try {
    final response = api.revelarSorteo(grupoId, idempotencyKey, entradaRevelacion);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->revelarSorteo: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **grupoId** | **String**|  | 
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaRevelacion** | [**EntradaRevelacion**](EntradaRevelacion.md)|  | 

### Return type

[**RevelacionDeSorteo**](RevelacionDeSorteo.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **solicitarPermuta**
> SalidaPermuta solicitarPermuta(idempotencyKey, entradaPermuta)

Solicitar la permuta de dos turnos

Sin aceptacion de la contraparte no hay nada: la solicitud nace PENDIENTE. Si alguna parte tiene deuda no se permuta — adelantar el cobro de un moroso lo paga el grupo entero.

### Example
```dart
import 'package:aportaya_cliente_grupos/api.dart';

final api = AportayaClienteGrupos().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaPermuta entradaPermuta = ; // EntradaPermuta | 

try {
    final response = api.solicitarPermuta(idempotencyKey, entradaPermuta);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->solicitarPermuta: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaPermuta** | [**EntradaPermuta**](EntradaPermuta.md)|  | 

### Return type

[**SalidaPermuta**](SalidaPermuta.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **solicitarRetiro**
> SalidaRetiro solicitarRetiro(grupoId, idempotencyKey, entradaRetiro)

Solicitar el retiro y ver la posición exacta antes de confirmar

El numero se calcula y se MUESTRA ANTES de confirmar, con su desglose. Un retiro donde el monto aparece despues es un retiro que alguien va a discutir, y con razon. Acreedora sin reemplazo se liquida AL CIERRE DEL CICLO, no antes: sacar plata de la bolsa a mitad de camino perjudica a los que faltan cobrar.

### Example
```dart
import 'package:aportaya_cliente_grupos/api.dart';

final api = AportayaClienteGrupos().getDefaultApi();
final String grupoId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaRetiro entradaRetiro = ; // EntradaRetiro | 

try {
    final response = api.solicitarRetiro(grupoId, idempotencyKey, entradaRetiro);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->solicitarRetiro: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **grupoId** | **String**|  | 
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaRetiro** | [**EntradaRetiro**](EntradaRetiro.md)|  | 

### Return type

[**SalidaRetiro**](SalidaRetiro.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **traspasarCupo**
> SalidaTraspaso traspasarCupo(grupoId, cupoId, idempotencyKey, entradaTraspaso)

Traspasar un cupo a otro participante

El TURNO no se toca: la posicion en el calendario es del cupo, no de la persona. Si se moviera, traspasar seria una forma de adelantar el propio turno y el sorteo dejaria de significar nada. Y la deuda no viaja con el cupo: las obligaciones vencidas se quedan con quien las genero. Sin eso, un cupo seria una forma de venderle a alguien la deuda de otro.

### Example
```dart
import 'package:aportaya_cliente_grupos/api.dart';

final api = AportayaClienteGrupos().getDefaultApi();
final String grupoId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final String cupoId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaTraspaso entradaTraspaso = ; // EntradaTraspaso | 

try {
    final response = api.traspasarCupo(grupoId, cupoId, idempotencyKey, entradaTraspaso);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->traspasarCupo: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **grupoId** | **String**|  | 
 **cupoId** | **String**|  | 
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaTraspaso** | [**EntradaTraspaso**](EntradaTraspaso.md)|  | 

### Return type

[**SalidaTraspaso**](SalidaTraspaso.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **votarAcuerdo**
> SalidaAcuerdo votarAcuerdo(acuerdoId, idempotencyKey, entradaVoto)

Emitir el voto, con el peso de los cupos

El afectado por el acuerdo vota y su voto QUEDA REGISTRADO, como abstencion con peso cero. Impedirle votar lo dejaria sin constancia de que se presento; dejarlo ponderar convertiria el quorum en una formalidad en los dos casos donde mas importa.

### Example
```dart
import 'package:aportaya_cliente_grupos/api.dart';

final api = AportayaClienteGrupos().getDefaultApi();
final String acuerdoId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaVoto entradaVoto = ; // EntradaVoto | 

try {
    final response = api.votarAcuerdo(acuerdoId, idempotencyKey, entradaVoto);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->votarAcuerdo: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **acuerdoId** | **String**|  | 
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaVoto** | [**EntradaVoto**](EntradaVoto.md)|  | 

### Return type

[**SalidaAcuerdo**](SalidaAcuerdo.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

