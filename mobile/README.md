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
flutter pub get
flutter run \
  --dart-define=API_BASE_URL=http://localhost:8080 \
  --dart-define=ENV=development
  
flutter build ipa --release --dart-define=API_BASE_URL=https://bloodconnectrd-production.up.railway.app
```

### Android (emulador)
```bash
cd mobile
flutter pub get
flutter run \
  --dart-define=API_BASE_URL=http://10.0.2.2:8080 \
  --dart-define=ENV=development
  
flutter build appbundle --release --dart-define=API_BASE_URL=https://bloodconnectrd-production.up.railway.app
```

### iPhone / Android físico
Usa la IP LAN de tu Mac/PC (no `localhost`):

```bash
flutter run \
  --dart-define=API_BASE_URL=http://192.168.x.x:8080 \
  --dart-define=ENV=development
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
  --dart-define=ENV=production
```

### Builds de release
iOS:
```bash
flutter build ipa \
  --dart-define=API_BASE_URL=https://api.tudominio.com \
  --dart-define=ENV=production
```

Android:
```bash
flutter build appbundle \
  --dart-define=API_BASE_URL=https://api.tudominio.com \
  --dart-define=ENV=production
```

Notas:
- `ENV=production` marca el ambiente; la URL real la define `API_BASE_URL`.
- Si cambias de backend, vuelve a compilar con el nuevo `--dart-define` (no basta reiniciar la app ya instalada con otra URL).
- En CI/CD, inyecta `API_BASE_URL` / `ENV` con `--dart-define`. La key de Maps de Android se inyecta con la variable de entorno `MAPS_API_KEY` (no uses `--dart-define` para la key).

## Google Maps

El mapa de **Centros** usa `google_maps_flutter`. Las API keys viven solo en archivos nativos locales (no en Dart, no en Git).

Usa **dos keys distintas**, cada una restringida a su plataforma.

| Plataforma | Identificador real del proyecto | API de Google Cloud | Dónde va la key |
|------------|---------------------------------|---------------------|-----------------|
| Android | `applicationId` `com.bloodconnect.bloodconnect_rd` | Maps SDK for Android | `android/local.properties` → `MAPS_API_KEY` |
| iOS | bundle `com.bloodconnect.bloodconnectRd` | Maps SDK for iOS | `ios/Flutter/Secrets.xcconfig` → `MAPS_API_KEY_IOS` |

No uses una API key global sin restricciones. No habilites Places u otras APIs salvo que BloodConnect las utilice (hoy el mapa de centros no usa Places).

### 1. Configuración local después de clonar

```bash
cd mobile
# Android: añade MAPS_API_KEY a android/local.properties (Flutter ya crea ese archivo).
# Ver android/local.properties.example
cp ios/Flutter/Secrets.xcconfig.example ios/Flutter/Secrets.xcconfig
# Edita Secrets.xcconfig con la key de iOS (no la de Android)
flutter pub get
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080 --dart-define=ENV=development
```

### 2. Android — `MAPS_API_KEY`

El manifiesto usa un placeholder, no una key escrita a mano:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="${MAPS_API_KEY}" />
```

Añade **solo esta línea** a `android/local.properties` (no borres `sdk.dir` ni `flutter.sdk`):

```properties
MAPS_API_KEY=YOUR_ANDROID_GOOGLE_MAPS_API_KEY
```

`android/local.properties` ya está en `.gitignore`. En CI puedes inyectar `MAPS_API_KEY` como variable de entorno; Gradle también lee `GOOGLE_MAPS_API_KEY` como respaldo.

### 3. Google Cloud — Android

1. Google Cloud Console → APIs y servicios → Biblioteca → habilita **Maps SDK for Android**.
2. Credenciales → crear API key **BloodConnect Android**.
3. Restricción de aplicación: **Aplicaciones para Android**.
4. Nombre del paquete: `com.bloodconnect.bloodconnect_rd`.
5. SHA-1: el de debug (desarrollo) y el de Play App Signing (producción). Ver abajo.
6. Restricción de API: **Maps SDK for Android** únicamente.

### 4. SHA-1 de desarrollo (debug)

El keystore de debug es el predeterminado de Android. En macOS/Linux:

```bash
keytool -list -v \
  -keystore ~/.android/debug.keystore \
  -alias androiddebugkey \
  -storepass android \
  -keypass android
```

En Windows (PowerShell):

```powershell
keytool -list -v `
  -keystore "$env:USERPROFILE\.android\debug.keystore" `
  -alias androiddebugkey `
  -storepass android `
  -keypass android
```

También, desde `mobile/android` (tras un `flutter build` que genere el wrapper de Gradle):

```powershell
cd android
.\gradlew signingReport
```

En Linux/macOS: `./gradlew signingReport`.

Usa el SHA-1 del variant **debug**. Pégalo en Google Cloud Console → la key de Android → huellas SHA-1.

### 5. Google Play / producción

Play App Signing firma de nuevo el AAB. Si solo autorizas el SHA-1 de debug, el mapa funciona con `flutter run` y falla en la ficha de Play (pantalla gris / `REQUEST_DENIED`).

1. Google Play Console → BloodConnect → Configuración → Integridad de la app.
2. Copia el SHA-1 de **Certificado de la clave de firma de la app**.
3. Añádelo a la misma API key de Android (junto al SHA-1 debug).
4. Si pruebas AABs locales firmados con tu upload key, autoriza también el SHA-1 de **Certificado de la clave de carga**.

Con esos SHA-1, el mapa debe funcionar en:

- `flutter run` (debug)
- builds locales
- la versión instalada desde Google Play

### 6. iOS — `MAPS_API_KEY_IOS`

1. Google Cloud Console → habilita **Maps SDK for iOS**.
2. Crea otra API key **BloodConnect iOS** (no reutilices la de Android).
3. Restricción de aplicación: **Aplicaciones para iOS**.
4. Identificador de paquete: `com.bloodconnect.bloodconnectRd`.
5. Restricción de API: **Maps SDK for iOS** únicamente.

```bash
cp ios/Flutter/Secrets.xcconfig.example ios/Flutter/Secrets.xcconfig
# MAPS_API_KEY_IOS=...
cd ios && pod install && cd ..
```

`AppDelegate.swift` lee `GMSApiKey` desde `Info.plist` (`$(MAPS_API_KEY_IOS)`) y llama a `GMSServices.provideAPIKey`. `Secrets.xcconfig` no se sube a git.

### 7. Cómo comprobar que el mapa funciona

1. Arranca el backend y la app.
2. Abre la pestaña **Centros**.
3. Pulsa el icono de mapa.
4. Debe verse el mapa con marcadores (si los centros tienen coordenadas), no una pantalla gris.

Si ves mapa gris, `REQUEST_DENIED` o errores de autorización:

- Android: `MAPS_API_KEY` en `local.properties`, paquete `com.bloodconnect.bloodconnect_rd`, SHA-1 debug y/o Play, API **Maps SDK for Android**.
- iOS: `Secrets.xcconfig` con `MAPS_API_KEY_IOS`, bundle `com.bloodconnect.bloodconnectRd`, API **Maps SDK for iOS**.
- Nunca pases la key por `--dart-define` ni la escribas en Dart.

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
- Centros (lista y mapa; las keys de Maps son nativas, no van en Dart)
- Perfil y disponibilidad
- Historial de donaciones
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
