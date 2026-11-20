/** Pages inyecta el origen público. Angular dev y Docker usan su proxy local. */
const apiOrigin = globalThis.document?.querySelector<HTMLMetaElement>('meta[name="aulasmart-api-origin"]')?.content ?? '';
export const apiUrl = `${apiOrigin}/api`;
