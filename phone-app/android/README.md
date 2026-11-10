# Android Studio (Java): Ứng dụng gọi hỗ trợ IoT

Đây là **ứng dụng Android native Java + XML**, nằm trong `phone-app/android`. Bản Web App cũ vẫn được giữ ở `phone-app/` để thử nghiệm nhanh; hai bản dùng cùng giao thức MQTT, không cần sửa ESP32 hoặc Broker.

## Công nghệ

- Android Java, XML, **minSdk 26**, `compileSdk 35`, `targetSdk 34`.
- Android Gradle Plugin **8.9.2**, Gradle **8.11.1**, JDK **17**.
- Eclipse Paho MQTT Java Client `1.2.5` (MQTT TCP).
- Không dùng Firebase/đám mây; máy tính cài Mosquitto làm Broker.

## Mở bằng Android Studio

Các file Android chính đã có sẵn: `settings.gradle`, `build.gradle`, `app/build.gradle`, manifest, layout XML, MainActivity.java. **Repository không chứa tệp nhị phân `gradle-wrapper.jar`**. Vì vậy, để chạy ngay bằng Android Studio:

1. Trong Android Studio, tạo một **Empty Views Activity** project mới, ngôn ngữ **Java**, `minSdk 26` (project mới sẽ có Gradle Wrapper hoàn chỉnh).
2. Đóng project và thay thế các file tương ứng bằng nội dung trong thư mục `phone-app/android` (giữ lại `gradlew`, `gradlew.bat` và `gradle/wrapper/gradle-wrapper.jar` từ project vừa tạo). Giữ package/namespace `com.example.iot.supportphone`.
3. Mở lại project và **Sync Project with Gradle Files**, để Android Studio cài SDK và các thư viện.
4. Chọn máy ảo Android (Emulator) hoặc cắm điện thoại thật và nhấn **Run**.

Nếu bạn đã cài Gradle 8.11.1 trong máy, có thể mở thẳng thư mục này và tạo wrapper bằng `gradle wrapper --gradle-version 8.11.1`, nhưng cách (1) thường dễ hơn.

## Chạy hệ thống

Trên máy tính Windows: cài Mosquitto và chạy cấu hình trong `pc-broker/mosquitto.conf`, tài khoản MQTT `support`, cổng TCP `1883`.

Trong ứng dụng:
- **IP Broker:** địa chỉ IPv4 LAN của máy tính, ví dụ `192.168.1.10`; **không** thêm `mqtt://`.
- **Port:** `1883`.
- **Username / password:** thông tin đã tạo cho Mosquitto.
- **Device ID:** `esp32-audio-01` (phải trùng code ESP32).
- **Location:** ví dụ `A101`.

Nhấn **Kết nối**. Khi thấy `Đã kết nối MQTT`, nhấn **Gọi hỗ trợ** hoặc **SOS khẩn cấp**. Ứng dụng Publish JSON lên `support/requests` với QoS 1; sau đó nhận ACK từ `support/ack` và thông báo:
- `PLAY_STARTED`: ESP32 đã ra lệnh phát MP3 tới DFPlayer (chưa chứng minh loa đã phát hết).
- `AUDIO_NOT_READY`: module âm thanh chưa sẵn sàng.
- `DEMO_ONLY`: ESP32 chạy chế độ mô phỏng.

Ứng dụng cảnh báo nếu sau 15 giây chưa có ACK.

**Android Emulator** chạy trên **cùng máy tính**: thử dùng host `10.0.2.2` để truy cập Mosquitto trên máy phát triển. **Điện thoại thật**: dùng IP LAN của máy tính, điện thoại và ESP32 phải cùng mạng Wi-Fi/truy cập được Broker; kiểm tra Windows Firewall Private Network.

## JSON MQTT gửi đi

```json
{
  "requestId": "REQ-1731234567890-a1b2c3",
  "deviceId": "esp32-audio-01",
  "from": "android",
  "location": "A101",
  "type": "SUPPORT",
  "timestamp": "2026-11-10T12:00:00.000Z"
}
```

`type` là `SUPPORT` hoặc `SOS`. ESP32 điều khiển `0001.mp3` hoặc `0002.mp3` trên thẻ microSD của DFPlayer Mini. Tên phòng được hiển thị trong log, **chưa được phát thành giọng nói**.

## Hạn chế

- Demo LAN hiện dùng MQTT **TCP không mã hóa** (tài khoản/mật khẩu đi qua mạng dưới dạng không mã hóa). Không expose port 1883 ra Internet; thực tế cần TLS, ACL, phân quyền, bảo mật.
- Ứng dụng giữ kết nối MQTT khi Activity đang chạy; **không cam kết nhận thông báo khi app bị đóng/chạy nền**. Nếu cần tính năng này hãy bổ sung foreground service hoặc dịch vụ push và chính sách quản lý Android phù hợp.
- Chưa thử build APK hoặc chạy thực nghiệm trên Android Studio tại thời điểm cập nhật repository.
- Bản mẫu học tập không thay thế hệ thống cảnh báo khẩn cấp có yêu cầu độ tin cậy cao.
