# Phone project: trang web gửi yêu cầu hỗ trợ

Trang web responsive, chạy trong Chrome/Safari trên điện thoại (không phải APK). Chạy **Vite dev server trên máy tính**, điện thoại truy cập qua Wi-Fi nội bộ. Dùng thư viện MQTT.js kết nối WebSocket trực tiếp đến Mosquitto ở máy tính.

## Chuẩn bị

- Node.js LTS và npm trên máy tính.
- Mosquitto đang chạy ở cổng MQTT 1883 và WebSocket 9001 theo `pc-broker/README.md`.

## Chạy

```powershell
cd phone-app
npm install
npm run dev -- --host 0.0.0.0
```

Trên điện thoại cùng Wi-Fi, mở `http://<IP_MAY_TINH>:5173`. Điền IP máy tính, port 9001, username/password MQTT, mã thiết bị `esp32-audio-01`, phòng `A101`. Nhấn **Kết nối** → **Gọi hỗ trợ** hoặc **SOS khẩn cấp**.

Thư mục **phone-app** là một project web riêng; bạn có thể nâng cấp thành PWA hoặc app Android sau. Trình duyệt trên HTTP LAN không dùng HTTPS; WebSocket ở đây là `ws://`, phù hợp **demo trong mạng nội bộ tin cậy**, không cho sử dụng qua Internet.

## MQTT

- Publish `support/requests` (QoS 1, retain false).
- Subscribe `support/ack` (QoS 1). Hiển thị ACK có `requestId` khớp với yêu cầu vừa gửi.
- Nội dung `PLAY_STARTED` chỉ xác nhận ESP32 đã gửi lệnh tới module phát âm thanh, **không chứng minh có người đến hỗ trợ**.
- Nếu không có ACK, kiểm tra `deviceId` của phone/ESP32, MQTT username/password, IP và dây loa.
