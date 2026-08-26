# Third-party notices

Apache-2.0 áp dụng cho mã nguồn do dự án DMS phát triển, không tự động thay thế giấy phép hoặc điều khoản của thành phần bên thứ ba.

| Thành phần | Phiên bản | Vai trò | Giấy phép/điều khoản |
|---|---:|---|---|
| Kotlin, AndroidX, CameraX, Room, WorkManager, Material Components | theo Gradle | Android/runtime/UI | Apache-2.0 |
| MediaPipe Tasks Vision | 0.10.14 | Face landmark inference | Apache-2.0 |
| Retrofit | 2.9.0 | HTTP client | Apache-2.0 |
| OkHttp | 4.12.0 | HTTP transport | Apache-2.0 |
| Kotlin Coroutines Play Services | 1.7.3 | coroutine adapters | Apache-2.0 |
| Glide | 4.16.0 | tải và hiển thị ảnh | BSD-2-Clause |
| Firebase Android SDK, Google Play Services | theo Gradle và BoM 33.1.2 | auth, database, location | Điều khoản Google/Firebase |
| ASP.NET Core JwtBearer | 10.0.11 | JWT authentication | MIT |
| Entity Framework Core SQLite | 10.0.11 | database access | MIT |
| Swashbuckle.AspNetCore | 10.2.3 | OpenAPI/Swagger | MIT |
| SQLite | qua EF Core | database engine | Public Domain |

Nguồn chính thức:

- Android: https://source.android.com/docs/setup/about/licenses
- AndroidX: https://github.com/androidx/androidx
- MediaPipe: https://github.com/google-ai-edge/mediapipe
- Retrofit: https://github.com/square/retrofit
- OkHttp: https://github.com/square/okhttp
- Glide: https://github.com/bumptech/glide
- Firebase Android SDK: https://github.com/firebase/firebase-android-sdk
- Mockito: https://github.com/mockito/mockito
- ASP.NET Core: https://github.com/dotnet/aspnetcore
- Entity Framework Core: https://github.com/dotnet/efcore
- Swashbuckle: https://github.com/domaindrivendev/Swashbuckle.AspNetCore
- SQLite: https://sqlite.org/copyright.html

## Model được bundle

File app/src/main/assets/face_landmarker.task có SHA-256:

    64184E229B263107BC2B804C6625DB1341FF2BB731874B0BCC2FE6544E0BC9FF

Nguồn tham chiếu: https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task

MediaPipe source code được phát hành theo Apache-2.0. Tuy nhiên, giấy phép framework không mặc nhiên cấp quyền cho mọi model hoặc data. Người phân phối phải xác nhận điều khoản model tại nguồn trước khi phân phối APK. Xem [docs/MODEL_CARD.md](docs/MODEL_CARD.md).

## Dịch vụ bên ngoài

Groq, Brevo và Firebase được gọi qua API, không phải mã nguồn bundle. Người triển khai tự cung cấp tài khoản/API key và tuân thủ điều khoản, hạn mức, chính sách dữ liệu của từng dịch vụ.

Danh sách trên tập trung vào dependency trực tiếp. Dependency bắc cầu được xuất bằng lệnh trong [DEPENDENCIES.md](DEPENDENCIES.md); thông báo license trong artifact phải được giữ khi phân phối lại.
