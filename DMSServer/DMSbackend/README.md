# DMS Backend

Backend ASP.NET Core .NET 10 cho ứng dụng DMS.

## Bảo mật

- Mọi API dưới `/api` yêu cầu Firebase ID token.
- `/health` là endpoint công khai duy nhất.
- Swagger chỉ bật trong môi trường `Development`.
- Groq và Brevo API key nằm trong .NET User Secrets hoặc biến môi trường.
- Ảnh được phân vùng theo Firebase UID; người dùng không thể đọc ảnh của UID khác.
- Ảnh quá 72 giờ được tác vụ nền xóa tự động.

## Cấu hình secrets khi phát triển

Project dùng `.NET User Secrets`. File secret thật được lưu trong hồ sơ Windows,
nằm ngoài repository Git và không được sao chép vào thư mục project.

Trong Visual Studio, nhấp chuột phải project `DMSbackend` rồi chọn
**Manage User Secrets**. Nội dung `secrets.json` trên máy có dạng:

```json
{
  "Groq:ApiKey": "YOUR_GROQ_API_KEY",
  "Brevo:ApiKey": "YOUR_BREVO_API_KEY"
}
```

Hoặc cấu hình bằng PowerShell tại thư mục chứa `DMSbackend.csproj`:

```powershell
dotnet user-secrets set "Groq:ApiKey" "YOUR_GROQ_API_KEY"
dotnet user-secrets set "Brevo:ApiKey" "YOUR_BREVO_API_KEY"
```

Không tạo file chứa API key bên trong repository. Không ghi key thật vào
`appsettings.json`, `appsettings.Development.json` hoặc file `.http`.

## Chạy từ Visual Studio

1. Mở `DMSServer/DMSbackend.slnx`.
2. Chọn project `DMSbackend` làm Startup Project.
3. Chọn launch profile `https` hoặc `http`.
4. Nhấn **F5** hoặc nút **Run**. Backend dừng khi bạn nhấn Stop trong Visual Studio.

Để điện thoại truy cập qua Internet chỉ trong lúc Visual Studio chạy:

1. Đăng nhập Visual Studio bằng tài khoản Microsoft/GitHub đã dùng cho Dev Tunnel.
2. Trong menu cạnh nút Run, chọn **Dev Tunnels**.
3. Chọn tunnel persistent hiện có, hoặc tạo tunnel `Public` mới.
4. Nhấn Run. Tunnel chỉ chuyển tiếp khi project đang chạy trong Visual Studio.

Nếu tạo tunnel URL mới, phải cập nhật `SERVER_BASE_URL` của Android rồi build lại app.

## Chạy bằng dòng lệnh (tùy chọn)

```powershell
dotnet run --launch-profile http
```

- Health: `http://127.0.0.1:5078/health`
- Swagger: `http://127.0.0.1:5078/swagger`

Không đưa thư mục `App_Data`, database SQLite, file ảnh hoặc secrets lên Git.
Thư mục `Data` là mã nguồn Entity Framework và phải được commit.
