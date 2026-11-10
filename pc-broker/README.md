# PC project: Mosquitto Broker + event monitor (Windows)

Máy tính là **MQTT Broker** và có thể chạy thêm chương trình Python giám sát, lưu lịch sử. Broker bản thân là dịch vụ Mosquitto; không cần viết một broker mới.

## 1. Cài đặt Broker

1. Cài Eclipse Mosquitto cho Windows: https://mosquitto.org/download/
2. Mở PowerShell và tạo thư mục:

```powershell
New-Item -ItemType Directory -Force C:\mosquitto\auth, C:\mosquitto\data
```

3. Tạo user MQTT (nhập mật khẩu riêng khi được hỏi):

```powershell
& "$env:ProgramFiles\mosquitto\mosquitto_passwd.exe" -c "C:\mosquitto\auth\passwords.txt" support
```

4. Chạy Broker từ thư mục này (dừng Mosquitto service cũ nếu port 1883 đang bị chiếm):

```powershell
& "$env:ProgramFiles\mosquitto\mosquitto.exe" -c ".\mosquitto.conf" -v
```

5. Dùng `ipconfig` để lấy **IPv4 LAN của máy tính**, ví dụ `192.168.1.10`. Điện thoại và ESP32 phải truy cập được IP này.
6. Cho phép kết nối **inbound trong Private LAN** tới TCP 1883 (ESP32), 9001 (WebSocket điện thoại), 5173 (web development server). Không mở NAT/port forwarding ra Internet.
7. Nếu Mosquitto Windows build không hỗ trợ WebSocket, bản này cần Mosquitto được build với WebSocket; kiểm tra log khi chạy ở bước 4.

## 2. Chạy app điện thoại

Ở thư mục `phone-app`:

```powershell
npm install
npm run dev -- --host 0.0.0.0
```

Từ **điện thoại cùng mạng**, mở `http://<IP_MAY_TINH>:5173`, nhập:
- MQTT Host = IPv4 LAN của máy tính (không có http://).
- WS Port = 9001; Username = support; Password = mật khẩu đã đặt.
- Device ID = esp32-audio-01; vị trí/phòng = A101.

Điện thoại dùng MQTT qua WebSocket `ws://IP:9001`, ESP32 dùng MQTT TCP `IP:1883`. Trình duyệt truy cập từ HTTP LAN tương thích với ws://. Không lưu mật khẩu MQTT vào GitHub.

## 3. Giám sát/lưu lịch sử trên máy tính (tuỳ chọn)

```powershell
cd pc-broker
python -m pip install -r requirements.txt
$env:MQTT_HOST="127.0.0.1"
$env:MQTT_USER="support"
$env:MQTT_PASSWORD="MAT_KHAU_CUA_BAN"
python monitor.py
```

Dữ liệu được lưu tại `events.db`, không commit lên GitHub. Chương trình chỉ lưu các sự kiện `support/requests` và `support/ack`.

## 4. Thử MQTT độc lập

Subscribe:
```powershell
& "$env:ProgramFiles\mosquitto\mosquitto_sub.exe" -h 127.0.0.1 -p 1883 -u support -P "MAT_KHAU_CUA_BAN" -t "support/#" -v
```

Điện thoại gửi yêu cầu `SUPPORT` hay `SOS`; máy tính sẽ thấy message. Sau khi ESP32 ra lệnh phát, topic `support/ack` trả `PLAY_STARTED`.

**Bảo mật:** chỉ thử nghiệm trên mạng tin cậy; MQTT TCP/WebSocket đang không mã hóa. Khi triển khai thực tế cần TLS, quản lý tài khoản/ACL và mạng riêng. Không dùng cho cảnh báo cứu hộ có yêu cầu an toàn tính mạng.
