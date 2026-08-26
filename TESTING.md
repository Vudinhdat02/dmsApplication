# Testing

## Trạng thái hiện tại

Repository hiện chưa có bộ unit test hay instrumentation test hoàn chỉnh và không tuyên bố số lượng test chưa tồn tại. CI hiện kiểm tra restore và build sạch của Android app cùng backend.

## Kiểm tra tự động hiện có

    .\gradlew.bat clean assembleDebug
    dotnet build DMSServer/DMSbackend.slnx -c Release

## Checklist thủ công trước khi phát hành

- Đăng ký, đăng nhập và đăng xuất bằng Firebase Authentication.
- Chuyển Light/Dark Mode khi app đang mở; navbar và nội dung vẫn phản hồi.
- Cấp hoặc từ chối quyền camera, vị trí, thông báo.
- Hiệu chỉnh EAR; kiểm tra nhắm mắt, ngáp, quay đầu và mất khuôn mặt.
- Mất mạng khi tải ảnh; xác nhận WorkManager đồng bộ lại.
- Tài khoản chỉ xem hoặc xóa được ảnh của chính mình.
- Ảnh quá 72 giờ bị backend dọn.
- Cảnh báo va chạm ở ngưỡng 4.5 g, cooldown và email người liên hệ.
- Groq/Brevo dùng secret cục bộ, không ghi secret vào log.

## Kế hoạch kiểm thử

Ưu tiên bổ sung unit test cho EAR/MAR, hiệu chỉnh, va chạm và chính sách 72 giờ; instrumentation test cho điều hướng và đổi theme; integration test cho xác thực và phân quyền ảnh.
