# He thong goi ho tro va thong bao qua MQTT

Mo hinh 3 thanh phan tren mang Wi-Fi noi bo:

1. **phone-app/**: trang web tren dien thoai (Vite + MQTT.js) gui yeu cau SUPPORT/SOS, nhan thong bao xac nhan.
2. **pc-broker/**: Mosquitto MQTT Broker tren may tinh, WebSocket cho trinh duyet, chuong trinh Python theo doi va luu SQLite.
3. **esp32-audio/**: Arduino ESP32 Subscribe yeu cau, dieu khien DFPlayer Mini + loa phat am thanh va gui ACK.

```text
Dien thoai (Web, MQTT over WebSocket :9001)
                  |
                  v
May tinh Windows (Mosquitto broker :1883/:9001)
                  |
                  v
ESP32 (MQTT TCP :1883) --> DFPlayer Mini + Loa
                  |
                  +------> support/ack --> Dien thoai va may tinh
```

## Bat dau nhanh

1. Xem [pc-broker/README.md](pc-broker/README.md): cai Mosquitto, tao tai khoan, mo cong 1883/9001 trong mang LAN, chay Broker.
2. Xem [esp32-audio/README.md](esp32-audio/README.md): dau noi ESP32 - DFPlayer Mini - loa, sao chep `secrets.example.h` thanh `secrets.h`, nap chuong trinh Arduino.
3. Xem [phone-app/README.md](phone-app/README.md): `npm install`, `npm run dev -- --host 0.0.0.0`; truy cap `http://<IP_MAY_TINH>:5173` tren dien thoai.
4. Tu dien thoai nhap IP may tinh, tai khoan MQTT, chon phong va nhan **Goi ho tro** / **SOS**.
5. Loa phat file `0001.mp3` (SUPPORT) hoac `0002.mp3` (SOS); giao dien nhan trang thai `PLAY_STARTED`.

## Giao thuc MQTT

- Broker: IP LAN cua may tinh, TCP 1883 cho ESP32; WebSocket 9001 cho dien thoai.
- `support/requests`: dien thoai Publish JSON `{requestId,deviceId,from,location,type,timestamp}`.
- `support/ack`: ESP32 Publish JSON `{requestId,deviceId,location,state}` sau khi ra lenh phat am thanh.
- `deviceId` mac dinh: `esp32-audio-01`; `type`: `SUPPORT` hoac `SOS`.
- `PLAY_STARTED` **chi xac nhan lenh phat da duoc gui cho module**, khong chung minh am thanh da phat het hay co nguoi den ho tro.

## Gioi han va an toan

- Ban mau phu hop thuc hanh/mang LAN, **khong phai he thong bao dong cap cuu du tin cay**.
- Khong mo cong MQTT ra Internet; su dung mat khau, gioi han Windows Firewall cho mang Private.
- Ket noi dang dung TCP MQTT va WebSocket **khong ma hoa**; neu trien khai thuc te hay dung TLS, xac thuc, giu bi mat.
- ESP32 phat cac file MP3 da ghi san, **khong tu doc duoc noi dung phong thay doi**. Truong location chi duoc luu va hien thi.
- Khong commit `esp32-audio/secrets.h` hay file mat khau Mosquitto.

Chi tiet va lenh kiem thu nam trong README cua tung project.
