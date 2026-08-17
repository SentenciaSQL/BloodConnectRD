abstract final class ApiPaths {
  static const auth = '/api/auth';
  static const locations = '/api/locations';
  static const donors = '/api/donors';
  static const bloodRequests = '/api/blood-requests';
  static const donationCenters = '/api/donation-centers';
  static const donations = '/api/donations';
  static const notifications = '/api/notifications';
  static const conversations = '/api/conversations';
  static const messages = '/api/messages';
  static const devices = '/api/devices';
}

abstract final class BloodTypes {
  static const values = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];
}
