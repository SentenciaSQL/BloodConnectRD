import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { environment } from '../../../environments/environment';
import {
  Availability,
  BloodRequest,
  BloodRequestPayload,
  BloodType,
  CenterPayload,
  Compatibility,
  DashboardStatistics,
  Donation,
  DonationCenter,
  DonationHistory,
  DonationResponse,
  Donor,
  DonorPayload,
  Municipality,
  Notification,
  PageResponse,
  Province,
  Role,
  Urgency,
  User,
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  provinces() {
    return this.http.get<Province[]>(`${this.base}/locations/provinces`);
  }

  municipalities(provinceId: number) {
    return this.http.get<Municipality[]>(
      `${this.base}/locations/provinces/${provinceId}/municipalities`,
    );
  }

  donors(filters: Record<string, string | number | boolean | null | undefined> = {}) {
    return this.http.get<PageResponse<Donor>>(`${this.base}/donors`, {
      params: this.params(filters),
    });
  }

  compatibleDonors(bloodType: BloodType, filters: Record<string, unknown> = {}) {
    return this.http.get<Donor[]>(`${this.base}/donors/compatible`, {
      params: this.params({ bloodType, ...filters }),
    });
  }

  myDonorProfile() {
    return this.http.get<Donor>(`${this.base}/donors/me`);
  }

  createDonor(payload: DonorPayload) {
    return this.http.post<Donor>(`${this.base}/donors`, payload);
  }

  updateDonor(payload: DonorPayload) {
    return this.http.put<Donor>(`${this.base}/donors/me`, payload);
  }

  updateAvailability(availability: Availability) {
    return this.http.patch<Donor>(`${this.base}/donors/me/availability`, { availability });
  }

  requests(filters: Record<string, string | number | boolean | string[] | null | undefined> = {}) {
    return this.http.get<PageResponse<BloodRequest>>(`${this.base}/blood-requests`, {
      params: this.params(filters),
    });
  }

  urgentRequests(size = 6) {
    return this.http.get<PageResponse<BloodRequest>>(`${this.base}/blood-requests/urgent`, {
      params: { page: 0, size },
    });
  }

  compatibleRequests(page = 0) {
    return this.http.get<PageResponse<BloodRequest>>(`${this.base}/blood-requests/compatible`, {
      params: { page, size: 12 },
    });
  }

  nearbyRequests(latitude: number, longitude: number, radius = 25) {
    return this.http.get<BloodRequest[]>(`${this.base}/blood-requests/nearby`, {
      params: { latitude, longitude, radius },
    });
  }

  myRequests(page = 0) {
    return this.http.get<PageResponse<BloodRequest>>(`${this.base}/blood-requests/my`, {
      params: { page, size: 12 },
    });
  }

  request(id: number) {
    return this.http.get<BloodRequest>(`${this.base}/blood-requests/${id}`);
  }

  requestDonations(id: number) {
    return this.http.get<Donation[]>(`${this.base}/blood-requests/${id}/donations`);
  }

  reportDonation(requestId: number, payload: { units: number; donationDate?: string; notes?: string }) {
    return this.http.post<Donation>(`${this.base}/blood-requests/${requestId}/donations`, payload);
  }

  confirmDonation(donationId: number, confirmedUnits: number) {
    return this.http.patch<Donation>(`${this.base}/donations/${donationId}/confirm`, { confirmedUnits });
  }

  createRequest(payload: BloodRequestPayload) {
    return this.http.post<BloodRequest>(`${this.base}/blood-requests`, payload);
  }

  updateRequest(id: number, payload: BloodRequestPayload) {
    return this.http.put<BloodRequest>(`${this.base}/blood-requests/${id}`, payload);
  }

  cancelRequest(id: number) {
    return this.http.delete<BloodRequest>(`${this.base}/blood-requests/${id}`);
  }

  respondToRequest(id: number, message?: string) {
    return this.http.post<DonationResponse>(`${this.base}/blood-requests/${id}/responses`, { message });
  }

  requestResponses(id: number) {
    return this.http.get<DonationResponse[]>(`${this.base}/blood-requests/${id}/responses`);
  }

  centers(filters: Record<string, string | number | boolean | null | undefined> = {}) {
    return this.http.get<PageResponse<DonationCenter>>(`${this.base}/donation-centers`, {
      params: this.params(filters),
    });
  }

  nearbyCenters(latitude: number, longitude: number, radius = 25) {
    return this.http.get<DonationCenter[]>(`${this.base}/donation-centers/nearby`, {
      params: { latitude, longitude, radius },
    });
  }

  center(id: number) {
    return this.http.get<DonationCenter>(`${this.base}/donation-centers/${id}`);
  }

  compatibility(bloodType: BloodType) {
    return this.http.get<Compatibility>(`${this.base}/blood-compatibility/${bloodType}`);
  }

  donationHistory() {
    return this.http.get<DonationHistory>(`${this.base}/donations/me`);
  }

  notifications(page = 0, unread = false) {
    const suffix = unread ? '/unread' : '';
    return this.http.get<PageResponse<Notification>>(`${this.base}/notifications${suffix}`, {
      params: { page, size: 20 },
    });
  }

  markNotificationRead(id: number) {
    return this.http.patch<Notification>(`${this.base}/notifications/${id}/read`, {});
  }

  markAllNotificationsRead() {
    return this.http.patch(`${this.base}/notifications/read-all`, {});
  }

  adminUsers(
    filters: {
      search?: string;
      role?: Role | '';
      enabled?: boolean;
      page?: number;
    } = {},
  ) {
    return this.http.get<PageResponse<User>>(`${this.base}/admin/users`, {
      params: this.params({ ...filters, size: 20 }),
    });
  }

  setUserEnabled(id: number, enabled: boolean) {
    return this.http.patch<User>(`${this.base}/admin/users/${id}/status`, { enabled });
  }

  adminDonations(page = 0) {
    return this.http.get<PageResponse<Donation>>(`${this.base}/admin/donations`, {
      params: { page, size: 20 },
    });
  }

  createCenter(payload: CenterPayload) {
    return this.http.post<DonationCenter>(`${this.base}/admin/donation-centers`, payload);
  }

  updateCenter(id: number, payload: CenterPayload) {
    return this.http.put<DonationCenter>(`${this.base}/admin/donation-centers/${id}`, payload);
  }

  deleteCenter(id: number) {
    return this.http.delete(`${this.base}/admin/donation-centers/${id}`);
  }

  dashboardStatistics() {
    return this.http.get<DashboardStatistics>(`${this.base}/admin/statistics/dashboard`);
  }

  private params(values: Record<string, unknown>): HttpParams {
    let params = new HttpParams();
    Object.entries(values).forEach(([key, value]) => {
      if (value === undefined || value === null || value === '') return;
      if (Array.isArray(value)) {
        value.forEach((item) => {
          if (item !== undefined && item !== null && item !== '') {
            params = params.append(key, String(item));
          }
        });
        return;
      }
      params = params.set(key, String(value));
    });
    return params;
  }
}

export const urgencyLabels: Record<Urgency, string> = {
  LOW: 'Baja',
  MEDIUM: 'Media',
  HIGH: 'Alta',
  CRITICAL: 'Crítica',
};
