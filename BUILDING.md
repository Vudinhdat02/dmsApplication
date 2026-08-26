# Building DMS from source

Tài liệu này mô tả cách dựng Android app và ASP.NET Core backend hoàn toàn từ source bằng công cụ dòng lệnh. Android Studio và Visual Studio chỉ là lựa chọn hỗ trợ.

## Yêu cầu

- Git, JDK 17.
- Android SDK Platform 36 và Build-Tools tương thích.
- .NET SDK 10.
- Firebase project và file app/google-services.json.
- API key Groq/Brevo chỉ cần khi chạy chức năng trực tuyến, không cần để biên dịch.

Kiểm tra công cụ:

    java -version
    dotnet --info
    .\gradlew.bat --version

## Clone và cấu hình Android

    git clone https://github.com/Vudinhdat02/dmsApplication.git
    cd dmsApplication

Đặt file Firebase của project riêng tại app/google-services.json. Tạo hoặc cập nhật local.properties; file này bị Git bỏ qua:

    sdk.dir=C\:\\Users\\YOUR_NAME\\AppData\\Local\\Android\\Sdk
    SERVER_BASE_URL=https://your-backend.example/

Android Emulator gọi backend trên máy phát triển bằng SERVER_BASE_URL=http://10.0.2.2:5078/.

## Build Android

Windows:

    .\gradlew.bat clean assembleDebug

Linux/macOS:

    chmod +x gradlew
    ./gradlew clean assembleDebug

APK debug nằm tại app/build/outputs/apk/debug/app-debug.apk.

## Cấu hình và build backend

Không ghi API key vào appsettings*.json. Tại thư mục repository:

    dotnet user-secrets set --project DMSServer/DMSbackend/DMSbackend.csproj "Groq:ApiKey" "YOUR_GROQ_API_KEY"
    dotnet user-secrets set --project DMSServer/DMSbackend/DMSbackend.csproj "Brevo:ApiKey" "YOUR_BREVO_API_KEY"
    dotnet user-secrets set --project DMSServer/DMSbackend/DMSbackend.csproj "Brevo:SenderEmail" "YOUR_VERIFIED_SENDER_EMAIL"
    dotnet restore DMSServer/DMSbackend.slnx
    dotnet build DMSServer/DMSbackend.slnx --configuration Release --no-restore
    dotnet run --project DMSServer/DMSbackend/DMSbackend.csproj

Ở Development: health http://localhost:5078/health, Swagger http://localhost:5078/swagger.

Triển khai độc lập:

    dotnet publish DMSServer/DMSbackend/DMSbackend.csproj -c Release -o publish/DMSbackend

## Ký APK release

Tạo keystore riêng và lưu ngoài Git. Sao chép keystore.properties.example thành keystore.properties, điền đường dẫn, alias và mật khẩu. File cấu hình thật và keystore đều bị .gitignore loại trừ.

    .\gradlew.bat clean assembleRelease

Nếu chưa có keystore.properties, Gradle tạo APK release chưa ký. Không chia sẻ keystore hoặc mật khẩu.

## Build sạch tối thiểu

Từ clone sạch, phải chạy thành công:

    dotnet restore DMSServer/DMSbackend.slnx
    dotnet build DMSServer/DMSbackend.slnx -c Release --no-restore
    .\gradlew.bat clean assembleDebug

Có thể dùng scripts/build.ps1 hoặc scripts/build.sh. Dependency cần Internet ở lần restore đầu tiên.
