// Lighthouse CI, bloqueante (ficha F10). No hay `@lhci/cli` en el catálogo de dependencias
// (agregarlo es un micro-PR a `gradle`/`yarn.lock`, fuera de lo que este carril puede tocar),
// así que se corre con `npx --yes @lhci/cli autorun` contra este archivo, o directo con
// `npx --yes lighthouse <url> --only-categories=performance,accessibility,best-practices,seo`.
// Antes: `node scripts/contenido.mjs && yarn ng build` y `PORT=4173 node dist/web/server/server.mjs`.
//
// Umbrales medidos contra el build SSR local el 2026-09-11 (ver planes/informes/carril-W1.md):
// inicio — performance 91 · accesibilidad 100 · buenas prácticas 100 · SEO 100 · CLS 0 · TBT 0ms
// plazos — performance 92 · accesibilidad 100 · buenas prácticas 100 · SEO 100 · CLS 0
module.exports = {
  ci: {
    collect: {
      url: ['http://127.0.0.1:4173/', 'http://127.0.0.1:4173/plazos'],
      startServerCommand: 'node dist/web/server/server.mjs',
      startServerReadyPattern: 'listening',
      numberOfRuns: 1,
      settings: { chromeFlags: '--headless --no-sandbox' },
    },
    assert: {
      assertions: {
        'categories:performance': ['error', { minScore: 0.85 }],
        'categories:accessibility': ['error', { minScore: 1 }],
        'categories:best-practices': ['error', { minScore: 0.95 }],
        'categories:seo': ['error', { minScore: 1 }],
        'cumulative-layout-shift': ['error', { maxNumericValue: 0.1 }],
        'total-blocking-time': ['error', { maxNumericValue: 300 }],
        'largest-contentful-paint': ['error', { maxNumericValue: 4000 }],
      },
    },
    upload: { target: 'filesystem', outputDir: '.lighthouseci' },
  },
}
