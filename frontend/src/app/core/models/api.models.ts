export type Role = 'USER' | 'DONOR' | 'ADMIN';
export type BloodType = 'A+' | 'A-' | 'B+' | 'B-' | 'AB+' | 'AB-' | 'O+' | 'O-';
export type Urgency = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type Availability = 'AVAILABLE' | 'TEMPORARILY_UNAVAILABLE' | 'INACTIVE';
export type RequestStatus = 'OPEN' | 'IN_PROGRESS' | 'FULFILLED' | 'CANCELLED' | 'EXPIRED';
export type DonationStatus =
  | 'REPORTED'
  | 'PARTIALLY_CONFIRMED'
  | 'CONFIRMED'
  | 'CANCELLED'
  | 'COMPLETED';
export type CenterType = 'HOSPITAL' | 'CLINIC' | 'BLOOD_BANK' | 'MEDICAL_CENTER' | 'OTHER';
export type Sex = 'MALE' | 'FEMALE' | 'OTHER';

export const BLOOD_TYPES: BloodType[] = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];

export const DONATION_STATUS_LABELS: Record<string, string> = {
  REPORTED: 'Pendiente de confirmación',
  PARTIALLY_CONFIRMED: 'Confirmada parcialmente',
  CONFIRMED: 'Confirmada',
  CANCELLED: 'Cancelada',
  COMPLETED: 'Confirmada',
};

export interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  role: Role;
  enabled: boolean;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface Province {
  id: number;
  name: string;
  code: string;
}

export interface Municipality {
  id: number;
  provinceId: number;
  name: string;
  code: string;
}

export interface Donor {
  id: number;
  userId?: number;
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  bloodType: BloodType;
  birthDate?: string;
  sex?: Sex;
  provinceId: number;
  provinceName: string;
  municipalityId: number;
  municipalityName: string;
  sector?: string;
  approximateAddress?: string;
  latitude?: number | null;
  longitude?: number | null;
  availability: Availability;
  lastDonationDate?: string | null;
  approximateDistanceKm?: number | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface BloodRequest {
  id: number;
  createdById: number;
  createdByName: string;
  patientName: string;
  bloodType: BloodType;
  unitsRequired: number;
  completedUnits: number;
  pendingUnits: number;
  progressPercent: number;
  progress?: number;
  hospital: string;
  provinceId: number;
  provinceName: string;
  municipalityId: number;
  municipalityName: string;
  sector?: string;
  address: string;
  reference?: string;
  latitude?: number | null;
  longitude?: number | null;
  deadline: string;
  description?: string;
  contactPhone: string;
  urgency: Urgency;
  status: RequestStatus;
  approximateDistanceKm?: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface DonationCenter {
  id: number;
  name: string;
  type: CenterType;
  provinceId: number;
  provinceName: string;
  municipalityId: number;
  municipalityName: string;
  sector?: string;
  address: string;
  reference?: string;
  phone?: string;
  schedule?: string;
  latitude?: number | null;
  longitude?: number | null;
  active: boolean;
  approximateDistanceKm?: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface Donation {
  id: number;
  donorId: number;
  donorUserId: number;
  donorName: string;
  bloodRequestId?: number | null;
  patientName?: string | null;
  hospital?: string | null;
  receiverName?: string | null;
  donationCenterId?: number | null;
  donationCenterName?: string | null;
  donationDate: string;
  units: number;
  confirmedUnits: number;
  notes?: string;
  status: DonationStatus;
  createdAt: string;
  updatedAt?: string;
}

export interface DonationHistory {
  totalDonations: number;
  totalUnits: number;
  lastDonation?: string | null;
  estimatedNextDate?: string | null;
  orientationNote: string;
  history: Donation[];
}

export interface Notification {
  id: number;
  type: string;
  title: string;
  message: string;
  resourceType?: string | null;
  resourceId?: number | null;
  read: boolean;
  createdAt: string;
}

export interface Compatibility {
  bloodType: BloodType;
  canDonateTo: BloodType[];
  canReceiveFrom: BloodType[];
  disclaimer: string;
}

export interface DashboardStatistics {
  users: number;
  donors: number;
  availableDonors: number;
  openRequests: number;
  fulfilledRequests: number;
  donations: number;
  bloodTypeDistribution: Record<string, number>;
  requestsByProvince: Record<string, number>;
  requestsByMunicipality: Record<string, number>;
  donationsByMonth: Record<string, number>;
}

export interface BloodRequestPayload {
  patientName: string;
  bloodType: BloodType;
  unitsRequired: number;
  hospital: string;
  provinceId: number;
  municipalityId: number;
  sector?: string;
  address: string;
  reference?: string;
  latitude?: number | null;
  longitude?: number | null;
  deadline: string;
  description?: string;
  contactPhone: string;
  urgency: Urgency;
}

export interface DonorPayload {
  bloodType: BloodType;
  birthDate: string;
  sex: Sex;
  phone: string;
  provinceId: number;
  municipalityId: number;
  sector?: string;
  approximateAddress?: string;
  latitude?: number | null;
  longitude?: number | null;
  lastDonationDate?: string | null;
}

export type CenterPayload = Omit<
  DonationCenter,
  'id' | 'provinceName' | 'municipalityName' | 'createdAt' | 'updatedAt' | 'approximateDistanceKm'
>;

export interface ApiError {
  message?: string;
  detail?: string;
  errors?: Record<string, string>;
}

export function requestPendingUnits(request: Pick<BloodRequest, 'unitsRequired' | 'completedUnits' | 'pendingUnits'>): number {
  if (request.pendingUnits != null) return request.pendingUnits;
  return Math.max(0, request.unitsRequired - request.completedUnits);
}

export function requestProgressPercent(
  request: Pick<BloodRequest, 'unitsRequired' | 'completedUnits' | 'progressPercent' | 'progress'>,
): number {
  if (!request.unitsRequired) return 0;
  const ratio =
    request.progress != null
      ? request.progress
      : request.completedUnits / request.unitsRequired;
  return Math.min(100, Math.max(0, Math.round(ratio * 100)));
}

export function donationStatusLabel(status: string): string {
  return DONATION_STATUS_LABELS[status] ?? status;
}

export function donationStatusTone(status: string): 'red' | 'green' | 'amber' | 'neutral' {
  if (status === 'CONFIRMED' || status === 'COMPLETED') return 'green';
  if (status === 'PARTIALLY_CONFIRMED') return 'amber';
  if (status === 'CANCELLED') return 'red';
  return 'neutral';
}
