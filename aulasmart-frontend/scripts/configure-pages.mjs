import { readFile, writeFile } from 'node:fs/promises';

const configuredOrigin = process.env.AULASMART_API_ORIGIN?.trim().replace(/\/$/, '') ?? '';
if (!/^https:\/\/[a-z0-9.-]+(?::\d+)?$/i.test(configuredOrigin)) {
  throw new Error('AULASMART_API_ORIGIN debe ser un origen HTTPS sin ruta.');
}

const origin = new URL(configuredOrigin).origin;
const index = new URL('../dist/aulasmart-frontend/browser/index.html', import.meta.url);
const html = await readFile(index, 'utf8');
const marker = /<meta name="aulasmart-api-origin" content(?:="")?>/;
if (!marker.test(html)) {
  throw new Error('No se encontró la configuración de API en el HTML de Pages.');
}
await writeFile(index, html.replace(marker, `<meta name="aulasmart-api-origin" content="${origin}">`));
