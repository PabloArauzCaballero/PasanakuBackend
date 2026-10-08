# aportaya_cliente_publicidad.api.DefaultApi

## Load the API package
```dart
import 'package:aportaya_cliente_publicidad/api.dart';
```

All URIs are relative to */api/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**aprobarCampana**](DefaultApi.md#aprobarcampana) | **POST** /publicidad/campanas/{campanaId}/aprobacion | CU-111 · Operaciones aprueba y la campana empieza a entregar
[**consumoDelPeriodo**](DefaultApi.md#consumodelperiodo) | **GET** /publicidad/cuentas/{cuentaId}/consumo | CU-114 · Lo consumido en un mes, sin emitir nada
[**crearCampana**](DefaultApi.md#crearcampana) | **POST** /publicidad/campanas | CU-111 · Crea la campana con sus conjuntos y la manda a revision
[**darDeAltaAnunciante**](DefaultApi.md#dardealtaanunciante) | **POST** /publicidad/anunciantes | CU-110 · Da de alta el anunciante y abre su cuenta publicitaria
[**entregarAnuncio**](DefaultApi.md#entregaranuncio) | **POST** /publicidad/espacios/{espacioId}/entrega | CU-113 · Elige que anuncio mostrar y registra su impresion
[**liquidarPeriodo**](DefaultApi.md#liquidarperiodo) | **POST** /publicidad/cuentas/{cuentaId}/liquidaciones | CU-114 · Factura el gasto publicitario del mes
[**moderarPiezaCreativa**](DefaultApi.md#moderarpiezacreativa) | **POST** /publicidad/piezas-creativas/{piezaId}/revision | CU-112 · Un moderador aprueba o rechaza la pieza
[**postularSocioComercial**](DefaultApi.md#postularsociocomercial) | **POST** /publicidad/socios-comerciales | CU-110 · Postula un negocio externo como socio comercial
[**programarAnuncio**](DefaultApi.md#programaranuncio) | **POST** /publicidad/anuncios | CU-113 · Programa un anuncio con una pieza aprobada
[**rechazarCampana**](DefaultApi.md#rechazarcampana) | **POST** /publicidad/campanas/{campanaId}/rechazo | CU-111 · Operaciones rechaza la campana, con motivo
[**registrarClic**](DefaultApi.md#registrarclic) | **POST** /publicidad/impresiones/{impresionId}/clic | CU-113 · Registra el clic sobre una impresion
[**registrarConversion**](DefaultApi.md#registrarconversion) | **POST** /publicidad/conversiones | CU-113 · Registra el hecho de negocio al que llevo el anuncio
[**subirPiezaCreativa**](DefaultApi.md#subirpiezacreativa) | **POST** /publicidad/piezas-creativas | CU-112 · El anunciante sube una pieza, que nace PENDIENTE
[**verificarSocioComercial**](DefaultApi.md#verificarsociocomercial) | **POST** /publicidad/socios-comerciales/{socioId}/verificacion | CU-110 · Operaciones verifica al socio y lo deja ACTIVO


# **aprobarCampana**
> SalidaCampana aprobarCampana(campanaId, idempotencyKey)

CU-111 · Operaciones aprueba y la campana empieza a entregar

Exige PUBLICIDAD_APROBAR_CAMPANA.

### Example
```dart
import 'package:aportaya_cliente_publicidad/api.dart';

final api = AportayaClientePublicidad().getDefaultApi();
final String campanaId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).

try {
    final response = api.aprobarCampana(campanaId, idempotencyKey);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->aprobarCampana: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **campanaId** | **String**|  | 
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 

### Return type

[**SalidaCampana**](SalidaCampana.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **consumoDelPeriodo**
> SalidaConsumo consumoDelPeriodo(cuentaId, periodo)

CU-114 · Lo consumido en un mes, sin emitir nada

Exige PUBLICIDAD_ANUNCIANTES. Sin efecto, asi que no exige clave: es lo que mira Contabilidad antes de correr la liquidacion.

### Example
```dart
import 'package:aportaya_cliente_publicidad/api.dart';

final api = AportayaClientePublicidad().getDefaultApi();
final String cuentaId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final String periodo = periodo_example; // String | El mes a mirar, en formato YYYY-MM.

try {
    final response = api.consumoDelPeriodo(cuentaId, periodo);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->consumoDelPeriodo: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **cuentaId** | **String**|  | 
 **periodo** | **String**| El mes a mirar, en formato YYYY-MM. | 

### Return type

[**SalidaConsumo**](SalidaConsumo.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **crearCampana**
> SalidaCampana crearCampana(idempotencyKey, entradaCampana)

CU-111 · Crea la campana con sus conjuntos y la manda a revision

Exige PUBLICIDAD_APROBAR_CAMPANA para el anunciante autenticado.

### Example
```dart
import 'package:aportaya_cliente_publicidad/api.dart';

final api = AportayaClientePublicidad().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaCampana entradaCampana = ; // EntradaCampana | 

try {
    final response = api.crearCampana(idempotencyKey, entradaCampana);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->crearCampana: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaCampana** | [**EntradaCampana**](EntradaCampana.md)|  | 

### Return type

[**SalidaCampana**](SalidaCampana.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **darDeAltaAnunciante**
> SalidaAnunciante darDeAltaAnunciante(idempotencyKey, entradaAnunciante)

CU-110 · Da de alta el anunciante y abre su cuenta publicitaria

Exige PUBLICIDAD_ANUNCIANTES. El anunciante es un organizador o un socio comercial, nunca los dos (R-PUB-01).

### Example
```dart
import 'package:aportaya_cliente_publicidad/api.dart';

final api = AportayaClientePublicidad().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaAnunciante entradaAnunciante = ; // EntradaAnunciante | 

try {
    final response = api.darDeAltaAnunciante(idempotencyKey, entradaAnunciante);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->darDeAltaAnunciante: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaAnunciante** | [**EntradaAnunciante**](EntradaAnunciante.md)|  | 

### Return type

[**SalidaAnunciante**](SalidaAnunciante.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **entregarAnuncio**
> SalidaEntrega entregarAnuncio(espacioId, idempotencyKey, entradaEntrega)

CU-113 · Elige que anuncio mostrar y registra su impresion

Interno del motor de entrega. Si ningun anuncio tiene presupuesto para hoy, responde 200 con `anuncioId` nulo y el motivo: que el espacio no muestre nada es el comportamiento correcto, no un error.

### Example
```dart
import 'package:aportaya_cliente_publicidad/api.dart';

final api = AportayaClientePublicidad().getDefaultApi();
final String espacioId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaEntrega entradaEntrega = ; // EntradaEntrega | 

try {
    final response = api.entregarAnuncio(espacioId, idempotencyKey, entradaEntrega);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->entregarAnuncio: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **espacioId** | **String**|  | 
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaEntrega** | [**EntradaEntrega**](EntradaEntrega.md)|  | [optional] 

### Return type

[**SalidaEntrega**](SalidaEntrega.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **liquidarPeriodo**
> SalidaLiquidacion liquidarPeriodo(cuentaId, idempotencyKey, entradaLiquidacion)

CU-114 · Factura el gasto publicitario del mes

Interno (worker mensual). El comprobante fiscal y la cuenta por cobrar viven en otros esquemas: llegan como dato, ya emitidos. La factura es append-only y nace con ellos o no los tiene nunca.

### Example
```dart
import 'package:aportaya_cliente_publicidad/api.dart';

final api = AportayaClientePublicidad().getDefaultApi();
final String cuentaId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaLiquidacion entradaLiquidacion = ; // EntradaLiquidacion | 

try {
    final response = api.liquidarPeriodo(cuentaId, idempotencyKey, entradaLiquidacion);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->liquidarPeriodo: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **cuentaId** | **String**|  | 
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaLiquidacion** | [**EntradaLiquidacion**](EntradaLiquidacion.md)|  | 

### Return type

[**SalidaLiquidacion**](SalidaLiquidacion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **moderarPiezaCreativa**
> SalidaRevision moderarPiezaCreativa(piezaId, idempotencyKey, entradaRevision)

CU-112 · Un moderador aprueba o rechaza la pieza

Exige PUBLICIDAD_MODERAR. Quien subio la pieza no la modera (R-PUB-05), y un rechazo exige motivo.

### Example
```dart
import 'package:aportaya_cliente_publicidad/api.dart';

final api = AportayaClientePublicidad().getDefaultApi();
final String piezaId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaRevision entradaRevision = ; // EntradaRevision | 

try {
    final response = api.moderarPiezaCreativa(piezaId, idempotencyKey, entradaRevision);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->moderarPiezaCreativa: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **piezaId** | **String**|  | 
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaRevision** | [**EntradaRevision**](EntradaRevision.md)|  | 

### Return type

[**SalidaRevision**](SalidaRevision.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **postularSocioComercial**
> SalidaSocio postularSocioComercial(idempotencyKey, entradaSocio)

CU-110 · Postula un negocio externo como socio comercial

Exige PUBLICIDAD_ANUNCIANTES. Reintentar con el mismo documento devuelve el socio que ya hay.

### Example
```dart
import 'package:aportaya_cliente_publicidad/api.dart';

final api = AportayaClientePublicidad().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaSocio entradaSocio = ; // EntradaSocio | 

try {
    final response = api.postularSocioComercial(idempotencyKey, entradaSocio);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->postularSocioComercial: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaSocio** | [**EntradaSocio**](EntradaSocio.md)|  | 

### Return type

[**SalidaSocio**](SalidaSocio.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **programarAnuncio**
> SalidaAnuncio programarAnuncio(idempotencyKey, entradaAnuncio)

CU-113 · Programa un anuncio con una pieza aprobada

Exige PUBLICIDAD_ANUNCIANTES. Una pieza que no esta APROBADA no entra (R-PUB-04).

### Example
```dart
import 'package:aportaya_cliente_publicidad/api.dart';

final api = AportayaClientePublicidad().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaAnuncio entradaAnuncio = ; // EntradaAnuncio | 

try {
    final response = api.programarAnuncio(idempotencyKey, entradaAnuncio);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->programarAnuncio: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaAnuncio** | [**EntradaAnuncio**](EntradaAnuncio.md)|  | 

### Return type

[**SalidaAnuncio**](SalidaAnuncio.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **rechazarCampana**
> SalidaCampana rechazarCampana(campanaId, idempotencyKey, entradaRechazo)

CU-111 · Operaciones rechaza la campana, con motivo

Exige PUBLICIDAD_APROBAR_CAMPANA. El motivo viaja en el evento: la tabla no tiene columna donde guardarlo.

### Example
```dart
import 'package:aportaya_cliente_publicidad/api.dart';

final api = AportayaClientePublicidad().getDefaultApi();
final String campanaId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaRechazo entradaRechazo = ; // EntradaRechazo | 

try {
    final response = api.rechazarCampana(campanaId, idempotencyKey, entradaRechazo);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->rechazarCampana: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **campanaId** | **String**|  | 
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaRechazo** | [**EntradaRechazo**](EntradaRechazo.md)|  | 

### Return type

[**SalidaCampana**](SalidaCampana.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **registrarClic**
> SalidaClic registrarClic(impresionId, idempotencyKey)

CU-113 · Registra el clic sobre una impresion

Interno del motor de entrega. En CPC, es el clic el que cuesta.

### Example
```dart
import 'package:aportaya_cliente_publicidad/api.dart';

final api = AportayaClientePublicidad().getDefaultApi();
final String impresionId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).

try {
    final response = api.registrarClic(impresionId, idempotencyKey);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->registrarClic: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **impresionId** | **String**|  | 
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 

### Return type

[**SalidaClic**](SalidaClic.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **registrarConversion**
> SalidaConversion registrarConversion(idempotencyKey, entradaConversion)

CU-113 · Registra el hecho de negocio al que llevo el anuncio

Interno. Una postulacion a un grupo, un registro o una descarga.

### Example
```dart
import 'package:aportaya_cliente_publicidad/api.dart';

final api = AportayaClientePublicidad().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaConversion entradaConversion = ; // EntradaConversion | 

try {
    final response = api.registrarConversion(idempotencyKey, entradaConversion);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->registrarConversion: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaConversion** | [**EntradaConversion**](EntradaConversion.md)|  | 

### Return type

[**SalidaConversion**](SalidaConversion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **subirPiezaCreativa**
> SalidaPieza subirPiezaCreativa(idempotencyKey, entradaPieza)

CU-112 · El anunciante sube una pieza, que nace PENDIENTE

Exige PUBLICIDAD_ANUNCIANTES para el anunciante autenticado.

### Example
```dart
import 'package:aportaya_cliente_publicidad/api.dart';

final api = AportayaClientePublicidad().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaPieza entradaPieza = ; // EntradaPieza | 

try {
    final response = api.subirPiezaCreativa(idempotencyKey, entradaPieza);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->subirPiezaCreativa: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaPieza** | [**EntradaPieza**](EntradaPieza.md)|  | 

### Return type

[**SalidaPieza**](SalidaPieza.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **verificarSocioComercial**
> SalidaSocio verificarSocioComercial(socioId, idempotencyKey)

CU-110 · Operaciones verifica al socio y lo deja ACTIVO

Exige PUBLICIDAD_ANUNCIANTES.

### Example
```dart
import 'package:aportaya_cliente_publicidad/api.dart';

final api = AportayaClientePublicidad().getDefaultApi();
final String socioId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).

try {
    final response = api.verificarSocioComercial(socioId, idempotencyKey);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->verificarSocioComercial: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **socioId** | **String**|  | 
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 

### Return type

[**SalidaSocio**](SalidaSocio.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

