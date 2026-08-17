import { BloodRequest, BloodType, RequestStatus } from '../models/api.models';

const BLOOD_TYPE_SLUGS: Record<BloodType, string> = {
  'A+': 'a-positivo',
  'A-': 'a-negativo',
  'B+': 'b-positivo',
  'B-': 'b-negativo',
  'AB+': 'ab-positivo',
  'AB-': 'ab-negativo',
  'O+': 'o-positivo',
  'O-': 'o-negativo',
};

const INDEXABLE_STATUSES: RequestStatus[] = ['OPEN', 'IN_PROGRESS'];

export function slugify(value: string | null | undefined): string {
  return (value ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
}

export function bloodTypeSlug(bloodType: string): string {
  return BLOOD_TYPE_SLUGS[bloodType as BloodType] ?? slugify(bloodType);
}

export function requestPlaceName(request: {
  municipalityName?: string | null;
  provinceName?: string | null;
}): string {
  return request.municipalityName?.trim() || request.provinceName?.trim() || 'República Dominicana';
}

export function bloodRequestSlug(request: {
  id: number;
  bloodType: string;
  municipalityName?: string | null;
  provinceName?: string | null;
}): string {
  const place = slugify(requestPlaceName(request)) || 'republica-dominicana';
  return `${bloodTypeSlug(request.bloodType)}-${place}-${request.id}`;
}

export function bloodRequestPath(request: {
  id: number;
  bloodType: string;
  municipalityName?: string | null;
  provinceName?: string | null;
}): string {
  return `/solicitudes/${bloodRequestSlug(request)}`;
}

export function parseRequestId(param: string | null | undefined): number | null {
  if (!param) return null;
  if (/^\d+$/.test(param)) {
    const id = Number(param);
    return Number.isSafeInteger(id) && id > 0 ? id : null;
  }
  const match = param.match(/-(\d+)$/);
  if (!match) return null;
  const id = Number(match[1]);
  return Number.isSafeInteger(id) && id > 0 ? id : null;
}

export function isIndexableRequestStatus(status: RequestStatus | string | null | undefined): boolean {
  return INDEXABLE_STATUSES.includes(status as RequestStatus);
}

export function isUrgentRequest(request: Pick<BloodRequest, 'urgency'>): boolean {
  return request.urgency === 'HIGH' || request.urgency === 'CRITICAL';
}
