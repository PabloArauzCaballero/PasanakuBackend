export declare const RUTAS_EXCLUIDAS_SITEMAP: readonly string[]
export declare function rutaExcluidaDelSitemap(ruta: string): boolean
export declare function generarSitemap(rutasIndexables: { ruta: string; actualizado: string }[], base: string): string
