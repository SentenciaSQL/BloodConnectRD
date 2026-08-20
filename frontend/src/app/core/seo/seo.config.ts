export const INDEX_FOLLOW = 'index, follow';
export const NOINDEX_NOFOLLOW = 'noindex, nofollow';

export interface PageSeo {
  title: string;
  description: string;
  robots: string;
  type?: 'website' | 'article';
  path: string;
}

export const DEFAULT_TITLE = 'BloodConnect RD | Conectando donantes de sangre en República Dominicana';
export const DEFAULT_DESCRIPTION =
  'Encuentra donantes de sangre y ayuda a personas que necesitan una donación en República Dominicana con BloodConnect RD.';

export const ORGANIZATION_DESCRIPTION =
  'Plataforma que conecta donantes de sangre con personas que necesitan una donación en República Dominicana.';

export const PAGE_SEO: Record<string, PageSeo> = {
  '/': {
    path: '/',
    title: DEFAULT_TITLE,
    description: DEFAULT_DESCRIPTION,
    robots: INDEX_FOLLOW,
  },
  '/solicitudes': {
    path: '/solicitudes',
    title: 'Solicitudes de Sangre | BloodConnect RD',
    description:
      'Consulta solicitudes de sangre activas y encuentra personas que necesitan donantes cerca de ti.',
    robots: INDEX_FOLLOW,
  },
  '/donantes': {
    path: '/donantes',
    title: 'Donantes de Sangre en República Dominicana | BloodConnect RD',
    description:
      'Encuentra donantes de sangre por tipo sanguíneo y ubicación en República Dominicana, sin exponer datos personales.',
    robots: INDEX_FOLLOW,
  },
  '/centros': {
    path: '/centros',
    title: 'Centros de Donación de Sangre | BloodConnect RD',
    description:
      'Consulta centros de donación de sangre registrados en República Dominicana, con ubicación y datos de contacto.',
    robots: INDEX_FOLLOW,
  },
  '/como-donar': {
    path: '/como-donar',
    title: 'Cómo Donar Sangre en República Dominicana | BloodConnect RD',
    description:
      'Guía para donar sangre en República Dominicana: preparación, el proceso en el centro y cuidados después de donar.',
    robots: INDEX_FOLLOW,
  },
  '/compatibilidad': {
    path: '/compatibilidad',
    title: 'Compatibilidad Sanguínea | BloodConnect RD',
    description:
      'Consulta qué tipos de sangre pueden donar y recibir glóbulos rojos. Información educativa para donar sangre en República Dominicana.',
    robots: INDEX_FOLLOW,
  },
  '/preguntas-frecuentes': {
    path: '/preguntas-frecuentes',
    title: 'Preguntas Frecuentes | BloodConnect RD',
    description:
      'Respuestas sobre BloodConnect RD, donación de sangre, privacidad de donantes y cómo ayudar a quien necesita una donación.',
    robots: INDEX_FOLLOW,
  },
  '/eliminacion-de-cuenta': {
    path: '/eliminacion-de-cuenta',
    title: 'Eliminación de Cuenta | BloodConnect RD',
    description:
      'Cómo solicitar la eliminación permanente de tu cuenta y de los datos asociados en BloodConnect RD.',
    robots: INDEX_FOLLOW,
  },
  '/login': {
    path: '/login',
    title: 'Iniciar sesión | BloodConnect RD',
    description: 'Accede a tu cuenta de BloodConnect RD para gestionar solicitudes, donaciones y mensajes.',
    robots: NOINDEX_NOFOLLOW,
  },
  '/registro': {
    path: '/registro',
    title: 'Crear cuenta | BloodConnect RD',
    description: 'Regístrate en BloodConnect RD para donar sangre o publicar una solicitud en República Dominicana.',
    robots: NOINDEX_NOFOLLOW,
  },
};

export const PRIVATE_SEO: PageSeo = {
  path: '/dashboard',
  title: 'Mi cuenta | BloodConnect RD',
  description: 'Área privada de BloodConnect RD.',
  robots: NOINDEX_NOFOLLOW,
};

export const ADMIN_SEO: PageSeo = {
  path: '/admin',
  title: 'Administración | BloodConnect RD',
  description: 'Panel administrativo de BloodConnect RD.',
  robots: NOINDEX_NOFOLLOW,
};

export const REQUEST_UNAVAILABLE_SEO: PageSeo = {
  path: '/solicitudes',
  title: 'Solicitud no disponible | BloodConnect RD',
  description: 'Esta solicitud de sangre no está disponible o ya no se puede consultar.',
  robots: NOINDEX_NOFOLLOW,
};

export const NOT_FOUND_SEO: PageSeo = {
  path: '',
  title: 'Página no encontrada | BloodConnect RD',
  description: 'La página que buscas no existe o fue movida.',
  robots: NOINDEX_NOFOLLOW,
};

export const PUBLIC_SITEMAP_PATHS: { path: string; changefreq: string; priority: string }[] = [
  { path: '/', changefreq: 'daily', priority: '1.0' },
  { path: '/solicitudes', changefreq: 'daily', priority: '0.9' },
  { path: '/donantes', changefreq: 'weekly', priority: '0.8' },
  { path: '/centros', changefreq: 'weekly', priority: '0.7' },
  { path: '/como-donar', changefreq: 'monthly', priority: '0.8' },
  { path: '/compatibilidad', changefreq: 'monthly', priority: '0.8' },
  { path: '/preguntas-frecuentes', changefreq: 'monthly', priority: '0.6' },
  { path: '/eliminacion-de-cuenta', changefreq: 'yearly', priority: '0.3' },
];
