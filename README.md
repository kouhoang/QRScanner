# QRScanner

QRScanner là một ứng dụng Android cho phép người dùng quét mã QR bằng cách sử dụng camera của thiết bị hoặc hình ảnh từ thư viện. Ứng dụng xử lý mã QR và hiển thị nội dung của chúng, chẳng hạn như URL, trong một hộp thoại tùy chỉnh, cung cấp tùy chọn mở liên kết trực tiếp.

## Tính năng

- **Quét mã QR bằng camera**: Sử dụng camera của thiết bị để quét mã QR trong thời gian thực.
- **Quét mã QR từ thư viện ảnh**: Chọn hình ảnh từ thư viện và quét mã QR.
- **Hộp thoại kết quả mã QR tùy chỉnh**: Hiển thị nội dung mã QR được phát hiện trong một hộp thoại với tùy chọn mở URL.
- **Lưu ảnh đã chụp**: Lưu ảnh đã chụp vào thư mục Tải xuống của thiết bị.

## Quyền truy cập

Ứng dụng yêu cầu các quyền sau:

- `CAMERA`: Để truy cập camera của thiết bị cho việc quét mã QR.
- `READ_EXTERNAL_STORAGE`: Để đọc hình ảnh từ thư viện.
- `WRITE_EXTERNAL_STORAGE`: Để lưu ảnh vào bộ nhớ thiết bị.

## Cài đặt và sử dụng

1. Clone repository này về máy của bạn:

    ```sh
    git clone https://github.com/kouhoang/QRScanner.git
    ```

2. Mở project trong Android Studio.
3. Kết nối thiết bị Android của bạn hoặc sử dụng trình giả lập.
4. Chạy ứng dụng trên thiết bị hoặc trình giả lập.

## Hướng dẫn sử dụng

1. **Quét mã QR bằng camera**:
   - Mở ứng dụng và cho phép truy cập camera.
   - Mã QR sẽ được quét tự động khi xuất hiện trong khung camera.

2. **Quét mã QR từ thư viện ảnh**:
   - Nhấn nút "Library" để chọn ảnh từ thư viện.
   - Ứng dụng sẽ tự động quét mã QR trong ảnh đã chọn. (Tuy nhiên với trường hợp ảnh mã QR chụp thẳng trong môi trường thực tế vẫn chưa nhận diện được).

3. **Xem và mở liên kết từ mã QR**:
   - Khi mã QR chứa URL được phát hiện, một hộp thoại sẽ hiển thị URL đó.
   - Nhấn nút "Open Link" để mở URL trong trình duyệt.
