# Model card — MediaPipe Face Landmarker

## Tổng quan

DMS bundle face_landmarker.task để suy luận điểm mốc khuôn mặt ngay trên điện thoại. App không gửi khung camera lên Groq hoặc backend cho bước này.

- Nguồn: https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task
- Biến thể: float16, version path 1.
- SHA-256: 64184E229B263107BC2B804C6625DB1341FF2BB731874B0BCC2FE6544E0BC9FF.
- Runtime: MediaPipe Tasks Vision 0.10.14.
- Vị trí: app/src/main/assets/face_landmarker.task.

## Cách DMS dùng output

Landmark được dùng để tính EAR, MAR và hướng đầu. Ngưỡng mặc định hiện tại: EAR 0.16 (hiệu chỉnh 0.10–0.30), MAR 0.38, sai khác yaw/pitch 0.20/0.25. Đây là heuristic của ứng dụng, không phải chẩn đoán y tế hoặc bảo đảm an toàn.

## Giới hạn

Độ chính xác có thể giảm bởi thiếu sáng, rung, góc camera, kính, khẩu trang, che khuất, đặc điểm khuôn mặt, thiết bị yếu hoặc khuôn mặt ngoài khung hình. Người dùng phải hiệu chỉnh và không phụ thuộc duy nhất vào cảnh báo của app khi lái xe.

## Quyền riêng tư

Suy luận landmark diễn ra trên thiết bị. Chỉ ảnh sự kiện do luồng ứng dụng tạo mới được gửi lên backend có xác thực và lưu tối đa 72 giờ. Người triển khai phải công bố chính sách riêng tư phù hợp nơi phân phối.

## License và provenance

MediaPipe repository công bố Apache-2.0 cho source code. Điều khoản của model bundle có thể khác và không được suy ra chỉ từ giấy phép framework. Trước khi phân phối APK, maintainer phải đối chiếu checksum, kiểm tra model card hoặc điều khoản cập nhật của nhà cung cấp và lưu bằng chứng phiên bản.
