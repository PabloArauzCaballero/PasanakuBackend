# aportaya_cliente_organizador.api.DefaultApi

## Load the API package
```dart
import 'package:aportaya_cliente_organizador/api.dart';
```

All URIs are relative to */api/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**activarRegla**](DefaultApi.md#activarregla) | **POST** /automatizacion/reglas/{reglaId}/activacion | Encender la regla
[**anotarEjecucion**](DefaultApi.md#anotarejecucion) | **POST** /automatizacion/tareas/{tareaId}/ejecuciones | Registrar el resultado de una ejecucion
[**apelarSancion**](DefaultApi.md#apelarsancion) | **POST** /organizadores/sanciones/{sancionId}/apelacion | Presentar la apelacion
[**aprobarPostulacion**](DefaultApi.md#aprobarpostulacion) | **POST** /organizadores/postulaciones/{solicitudId}/aprobacion | Aprobar la postulacion y crear al organizador
[**aprobarTarea**](DefaultApi.md#aprobartarea) | **POST** /automatizacion/tareas/{tareaId}/aprobacion | La confirmacion humana que exige una accion sensible
[**consultarHabilitacion**](DefaultApi.md#consultarhabilitacion) | **GET** /organizadores/{organizadorId}/habilitacion | Si el organizador esta habilitado, y hasta donde
[**definirRegla**](DefaultApi.md#definirregla) | **POST** /automatizacion/reglas | Definir una regla de automatizacion
[**emitirContrato**](DefaultApi.md#emitircontrato) | **POST** /organizadores/{organizadorId}/contratos | Emitir el contrato de organizador
[**evaluarDesempeno**](DefaultApi.md#evaluardesempeno) | **POST** /organizadores/{organizadorId}/evaluaciones | Evaluar el desempeno de un periodo
[**firmarContrato**](DefaultApi.md#firmarcontrato) | **POST** /organizadores/contratos/{contratoId}/firma | Firmar el contrato
[**habilitarOrganizador**](DefaultApi.md#habilitarorganizador) | **POST** /organizadores/{organizadorId}/habilitacion | Habilitar tras completar la capacitacion
[**postularOrganizador**](DefaultApi.md#postularorganizador) | **POST** /organizadores/postulaciones | Postular para administrar grupos
[**programarTarea**](DefaultApi.md#programartarea) | **POST** /automatizacion/tareas | Programar una tarea automatizada
[**rescindirContrato**](DefaultApi.md#rescindircontrato) | **POST** /organizadores/contratos/{contratoId}/rescision | Rescindir el contrato
[**resolverApelacion**](DefaultApi.md#resolverapelacion) | **POST** /organizadores/sanciones/{sancionId}/resolucion | Resolver la apelacion
[**sancionarOrganizador**](DefaultApi.md#sancionarorganizador) | **POST** /organizadores/{organizadorId}/sanciones | Aplicar una sancion


# **activarRegla**
> SalidaRegla activarRegla(reglaId)

Encender la regla

Se revuelve a comprobar la coherencia y la prioridad: dos reglas activas con el mismo disparador y prioridad hacen que el orden de ejecucion dependa de como la base devuelva las filas.

### Example
```dart
import 'package:aportaya_cliente_organizador/api.dart';

final api = AportayaClienteOrganizador().getDefaultApi();
final String reglaId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 

try {
    final response = api.activarRegla(reglaId);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->activarRegla: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **reglaId** | **String**|  | 

### Return type

[**SalidaRegla**](SalidaRegla.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **anotarEjecucion**
> SalidaEjecucion anotarEjecucion(tareaId, entradaEjecucion)

Registrar el resultado de una ejecucion

La accion real la ejecuta el servicio dueno del efecto; aca se registra que paso. Ejecutarla desde este servicio seria escribir esquemas ajenos (invariante 11).

### Example
```dart
import 'package:aportaya_cliente_organizador/api.dart';

final api = AportayaClienteOrganizador().getDefaultApi();
final String tareaId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final EntradaEjecucion entradaEjecucion = ; // EntradaEjecucion | 

try {
    final response = api.anotarEjecucion(tareaId, entradaEjecucion);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->anotarEjecucion: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **tareaId** | **String**|  | 
 **entradaEjecucion** | [**EntradaEjecucion**](EntradaEjecucion.md)|  | 

### Return type

[**SalidaEjecucion**](SalidaEjecucion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **apelarSancion**
> SalidaApelacion apelarSancion(sancionId, entradaApelacion)

Presentar la apelacion

La sancion pasa a APELADA y no se puede dar por cumplida. La FILA de la apelacion se escribe al resolverla: la base no admite una apelacion abierta (hueco H-7 del informe del carril).

### Example
```dart
import 'package:aportaya_cliente_organizador/api.dart';

final api = AportayaClienteOrganizador().getDefaultApi();
final String sancionId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final EntradaApelacion entradaApelacion = ; // EntradaApelacion | 

try {
    final response = api.apelarSancion(sancionId, entradaApelacion);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->apelarSancion: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **sancionId** | **String**|  | 
 **entradaApelacion** | [**EntradaApelacion**](EntradaApelacion.md)|  | 

### Return type

[**SalidaApelacion**](SalidaApelacion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **aprobarPostulacion**
> SalidaHabilitacion aprobarPostulacion(solicitudId, entradaAprobacion)

Aprobar la postulacion y crear al organizador

Quien aprueba no puede ser quien postulo (R-SEG-04). El organizador nace en CAPACITACION_PENDIENTE: aprobar y entregar el acceso al fondo en el mismo acto saltaria el paso donde se le explica que responsabilidad esta tomando.

### Example
```dart
import 'package:aportaya_cliente_organizador/api.dart';

final api = AportayaClienteOrganizador().getDefaultApi();
final String solicitudId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final EntradaAprobacion entradaAprobacion = ; // EntradaAprobacion | 

try {
    final response = api.aprobarPostulacion(solicitudId, entradaAprobacion);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->aprobarPostulacion: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **solicitudId** | **String**|  | 
 **entradaAprobacion** | [**EntradaAprobacion**](EntradaAprobacion.md)|  | 

### Return type

[**SalidaHabilitacion**](SalidaHabilitacion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **aprobarTarea**
> SalidaProgramacion aprobarTarea(tareaId)

La confirmacion humana que exige una accion sensible

### Example
```dart
import 'package:aportaya_cliente_organizador/api.dart';

final api = AportayaClienteOrganizador().getDefaultApi();
final String tareaId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 

try {
    final response = api.aprobarTarea(tareaId);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->aprobarTarea: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **tareaId** | **String**|  | 

### Return type

[**SalidaProgramacion**](SalidaProgramacion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **consultarHabilitacion**
> Habilitacion consultarHabilitacion(organizadorId)

Si el organizador esta habilitado, y hasta donde

La pregunta CU-20 antes de dejar crear un grupo: un organizador suspendido no abre pasanakus nuevos. `grupos` no puede leer este esquema (invariante 11). Devuelve tambien los limites del nivel porque quien crea el grupo los necesita en el mismo momento, y dos viajes para dos datos del mismo renglon es un viaje de mas.

### Example
```dart
import 'package:aportaya_cliente_organizador/api.dart';

final api = AportayaClienteOrganizador().getDefaultApi();
final String organizadorId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 

try {
    final response = api.consultarHabilitacion(organizadorId);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->consultarHabilitacion: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organizadorId** | **String**|  | 

### Return type

[**Habilitacion**](Habilitacion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **definirRegla**
> SalidaRegla definirRegla(entradaRegla)

Definir una regla de automatizacion

Las acciones que mueven plata ajena exigen confirmacion humana (R-ORG-06). La regla nace INACTIVA: publicarla y encenderla en el mismo acto no deja momento para revisar que la condicion diga lo que se cree que dice.

### Example
```dart
import 'package:aportaya_cliente_organizador/api.dart';

final api = AportayaClienteOrganizador().getDefaultApi();
final EntradaRegla entradaRegla = ; // EntradaRegla | 

try {
    final response = api.definirRegla(entradaRegla);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->definirRegla: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **entradaRegla** | [**EntradaRegla**](EntradaRegla.md)|  | 

### Return type

[**SalidaRegla**](SalidaRegla.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **emitirContrato**
> SalidaContrato emitirContrato(organizadorId, entradaContrato)

Emitir el contrato de organizador

Un solo contrato vigente por organizador (R-ORG-02). Dos permiten invocar el texto que mas convenga ante un incumplimiento.

### Example
```dart
import 'package:aportaya_cliente_organizador/api.dart';

final api = AportayaClienteOrganizador().getDefaultApi();
final String organizadorId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final EntradaContrato entradaContrato = ; // EntradaContrato | 

try {
    final response = api.emitirContrato(organizadorId, entradaContrato);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->emitirContrato: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organizadorId** | **String**|  | 
 **entradaContrato** | [**EntradaContrato**](EntradaContrato.md)|  | 

### Return type

[**SalidaContrato**](SalidaContrato.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **evaluarDesempeno**
> SalidaEvaluacion evaluarDesempeno(organizadorId, entradaEvaluacion)

Evaluar el desempeno de un periodo

Una evaluacion por organizador y periodo (R-ORG-04). El puntaje sale de metricas con su peso, y las metricas se guardan: si alguien va a perder su habilitacion por un numero, ese numero tiene que poder abrirse. El descenso se aplica; el ascenso queda SUGERIDO.

### Example
```dart
import 'package:aportaya_cliente_organizador/api.dart';

final api = AportayaClienteOrganizador().getDefaultApi();
final String organizadorId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final EntradaEvaluacion entradaEvaluacion = ; // EntradaEvaluacion | 

try {
    final response = api.evaluarDesempeno(organizadorId, entradaEvaluacion);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->evaluarDesempeno: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organizadorId** | **String**|  | 
 **entradaEvaluacion** | [**EntradaEvaluacion**](EntradaEvaluacion.md)|  | 

### Return type

[**SalidaEvaluacion**](SalidaEvaluacion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **firmarContrato**
> SalidaFirma firmarContrato(contratoId, firmarContratoRequest)

Firmar el contrato

Lo firma el propio organizador: una firma puesta por otro no obliga a nadie. Firmar dos veces no sobrescribe la evidencia de cuando y con que token se firmo.

### Example
```dart
import 'package:aportaya_cliente_organizador/api.dart';

final api = AportayaClienteOrganizador().getDefaultApi();
final String contratoId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final FirmarContratoRequest firmarContratoRequest = ; // FirmarContratoRequest | 

try {
    final response = api.firmarContrato(contratoId, firmarContratoRequest);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->firmarContrato: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **contratoId** | **String**|  | 
 **firmarContratoRequest** | [**FirmarContratoRequest**](FirmarContratoRequest.md)|  | 

### Return type

[**SalidaFirma**](SalidaFirma.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **habilitarOrganizador**
> SalidaHabilitacion habilitarOrganizador(organizadorId)

Habilitar tras completar la capacitacion

### Example
```dart
import 'package:aportaya_cliente_organizador/api.dart';

final api = AportayaClienteOrganizador().getDefaultApi();
final String organizadorId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 

try {
    final response = api.habilitarOrganizador(organizadorId);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->habilitarOrganizador: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organizadorId** | **String**|  | 

### Return type

[**SalidaHabilitacion**](SalidaHabilitacion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **postularOrganizador**
> SalidaPostulacion postularOrganizador(entradaPostulacion)

Postular para administrar grupos

Un organizador administra la plata de otros: la habilitacion es la ultima puerta antes de que alguien tenga acceso al fondo de un grupo entero. Los requisitos son catalogo, y un obligatorio SIN DATO no se da por cumplido.

### Example
```dart
import 'package:aportaya_cliente_organizador/api.dart';

final api = AportayaClienteOrganizador().getDefaultApi();
final EntradaPostulacion entradaPostulacion = ; // EntradaPostulacion | 

try {
    final response = api.postularOrganizador(entradaPostulacion);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->postularOrganizador: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **entradaPostulacion** | [**EntradaPostulacion**](EntradaPostulacion.md)|  | 

### Return type

[**SalidaPostulacion**](SalidaPostulacion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **programarTarea**
> SalidaProgramacion programarTarea(entradaProgramacion)

Programar una tarea automatizada

Una tarea por clave de idempotencia (R-ORG-07). La clave es determinista —regla, grupo y minuto—: si dependiera del reloj de la corrida, cada reintento del planificador generaria una tarea nueva.

### Example
```dart
import 'package:aportaya_cliente_organizador/api.dart';

final api = AportayaClienteOrganizador().getDefaultApi();
final EntradaProgramacion entradaProgramacion = ; // EntradaProgramacion | 

try {
    final response = api.programarTarea(entradaProgramacion);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->programarTarea: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **entradaProgramacion** | [**EntradaProgramacion**](EntradaProgramacion.md)|  | 

### Return type

[**SalidaProgramacion**](SalidaProgramacion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **rescindirContrato**
> SalidaRescision rescindirContrato(contratoId, rescindirContratoRequest)

Rescindir el contrato

No se rescinde con grupos en curso: irse dejando participantes a mitad del ciclo es lo que el contrato existe para que no pase sin consecuencias.

### Example
```dart
import 'package:aportaya_cliente_organizador/api.dart';

final api = AportayaClienteOrganizador().getDefaultApi();
final String contratoId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final RescindirContratoRequest rescindirContratoRequest = ; // RescindirContratoRequest | 

try {
    final response = api.rescindirContrato(contratoId, rescindirContratoRequest);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->rescindirContrato: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **contratoId** | **String**|  | 
 **rescindirContratoRequest** | [**RescindirContratoRequest**](RescindirContratoRequest.md)|  | 

### Return type

[**SalidaRescision**](SalidaRescision.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **resolverApelacion**
> SalidaResolucion resolverApelacion(sancionId, entradaResolucion)

Resolver la apelacion

La resuelve quien NO aplico la sancion (R-ORG-05). Sin eso, apelar es pedirle a la misma persona que se desdiga.

### Example
```dart
import 'package:aportaya_cliente_organizador/api.dart';

final api = AportayaClienteOrganizador().getDefaultApi();
final String sancionId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final EntradaResolucion entradaResolucion = ; // EntradaResolucion | 

try {
    final response = api.resolverApelacion(sancionId, entradaResolucion);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->resolverApelacion: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **sancionId** | **String**|  | 
 **entradaResolucion** | [**EntradaResolucion**](EntradaResolucion.md)|  | 

### Return type

[**SalidaResolucion**](SalidaResolucion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **sancionarOrganizador**
> SalidaSancion sancionarOrganizador(organizadorId, entradaSancion)

Aplicar una sancion

Sin motivo escrito no hay sancion: «bajo desempeno» a secas no se puede apelar porque no dice contra que defenderse. El plazo para apelar se calcula al sancionar y no se recalcula despues.

### Example
```dart
import 'package:aportaya_cliente_organizador/api.dart';

final api = AportayaClienteOrganizador().getDefaultApi();
final String organizadorId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final EntradaSancion entradaSancion = ; // EntradaSancion | 

try {
    final response = api.sancionarOrganizador(organizadorId, entradaSancion);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->sancionarOrganizador: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organizadorId** | **String**|  | 
 **entradaSancion** | [**EntradaSancion**](EntradaSancion.md)|  | 

### Return type

[**SalidaSancion**](SalidaSancion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

