# ESP32 project: Arduino + DFPlayer Mini + Loa

**Đây là Arduino Sketch cho ESP32 DevKit V1 thực tế**, khác với app điện thoại và phần mềm Broker. Thư mục sketch Arduino: `esp32_audio/`; mở `esp32_audio.ino` bằng Arduino IDE.

## Phần cứng

- ESP32 DevKit V1.
- Module DFPlayer Mini, thẻ microSD FAT32.
- Loa nhỏ phù hợp ngõ ra SPK1/SPK2 của DFPlayer (ví dụ 4Ω, 3W).
- LED trạng thái GPIO26 (nối tiếp trở hạn dòng 220Ω).
- Nguồn 5V đủ dòng cho DFPlayer; **phải nối chung GND**.

### Đấu dây

| ESP32 | DFPlayer Mini |
|---|---|
| 5V/VIN (nguồn 5V phù hợp) | VCC |
| GND | GND |
| GPIO17 (TX2) qua điện trở 1 kΩ | RX |
| GPIO16 (RX2) | TX |

Nối loa vào **SPK1** và **SPK2** của DFPlayer (không nối trực tiếp loa vào GPIO ESP32). Không nối nguồn 5V vào chân GPIO ESP32.

Thẻ microSD: tạo thư mục `/mp3`, chép hai tệp âm thanh **do bạn tự ghi**:
- `/mp3/0001.mp3`: “Có yêu cầu hỗ trợ.”
- `/mp3/0002.mp3`: “Có yêu cầu hỗ trợ khẩn cấp.”

ESP32 chọn file theo `type` MQTT (SUPPORT/SOS). Trường `location` được ghi trên Serial và trong ACK, **không được tự động phát thành giọng nói**. Muốn đọc tên từng phòng cần làm thêm file âm thanh theo phòng hoặc TTS.

## Arduino IDE

1. Cài board **esp32 by Espressif Systems**, chọn ESP32 Dev Module.
2. Cài thư viện **PubSubClient**, **ArduinoJson v6**, **DFRobotDFPlayerMini** qua Library Manager.
3. Sao chép `esp32_audio/secrets.example.h` thành `esp32_audio/secrets.h`, điền Wi-Fi thật, MQTT_HOST = IPv4 LAN máy tính, và tài khoản/mật khẩu Broker.
4. Mở `esp32_audio.ino`, chọn Port, Upload.
5. Mở Serial Monitor 115200 baud. Mong đợi: `DFPlayer ready`, `Connecting to MQTT ... connected`, `Subscribed: support/requests`.
6. Dùng điện thoại mở web app, gửi SUPPORT/SOS, nghe loa và xem `ACK sent`.

`secrets.h` đã được thêm vào `.gitignore` để không công khai thông tin riêng.

## Chế độ mô phỏng

Để **biên dịch với Wokwi mà không có DFPlayer**, đặt `#define USE_DFPLAYER 0` trước phần include, hoặc đổi mặc định từ 1 thành 0. Serial ghi `DEMO: Would play audio notification`, ACK là `DEMO_ONLY`; **chế độ này không phát âm thanh**.

Lưu ý Wokwi chạy trên môi trường mạng mô phỏng, thông thường **không thể truy cập trực tiếp IP riêng 192.168.x.x của máy tính** làm Broker. Muốn thử kiến trúc Broker tại PC và loa thật, hãy nạp chương trình vào ESP32 vật lý. Đừng coi `DEMO_ONLY` là đã phát âm thanh.

## Dữ liệu MQTT

- Subscribe: `support/requests`, QoS 1.
- Publish: `support/ack`, mặc định QoS 0.
- Chỉ xử lý các request có `deviceId` bằng `esp32-audio-01`.
- Với module phát hoạt động: `PLAY_STARTED` = đã gửi lệnh phát tới DFPlayer; không đảm bảo file đã phát hết.

Mẫu JSON:

```json
{"requestId":"REQ-123","deviceId":"esp32-audio-01","from":"phone","location":"A101","type":"SUPPORT","timestamp":"2026-11-10T12:00:00.000Z"}
```

**Giới hạn:** bản thử nghiệm cho lớp học, không phù hợp làm hệ thống khẩn cấp an toàn tính mạng.
