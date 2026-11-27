-- Activa el proveedor de pago QR_INTEROP SOLO en DEV y TEST, para que la pasarela simulada de aportes pueda recorrer orden → aviso → conciliacion (el aviso simulado exige un proveedor activo con webhook).
-- GENERADO desde seeders/dev/19-proveedor-qr-interop-activo-test.json — no editar a mano.

-- SINTETICO · apto_produccion=false. Activa QR_INTEROP solo en dev/test; la semilla minima de produccion lo deja inactivo. UPDATE re-ejecutable: sin filas que cambiar en la segunda corrida.
UPDATE proveedor_pago SET activo = TRUE WHERE codigo = 'QR_INTEROP' AND activo = FALSE;
