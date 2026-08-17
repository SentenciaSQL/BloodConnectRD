import 'package:intl/intl.dart';

final _dateFormat = DateFormat("d 'de' MMMM 'de' y", 'es_DO');
final _shortDateFormat = DateFormat('dd/MM/yyyy', 'es_DO');
final _dateTimeFormat = DateFormat('dd/MM/yyyy HH:mm', 'es_DO');

String formatDate(DateTime? date, {bool short = false}) {
  if (date == null) return 'No disponible';
  return (short ? _shortDateFormat : _dateFormat).format(date.toLocal());
}

String formatDateTime(DateTime? date) {
  if (date == null) return '';
  return _dateTimeFormat.format(date.toLocal());
}

String urgencyLabel(String urgency) => switch (urgency) {
  'CRITICAL' => 'Crítica',
  'HIGH' => 'Alta',
  'MEDIUM' => 'Media',
  _ => 'Baja',
};

String availabilityLabel(String status) => switch (status) {
  'AVAILABLE' => 'Disponible',
  'TEMPORARILY_UNAVAILABLE' => 'No disponible temporalmente',
  _ => 'Inactivo',
};

String centerTypeLabel(String type) => switch (type) {
  'HOSPITAL' => 'Hospital',
  'CLINIC' => 'Clínica',
  'BLOOD_BANK' => 'Banco de sangre',
  'MEDICAL_CENTER' => 'Centro médico',
  _ => 'Centro de donación',
};
