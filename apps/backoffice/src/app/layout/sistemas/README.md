# layout/sistemas

Vacío a propósito. Es de `B5` (fase `F8.D`, backoffice de sistemas): el shell de
sistemas (menú y cabecera para `PLATAFORMA`/`SEGURIDAD`) se dibuja ahí, no en `F6`.

`F6` deja este directorio creado y congelado para que `B5` no tenga que tocar
`layout/` fuera de su carpeta. Mientras tanto, la ruta `sistemas` reutiliza
`ShellFinanciero` desde `app.routes.ts`.
