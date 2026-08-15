# BloodConnect RD — App Móvil

Flutter (Android / iOS) consumiendo la misma API Spring Boot.

## Stack

- Flutter / Dart / Material 3
- Riverpod / Dio / GoRouter
- flutter_secure_storage
- Freezed / json_serializable
- geolocator / google_maps_flutter
- Firebase Messaging / Crashlytics (opcionales en desarrollo)

## Ejecutar

Asegúrate de que el backend Spring Boot esté corriendo en el puerto **8080**.

### iOS (simulador)
```bash
cd mobile
flutter run \
  --dart-define=API_BASE_URL=http://localhost:8080 \
  --dart-define=ENV=development \
  --dart-define=GOOGLE_MAPS_API_KEY=AIza...
```

### Android (emulador)
```bash
cd mobile
flutter run \
  --dart-define=API_BASE_URL=http://10.0.2.2:8080 \
  --dart-define=ENV=development \
  --dart-define=GOOGLE_MAPS_API_KEY=AIza...
```

### iPhone / Android físico
Usa la IP LAN de tu Mac/PC (no `localhost`):

```bash
flutter run \
  --dart-define=API_BASE_URL=http://192.168.x.x:8080 \
  --dart-define=ENV=development \
  --dart-define=GOOGLE_MAPS_API_KEY=AIza...
```

Si no pasas `API_BASE_URL`, el default es:
- iOS → `http://localhost:8080`
- Android → `http://10.0.2.2:8080`

| Ambiente | API |
|----------|-----|
| Android emulator | `http://10.0.2.2:8080` |
| iOS simulator | `http://localhost:8080` |
| Dispositivo físico | `http://<IP-de-tu-Mac>:8080` |
| staging/production | HTTPS del backend |

**Importante iOS:** no uses `http://10.0.2.2:8080` (eso solo existe en el emulador Android). Sin `--dart-define=API_BASE_URL=...` correcto verás *“No pudimos conectar con el servidor…”*.

## Backend de producción / staging

La URL del API **no** se hardcodea en el código. Se pasa al compilar o ejecutar con `--dart-define=API_BASE_URL=...`.

La lee `lib/core/config/app_config.dart` (`String.fromEnvironment('API_BASE_URL')`).

Usa **HTTPS + dominio** (ej. `https://api.tudominio.com`), no una IP pública.

### Probar contra producción / staging
```bash
cd mobile
flutter run \
  --dart-define=API_BASE_URL=https://api.tudominio.com \
  --dart-define=ENV=production \
  --dart-define=GOOGLE_MAPS_API_KEY=AIza...
```

### Builds de release
iOS:
```bash
flutter build ipa \
  --dart-define=API_BASE_URL=https://api.tudominio.com \
  --dart-define=ENV=production \
  --dart-define=GOOGLE_MAPS_API_KEY=AIza...
```

Android:
```bash
flutter build appbundle \
  --dart-define=API_BASE_URL=https://api.tudominio.com \
  --dart-define=ENV=production \
  --dart-define=GOOGLE_MAPS_API_KEY=AIza...
```

Notas:
- `ENV=production` marca el ambiente; la URL real la define `API_BASE_URL`.
- Si cambias de backend, vuelve a compilar con el nuevo `--dart-define` (no basta reiniciar la app ya instalada con otra URL).
- En CI/CD, inyecta los mismos `--dart-define` en el job de build.

## Google Maps

La app muestra el mapa en **Centros** solo si recibe `GOOGLE_MAPS_API_KEY` por `--dart-define`.
Además, Android e iOS necesitan la misma key en el lado nativo.

### Google Cloud
1. Activa **Maps SDK for Android** y **Maps SDK for iOS**.
2. Crea una API key.
3. Restricciones recomendadas:
   - Android: package `com.bloodconnect.bloodconnect_rd` + SHA-1 de debug.
   - iOS: bundle ID `com.bloodconnect.bloodconnectRd`.

### Android
En `android/local.properties`:

```properties
GOOGLE_MAPS_API_KEY=AIza...
```

### iOS
```bash
cp ios/Flutter/Secrets.xcconfig.example ios/Flutter/Secrets.xcconfig
# Edita Secrets.xcconfig y pon tu key real
cd ios && pod install && cd ..
```

### Correr con Maps
Pasa también `--dart-define=GOOGLE_MAPS_API_KEY=AIza...` junto con el `API_BASE_URL` de la sección **Ejecutar**.

Si la app se cierra al abrir en iOS:
1. Abre `ios/Runner.xcworkspace` (no el `.xcodeproj`).
2. `cd ios && pod install && cd ..`
3. Confirma que `ios/Flutter/Secrets.xcconfig` tiene una key que empieza por `AIza`.
4. Firebase es opcional: sin `GoogleService-Info.plist` la app debe arrancar igual (sin push).
5. Mira el crash en Xcode → Window → Devices and Simulators → Open Console, o `flutter run -v`.

## Tabs

Inicio · Solicitudes · Donar · Centros · Perfil

## Funciones

- Login / registro con JWT en secure storage
- Refresh automático vía Dio interceptor
- Solicitudes, filtros, detalle, “Quiero ayudar”
- Crear solicitud (provincia → municipio)
- Centros (lista / mapa degradable sin API key)
- Perfil y disponibilidad
- Mis donaciones (ofrecimientos e historial)
- Notificaciones
- FCM: registra `POST /api/devices` si Firebase está configurado

## Arquitectura

```text
lib/core|features|shared
```

## Analizar

```bash
flutter analyze
```

Sin pruebas automatizadas (`*_test.dart` no forman parte del proyecto).
