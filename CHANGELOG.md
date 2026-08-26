# Changelog

Mọi thay đổi đáng chú ý được ghi tại đây theo Keep a Changelog; phiên bản tuân theo Semantic Versioning.

## [1.0.0] - 2026-08-26

### Added

- Android DMS với phân tích khuôn mặt trên thiết bị, hiệu chỉnh EAR và cảnh báo.
- Phát hiện va chạm, vị trí và email khẩn cấp.
- ASP.NET Core backend bảo vệ API key, Firebase JWT và giới hạn request.
- Sao lưu ảnh theo tài khoản, retry và dọn dữ liệu quá 72 giờ.
- Hỗ trợ Light/Dark Mode.
- Apache-2.0, third-party notices, model card, hướng dẫn build và CI.

### Changed

- Groq/Brevo được gọi qua backend thay vì nhúng key trong app.
- Bỏ Firebase Storage và Gemini Android không còn sử dụng.

### Security

- Secret dùng .NET User Secrets hoặc biến môi trường.
- Keystore, mật khẩu, database và ảnh backend bị loại khỏi Git.
