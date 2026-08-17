import {
  AngularNodeAppEngine,
  createNodeRequestHandler,
  isMainModule,
  writeResponseToNodeResponse,
} from '@angular/ssr/node';
import express from 'express';
import { join } from 'node:path';

import { PUBLIC_SITEMAP_PATHS } from './app/core/seo/seo.config';
import { bloodRequestPath } from './app/core/seo/request-slug';

const browserDistFolder = join(import.meta.dirname, '../browser');

const app = express();
const angularApp = new AngularNodeAppEngine({
  allowedHosts: resolveAllowedHosts(),
  trustProxyHeaders: true,
});

app.get('/robots.txt', (request, response) => {
  const origin = siteOrigin(request);
  response.type('text/plain').send(robotsTxt(origin));
});

app.get('/sitemap.xml', async (request, response) => {
  const origin = siteOrigin(request);
  try {
    const apiSitemap = await fetch(`${apiBase()}/public/sitemap.xml`, {
      headers: {
        Accept: 'application/xml',
        'X-Forwarded-Host': request.headers.host ?? '',
        'X-Forwarded-Proto': forwardedProto(request),
        'X-Forwarded-Site-Url': origin,
      },
    });
    if (apiSitemap.ok) {
      response.status(200).type('application/xml').send(await apiSitemap.text());
      return;
    }
  } catch {
    /* Usa el sitemap estático si la API no está disponible. */
  }
  response.status(200).type('application/xml').send(await buildFallbackSitemap(origin));
});

app.get('/solicitudes/:id', async (request, response, next) => {
  const id = request.params.id;
  if (!/^\d+$/.test(id)) {
    next();
    return;
  }
  try {
    const apiResponse = await fetch(`${apiBase()}/blood-requests/${id}`);
    if (!apiResponse.ok) {
      next();
      return;
    }
    const payload = (await apiResponse.json()) as {
      id?: number;
      bloodType?: string;
      municipalityName?: string;
      provinceName?: string;
    };
    if (!payload?.id || !payload.bloodType) {
      next();
      return;
    }
    response.redirect(301, `${siteOrigin(request)}${bloodRequestPath({
      id: payload.id,
      bloodType: payload.bloodType,
      municipalityName: payload.municipalityName,
      provinceName: payload.provinceName,
    })}`);
  } catch {
    next();
  }
});

app.use(
  express.static(browserDistFolder, {
    maxAge: '1y',
    index: false,
    redirect: false,
  }),
);

app.use((request, response, next) => {
  angularApp
    .handle(request)
    .then((res) => (res ? writeResponseToNodeResponse(res, response) : next()))
    .catch(next);
});

if (isMainModule(import.meta.url) || process.env['pm_id']) {
  const port = process.env['PORT'] || 4000;
  app.listen(port, (error) => {
    if (error) {
      throw error;
    }
    console.log(`Node Express server listening on http://localhost:${port}`);
  });
}

export const reqHandler = createNodeRequestHandler(app);

function resolveAllowedHosts(): string[] {
  const fromEnv = process.env['NG_ALLOWED_HOSTS']?.trim();
  if (fromEnv) {
    return fromEnv.split(',').map((host) => host.trim()).filter(Boolean);
  }
  const site = process.env['SITE_URL'] || process.env['PUBLIC_SITE_URL'];
  if (site) {
    try {
      return [new URL(site).hostname, 'localhost'];
    } catch {
      /* ignore */
    }
  }
  return ['*'];
}

function apiBase(): string {
  const raw = (process.env['API_BASE_URL'] || 'http://localhost:8080').replace(/\/$/, '');
  return raw.endsWith('/api') ? raw : `${raw}/api`;
}

function forwardedProto(request: express.Request): string {
  const header = request.headers['x-forwarded-proto'];
  if (typeof header === 'string' && header.trim()) {
    return header.split(',')[0].trim();
  }
  return request.protocol || 'http';
}

function siteOrigin(request: express.Request): string {
  const configured = (process.env['SITE_URL'] || process.env['PUBLIC_SITE_URL'] || '').replace(/\/$/, '');
  if (configured) return configured;
  const hostHeader = request.headers['x-forwarded-host'] ?? request.headers.host;
  const host = Array.isArray(hostHeader) ? hostHeader[0] : hostHeader;
  if (!host) return '';
  return `${forwardedProto(request)}://${host}`;
}

function xmlEscape(value: string): string {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;');
}

function urlEntry(origin: string, path: string, changefreq: string, priority: string, lastmod?: string): string {
  const loc = `${origin}${path === '/' ? '/' : path}`;
  const lastmodTag = lastmod ? `\n    <lastmod>${xmlEscape(lastmod)}</lastmod>` : '';
  return `  <url>
    <loc>${xmlEscape(loc)}</loc>${lastmodTag}
    <changefreq>${changefreq}</changefreq>
    <priority>${priority}</priority>
  </url>`;
}

function robotsTxt(origin: string): string {
  const sitemap = origin ? `${origin}/sitemap.xml` : '/sitemap.xml';
  return `User-agent: *
Allow: /
Allow: /solicitudes
Allow: /donantes
Allow: /centros
Allow: /como-donar
Allow: /compatibilidad
Allow: /preguntas-frecuentes
Allow: /eliminacion-de-cuenta
Allow: /assets/

Disallow: /login
Disallow: /registro
Disallow: /dashboard
Disallow: /dashboard/
Disallow: /admin
Disallow: /admin/

Sitemap: ${sitemap}
`;
}

async function buildFallbackSitemap(origin: string): Promise<string> {
  const urls = PUBLIC_SITEMAP_PATHS.map((page) =>
    urlEntry(origin, page.path, page.changefreq, page.priority),
  );
  try {
    const response = await fetch(
      `${apiBase()}/blood-requests?status=OPEN&status=IN_PROGRESS&size=1000&sort=updatedAt&direction=desc`,
    );
    if (response.ok) {
      const page = (await response.json()) as {
        content?: Array<{
          id: number;
          bloodType: string;
          municipalityName?: string;
          provinceName?: string;
          updatedAt?: string;
        }>;
      };
      for (const request of page.content ?? []) {
        if (!request?.id || !request.bloodType) continue;
        const path = bloodRequestPath(request);
        const lastmod = request.updatedAt?.slice(0, 10);
        urls.push(urlEntry(origin, path, 'daily', '0.8', lastmod));
      }
    }
  } catch {
    /* Solo páginas estáticas si la API no responde. */
  }
  return `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
${urls.join('\n')}
</urlset>
`;
}
