import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

import { serve } from '@hono/node-server'
import { createMockServer } from '@scalar/mock-server'
import YAML from 'yaml'

const origen = fileURLToPath(new URL('../generado/prism/todos.yaml', import.meta.url))
const documento = YAML.parse(readFileSync(origen, 'utf8'))
const app = await createMockServer({ document: documento, origin: origen, logger: false })

serve({ fetch: app.fetch, hostname: '0.0.0.0', port: 4010 }, ({ port }) => {
  console.log(`Simulado OpenAPI escuchando en el puerto ${port}`)
})
