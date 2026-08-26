<div align="center">

# 🚘 DMS — Driver Monitoring System

### On-device driver monitoring, intelligent alerts, and emergency assistance

[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![.NET](https://img.shields.io/badge/.NET-10.0-512BD4?logo=dotnet&logoColor=white)](https://dotnet.microsoft.com/)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)
[![Release](https://img.shields.io/github/v/release/Vudinhdat02/dmsApplication?color=00BCD5)](https://github.com/Vudinhdat02/dmsApplication/releases)

**DMS** is an open-source driver monitoring system for Android. It uses the phone's front-facing camera and on-device facial landmark analysis to identify signs of fatigue or distraction, deliver immediate alerts, and coordinate secure online services through a dedicated ASP.NET Core backend.

</div>

---

## System Overview

~~~mermaid
flowchart LR
    A[Front Camera] --> B[On-device Analysis]
    B --> C{Driver State}
    C -->|Risk detected| D[Driver Alert]
    C -->|Event captured| E[Secure Backup]
    F[Motion Sensors + GPS] --> G[DMS Backend]
    E --> G
    G --> H[(72-hour Storage)]
    G --> I[Emergency Email]
    G --> J[AI Safety Insights]

    classDef primary fill:#00BCD5,color:#001F29,stroke:#007D8C,stroke-width:2px;
    classDef danger fill:#FFEBEE,color:#B71C1C,stroke:#E53935,stroke-width:2px;
    classDef server fill:#E8F5E9,color:#1B5E20,stroke:#43A047,stroke-width:2px;
    class A,B,C,E,H,J primary;
    class D,I danger;
    class G server;
~~~

## Highlights

| Area | Capability |
|---|---|
| Driver monitoring | Detects prolonged eye closure, yawning, head distraction, and face absence |
| Personal calibration | Adapts eye-aspect-ratio thresholds to the individual driver |
| Immediate alerts | Provides audible, visual, and vibration warnings on the device |
| Emergency assistance | Uses motion sensors and location data to support crash-alert workflows |
| Secure event backup | Uploads event images through an authenticated backend with offline retry |
| Data retention | Isolates data by account and automatically removes images older than 72 hours |
| Driving insights | Presents history, statistics, and AI-assisted safety recommendations |
| Adaptive interface | Supports system Light and Dark themes |

## Architecture

### Android Application

- Kotlin with Android SDK 26+ and an MVVM-oriented structure.
- CameraX for front-camera frame capture.
- MediaPipe Face Landmarker for on-device facial landmark inference.
- EAR, MAR, and head-pose heuristics for driver-state assessment.
- Room for local persistence and WorkManager for resilient background synchronization.
- Firebase Authentication and Firestore for identity and account-related data.
- Retrofit, OkHttp, and Glide for authenticated backend communication and image delivery.

### Backend

- ASP.NET Core on .NET 10 with a REST API.
- Firebase ID token validation for protected endpoints.
- Rate limiting, upload-size limits, and file-type validation.
- SQLite metadata with account-isolated image storage.
- Background cleanup for the 72-hour retention policy.
- Server-side integration with Groq and Brevo so provider API keys are never embedded in the Android application.

Backend documentation is available in [DMSServer/DMSbackend/README.md](DMSServer/DMSbackend/README.md).

## Repository Layout

~~~text
dmsApplication/
├── app/                         # Android application
│   └── src/main/
│       ├── java/.../data/       # Local storage, repositories, and API clients
│       ├── java/.../ml/         # Facial analysis and DMS algorithms
│       ├── java/.../ui/         # Activities, fragments, and view models
│       ├── assets/              # On-device model asset
│       └── res/                 # Layouts, themes, and Android resources
├── DMSServer/
│   └── DMSbackend/              # ASP.NET Core backend
├── docs/                        # Model and technical documentation
├── scripts/                     # Cross-platform build helpers
└── gradle/                      # Android build configuration
~~~

## Documentation

- [Building from source](BUILDING.md)
- [Backend configuration](DMSServer/DMSbackend/README.md)
- [Dependencies and bundling](DEPENDENCIES.md)
- [Third-party notices](THIRD_PARTY_NOTICES.md)
- [Model card](docs/MODEL_CARD.md)
- [Contribution guidelines](CONTRIBUTING.md)
- [Changelog](CHANGELOG.md)

## Security and Privacy

- Provider API keys are stored by the backend through user secrets or environment configuration and are not included in the APK.
- The Android application authenticates backend requests with Firebase ID tokens.
- Image access is scoped to the authenticated account.
- Event images are retained for no longer than 72 hours by the backend cleanup policy.
- Signing keys, local configuration, databases, uploaded images, certificates, and secret files are excluded from version control.
- Facial landmark inference runs on the Android device; see the [model card](docs/MODEL_CARD.md) for limitations and provenance.

## Open-Source License

Project-owned source code is licensed under the [Apache License 2.0](LICENSE). Third-party libraries, services, and model assets remain subject to their respective terms; see [NOTICE](NOTICE) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

> [!IMPORTANT]
> DMS is a research and driver-assistance project. It does not replace attentive driving, certified vehicle safety systems, emergency services, or professional medical advice.

---

<div align="center">

Built to explore practical, privacy-aware AI for safer driving.

</div>
