# aportaya_cliente_erp.api.DefaultApi

## Load the API package
```dart
import 'package:aportaya_cliente_erp/api.dart';
```

All URIs are relative to */api/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**abrirCuentaPorCobrar**](DefaultApi.md#abrircuentaporcobrar) | **POST** /erp/cuentas-por-cobrar | CU-104 · Abre la cuenta por cobrar de un tercero
[**abrirEjercicioFiscal**](DefaultApi.md#abrirejerciciofiscal) | **POST** /erp/ejercicios | CU-100 · Abre el ejercicio y sus doce periodos
[**aprobarOrdenDeCompra**](DefaultApi.md#aprobarordendecompra) | **POST** /erp/ordenes-de-compra/{ordenId}/aprobacion | CU-102 · Aprueba la orden y habilita facturarla
[**aprobarPresupuesto**](DefaultApi.md#aprobarpresupuesto) | **POST** /erp/presupuestos/{presupuestoId}/aprobacion | CU-101 · Aprueba el presupuesto y lo deja vigente
[**cerrarPeriodoContable**](DefaultApi.md#cerrarperiodocontable) | **POST** /erp/periodos/{periodoId}/cierre | CU-100 · Cierra el mes contra su cuadre
[**correrDepreciacionDelPeriodo**](DefaultApi.md#correrdepreciaciondelperiodo) | **POST** /erp/periodos/{periodoId}/depreciaciones | CU-105 · Corre la depreciacion de todos los activos del mes
[**crearOrdenDeCompra**](DefaultApi.md#crearordendecompra) | **POST** /erp/ordenes-de-compra | CU-102 · Crea la orden de compra en BORRADOR
[**crearPresupuesto**](DefaultApi.md#crearpresupuesto) | **POST** /erp/presupuestos | CU-101 · Presupuesta un centro de costo para el ejercicio
[**darDeAltaTercero**](DefaultApi.md#dardealtatercero) | **POST** /erp/terceros | CU-102 · Da de alta un tercero comercial
[**depreciarActivo**](DefaultApi.md#depreciaractivo) | **POST** /erp/activos/{activoId}/depreciaciones | CU-105 · Calcula la cuota del activo en el periodo
[**generarEstadoFinanciero**](DefaultApi.md#generarestadofinanciero) | **POST** /erp/periodos/{periodoId}/estados-financieros | CU-106 · Genera el balance o el estado de resultados del mes
[**pagarFacturaDeProveedor**](DefaultApi.md#pagarfacturadeproveedor) | **POST** /erp/facturas-de-proveedor/{facturaId}/pagos | CU-103 · Paga, total o parcialmente, una factura aprobada
[**registrarCobro**](DefaultApi.md#registrarcobro) | **POST** /erp/cuentas-por-cobrar/{cuentaId}/cobros | CU-104 · Registra un cobro contra la cuenta
[**registrarFacturaDeProveedor**](DefaultApi.md#registrarfacturadeproveedor) | **POST** /erp/facturas-de-proveedor | CU-103 · Registra la factura del proveedor
[**validarPlantillaDeAsiento**](DefaultApi.md#validarplantilladeasiento) | **GET** /erp/plantillas-de-asiento/{plantillaId}/validacion | CU-106 · Comprueba que la plantilla no apunte a sumarizadoras


# **abrirCuentaPorCobrar**
> SalidaCuentaPorCobrar abrirCuentaPorCobrar(idempotencyKey, entradaCuentaPorCobrar)

CU-104 · Abre la cuenta por cobrar de un tercero

Exige CONTABILIDAD_ERP_COBRAR.

### Example
```dart
import 'package:aportaya_cliente_erp/api.dart';

final api = AportayaClienteErp().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaCuentaPorCobrar entradaCuentaPorCobrar = ; // EntradaCuentaPorCobrar | 

try {
    final response = api.abrirCuentaPorCobrar(idempotencyKey, entradaCuentaPorCobrar);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->abrirCuentaPorCobrar: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaCuentaPorCobrar** | [**EntradaCuentaPorCobrar**](EntradaCuentaPorCobrar.md)|  | 

### Return type

[**SalidaCuentaPorCobrar**](SalidaCuentaPorCobrar.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **abrirEjercicioFiscal**
> SalidaEjercicio abrirEjercicioFiscal(idempotencyKey, entradaEjercicio)

CU-100 · Abre el ejercicio y sus doce periodos

Exige CONTABILIDAD_ERP_CERRAR.

### Example
```dart
import 'package:aportaya_cliente_erp/api.dart';

final api = AportayaClienteErp().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaEjercicio entradaEjercicio = ; // EntradaEjercicio | 

try {
    final response = api.abrirEjercicioFiscal(idempotencyKey, entradaEjercicio);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->abrirEjercicioFiscal: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaEjercicio** | [**EntradaEjercicio**](EntradaEjercicio.md)|  | 

### Return type

[**SalidaEjercicio**](SalidaEjercicio.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **aprobarOrdenDeCompra**
> SalidaOrden aprobarOrdenDeCompra(ordenId, idempotencyKey)

CU-102 · Aprueba la orden y habilita facturarla

Exige CONTABILIDAD_ERP_COMPRAS.

### Example
```dart
import 'package:aportaya_cliente_erp/api.dart';

final api = AportayaClienteErp().getDefaultApi();
final String ordenId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).

try {
    final response = api.aprobarOrdenDeCompra(ordenId, idempotencyKey);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->aprobarOrdenDeCompra: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **ordenId** | **String**|  | 
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 

### Return type

[**SalidaOrden**](SalidaOrden.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **aprobarPresupuesto**
> SalidaPresupuesto aprobarPresupuesto(presupuestoId, idempotencyKey)

CU-101 · Aprueba el presupuesto y lo deja vigente

Exige CONTABILIDAD_ERP_PRESUPUESTO.

### Example
```dart
import 'package:aportaya_cliente_erp/api.dart';

final api = AportayaClienteErp().getDefaultApi();
final String presupuestoId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).

try {
    final response = api.aprobarPresupuesto(presupuestoId, idempotencyKey);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->aprobarPresupuesto: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **presupuestoId** | **String**|  | 
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 

### Return type

[**SalidaPresupuesto**](SalidaPresupuesto.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **cerrarPeriodoContable**
> SalidaCierre cerrarPeriodoContable(periodoId, idempotencyKey, entradaCierre)

CU-100 · Cierra el mes contra su cuadre

Exige CONTABILIDAD_ERP_CERRAR. El cierre es irreversible.

### Example
```dart
import 'package:aportaya_cliente_erp/api.dart';

final api = AportayaClienteErp().getDefaultApi();
final String periodoId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaCierre entradaCierre = ; // EntradaCierre | 

try {
    final response = api.cerrarPeriodoContable(periodoId, idempotencyKey, entradaCierre);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->cerrarPeriodoContable: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **periodoId** | **String**|  | 
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaCierre** | [**EntradaCierre**](EntradaCierre.md)|  | 

### Return type

[**SalidaCierre**](SalidaCierre.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **correrDepreciacionDelPeriodo**
> SalidaCorrida correrDepreciacionDelPeriodo(periodoId, idempotencyKey)

CU-105 · Corre la depreciacion de todos los activos del mes

Exige CONTABILIDAD_ERP_ACTIVOS_FIJOS. Es idempotente: los activos ya corridos se informan en `yaCorridos` y no se vuelven a depreciar.

### Example
```dart
import 'package:aportaya_cliente_erp/api.dart';

final api = AportayaClienteErp().getDefaultApi();
final String periodoId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).

try {
    final response = api.correrDepreciacionDelPeriodo(periodoId, idempotencyKey);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->correrDepreciacionDelPeriodo: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **periodoId** | **String**|  | 
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 

### Return type

[**SalidaCorrida**](SalidaCorrida.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **crearOrdenDeCompra**
> SalidaOrden crearOrdenDeCompra(idempotencyKey, entradaOrden)

CU-102 · Crea la orden de compra en BORRADOR

Exige CONTABILIDAD_ERP_COMPRAS.

### Example
```dart
import 'package:aportaya_cliente_erp/api.dart';

final api = AportayaClienteErp().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaOrden entradaOrden = ; // EntradaOrden | 

try {
    final response = api.crearOrdenDeCompra(idempotencyKey, entradaOrden);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->crearOrdenDeCompra: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaOrden** | [**EntradaOrden**](EntradaOrden.md)|  | 

### Return type

[**SalidaOrden**](SalidaOrden.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **crearPresupuesto**
> SalidaPresupuesto crearPresupuesto(idempotencyKey, entradaPresupuesto)

CU-101 · Presupuesta un centro de costo para el ejercicio

Exige CONTABILIDAD_ERP_PRESUPUESTO.

### Example
```dart
import 'package:aportaya_cliente_erp/api.dart';

final api = AportayaClienteErp().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaPresupuesto entradaPresupuesto = ; // EntradaPresupuesto | 

try {
    final response = api.crearPresupuesto(idempotencyKey, entradaPresupuesto);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->crearPresupuesto: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaPresupuesto** | [**EntradaPresupuesto**](EntradaPresupuesto.md)|  | 

### Return type

[**SalidaPresupuesto**](SalidaPresupuesto.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **darDeAltaTercero**
> SalidaTercero darDeAltaTercero(idempotencyKey, entradaTercero)

CU-102 · Da de alta un tercero comercial

Exige CONTABILIDAD_ERP_COMPRAS. Reintentar con el mismo documento devuelve el tercero que ya hay; con otro tipo devuelve AP-CU102-02.

### Example
```dart
import 'package:aportaya_cliente_erp/api.dart';

final api = AportayaClienteErp().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaTercero entradaTercero = ; // EntradaTercero | 

try {
    final response = api.darDeAltaTercero(idempotencyKey, entradaTercero);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->darDeAltaTercero: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaTercero** | [**EntradaTercero**](EntradaTercero.md)|  | 

### Return type

[**SalidaTercero**](SalidaTercero.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **depreciarActivo**
> SalidaDepreciacion depreciarActivo(activoId, idempotencyKey, entradaDepreciacion)

CU-105 · Calcula la cuota del activo en el periodo

Exige CONTABILIDAD_ERP_ACTIVOS_FIJOS.

### Example
```dart
import 'package:aportaya_cliente_erp/api.dart';

final api = AportayaClienteErp().getDefaultApi();
final String activoId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaDepreciacion entradaDepreciacion = ; // EntradaDepreciacion | 

try {
    final response = api.depreciarActivo(activoId, idempotencyKey, entradaDepreciacion);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->depreciarActivo: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **activoId** | **String**|  | 
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaDepreciacion** | [**EntradaDepreciacion**](EntradaDepreciacion.md)|  | 

### Return type

[**SalidaDepreciacion**](SalidaDepreciacion.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **generarEstadoFinanciero**
> SalidaEstadoFinanciero generarEstadoFinanciero(periodoId, idempotencyKey, entradaEstadoFinanciero)

CU-106 · Genera el balance o el estado de resultados del mes

Exige CONTABILIDAD_ERP_REPORTES.

### Example
```dart
import 'package:aportaya_cliente_erp/api.dart';

final api = AportayaClienteErp().getDefaultApi();
final String periodoId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaEstadoFinanciero entradaEstadoFinanciero = ; // EntradaEstadoFinanciero | 

try {
    final response = api.generarEstadoFinanciero(periodoId, idempotencyKey, entradaEstadoFinanciero);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->generarEstadoFinanciero: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **periodoId** | **String**|  | 
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaEstadoFinanciero** | [**EntradaEstadoFinanciero**](EntradaEstadoFinanciero.md)|  | 

### Return type

[**SalidaEstadoFinanciero**](SalidaEstadoFinanciero.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **pagarFacturaDeProveedor**
> SalidaFactura pagarFacturaDeProveedor(facturaId, idempotencyKey, entradaPago)

CU-103 · Paga, total o parcialmente, una factura aprobada

Exige CONTABILIDAD_ERP_PAGAR. Quien aprobo la factura no puede autorizar su pago (R-CTB-05).

### Example
```dart
import 'package:aportaya_cliente_erp/api.dart';

final api = AportayaClienteErp().getDefaultApi();
final String facturaId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaPago entradaPago = ; // EntradaPago | 

try {
    final response = api.pagarFacturaDeProveedor(facturaId, idempotencyKey, entradaPago);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->pagarFacturaDeProveedor: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **facturaId** | **String**|  | 
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaPago** | [**EntradaPago**](EntradaPago.md)|  | 

### Return type

[**SalidaFactura**](SalidaFactura.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **registrarCobro**
> SalidaCobro registrarCobro(cuentaId, idempotencyKey, entradaCobro)

CU-104 · Registra un cobro contra la cuenta

Exige CONTABILIDAD_ERP_COBRAR.

### Example
```dart
import 'package:aportaya_cliente_erp/api.dart';

final api = AportayaClienteErp().getDefaultApi();
final String cuentaId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaCobro entradaCobro = ; // EntradaCobro | 

try {
    final response = api.registrarCobro(cuentaId, idempotencyKey, entradaCobro);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->registrarCobro: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **cuentaId** | **String**|  | 
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaCobro** | [**EntradaCobro**](EntradaCobro.md)|  | 

### Return type

[**SalidaCobro**](SalidaCobro.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **registrarFacturaDeProveedor**
> SalidaFactura registrarFacturaDeProveedor(idempotencyKey, entradaFactura)

CU-103 · Registra la factura del proveedor

Exige CONTABILIDAD_ERP_CUENTAS_POR_PAGAR. La aprobacion es un dato del alta: `factura_proveedor` es append-only y no admite aprobarse despues.

### Example
```dart
import 'package:aportaya_cliente_erp/api.dart';

final api = AportayaClienteErp().getDefaultApi();
final String idempotencyKey = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7).
final EntradaFactura entradaFactura = ; // EntradaFactura | 

try {
    final response = api.registrarFacturaDeProveedor(idempotencyKey, entradaFactura);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->registrarFacturaDeProveedor: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **idempotencyKey** | **String**| Toda operacion con efecto la exige. La red duplica; reintentar tiene que ser seguro, y la clave se valida ANTES de escribir (invariante 7). | 
 **entradaFactura** | [**EntradaFactura**](EntradaFactura.md)|  | 

### Return type

[**SalidaFactura**](SalidaFactura.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **validarPlantillaDeAsiento**
> SalidaPlantilla validarPlantillaDeAsiento(plantillaId)

CU-106 · Comprueba que la plantilla no apunte a sumarizadoras

Exige CONTABILIDAD_ERP_REPORTES. Sin efecto, asi que no exige clave.

### Example
```dart
import 'package:aportaya_cliente_erp/api.dart';

final api = AportayaClienteErp().getDefaultApi();
final String plantillaId = 38400000-8cf0-11bd-b23e-10b96e4ef00d; // String | 

try {
    final response = api.validarPlantillaDeAsiento(plantillaId);
    print(response);
} catch on DioException (e) {
    print('Exception when calling DefaultApi->validarPlantillaDeAsiento: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **plantillaId** | **String**|  | 

### Return type

[**SalidaPlantilla**](SalidaPlantilla.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

