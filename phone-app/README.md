# Ứng dụng điện thoại (Phone App)

Có **hai cách sử dụng**, cùng gửi dữ liệu MQTT tới máy tính Mosquitto và ESP32 phát loa.

## 1. Android native bằng Java (khuyên dùng)

Mã nguồn ở [android/](android/). Có giao diện XML, nút **Gọi hỗ trợ**, **SOS**, cấu hình IP Broker/tài khoản và nhận phản hồi từ ESP32. Xem **[Hướng dẫn Android Studio](android/README.md)**.

## 2. Web app dùng để kiểm thử

Bản Web App JavaScript hiện có được giữ nguyên.

```powershell
cd phone-app
npm install
npm run dev -- --host 0.0.0.0
```

Mở điện thoại cùng Wi-Fi: `http://<IP_MAY_TINH>:5173`, kết nối tới Mosquitto WebSocket port `9001`.

## Giao thức chung

- Điện thoại gửi: `support/requests` (QoS 1; JSON).
- Điện thoại nhận: `support/ack` (QoS 1).
- ESP32 đích mặc định: `esp32-audio-01`.
- Broker chạy trên máy tính với `pc-broker/mosquitto.conf`.

**Android Java dùng MQTT TCP cổng 1883**, còn **Web JavaScript dùng MQTT WebSocket cổng 9001**. Không đổi tên topic giữa hai bản.
