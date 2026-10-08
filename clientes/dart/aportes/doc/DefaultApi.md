# aportaya_cliente_aportes.api.DefaultApi

## Load the API package
```dart
import 'package:aportaya_cliente_aportes/api.dart';
```

All URIs are relative to */api/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**aprobarReembolso**](DefaultApi.md#aprobarreembolso) | **POST** /pagos/reembolsos/{reembolsoId}/aprobacion | Aprobar y ejecutar un reembolso solicitado
[**cobrarAporte**](DefaultApi.md#cobraraporte) | **POST** /aportes/obligaciones/{obligacionId}/pagos | Acreditar el pago de una obligacion de aporte
[**consultarEstadoDelParticipante**](DefaultApi.md#consultarestadodelparticipante) | **GET** /aportes/participantes/{participanteId}/estado | Si esta al dia, cuanto aporto y cuanto debe
[**contarMorosos**](DefaultApi.md#contarmorosos) | **GET** /aportes/grupos/{grupoId}/morosos | Cuantos participantes del grupo estan en mora
[**darDeAltaProveedor**](DefaultApi.md#dardealtaproveedor) | **POST** /pagos/proveedores | Dar de alta un proveedor de pago
[**enrutarCobro**](DefaultApi.md#enrutarcobro) | **POST** /pagos/enrutamiento | Elegir por que proveedor cobrar una orden
[**registrarDisputa**](DefaultApi.md#registrardisputa) | **POST** /pagos/{pagoId}/disputas | Registrar una disputa recibida del proveedor
[**solicitarReembolso**](DefaultApi.md#solicitarreembolso) | **POST** /pagos/{pagoId}/reembolsos | Solicitar el reembolso de un pago


# **aprobarReembolso**
> SalidaEjecucionReembolso aprobarReembolso(idempotencyKey, reembolsoId)

Aprobar y ejecutar un reembolso solicitado

Quien aprueba no puede ser quien solicito. Al ejecutar, la obligacion vuelve a estar pendiente por ese importe: si quedara dada por pagada, el faltante lo absorben los demas del grupo sin enterarse.

### Example
```dart
import 'package:aportaya_cliente_aportes/api.dart';

final api = AportayaClienteAportes().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final String reembolsoId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 

try {
    final response = api.aprobarReembolso(idempotencyKey, reembolsoId);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->aprobarReembolso: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **reembolsoId** | **String**|  | 

### Return type

[**SalidaEjecucionReembolso**](SalidaEjecucionReembolso.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **cobrarAporte**
> SalidaCobro cobrarAporte(idempotencyKey, obligacionId, entradaCobro)

Acreditar el pago de una obligacion de aporte

El dinero lo mueve nucleo-financiero (invariante 12): esta operacion aplica el pago a la obligacion y pide el movimiento por evento. La clave de idempotencia se valida ANTES de escribir.

### Example
```dart
import 'package:aportaya_cliente_aportes/api.dart';

final api = AportayaClienteAportes().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final String obligacionId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final EntradaCobro entradaCobro = ; // EntradaCobro | 

try {
    final response = api.cobrarAporte(idempotencyKey, obligacionId, entradaCobro);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->cobrarAporte: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **obligacionId** | **String**|  | 
 **entradaCobro** | [**EntradaCobro**](EntradaCobro.md)|  | 

### Return type

[**SalidaCobro**](SalidaCobro.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **consultarEstadoDelParticipante**
> EstadoDelParticipante consultarEstadoDelParticipante(participanteId)

Si esta al dia, cuanto aporto y cuanto debe

La consulta que hace posible que otros servicios decidan sin leer este esquema (invariante 11). `grupos` la usa antes de permitir una permuta o un traspaso; `nucleo-financiero`, antes de cerrar una billetera. Sin ella, el dato lo tendria que afirmar quien pide — y el que pide seria juez de si esta al dia. Quien no tiene ninguna obligacion esta al dia: no deber nada porque todavia no se le pidio nada no es estar en mora.

### Example
```dart
import 'package:aportaya_cliente_aportes/api.dart';

final api = AportayaClienteAportes().getDefaultApi();
final String participanteId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 

try {
    final response = api.consultarEstadoDelParticipante(participanteId);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->consultarEstadoDelParticipante: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **participanteId** | **String**|  | 

### Return type

[**EstadoDelParticipante**](EstadoDelParticipante.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **contarMorosos**
> Morosos contarMorosos(grupoId)

Cuantos participantes del grupo estan en mora

La usa CU-68 para medir la concentracion de riesgo antes de aceptar a alguien nuevo: un grupo con la mitad en mora no admite mas gente, la ayuda.

### Example
```dart
import 'package:aportaya_cliente_aportes/api.dart';

final api = AportayaClienteAportes().getDefaultApi();
final String grupoId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 

try {
    final response = api.contarMorosos(grupoId);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->contarMorosos: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **grupoId** | **String**|  | 

### Return type

[**Morosos**](Morosos.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **darDeAltaProveedor**
> SalidaProveedor darDeAltaProveedor(idempotencyKey, entradaProveedor)

Dar de alta un proveedor de pago

La columna referencia_credenciales guarda una REFERENCIA al almacen de secretos, nunca el secreto. Se comprueba antes de tocar la base: una clave que llega a la fila ya se filtro aunque la transaccion se revierta.

### Example
```dart
import 'package:aportaya_cliente_aportes/api.dart';

final api = AportayaClienteAportes().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaProveedor entradaProveedor = ; // EntradaProveedor | 

try {
    final response = api.darDeAltaProveedor(idempotencyKey, entradaProveedor);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->darDeAltaProveedor: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaProveedor** | [**EntradaProveedor**](EntradaProveedor.md)|  | 

### Return type

[**SalidaProveedor**](SalidaProveedor.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **enrutarCobro**
> SalidaEnrutamiento enrutarCobro(entradaEnrutamiento)

Elegir por que proveedor cobrar una orden

No escribe. Al conmutar de proveedor se conserva la MISMA clave de idempotencia: si el primero cobro y no alcanzo a responder, cambiarla es exactamente como se cobra dos veces.

### Example
```dart
import 'package:aportaya_cliente_aportes/api.dart';

final api = AportayaClienteAportes().getDefaultApi();
final EntradaEnrutamiento entradaEnrutamiento = ; // EntradaEnrutamiento | 

try {
    final response = api.enrutarCobro(entradaEnrutamiento);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->enrutarCobro: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **entradaEnrutamiento** | [**EntradaEnrutamiento**](EntradaEnrutamiento.md)|  | 

### Return type

[**SalidaEnrutamiento**](SalidaEnrutamiento.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **registrarDisputa**
> SalidaDisputa registrarDisputa(pagoId, entradaDisputa)

Registrar una disputa recibida del proveedor

El plazo de respuesta se calcula al ingresar y queda GUARDADO (R-CON-01, invariante 8): recalcularlo al consultar es un argumento que el proveedor no acepta.

### Example
```dart
import 'package:aportaya_cliente_aportes/api.dart';

final api = AportayaClienteAportes().getDefaultApi();
final String pagoId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final EntradaDisputa entradaDisputa = ; // EntradaDisputa | 

try {
    final response = api.registrarDisputa(pagoId, entradaDisputa);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->registrarDisputa: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **pagoId** | **String**|  | 
 **entradaDisputa** | [**EntradaDisputa**](EntradaDisputa.md)|  | 

### Return type

[**SalidaDisputa**](SalidaDisputa.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **solicitarReembolso**
> SalidaReembolso solicitarReembolso(idempotencyKey, pagoId, entradaReembolso)

Solicitar el reembolso de un pago

Solo SOLICITA. Ejecutar exige una segunda persona (R-SEG-04): un reembolso es la via mas corta para sacar dinero con una excusa verosimil.

### Example
```dart
import 'package:aportaya_cliente_aportes/api.dart';

final api = AportayaClienteAportes().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final String pagoId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final EntradaReembolso entradaReembolso = ; // EntradaReembolso | 

try {
    final response = api.solicitarReembolso(idempotencyKey, pagoId, entradaReembolso);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->solicitarReembolso: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **pagoId** | **String**|  | 
 **entradaReembolso** | [**EntradaReembolso**](EntradaReembolso.md)|  | 

### Return type

[**SalidaReembolso**](SalidaReembolso.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

