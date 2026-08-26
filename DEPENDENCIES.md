# Dependencies and bundling

DMS không copy mã nguồn thư viện bên thứ ba vào repository. Dependency Android nằm trong gradle/libs.versions.toml và app/build.gradle.kts; package backend nằm trong DMSServer/DMSbackend/DMSbackend.csproj.

## Kiểm kê đầy đủ

Android:

    .\gradlew.bat app:dependencies

Backend, gồm dependency bắc cầu:

    dotnet list DMSServer/DMSbackend/DMSbackend.csproj package --include-transitive

Dependency trực tiếp, giấy phép và tài sản được bundle được ghi tại [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Khi thêm hoặc nâng phiên bản, người đóng góp phải kiểm tra tương thích giấy phép và cập nhật tài liệu.

## Thành phần được bundle

APK bundle MediaPipe Face Landmarker model tại app/src/main/assets/face_landmarker.task. Groq, Brevo và Firebase server là dịch vụ bên ngoài, không được bundle. Repository không chứa API key, keystore phát hành, database người dùng hoặc ảnh đã tải lên.
