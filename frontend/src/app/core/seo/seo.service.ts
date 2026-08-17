import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { Injectable, PLATFORM_ID, REQUEST, inject } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';

import { environment } from '../../../environments/environment';
import { BloodRequest } from '../models/api.models';
import {
  ADMIN_SEO,
  DEFAULT_DESCRIPTION,
  ORGANIZATION_DESCRIPTION,
  PAGE_SEO,
  PRIVATE_SEO,
  PageSeo,
  REQUEST_UNAVAILABLE_SEO,
} from './seo.config';
import { FAQ_QUESTIONS, HOW_TO_STEPS } from './content';
import {
  bloodRequestPath,
  isIndexableRequestStatus,
  isUrgentRequest,
  requestPlaceName,
} from './request-slug';

const OG_IMAGE_PATH = '/assets/og-default.png';
const JSON_LD_GRAPH = 'ld-json-graph';
const JSON_LD_PAGE = 'ld-json-page';

@Injectable({ providedIn: 'root' })
export class SeoService {
  private readonly title = inject(Title);
  private readonly meta = inject(Meta);
  private readonly document = inject(DOCUMENT);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly request = inject(REQUEST, { optional: true });
  private readonly isBrowser = isPlatformBrowser(this.platformId);

  applyForUrl(rawUrl: string): void {
    const path = this.normalizePath(rawUrl);
    if (path.startsWith('/dashboard')) {
      this.apply({ ...PRIVATE_SEO, path });
      return;
    }
    if (path.startsWith('/admin')) {
      this.apply({ ...ADMIN_SEO, path });
      return;
    }
    if (/^\/solicitudes\/.+/.test(path)) {
      return;
    }
    this.apply(PAGE_SEO[path] ?? { ...PAGE_SEO['/'], path });
    this.applyPathJsonLd(path);
  }

  applyBloodRequest(request: BloodRequest): void {
    const place = requestPlaceName(request);
    const path = bloodRequestPath(request);
    const urgent = isUrgentRequest(request);
    const indexable = isIndexableRequestStatus(request.status);
    const title = `Se necesitan donantes ${request.bloodType} en ${place} | BloodConnect RD`;
    const description = urgent
      ? `Solicitud urgente de sangre ${request.bloodType} en ${place}. Conoce los detalles y descubre cómo puedes ayudar.`
      : `Solicitud de sangre ${request.bloodType} en ${place}. Conoce los detalles y descubre cómo puedes ayudar.`;

    this.apply({
      path,
      title,
      description,
      robots: indexable ? 'index, follow' : 'noindex, nofollow',
      type: 'article',
    });

    this.setJsonLd(JSON_LD_PAGE, {
      '@context': 'https://schema.org',
      '@type': 'WebPage',
      name: title,
      description,
      url: this.absoluteUrl(path),
      inLanguage: 'es-DO',
      isPartOf: { '@id': `${this.siteOrigin()}/#website` },
      about: {
        '@type': 'Thing',
        name: `Donación de sangre ${request.bloodType}`,
      },
      contentLocation: {
        '@type': 'Place',
        name: place,
        address: {
          '@type': 'PostalAddress',
          addressLocality: request.municipalityName,
          addressRegion: request.provinceName,
          addressCountry: 'DO',
        },
      },
    });
  }

  applyUnavailableRequest(path = '/solicitudes'): void {
    this.apply({ ...REQUEST_UNAVAILABLE_SEO, path });
  }

  setPageJsonLd(data: Record<string, unknown> | null): void {
    this.setJsonLd(JSON_LD_PAGE, data);
  }

  apply(seo: PageSeo): void {
    const origin = this.siteOrigin();
    const url = this.absoluteUrl(seo.path);
    const image = origin ? `${origin}${OG_IMAGE_PATH}` : OG_IMAGE_PATH;
    const indexable = seo.robots.startsWith('index');

    this.title.setTitle(seo.title);
    this.upsertMeta('name', 'description', seo.description);
    this.upsertMeta('name', 'robots', seo.robots);
    this.upsertMeta('name', 'googlebot', seo.robots);
    this.setCanonical(url);

    this.upsertMeta('property', 'og:title', seo.title);
    this.upsertMeta('property', 'og:description', seo.description);
    this.upsertMeta('property', 'og:type', seo.type ?? 'website');
    this.upsertMeta('property', 'og:url', url);
    this.upsertMeta('property', 'og:image', image);
    this.upsertMeta('property', 'og:image:alt', 'BloodConnect RD, donación de sangre en República Dominicana');
    this.upsertMeta('property', 'og:image:width', '1200');
    this.upsertMeta('property', 'og:image:height', '630');
    this.upsertMeta('property', 'og:locale', 'es_DO');
    this.upsertMeta('property', 'og:site_name', 'BloodConnect RD');

    this.upsertMeta('name', 'twitter:card', 'summary_large_image');
    this.upsertMeta('name', 'twitter:title', seo.title);
    this.upsertMeta('name', 'twitter:description', seo.description);
    this.upsertMeta('name', 'twitter:image', image);
    this.upsertMeta('name', 'twitter:image:alt', 'BloodConnect RD, donación de sangre en República Dominicana');

    const verification = environment.googleSiteVerification?.trim();
    if (verification) {
      this.upsertMeta('name', 'google-site-verification', verification);
    } else if (this.isBrowser) {
      this.meta.removeTag("name='google-site-verification'");
    }

    if (indexable) {
      this.setJsonLd(JSON_LD_GRAPH, this.platformGraph(origin || url));
    } else {
      this.setJsonLd(JSON_LD_GRAPH, null);
    }
    this.setJsonLd(JSON_LD_PAGE, null);
  }

  private applyPathJsonLd(path: string): void {
    if (path === '/preguntas-frecuentes') {
      this.setJsonLd(JSON_LD_PAGE, {
        '@context': 'https://schema.org',
        '@type': 'FAQPage',
        inLanguage: 'es-DO',
        mainEntity: FAQ_QUESTIONS.map((item) => ({
          '@type': 'Question',
          name: item.question,
          acceptedAnswer: {
            '@type': 'Answer',
            text: item.answer,
          },
        })),
      });
      return;
    }
    if (path === '/como-donar') {
      this.setJsonLd(JSON_LD_PAGE, {
        '@context': 'https://schema.org',
        '@type': 'HowTo',
        name: 'Cómo donar sangre en República Dominicana',
        description:
          'Guía práctica para donar sangre: preparación, el proceso en el centro y cuidados posteriores.',
        inLanguage: 'es-DO',
        step: HOW_TO_STEPS.map((item, index) => ({
          '@type': 'HowToStep',
          position: index + 1,
          name: item.title,
          text: item.copy,
        })),
      });
    }
  }

  siteOrigin(): string {
    const configured = environment.siteUrl?.trim().replace(/\/$/, '');
    if (configured) return configured;
    if (this.isBrowser && this.document.location?.origin) {
      return this.document.location.origin;
    }
    const requestUrl = this.request?.url;
    if (requestUrl) {
      try {
        return new URL(requestUrl, 'http://localhost').origin;
      } catch {
        return '';
      }
    }
    return '';
  }

  absoluteUrl(path: string): string {
    const normalized = path.startsWith('/') ? path : `/${path}`;
    const origin = this.siteOrigin();
    return origin ? `${origin}${normalized === '/' ? '/' : normalized}` : normalized;
  }

  private normalizePath(rawUrl: string): string {
    const withoutOrigin = rawUrl.replace(/^[a-z]+:\/\/[^/]+/i, '');
    const path = withoutOrigin.split('?')[0].split('#')[0] || '/';
    if (path.length > 1 && path.endsWith('/')) {
      return path.slice(0, -1);
    }
    return path || '/';
  }

  private platformGraph(origin: string): Record<string, unknown> {
    const siteUrl = origin || '/';
    return {
      '@context': 'https://schema.org',
      '@graph': [
        {
          '@type': 'Organization',
          '@id': `${siteUrl}/#organization`,
          name: 'BloodConnect RD',
          url: siteUrl,
          description: ORGANIZATION_DESCRIPTION,
          logo: {
            '@type': 'ImageObject',
            url: `${siteUrl}${OG_IMAGE_PATH}`,
          },
          areaServed: {
            '@type': 'Country',
            name: 'República Dominicana',
          },
        },
        {
          '@type': 'WebSite',
          '@id': `${siteUrl}/#website`,
          name: 'BloodConnect RD',
          url: siteUrl,
          inLanguage: 'es-DO',
          description: DEFAULT_DESCRIPTION,
          publisher: { '@id': `${siteUrl}/#organization` },
        },
      ],
    };
  }

  private setCanonical(url: string): void {
    let link = this.document.querySelector<HTMLLinkElement>('link[rel="canonical"]');
    if (!link) {
      link = this.document.createElement('link');
      link.setAttribute('rel', 'canonical');
      this.document.head.appendChild(link);
    }
    link.setAttribute('href', url);
  }

  private upsertMeta(attr: 'name' | 'property', key: string, content: string): void {
    const selector = `${attr}="${key}"`;
    if (this.meta.getTag(selector)) {
      this.meta.updateTag({ [attr]: key, content });
      return;
    }
    this.meta.addTag({ [attr]: key, content });
  }

  private setJsonLd(id: string, data: Record<string, unknown> | null): void {
    const existing = this.document.getElementById(id);
    if (!data) {
      existing?.remove();
      return;
    }
    const script =
      existing instanceof HTMLScriptElement
        ? existing
        : this.document.createElement('script');
    script.id = id;
    script.type = 'application/ld+json';
    script.textContent = JSON.stringify(data);
    if (!existing) {
      this.document.head.appendChild(script);
    }
  }
}

export { DEFAULT_TITLE } from './seo.config';
