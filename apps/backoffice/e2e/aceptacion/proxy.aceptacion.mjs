// Proxy del `ng serve` del backoffice hacia la API de la corrida (el doble con estado, o el gateway del stack real).
export default {
  '/api/v1': {
    target: process.env.BASE_API ?? `http://127.0.0.1:${Number(process.env.PUERTO_BASE ?? 5445) + 1}`,
    secure: false,
    changeOrigin: true,
  },
}
