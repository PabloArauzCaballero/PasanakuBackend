// Configuración de vitest para `ng test --runner-config=...`: un solo worker y un archivo a la vez.
// La usa scripts/entrega_simulada.sh en máquinas de desarrollo (regla 70.3: ejecución serial; la paralelización se habilita en CI).
export default { test: { maxWorkers: 1, minWorkers: 1, fileParallelism: false } }
