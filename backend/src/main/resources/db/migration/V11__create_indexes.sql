CREATE INDEX idx_donors_blood_availability ON donors (blood_type, availability);
CREATE INDEX idx_donors_location ON donors (province_id, municipality_id);
CREATE INDEX idx_donors_geo ON donors (latitude, longitude);

CREATE INDEX idx_blood_requests_creator ON blood_requests (created_by);
CREATE INDEX idx_blood_requests_blood_status ON blood_requests (blood_type, status);
CREATE INDEX idx_blood_requests_location ON blood_requests (province_id, municipality_id);
CREATE INDEX idx_blood_requests_urgency_status ON blood_requests (urgency, status);
CREATE INDEX idx_blood_requests_deadline ON blood_requests (deadline);
CREATE INDEX idx_blood_requests_geo ON blood_requests (latitude, longitude);

CREATE INDEX idx_donation_responses_request ON donation_responses (blood_request_id);
CREATE INDEX idx_donation_responses_donor ON donation_responses (donor_id);
CREATE INDEX idx_donation_responses_status ON donation_responses (status);

CREATE INDEX idx_donation_centers_location ON donation_centers (province_id, municipality_id);
CREATE INDEX idx_donation_centers_active ON donation_centers (active);
CREATE INDEX idx_donation_centers_geo ON donation_centers (latitude, longitude);

CREATE INDEX idx_donations_donor_date ON donations (donor_id, donation_date DESC);
CREATE INDEX idx_donations_request ON donations (blood_request_id);
CREATE INDEX idx_donations_center ON donations (donation_center_id);
CREATE INDEX idx_donations_status_date ON donations (status, donation_date);

CREATE INDEX idx_notifications_user_read ON notifications (user_id, read);
CREATE INDEX idx_notifications_user_created ON notifications (user_id, created_at DESC);
CREATE INDEX idx_device_tokens_user ON device_tokens (user_id);
