# Hệ thống gọi hỗ trợ và thông báo thông minh qua MQTT

Mô hình **Điện thoại gọi hỗ trợ/SOS → máy tính Mosquitto MQTT Broker → ESP32 + DFPlayer Mini + loa**.

## Cấu trúc repository

- [phone-app/android/](phone-app/android/) — **Android native Java + XML** (mới), có nút gọi hỗ trợ, SOS và nhận ACK.
- [phone-app/](phone-app/) — Web app JavaScript (giữ lại để thử nghiệm).
- [pc-broker/](pc-broker/) — Mosquitto Broker trên PC (TCP 1883, WebSocket 9001), Python monitor lưu SQLite.
- [esp32-audio/](esp32-audio/) — Arduino ESP32 + DFPlayer Mini + loa.

```text
Android Java ---MQTT TCP:1883--+
                               |
Web App ------MQTT WS:9001-----+--> Mosquitto Broker (PC)
                                       |
                                 MQTT TCP:1883
                                       |
                                       v
                              ESP32 + DFPlayer + loa
                                       |
                                       +--> support/ack --> Android/Web
```

## Bắt đầu

1. Cài Broker trên PC: [pc-broker/README.md](pc-broker/README.md).
2. Nạp firmware Arduino và chuẩn bị file MP3: [esp32-audio/README.md](esp32-audio/README.md).
3. Chạy Android Java: [phone-app/android/README.md](phone-app/android/README.md).
   Hoặc chạy Web bản cũ: [phone-app/README.md](phone-app/README.md).

Cùng hai topic MQTT: `support/requests` (điện thoại gửi), `support/ack` (ESP32 phản hồi).
ESP32 nhận `type: "SUPPORT"` để phát file MP3 0001 hoặc `type: "SOS"` để phát file MP3 0002.
Trường `location` chỉ hiển thị trong log, chưa đọc thành giọng nói. ACK `PLAY_STARTED` chỉ có nghĩa ESP32 đã ra lệnh phát file.

**Chỉ phù hợp thử nghiệm trên LAN tin cậy**. MQTT TCP/WebSocket chưa có TLS; không mở Broker ra Internet hoặc dùng làm hệ thống cấp cứu đòi hỏi độ tin cậy cao.
