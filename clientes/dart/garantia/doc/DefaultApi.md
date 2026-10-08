# aportaya_cliente_garantia.api.DefaultApi

## Load the API package
```dart
import 'package:aportaya_cliente_garantia/api.dart';
```

All URIs are relative to */api/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**aprobarReemplazo**](DefaultApi.md#aprobarreemplazo) | **POST** /garantia/reemplazos/{reemplazoId}/aprobacion | Aprobar el reemplazo — lo decide el grupo
[**cerrarDisolucion**](DefaultApi.md#cerrardisolucion) | **POST** /garantia/disoluciones/{disolucionId}/cierre | Cerrar la disolucion
[**consultarRestriccion**](DefaultApi.md#consultarrestriccion) | **GET** /cobranza/restricciones/vigentes/{usuarioId} | Si alguien esta en la lista de restriccion interna
[**cubrirIncumplimiento**](DefaultApi.md#cubririncumplimiento) | **POST** /incumplimientos/{expedienteId}/cobertura | Cubrir el incumplimiento con el fondo
[**declararIncumplimiento**](DefaultApi.md#declararincumplimiento) | **POST** /incumplimientos | Declarar un incumplimiento con su evidencia
[**devolverFondo**](DefaultApi.md#devolverfondo) | **POST** /garantia/fondos/{grupoId}/devolucion | Devolver los aportes del fondo al cerrar el grupo
[**ejecutarAval**](DefaultApi.md#ejecutaraval) | **POST** /incumplimientos/{expedienteId}/aval/ejecucion | Ejecutar el aval y subrogar la deuda
[**ejecutarReemplazo**](DefaultApi.md#ejecutarreemplazo) | **POST** /garantia/reemplazos/{reemplazoId}/ejecucion | Ejecutar el reemplazo aprobado
[**iniciarDisolucion**](DefaultApi.md#iniciardisolucion) | **POST** /garantia/grupos/{grupoId}/disolucion | Calcular la disolucion anticipada del grupo
[**levantarRestriccion**](DefaultApi.md#levantarrestriccion) | **POST** /cobranza/restricciones/{restriccionId}/levantamiento | Levantar la restriccion, con motivo escrito
[**presentarDescargo**](DefaultApi.md#presentardescargo) | **POST** /incumplimientos/{expedienteId}/descargo | Presentar el descargo, dentro del plazo
[**proponerReemplazo**](DefaultApi.md#proponerreemplazo) | **POST** /incumplimientos/{expedienteId}/reemplazo | Proponer el reemplazo de un participante moroso
[**resolverDescargo**](DefaultApi.md#resolverdescargo) | **POST** /incumplimientos/{expedienteId}/descargo/resolucion | Resolver el descargo
[**restringirDeudor**](DefaultApi.md#restringirdeudor) | **POST** /incumplimientos/{expedienteId}/restriccion | Restringir al deudor


# **aprobarReemplazo**
> AprobarReemplazo200Response aprobarReemplazo(reemplazoId)

Aprobar el reemplazo — lo decide el grupo

### Example
```dart
import 'package:aportaya_cliente_garantia/api.dart';

final api = AportayaClienteGarantia().getDefaultApi();
final String reemplazoId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 

try {
    final response = api.aprobarReemplazo(reemplazoId);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->aprobarReemplazo: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **reemplazoId** | **String**|  | 

### Return type

[**AprobarReemplazo200Response**](AprobarReemplazo200Response.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **cerrarDisolucion**
> SalidaCierreDisolucion cerrarDisolucion(disolucionId)

Cerrar la disolucion

La cuenta del grupo cierra EN CERO (R-GRP-13). Un grupo disuelto con saldo es plata de alguien que quedo sin dueno.

### Example
```dart
import 'package:aportaya_cliente_garantia/api.dart';

final api = AportayaClienteGarantia().getDefaultApi();
final String disolucionId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 

try {
    final response = api.cerrarDisolucion(disolucionId);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->cerrarDisolucion: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **disolucionId** | **String**|  | 

### Return type

[**SalidaCierreDisolucion**](SalidaCierreDisolucion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **consultarRestriccion**
> RestriccionVigente consultarRestriccion(usuarioId)

Si alguien esta en la lista de restriccion interna

La pregunta CU-68 de `grupos` antes de aceptar a alguien. La lista es de este servicio y nadie mas la puede leer (invariante 11). Devuelve tambien cuanto tendria que pagar para salir: una restriccion sin salida es una condena, y quien consulta necesita poder decirle a la persona que hacer, no solo que no puede entrar.

### Example
```dart
import 'package:aportaya_cliente_garantia/api.dart';

final api = AportayaClienteGarantia().getDefaultApi();
final String usuarioId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 

try {
    final response = api.consultarRestriccion(usuarioId);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->consultarRestriccion: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **usuarioId** | **String**|  | 

### Return type

[**RestriccionVigente**](RestriccionVigente.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **cubrirIncumplimiento**
> SalidaCobertura cubrirIncumplimiento(expedienteId, entradaCobertura)

Cubrir el incumplimiento con el fondo

El fondo existe para que el incumplimiento de uno no se lleve puesto al grupo entero. Pero cubrir todo siempre lo VACIA: gana el mas chico de cuatro limites —porcentaje, tope por participante, tope del periodo y saldo—. Y cubrir NO perdona: deja una deuda contra quien incumplio.

### Example
```dart
import 'package:aportaya_cliente_garantia/api.dart';

final api = AportayaClienteGarantia().getDefaultApi();
final String expedienteId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final EntradaCobertura entradaCobertura = ; // EntradaCobertura | 

try {
    final response = api.cubrirIncumplimiento(expedienteId, entradaCobertura);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->cubrirIncumplimiento: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **expedienteId** | **String**|  | 
 **entradaCobertura** | [**EntradaCobertura**](EntradaCobertura.md)|  | 

### Return type

[**SalidaCobertura**](SalidaCobertura.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **declararIncumplimiento**
> SalidaDeclaracion declararIncumplimiento(idempotencyKey, entradaDeclaracion)

Declarar un incumplimiento con su evidencia

Declarar que alguien incumplio tiene consecuencias reales: le bloquea grupos, lo pone en una lista, le genera una deuda. El expediente nace NOTIFICADO y con su plazo de descargo GUARDADO (R-GAR-01) — enterarse despues de que el plazo empezo a correr es no tener plazo. La evidencia es inmutable (R-GAR-02).

### Example
```dart
import 'package:aportaya_cliente_garantia/api.dart';

final api = AportayaClienteGarantia().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaDeclaracion entradaDeclaracion = ; // EntradaDeclaracion | 

try {
    final response = api.declararIncumplimiento(idempotencyKey, entradaDeclaracion);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->declararIncumplimiento: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaDeclaracion** | [**EntradaDeclaracion**](EntradaDeclaracion.md)|  | 

### Return type

[**SalidaDeclaracion**](SalidaDeclaracion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **devolverFondo**
> SalidaDevolucionFondo devolverFondo(grupoId, entradaDevolucionFondo)

Devolver los aportes del fondo al cerrar el grupo

No se devuelve mas de lo aportado ni un importe negativo (R-GAR-06), y lo que el fondo gasto en cubrir lo pierden TODOS en proporcion: esa es la idea de un fondo mutual. El remanente se ajusta en la ultima devolucion para que la suma cierre exacta contra el saldo.

### Example
```dart
import 'package:aportaya_cliente_garantia/api.dart';

final api = AportayaClienteGarantia().getDefaultApi();
final String grupoId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final EntradaDevolucionFondo entradaDevolucionFondo = ; // EntradaDevolucionFondo | 

try {
    final response = api.devolverFondo(grupoId, entradaDevolucionFondo);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->devolverFondo: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **grupoId** | **String**|  | 
 **entradaDevolucionFondo** | [**EntradaDevolucionFondo**](EntradaDevolucionFondo.md)|  | 

### Return type

[**SalidaDevolucionFondo**](SalidaDevolucionFondo.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **ejecutarAval**
> SalidaEjecucionAval ejecutarAval(expedienteId)

Ejecutar el aval y subrogar la deuda

Un avalista acepto responder por una cantidad CONCRETA. Cobrarle mas es cobrarle algo que nunca acepto (R-GAR-04), y el tope se mide contra lo YA ejecutado. Quien paga se subroga: la deuda no desaparece, cambia de acreedor.

### Example
```dart
import 'package:aportaya_cliente_garantia/api.dart';

final api = AportayaClienteGarantia().getDefaultApi();
final String expedienteId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 

try {
    final response = api.ejecutarAval(expedienteId);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->ejecutarAval: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **expedienteId** | **String**|  | 

### Return type

[**SalidaEjecucionAval**](SalidaEjecucionAval.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **ejecutarReemplazo**
> SalidaReemplazo ejecutarReemplazo(reemplazoId)

Ejecutar el reemplazo aprobado

### Example
```dart
import 'package:aportaya_cliente_garantia/api.dart';

final api = AportayaClienteGarantia().getDefaultApi();
final String reemplazoId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 

try {
    final response = api.ejecutarReemplazo(reemplazoId);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->ejecutarReemplazo: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **reemplazoId** | **String**|  | 

### Return type

[**SalidaReemplazo**](SalidaReemplazo.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **iniciarDisolucion**
> SalidaDisolucion iniciarDisolucion(grupoId, entradaDisolucion)

Calcular la disolucion anticipada del grupo

Es el peor momento posible: unos ya cobraron y otros no, y todos pusieron. La posicion de cada uno es lo aportado menos lo recibido. Repartir sin mirar eso le devolveria a quien ya cobro lo mismo que a quien nunca cobro.

### Example
```dart
import 'package:aportaya_cliente_garantia/api.dart';

final api = AportayaClienteGarantia().getDefaultApi();
final String grupoId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final EntradaDisolucion entradaDisolucion = ; // EntradaDisolucion | 

try {
    final response = api.iniciarDisolucion(grupoId, entradaDisolucion);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->iniciarDisolucion: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **grupoId** | **String**|  | 
 **entradaDisolucion** | [**EntradaDisolucion**](EntradaDisolucion.md)|  | 

### Return type

[**SalidaDisolucion**](SalidaDisolucion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **levantarRestriccion**
> LevantarRestriccion200Response levantarRestriccion(restriccionId, levantarRestriccionRequest)

Levantar la restriccion, con motivo escrito

El levantamiento SE MOTIVA (R-GAR-05): sin motivo escrito se convierte en un favor que nadie puede auditar.

### Example
```dart
import 'package:aportaya_cliente_garantia/api.dart';

final api = AportayaClienteGarantia().getDefaultApi();
final String restriccionId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final LevantarRestriccionRequest levantarRestriccionRequest = ; // LevantarRestriccionRequest | 

try {
    final response = api.levantarRestriccion(restriccionId, levantarRestriccionRequest);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->levantarRestriccion: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **restriccionId** | **String**|  | 
 **levantarRestriccionRequest** | [**LevantarRestriccionRequest**](LevantarRestriccionRequest.md)|  | 

### Return type

[**LevantarRestriccion200Response**](LevantarRestriccion200Response.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **presentarDescargo**
> SalidaDescargo presentarDescargo(expedienteId, entradaDescargo)

Presentar el descargo, dentro del plazo

### Example
```dart
import 'package:aportaya_cliente_garantia/api.dart';

final api = AportayaClienteGarantia().getDefaultApi();
final String expedienteId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final EntradaDescargo entradaDescargo = ; // EntradaDescargo | 

try {
    final response = api.presentarDescargo(expedienteId, entradaDescargo);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->presentarDescargo: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **expedienteId** | **String**|  | 
 **entradaDescargo** | [**EntradaDescargo**](EntradaDescargo.md)|  | 

### Return type

[**SalidaDescargo**](SalidaDescargo.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **proponerReemplazo**
> SalidaReemplazo proponerReemplazo(expedienteId, entradaReemplazo)

Proponer el reemplazo de un participante moroso

La deuda del saliente NO se le traslada al entrante salvo que este la asuma expresamente: nadie entra a un grupo a pagar lo que otro dejo sin decirselo. Lo que no asume queda como deuda del saliente — perdonarla se la cobraria a los demas.

### Example
```dart
import 'package:aportaya_cliente_garantia/api.dart';

final api = AportayaClienteGarantia().getDefaultApi();
final String expedienteId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final EntradaReemplazo entradaReemplazo = ; // EntradaReemplazo | 

try {
    final response = api.proponerReemplazo(expedienteId, entradaReemplazo);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->proponerReemplazo: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **expedienteId** | **String**|  | 
 **entradaReemplazo** | [**EntradaReemplazo**](EntradaReemplazo.md)|  | 

### Return type

[**SalidaReemplazo**](SalidaReemplazo.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **resolverDescargo**
> SalidaResolucionDescargo resolverDescargo(expedienteId, entradaResolucionDescargo)

Resolver el descargo

Quien resuelve no puede ser el imputado. Aceptarlo cierra el expediente como SUBSANADO y NO habilita la cobertura del fondo.

### Example
```dart
import 'package:aportaya_cliente_garantia/api.dart';

final api = AportayaClienteGarantia().getDefaultApi();
final String expedienteId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final EntradaResolucionDescargo entradaResolucionDescargo = ; // EntradaResolucionDescargo | 

try {
    final response = api.resolverDescargo(expedienteId, entradaResolucionDescargo);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->resolverDescargo: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **expedienteId** | **String**|  | 
 **entradaResolucionDescargo** | [**EntradaResolucionDescargo**](EntradaResolucionDescargo.md)|  | 

### Return type

[**SalidaResolucionDescargo**](SalidaResolucionDescargo.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **restringirDeudor**
> SalidaRestriccion restringirDeudor(expedienteId, entradaRestriccion)

Restringir al deudor

Una restriccion vigente por usuario (R-GAR-05). **No le cierra la puerta a pagar ni a ver su estado**: bloquear al deudor de todo, incluido el camino para regularizarse, garantiza que no se regularice.

### Example
```dart
import 'package:aportaya_cliente_garantia/api.dart';

final api = AportayaClienteGarantia().getDefaultApi();
final String expedienteId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final EntradaRestriccion entradaRestriccion = ; // EntradaRestriccion | 

try {
    final response = api.restringirDeudor(expedienteId, entradaRestriccion);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->restringirDeudor: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **expedienteId** | **String**|  | 
 **entradaRestriccion** | [**EntradaRestriccion**](EntradaRestriccion.md)|  | 

### Return type

[**SalidaRestriccion**](SalidaRestriccion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

