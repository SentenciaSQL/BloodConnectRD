typedef JsonMap = Map<String, dynamic>;

JsonMap asJson(Object? value) => Map<String, dynamic>.from(value as Map);

List<JsonMap> asJsonList(Object? value) =>
    (value as List? ?? const []).map(asJson).toList();

List<JsonMap> pageContent(Object? value) {
  final json = asJson(value);
  return asJsonList(json['content']);
}

class AppUser {
  const AppUser({
    required this.id,
    required this.firstName,
    required this.lastName,
    required this.email,
    required this.phone,
    required this.role,
  });

  factory AppUser.fromJson(JsonMap json) => AppUser(
    id: (json['id'] as num).toInt(),
    firstName: json['firstName']?.toString() ?? '',
    lastName: json['lastName']?.toString() ?? '',
    email: json['email']?.toString() ?? '',
    phone: json['phone']?.toString() ?? '',
    role: json['role']?.toString() ?? 'USER',
  );

  final int id;
  final String firstName;
  final String lastName;
  final String email;
  final String phone;
  final String role;

  String get fullName => '$firstName $lastName'.trim();

  bool get isDonor => role == 'DONOR';
  bool get isAdmin => role == 'ADMIN';
}

class AuthTokens {
  const AuthTokens({
    required this.accessToken,
    required this.refreshToken,
    required this.user,
  });

  factory AuthTokens.fromJson(JsonMap json) => AuthTokens(
    accessToken: json['accessToken'].toString(),
    refreshToken: json['refreshToken'].toString(),
    user: AppUser.fromJson(asJson(json['user'])),
  );

  final String accessToken;
  final String refreshToken;
  final AppUser user;
}

class Province {
  const Province({required this.id, required this.name});

  factory Province.fromJson(JsonMap json) => Province(
    id: (json['id'] as num).toInt(),
    name: json['name']?.toString() ?? '',
  );

  final int id;
  final String name;
}

class Municipality {
  const Municipality({
    required this.id,
    required this.provinceId,
    required this.name,
  });

  factory Municipality.fromJson(JsonMap json) => Municipality(
    id: (json['id'] as num).toInt(),
    provinceId: (json['provinceId'] as num).toInt(),
    name: json['name']?.toString() ?? '',
  );

  final int id;
  final int provinceId;
  final String name;
}

class DonorProfile {
  const DonorProfile({
    required this.id,
    required this.firstName,
    required this.lastName,
    required this.bloodType,
    required this.phone,
    required this.provinceId,
    required this.provinceName,
    required this.municipalityId,
    required this.municipalityName,
    required this.availability,
    this.birthDate,
    this.sex,
    this.sector,
    this.approximateAddress,
    this.latitude,
    this.longitude,
    this.lastDonationDate,
  });

  factory DonorProfile.fromJson(JsonMap json) => DonorProfile(
    id: (json['id'] as num).toInt(),
    firstName: json['firstName']?.toString() ?? '',
    lastName: json['lastName']?.toString() ?? '',
    bloodType: json['bloodType']?.toString() ?? '',
    phone: json['phone']?.toString() ?? '',
    provinceId: (json['provinceId'] as num?)?.toInt() ?? 0,
    provinceName: json['provinceName']?.toString() ?? '',
    municipalityId: (json['municipalityId'] as num?)?.toInt() ?? 0,
    municipalityName: json['municipalityName']?.toString() ?? '',
    availability: json['availability']?.toString() ?? 'INACTIVE',
    birthDate: DateTime.tryParse(json['birthDate']?.toString() ?? ''),
    sex: json['sex']?.toString(),
    sector: json['sector']?.toString(),
    approximateAddress: json['approximateAddress']?.toString(),
    latitude: (json['latitude'] as num?)?.toDouble(),
    longitude: (json['longitude'] as num?)?.toDouble(),
    lastDonationDate: DateTime.tryParse(
      json['lastDonationDate']?.toString() ?? '',
    ),
  );

  final int id;
  final String firstName;
  final String lastName;
  final String bloodType;
  final String phone;
  final int provinceId;
  final String provinceName;
  final int municipalityId;
  final String municipalityName;
  final String availability;
  final DateTime? birthDate;
  final String? sex;
  final String? sector;
  final String? approximateAddress;
  final double? latitude;
  final double? longitude;
  final DateTime? lastDonationDate;

  bool get isAvailable => availability == 'AVAILABLE';
  String get location => [
    municipalityName,
    provinceName,
  ].where((part) => part.isNotEmpty).join(', ');
}

class BloodRequestModel {
  const BloodRequestModel({
    required this.id,
    required this.createdById,
    required this.patientName,
    required this.bloodType,
    required this.unitsRequired,
    required this.completedUnits,
    required this.pendingUnits,
    required this.progressPercent,
    required this.hospital,
    required this.provinceName,
    required this.municipalityName,
    required this.address,
    required this.deadline,
    required this.description,
    required this.contactPhone,
    required this.urgency,
    required this.status,
    this.distanceKm,
  });

  factory BloodRequestModel.fromJson(JsonMap json) {
    final unitsRequired = (json['unitsRequired'] as num?)?.toInt() ?? 0;
    final completedUnits = (json['completedUnits'] as num?)?.toInt() ?? 0;
    final computedPending = unitsRequired - completedUnits;
    final pendingUnits =
        (json['pendingUnits'] as num?)?.toInt() ??
        (computedPending < 0 ? 0 : computedPending);
    final computedPercent = unitsRequired == 0
        ? 0
        : ((completedUnits * 100) / unitsRequired).round();
    final progressPercent =
        (json['progressPercent'] as num?)?.toInt() ??
        (computedPercent < 0 ? 0 : (computedPercent > 100 ? 100 : computedPercent));
    return BloodRequestModel(
      id: (json['id'] as num).toInt(),
      createdById: (json['createdById'] as num?)?.toInt() ?? 0,
      patientName: json['patientName']?.toString() ?? '',
      bloodType: json['bloodType']?.toString() ?? '',
      unitsRequired: unitsRequired,
      completedUnits: completedUnits,
      pendingUnits: pendingUnits,
      progressPercent: progressPercent,
      hospital: json['hospital']?.toString() ?? '',
      provinceName: json['provinceName']?.toString() ?? '',
      municipalityName: json['municipalityName']?.toString() ?? '',
      address: json['address']?.toString() ?? '',
      deadline: DateTime.tryParse(json['deadline']?.toString() ?? ''),
      description: json['description']?.toString() ?? '',
      contactPhone: json['contactPhone']?.toString() ?? '',
      urgency: json['urgency']?.toString() ?? 'LOW',
      status: json['status']?.toString() ?? 'OPEN',
      distanceKm: (json['approximateDistanceKm'] as num?)?.toDouble(),
    );
  }

  final int id;
  final int createdById;
  final String patientName;
  final String bloodType;
  final int unitsRequired;
  final int completedUnits;
  final int pendingUnits;
  final int progressPercent;
  final String hospital;
  final String provinceName;
  final String municipalityName;
  final String address;
  final DateTime? deadline;
  final String description;
  final String contactPhone;
  final String urgency;
  final String status;
  final double? distanceKm;

  String get location => [
    municipalityName,
    provinceName,
  ].where((part) => part.isNotEmpty).join(', ');

  String get progressLabel =>
      '$completedUnits de $unitsRequired unidades ($progressPercent%)';
}

class DonationCenterModel {
  const DonationCenterModel({
    required this.id,
    required this.name,
    required this.type,
    required this.provinceName,
    required this.municipalityName,
    required this.address,
    required this.phone,
    required this.schedule,
    this.latitude,
    this.longitude,
    this.distanceKm,
  });

  factory DonationCenterModel.fromJson(JsonMap json) => DonationCenterModel(
    id: (json['id'] as num).toInt(),
    name: json['name']?.toString() ?? '',
    type: json['type']?.toString() ?? 'OTHER',
    provinceName: json['provinceName']?.toString() ?? '',
    municipalityName: json['municipalityName']?.toString() ?? '',
    address: json['address']?.toString() ?? '',
    phone: json['phone']?.toString() ?? '',
    schedule: json['schedule']?.toString() ?? '',
    latitude: (json['latitude'] as num?)?.toDouble(),
    longitude: (json['longitude'] as num?)?.toDouble(),
    distanceKm: (json['approximateDistanceKm'] as num?)?.toDouble(),
  );

  final int id;
  final String name;
  final String type;
  final String provinceName;
  final String municipalityName;
  final String address;
  final String phone;
  final String schedule;
  final double? latitude;
  final double? longitude;
  final double? distanceKm;
}

class DonationModel {
  const DonationModel({
    required this.id,
    required this.donorUserId,
    required this.donorName,
    required this.centerName,
    required this.date,
    required this.units,
    required this.confirmedUnits,
    required this.status,
    required this.notes,
  });

  factory DonationModel.fromJson(JsonMap json) => DonationModel(
    id: (json['id'] as num).toInt(),
    donorUserId: (json['donorUserId'] as num?)?.toInt() ?? 0,
    donorName: json['donorName']?.toString() ?? '',
    centerName: json['donationCenterName']?.toString() ?? '',
    date: DateTime.tryParse(json['donationDate']?.toString() ?? ''),
    units: (json['units'] as num?)?.toInt() ?? 0,
    confirmedUnits: (json['confirmedUnits'] as num?)?.toInt() ?? 0,
    status: json['status']?.toString() ?? '',
    notes: json['notes']?.toString() ?? '',
  );

  final int id;
  final int donorUserId;
  final String donorName;
  final String centerName;
  final DateTime? date;
  final int units;
  final int confirmedUnits;
  final String status;
  final String notes;

  bool get isPendingConfirmation =>
      status == 'REPORTED' || status == 'PARTIALLY_CONFIRMED';

  String get statusLabel => switch (status) {
    'REPORTED' => 'Reportada',
    'PARTIALLY_CONFIRMED' => 'Confirmada parcialmente',
    'CONFIRMED' => 'Confirmada',
    'CANCELLED' => 'Cancelada',
    'COMPLETED' => 'Completada',
    _ => status,
  };
}

class DonationHistory {
  const DonationHistory({
    required this.totalDonations,
    required this.totalUnits,
    required this.orientationNote,
    required this.history,
    this.lastDonation,
    this.estimatedNextDate,
  });

  factory DonationHistory.fromJson(JsonMap json) => DonationHistory(
    totalDonations: (json['totalDonations'] as num?)?.toInt() ?? 0,
    totalUnits: (json['totalUnits'] as num?)?.toInt() ?? 0,
    lastDonation: DateTime.tryParse(json['lastDonation']?.toString() ?? ''),
    estimatedNextDate: DateTime.tryParse(
      json['estimatedNextDate']?.toString() ?? '',
    ),
    orientationNote: json['orientationNote']?.toString() ?? '',
    history: asJsonList(json['history']).map(DonationModel.fromJson).toList(),
  );

  final int totalDonations;
  final int totalUnits;
  final DateTime? lastDonation;
  final DateTime? estimatedNextDate;
  final String orientationNote;
  final List<DonationModel> history;
}

class NotificationModel {
  const NotificationModel({
    required this.id,
    required this.title,
    required this.message,
    required this.type,
    required this.isRead,
    required this.createdAt,
    this.resourceType,
    this.resourceId,
  });

  factory NotificationModel.fromJson(JsonMap json) => NotificationModel(
    id: (json['id'] as num).toInt(),
    title: json['title']?.toString() ?? '',
    message: json['message']?.toString() ?? '',
    type: json['type']?.toString() ?? '',
    isRead: json['read'] == true,
    createdAt: DateTime.tryParse(json['createdAt']?.toString() ?? ''),
    resourceType: json['resourceType']?.toString(),
    resourceId: (json['resourceId'] as num?)?.toInt(),
  );

  final int id;
  final String title;
  final String message;
  final String type;
  final bool isRead;
  final DateTime? createdAt;
  final String? resourceType;
  final int? resourceId;
}
