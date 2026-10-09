// La politica de rastreadores es decision de negocio (ADR-042) y por eso tiene prueba.
// Busqueda si, entrenamiento no; y los datos de terceros fuera para TODOS.
export const BUSCADORES_PERMITIDOS = ['Googlebot', 'Bingbot', 'OAI-SearchBot', 'ChatGPT-User', 'ClaudeBot', 'Claude-User', 'PerplexityBot', 'Google-CloudVertexBot']
export const ENTRENAMIENTO_BLOQUEADO = ['GPTBot', 'Google-Extended', 'Applebot-Extended', 'CCBot', 'Bytespider', 'meta-externalagent']
export const RUTAS_NO_INDEXABLES = ['/verificar/', '/publico/', '/catalogo', '/api/']

export function robotsTxt(base) {
  const lineas = ['# Buscadores y motores generativos con cita: SI']
  for (const a of BUSCADORES_PERMITIDOS) lineas.push(`User-agent: ${a}`)
  lineas.push('Allow: /', '', '# Entrenamiento de modelos: NO')
  for (const a of ENTRENAMIENTO_BLOQUEADO) lineas.push(`User-agent: ${a}`)
  lineas.push('Disallow: /', '', '# Datos de terceros: bloqueados para TODOS, sin excepcion', 'User-agent: *')
  for (const r of RUTAS_NO_INDEXABLES) lineas.push(`Disallow: ${r}`)
  lineas.push('', `Sitemap: ${base}/sitemap.xml`, '')
  return lineas.join('\n')
}
