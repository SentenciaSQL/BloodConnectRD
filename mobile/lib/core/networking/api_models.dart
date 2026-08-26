typedef JsonMap = Map<String, dynamic>;

JsonMap asJson(Object? value) => Map<String, dynamic>.from(value as Map);

List<JsonMap> asJsonList(Object? value) =>
    (value as List? ?? const []).map(asJson).toList();

List<JsonMap> pageContent(Object? value) {
  final json = asJson(value);
  return asJsonList(json['content']);
}

int readJsonInt(JsonMap json, List<String> keys, [int fallback = 0]) {
  for (final key in keys) {
    final value = json[key];
    if (value is num) return value.toInt();
    if (value is String) {
      final parsed = num.tryParse(value);
      if (parsed != null) return parsed.toInt();
    }
  }
  return fallback;
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
    this.provinceId,
    this.municipalityId,
    this.sector,
    this.reference,
    this.latitude,
    this.longitude,
  });

  factory BloodRequestModel.fromJson(
      JsonMap json,
      ) {
    final unitsRequired = readJsonInt(
      json,
      ['unitsRequired', 'requiredUnits'],
    );

    final completedUnits = readJsonInt(
      json,
      [
        'completedUnits',
        'confirmedUnits',
        'receivedUnits',
        'unitsReceived',
      ],
    );

    final computedPending =
        unitsRequired - completedUnits;

    final pendingUnits = readJsonInt(
      json,
      ['pendingUnits'],
      computedPending < 0 ? 0 : computedPending,
    );

    return BloodRequestModel(
      id: (json['id'] as num).toInt(),
      createdById: readJsonInt(
        json,
        ['createdById'],
      ),
      patientName:
      json['patientName']?.toString() ?? '',
      bloodType:
      json['bloodType']?.toString() ?? '',
      unitsRequired: unitsRequired,
      completedUnits: completedUnits,
      pendingUnits: pendingUnits,
      hospital:
      json['hospital']?.toString() ?? '',
      provinceName:
      json['provinceName']?.toString() ?? '',
      municipalityName:
      json['municipalityName']?.toString() ?? '',
      address:
      json['address']?.toString() ?? '',
      deadline: DateTime.tryParse(
        json['deadline']?.toString() ?? '',
      ),
      description:
      json['description']?.toString() ?? '',
      contactPhone:
      json['contactPhone']?.toString() ?? '',
      urgency:
      json['urgency']?.toString() ?? 'LOW',
      status:
      json['status']?.toString() ?? 'OPEN',
      distanceKm:
      (json['approximateDistanceKm'] as num?)
          ?.toDouble(),
      provinceId:
      (json['provinceId'] as num?)?.toInt(),
      municipalityId:
      (json['municipalityId'] as num?)?.toInt(),
      sector: json['sector']?.toString(),
      reference: json['reference']?.toString(),
      latitude:
      (json['latitude'] as num?)?.toDouble(),
      longitude:
      (json['longitude'] as num?)?.toDouble(),
    );
  }

  final int id;
  final int createdById;
  final String patientName;
  final String bloodType;
  final int unitsRequired;
  final int completedUnits;
  final int pendingUnits;
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
  final int? provinceId;
  final int? municipalityId;
  final String? sector;
  final String? reference;
  final double? latitude;
  final double? longitude;

  bool get isExpired {
    final value = deadline;

    if (value == null) {
      return false;
    }

    return !value.isAfter(DateTime.now());
  }

  String get location {
    return [
      municipalityName,
      provinceName,
    ].where((part) => part.isNotEmpty).join(', ');
  }

  int get confirmedUnits => completedUnits;

  double get progress {
    if (unitsRequired <= 0) {
      return 0.0;
    }

    return (
        confirmedUnits.toDouble() /
            unitsRequired.toDouble()
    ).clamp(0.0, 1.0);
  }

  int get progressPercent {
    return (progress * 100).round();
  }

  String get progressLabel {
    return '$confirmedUnits de $unitsRequired '
        'unidades recibidas';
  }
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
    this.patientName,
    this.hospital,
    this.receiverName,
    this.bloodRequestId,
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
    patientName: json['patientName']?.toString(),
    hospital: json['hospital']?.toString(),
    receiverName: json['receiverName']?.toString(),
    bloodRequestId: (json['bloodRequestId'] as num?)?.toInt(),
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
  final String? patientName;
  final String? hospital;
  final String? receiverName;
  final int? bloodRequestId;

  bool get isPendingConfirmation =>
      status == 'REPORTED' || status == 'PARTIALLY_CONFIRMED';

  String get statusLabel => switch (status) {
    'REPORTED' => 'Pendiente de confirmación',
    'PARTIALLY_CONFIRMED' => 'Confirmada parcialmente',
    'CONFIRMED' || 'COMPLETED' => 'Confirmada',
    'CANCELLED' => 'Cancelada',
    _ => status,
  };

  String get title =>
      (hospital != null && hospital!.isNotEmpty)
          ? hospital!
          : (centerName.isEmpty ? 'Donación registrada' : centerName);
}

class DonationResponseModel {
  const DonationResponseModel({
    required this.id,
    required this.donorUserId,
    required this.donorName,
    required this.status,
    this.donorBloodType,
    this.message,
    this.createdAt,
  });

  factory DonationResponseModel.fromJson(JsonMap json) => DonationResponseModel(
    id: (json['id'] as num).toInt(),
    donorUserId: (json['donorUserId'] as num?)?.toInt() ?? 0,
    donorName: json['donorName']?.toString() ?? '',
    donorBloodType: json['donorBloodType']?.toString(),
    status: json['status']?.toString() ?? '',
    message: json['message']?.toString(),
    createdAt: DateTime.tryParse(json['createdAt']?.toString() ?? ''),
  );

  final int id;
  final int donorUserId;
  final String donorName;
  final String? donorBloodType;
  final String status;
  final String? message;
  final DateTime? createdAt;

  bool get isActiveOffer => status == 'PENDING' || status == 'ACCEPTED';

  String get statusLabel => switch (status) {
    'PENDING' || 'ACCEPTED' => 'Interesado en ayudar',
    'REJECTED' => 'No seleccionado',
    'COMPLETED' => 'Donación reportada',
    'CANCELLED' => 'Cancelado',
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

class ConversationModel {
  const ConversationModel({
    required this.id,
    required this.bloodRequestId,
    required this.bloodRequestPatientName,
    required this.bloodRequestHospital,
    required this.otherUserId,
    required this.otherUserName,
    required this.unreadCount,
    this.bloodRequestBloodType,
    this.lastMessage,
    this.lastMessageAt,
  });

  factory ConversationModel.fromJson(JsonMap json) => ConversationModel(
    id: (json['id'] as num).toInt(),
    bloodRequestId: (json['bloodRequestId'] as num?)?.toInt() ?? 0,
    bloodRequestPatientName: json['bloodRequestPatientName']?.toString() ?? '',
    bloodRequestHospital: json['bloodRequestHospital']?.toString() ?? '',
    bloodRequestBloodType: json['bloodRequestBloodType']?.toString(),
    otherUserId: (json['otherUserId'] as num?)?.toInt() ?? 0,
    otherUserName: json['otherUserName']?.toString() ?? '',
    lastMessage: json['lastMessage']?.toString(),
    lastMessageAt: DateTime.tryParse(json['lastMessageAt']?.toString() ?? ''),
    unreadCount: (json['unreadCount'] as num?)?.toInt() ?? 0,
  );

  final int id;
  final int bloodRequestId;
  final String bloodRequestPatientName;
  final String bloodRequestHospital;
  final String? bloodRequestBloodType;
  final int otherUserId;
  final String otherUserName;
  final String? lastMessage;
  final DateTime? lastMessageAt;
  final int unreadCount;

  String get requestLabel {
    final bloodType = bloodRequestBloodType;
    return [
      'Solicitud de $bloodRequestPatientName',
      if (bloodType != null && bloodType.isNotEmpty) bloodType,
      bloodRequestHospital,
    ].where((part) => part.isNotEmpty).join(' · ');
  }

  String get preview =>
      (lastMessage == null || lastMessage!.isEmpty)
          ? 'Conversación iniciada. Escribe el primer mensaje.'
          : lastMessage!;
}

class ChatMessageModel {
  const ChatMessageModel({
    required this.id,
    required this.conversationId,
    required this.senderId,
    required this.senderName,
    required this.body,
    required this.mine,
    this.status = 'SENT',
    this.createdAt,
  });

  factory ChatMessageModel.fromJson(JsonMap json) => ChatMessageModel(
    id: (json['id'] as num).toInt(),
    conversationId: (json['conversationId'] as num?)?.toInt() ?? 0,
    senderId: (json['senderId'] as num?)?.toInt() ?? 0,
    senderName: json['senderName']?.toString() ?? '',
    body: json['body']?.toString() ?? '',
    mine: json['mine'] == true,
    status: json['status']?.toString() ?? 'SENT',
    createdAt: DateTime.tryParse(json['createdAt']?.toString() ?? ''),
  );

  final int id;
  final int conversationId;
  final int senderId;
  final String senderName;
  final String body;
  final bool mine;
  final String status;
  final DateTime? createdAt;

  String get statusMark => switch (status) {
    'READ' => '✓✓',
    'DELIVERED' => '✓✓',
    _ => '✓',
  };
}
