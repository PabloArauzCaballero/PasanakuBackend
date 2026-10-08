# aportaya_cliente_transparencia.api.DefaultApi

## Load the API package
```dart
import 'package:aportaya_cliente_transparencia/api.dart';
```

All URIs are relative to */api/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**cerrarAlertaRiesgo**](DefaultApi.md#cerraralertariesgo) | **POST** /reputacion/riesgo/alertas/{alertaId}/cierre | Cerrar una alerta con su desenlace
[**compensarEventoReputacion**](DefaultApi.md#compensareventoreputacion) | **POST** /reputacion/eventos/{eventoId}/compensacion | Compensar un evento que sumo o resto puntos
[**consultarPuntaje**](DefaultApi.md#consultarpuntaje) | **GET** /reputacion/{usuarioId}/puntaje | El puntaje de reputacion de alguien
[**emitirCertificado**](DefaultApi.md#emitircertificado) | **POST** /reputacion/certificados | Emitir un certificado de reputacion verificable
[**evaluarInsignias**](DefaultApi.md#evaluarinsignias) | **POST** /reputacion/insignias/evaluacion | Evaluar y otorgar las insignias que un hecho desbloqueo
[**evaluarRiesgo**](DefaultApi.md#evaluarriesgo) | **POST** /reputacion/riesgo/evaluacion | Anticipar el riesgo con alertas tempranas
[**listarBloques**](DefaultApi.md#listarbloques) | **GET** /publico/grupos/{grupoId}/bloques | Los bloques publicados de un grupo
[**moderarResena**](DefaultApi.md#moderarresena) | **POST** /reputacion/resenas/{resenaId}/moderacion | Moderar una resena
[**publicarResena**](DefaultApi.md#publicarresena) | **POST** /reputacion/resenas | Resenar a un participante
[**recalcularPuntaje**](DefaultApi.md#recalcularpuntaje) | **POST** /reputacion/puntajes | Recalcular el puntaje de reputacion
[**registrarEventoReputacion**](DefaultApi.md#registrareventoreputacion) | **POST** /reputacion/eventos | Registrar un evento de reputacion
[**revocarCertificado**](DefaultApi.md#revocarcertificado) | **POST** /reputacion/certificados/{codigo}/revocacion | Revocar un certificado
[**revocarInsignia**](DefaultApi.md#revocarinsignia) | **POST** /reputacion/insignias/{otorgadaId}/revocacion | Revocar una insignia, sin borrarla
[**sellarBloque**](DefaultApi.md#sellarbloque) | **POST** /reputacion/bloques | Sellar el bloque de transparencia
[**tomarSnapshotReputacion**](DefaultApi.md#tomarsnapshotreputacion) | **POST** /reputacion/snapshots | Congelar una foto del puntaje
[**verificarCadena**](DefaultApi.md#verificarcadena) | **GET** /publico/grupos/{grupoId}/verificacion | Verificar la cadena de transparencia
[**verificarCertificado**](DefaultApi.md#verificarcertificado) | **GET** /verificar/{codigo} | Verificar un certificado por su codigo
[**verificarSorteo**](DefaultApi.md#verificarsorteo) | **GET** /publico/sorteos/{sorteoId}/verificacion | Verificar publicamente el sorteo


# **cerrarAlertaRiesgo**
> cerrarAlertaRiesgo(alertaId, entradaCierreAlerta)

Cerrar una alerta con su desenlace

Cerrar exige desenlace: una alerta que se cierra sin decir si era cierta no deja calibrar nada, y el modelo se queda con el mismo error para siempre.

### Example
```dart
import 'package:aportaya_cliente_transparencia/api.dart';

final api = AportayaClienteTransparencia().getDefaultApi();
final String alertaId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final EntradaCierreAlerta entradaCierreAlerta = ; // EntradaCierreAlerta | 

try {
    api.cerrarAlertaRiesgo(alertaId, entradaCierreAlerta);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->cerrarAlertaRiesgo: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **alertaId** | **String**|  | 
 **entradaCierreAlerta** | [**EntradaCierreAlerta**](EntradaCierreAlerta.md)|  | 

### Return type

void (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/problem+json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **compensarEventoReputacion**
> SalidaEventoReputacion compensarEventoReputacion(eventoId, entradaCompensacion)

Compensar un evento que sumo o resto puntos

No se borra el original ni se lo edita: evento_reputacion es append-only. Entra una fila nueva con el impacto exacto invertido y `revertidoPorId` apuntando al original.

### Example
```dart
import 'package:aportaya_cliente_transparencia/api.dart';

final api = AportayaClienteTransparencia().getDefaultApi();
final String eventoId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final EntradaCompensacion entradaCompensacion = ; // EntradaCompensacion | 

try {
    final response = api.compensarEventoReputacion(eventoId, entradaCompensacion);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->compensarEventoReputacion: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **eventoId** | **String**|  | 
 **entradaCompensacion** | [**EntradaCompensacion**](EntradaCompensacion.md)|  | 

### Return type

[**SalidaEventoReputacion**](SalidaEventoReputacion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json, application/problem+json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **consultarPuntaje**
> PuntajeDelUsuario consultarPuntaje(usuarioId)

El puntaje de reputacion de alguien

Lo preguntan CU-64 y CU-68 de `grupos`: aceptar a alguien o dejarle tomar un cupo depende de su historial, y el historial es de este servicio. Quien todavia no tiene puntaje NO tiene cero. Cero seria el peor puntaje posible y quien recien llega no hizo nada malo: se responde SIN_HISTORIAL y quien pregunta decide. Confundir «no se sabe» con «malo» le cierra la puerta a todo el que empieza.

### Example
```dart
import 'package:aportaya_cliente_transparencia/api.dart';

final api = AportayaClienteTransparencia().getDefaultApi();
final String usuarioId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 

try {
    final response = api.consultarPuntaje(usuarioId);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->consultarPuntaje: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **usuarioId** | **String**|  | 

### Return type

[**PuntajeDelUsuario**](PuntajeDelUsuario.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **emitirCertificado**
> SalidaCertificado emitirCertificado(idempotencyKey, entradaCertificado)

Emitir un certificado de reputacion verificable

El titular elige que incluir campo por campo, y lo que no eligio no entra ni al hash. Solo el titular emite el suyo (R-SEG-03).

### Example
```dart
import 'package:aportaya_cliente_transparencia/api.dart';

final api = AportayaClienteTransparencia().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaCertificado entradaCertificado = ; // EntradaCertificado | 

try {
    final response = api.emitirCertificado(idempotencyKey, entradaCertificado);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->emitirCertificado: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaCertificado** | [**EntradaCertificado**](EntradaCertificado.md)|  | 

### Return type

[**SalidaCertificado**](SalidaCertificado.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json, application/problem+json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **evaluarInsignias**
> List<SalidaInsignia> evaluarInsignias(idempotencyKey, entradaEvaluacionInsignias)

Evaluar y otorgar las insignias que un hecho desbloqueo

NO hay otorgamiento manual. Se gana contra los datos o no se gana: una insignia que alguien puede conceder a dedo no dice nada de quien la lleva.

### Example
```dart
import 'package:aportaya_cliente_transparencia/api.dart';

final api = AportayaClienteTransparencia().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaEvaluacionInsignias entradaEvaluacionInsignias = ; // EntradaEvaluacionInsignias | 

try {
    final response = api.evaluarInsignias(idempotencyKey, entradaEvaluacionInsignias);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->evaluarInsignias: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaEvaluacionInsignias** | [**EntradaEvaluacionInsignias**](EntradaEvaluacionInsignias.md)|  | 

### Return type

[**List&lt;SalidaInsignia&gt;**](SalidaInsignia.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json, application/problem+json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **evaluarRiesgo**
> SalidaEvaluacionRiesgo evaluarRiesgo(idempotencyKey, entradaEvaluacionRiesgo)

Anticipar el riesgo con alertas tempranas

Sin historial el nivel es SIN_DATOS, nunca riesgo alto. Al participante nunca se le muestra un puntaje: el mensaje habla de hechos concretos, con numeros, y de que puede hacer.

### Example
```dart
import 'package:aportaya_cliente_transparencia/api.dart';

final api = AportayaClienteTransparencia().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaEvaluacionRiesgo entradaEvaluacionRiesgo = ; // EntradaEvaluacionRiesgo | 

try {
    final response = api.evaluarRiesgo(idempotencyKey, entradaEvaluacionRiesgo);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->evaluarRiesgo: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaEvaluacionRiesgo** | [**EntradaEvaluacionRiesgo**](EntradaEvaluacionRiesgo.md)|  | 

### Return type

[**SalidaEvaluacionRiesgo**](SalidaEvaluacionRiesgo.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json, application/problem+json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **listarBloques**
> List<BloquePublicado> listarBloques(grupoId)

Los bloques publicados de un grupo

Ruta PUBLICA, sin sesion. Es el paquete que cualquiera descarga para verificar por fuera.

### Example
```dart
import 'package:aportaya_cliente_transparencia/api.dart';

final api = AportayaClienteTransparencia().getDefaultApi();
final String grupoId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 

try {
    final response = api.listarBloques(grupoId);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->listarBloques: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **grupoId** | **String**|  | 

### Return type

[**List&lt;BloquePublicado&gt;**](BloquePublicado.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **moderarResena**
> SalidaResena moderarResena(resenaId, entradaModeracion)

Moderar una resena

Nada se publica sin moderar. Lo que trae datos personales se retiene, no se recorta: recortar deja el dato en la base y la sospecha en el aire.

### Example
```dart
import 'package:aportaya_cliente_transparencia/api.dart';

final api = AportayaClienteTransparencia().getDefaultApi();
final String resenaId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final EntradaModeracion entradaModeracion = ; // EntradaModeracion | 

try {
    final response = api.moderarResena(resenaId, entradaModeracion);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->moderarResena: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **resenaId** | **String**|  | 
 **entradaModeracion** | [**EntradaModeracion**](EntradaModeracion.md)|  | 

### Return type

[**SalidaResena**](SalidaResena.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json, application/problem+json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **publicarResena**
> SalidaResena publicarResena(idempotencyKey, entradaResena)

Resenar a un participante

Solo resena quien convivio y una vez por autor, evaluado, grupo y dimension (R-REP-06). Se resena al cerrar el ciclo, no durante: resenar a quien todavia te debe plata convierte la resena en una herramienta de presion.

### Example
```dart
import 'package:aportaya_cliente_transparencia/api.dart';

final api = AportayaClienteTransparencia().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaResena entradaResena = ; // EntradaResena | 

try {
    final response = api.publicarResena(idempotencyKey, entradaResena);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->publicarResena: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaResena** | [**EntradaResena**](EntradaResena.md)|  | 

### Return type

[**SalidaResena**](SalidaResena.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json, application/problem+json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **recalcularPuntaje**
> SalidaPuntaje recalcularPuntaje(idempotencyKey, entradaRecalculo)

Recalcular el puntaje de reputacion

Un solo puntaje vigente por usuario (R-REP-02) y el total es la suma de sus componentes (R-REP-03). Con menos eventos que el minimo del modelo el nivel es SIN_HISTORIAL: no es un castigo, es decir la verdad.

### Example
```dart
import 'package:aportaya_cliente_transparencia/api.dart';

final api = AportayaClienteTransparencia().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaRecalculo entradaRecalculo = ; // EntradaRecalculo | 

try {
    final response = api.recalcularPuntaje(idempotencyKey, entradaRecalculo);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->recalcularPuntaje: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaRecalculo** | [**EntradaRecalculo**](EntradaRecalculo.md)|  | 

### Return type

[**SalidaPuntaje**](SalidaPuntaje.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json, application/problem+json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **registrarEventoReputacion**
> SalidaEventoReputacion registrarEventoReputacion(idempotencyKey, entradaEventoReputacion)

Registrar un evento de reputacion

Un evento por hecho (R-REP-01). El impacto sale del catalogo del modelo, no de constantes: sin regla no se puntua, porque inventarle un impacto a un tipo de evento es decidir a mano cuanto vale la conducta de alguien.

### Example
```dart
import 'package:aportaya_cliente_transparencia/api.dart';

final api = AportayaClienteTransparencia().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaEventoReputacion entradaEventoReputacion = ; // EntradaEventoReputacion | 

try {
    final response = api.registrarEventoReputacion(idempotencyKey, entradaEventoReputacion);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->registrarEventoReputacion: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaEventoReputacion** | [**EntradaEventoReputacion**](EntradaEventoReputacion.md)|  | 

### Return type

[**SalidaEventoReputacion**](SalidaEventoReputacion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json, application/problem+json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **revocarCertificado**
> revocarCertificado(codigo, entradaRevocacionCertificado)

Revocar un certificado

Lo usa tambien el derecho de supresion (CU-07): al suprimir, la URL deja de resolver.

### Example
```dart
import 'package:aportaya_cliente_transparencia/api.dart';

final api = AportayaClienteTransparencia().getDefaultApi();
final String codigo = codigo_example; // String | 
final EntradaRevocacionCertificado entradaRevocacionCertificado = ; // EntradaRevocacionCertificado | 

try {
    api.revocarCertificado(codigo, entradaRevocacionCertificado);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->revocarCertificado: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **codigo** | **String**|  | 
 **entradaRevocacionCertificado** | [**EntradaRevocacionCertificado**](EntradaRevocacionCertificado.md)|  | 

### Return type

void (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/problem+json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **revocarInsignia**
> SalidaInsignia revocarInsignia(otorgadaId, entradaRevocacionInsignia)

Revocar una insignia, sin borrarla

Revocar no borra (R-REP-05). Queda la fila, la fecha y el motivo: borrarla dejaria a la persona sin poder saber por que perdio algo que tenia.

### Example
```dart
import 'package:aportaya_cliente_transparencia/api.dart';

final api = AportayaClienteTransparencia().getDefaultApi();
final String otorgadaId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final EntradaRevocacionInsignia entradaRevocacionInsignia = ; // EntradaRevocacionInsignia | 

try {
    final response = api.revocarInsignia(otorgadaId, entradaRevocacionInsignia);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->revocarInsignia: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **otorgadaId** | **String**|  | 
 **entradaRevocacionInsignia** | [**EntradaRevocacionInsignia**](EntradaRevocacionInsignia.md)|  | 

### Return type

[**SalidaInsignia**](SalidaInsignia.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json, application/problem+json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **sellarBloque**
> SalidaSellado sellarBloque(idempotencyKey, entradaSellado)

Sellar el bloque de transparencia

Con excepciones de conciliacion abiertas NO se sella (R-BIL-12). Un hecho ya sellado no se reescribe: la correccion entra al bloque siguiente como movimiento compensatorio y los dos quedan visibles.

### Example
```dart
import 'package:aportaya_cliente_transparencia/api.dart';

final api = AportayaClienteTransparencia().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaSellado entradaSellado = ; // EntradaSellado | 

try {
    final response = api.sellarBloque(idempotencyKey, entradaSellado);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->sellarBloque: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaSellado** | [**EntradaSellado**](EntradaSellado.md)|  | 

### Return type

[**SalidaSellado**](SalidaSellado.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json, application/problem+json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **tomarSnapshotReputacion**
> SalidaSnapshot tomarSnapshotReputacion(entradaSnapshot)

Congelar una foto del puntaje

### Example
```dart
import 'package:aportaya_cliente_transparencia/api.dart';

final api = AportayaClienteTransparencia().getDefaultApi();
final EntradaSnapshot entradaSnapshot = ; // EntradaSnapshot | 

try {
    final response = api.tomarSnapshotReputacion(entradaSnapshot);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->tomarSnapshotReputacion: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **entradaSnapshot** | [**EntradaSnapshot**](EntradaSnapshot.md)|  | 

### Return type

[**SalidaSnapshot**](SalidaSnapshot.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json, application/problem+json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **verificarCadena**
> SalidaVerificacionCadena verificarCadena(grupoId)

Verificar la cadena de transparencia

Ruta PUBLICA, sin sesion. Recorre los bloques y devuelve el primero que falla y que componente no cuadra. Un grupo sin bloques no es un error de integridad.

### Example
```dart
import 'package:aportaya_cliente_transparencia/api.dart';

final api = AportayaClienteTransparencia().getDefaultApi();
final String grupoId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 

try {
    final response = api.verificarCadena(grupoId);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->verificarCadena: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **grupoId** | **String**|  | 

### Return type

[**SalidaVerificacionCadena**](SalidaVerificacionCadena.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/problem+json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **verificarCertificado**
> SalidaVerificacionCertificado verificarCertificado(codigo)

Verificar un certificado por su codigo

Ruta PUBLICA, sin sesion. Un codigo inexistente responde igual que uno revocado: NO_VALIDO, sin fechas. Distinguirlos le diria a quien prueba codigos al azar cuando acerto.

### Example
```dart
import 'package:aportaya_cliente_transparencia/api.dart';

final api = AportayaClienteTransparencia().getDefaultApi();
final String codigo = codigo_example; // String | 

try {
    final response = api.verificarCertificado(codigo);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->verificarCertificado: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **codigo** | **String**|  | 

### Return type

[**SalidaVerificacionCertificado**](SalidaVerificacionCertificado.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **verificarSorteo**
> SalidaVerificacionSorteo verificarSorteo(sorteoId)

Verificar publicamente el sorteo

Ruta PUBLICA, sin sesion. Recomputa el compromiso y el barajado con el mismo atomo que uso CU-60 para sortear: una sola implementacion para generar y para verificar. Antes del revelado no hay nada que verificar, y ese es el punto.

### Example
```dart
import 'package:aportaya_cliente_transparencia/api.dart';

final api = AportayaClienteTransparencia().getDefaultApi();
final String sorteoId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 

try {
    final response = api.verificarSorteo(sorteoId);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->verificarSorteo: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **sorteoId** | **String**|  | 

### Return type

[**SalidaVerificacionSorteo**](SalidaVerificacionSorteo.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json, application/problem+json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

