import 'package:dio/dio.dart';

sealed class AppException implements Exception {
  const AppException(this.message, {this.statusCode});

  final String message;
  final int? statusCode;

  @override
  String toString() => message;
}

class NetworkException extends AppException {
  const NetworkException()
    : super(
        'No pudimos conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.',
      );
}

class UnauthorizedException extends AppException {
  const UnauthorizedException([
    super.message = 'Tu sesión venció. Inicia sesión nuevamente.',
  ]) : super(statusCode: 401);
}

class ForbiddenException extends AppException {
  const ForbiddenException([
    super.message = 'No tienes permisos para realizar esta acción.',
  ]) : super(statusCode: 403);
}

class NotFoundException extends AppException {
  const NotFoundException([
    super.message = 'No encontramos el recurso solicitado.',
  ]) : super(statusCode: 404);
}

class ValidationException extends AppException {
  const ValidationException([
    super.message = 'Revisa los datos ingresados e inténtalo de nuevo.',
  ]) : super(statusCode: 422);
}

class ServerException extends AppException {
  const ServerException([
    super.message = 'Ocurrió un problema en el servidor. Inténtalo más tarde.',
  ]) : super(statusCode: 500);
}

AppException mapDioException(DioException error) {
  final response = error.response;
  if (response == null ||
      error.type == DioExceptionType.connectionError ||
      error.type == DioExceptionType.connectionTimeout ||
      error.type == DioExceptionType.receiveTimeout ||
      error.type == DioExceptionType.sendTimeout) {
    return const NetworkException();
  }

  final status = response.statusCode;
  final message = _safeMessage(response.data);
  return switch (status) {
    400 || 409 || 422 => ValidationException(message),
    401 => UnauthorizedException(message),
    403 => ForbiddenException(message),
    404 => NotFoundException(message),
    _ when status != null && status >= 500 => const ServerException(),
    _ => ServerException(
      message == _fallbackMessage ? _fallbackMessage : message,
    ),
  };
}

const _fallbackMessage = 'No fue posible completar la solicitud.';

String _safeMessage(Object? data) {
  if (data case {'message': final Object value}) {
    final message = value.toString().trim();
    if (message.isNotEmpty) return message;
  }
  return _fallbackMessage;
}

String friendlyError(Object error) {
  if (error is AppException) return error.message;
  return 'Ocurrió un error inesperado. Inténtalo de nuevo.';
}
