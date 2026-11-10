package com.example.iot.supportphone;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Android native Java MQTT client.
 * Phone publishes requests; PC Mosquitto routes them to an ESP32 audio receiver.
 * This demo keeps the connection while the Activity is open, not as a background service.
 */
public class MainActivity extends Activity {
    private static final String REQUEST_TOPIC = "support/requests";
    private static final String ACK_TOPIC = "support/ack";
    private static final String PREFS = "mqtt_settings";
    private static final String PREF_HOST = "host";

    private EditText editHost, editPort, editUsername, editPassword, editDeviceId, editLocation;
    private TextView textStatus, textLog;
    private Button btnConnect, btnDisconnect, btnSupport, btnSos;
    private MqttAsyncClient client;
    private final Handler handler = new Handler(Looper.getMainLooper());
    // This map is read/written only from the main (UI) thread.
    private final LinkedHashMap<String, String> pending = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);

        editHost = findViewById(R.id.editHost);
        editPort = findViewById(R.id.editPort);
        editUsername = findViewById(R.id.editUsername);
        editPassword = findViewById(R.id.editPassword);
        editDeviceId = findViewById(R.id.editDeviceId);
        editLocation = findViewById(R.id.editLocation);
        textStatus = findViewById(R.id.textStatus);
        textLog = findViewById(R.id.textLog);
        btnConnect = findViewById(R.id.btnConnect);
        btnDisconnect = findViewById(R.id.btnDisconnect);
        btnSupport = findViewById(R.id.btnSupport);
        btnSos = findViewById(R.id.btnSos);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        editHost.setText(prefs.getString(PREF_HOST, ""));

        btnConnect.setOnClickListener(v -> connectBroker());
        btnDisconnect.setOnClickListener(v -> {
            closeClient();
            setConnected(false, "Đã ngắt kết nối");
            appendLog("Đã ngắt MQTT.");
        });
        btnSupport.setOnClickListener(v -> sendRequest("SUPPORT"));
        btnSos.setOnClickListener(v -> sendRequest("SOS"));
    }

    private void setConnected(boolean ready, String message) {
        textStatus.setText(message);
        textStatus.setTextColor(Color.parseColor(ready ? "#117747" : "#A04C1C"));
        btnConnect.setEnabled(!ready);
        btnDisconnect.setEnabled(client != null);
        btnSupport.setEnabled(ready);
        btnSos.setEnabled(ready);
    }

    private void appendLog(String message) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String previous = textLog.getText().toString();
        if (previous.equals("Chưa có sự kiện.")) previous = "";
        String lines = "[" + time + "] " + message + "\n" + previous;
        if (lines.length() > 4000) lines = lines.substring(0, 4000);
        textLog.setText(lines);
    }

    private void connectBroker() {
        String host = editHost.getText().toString().trim();
        String portText = editPort.getText().toString().trim();
        String username = editUsername.getText().toString().trim();
        String password = editPassword.getText().toString();

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException ex) {
            appendLog("Cổng MQTT không hợp lệ.");
            return;
        }
        if (host.isEmpty() || host.contains("://") || host.contains("/") ||
                host.contains(" ") || port < 1 || port > 65535 ||
                username.isEmpty() || password.isEmpty()) {
            appendLog("Nhập IP Broker, cổng, tài khoản và mật khẩu hợp lệ.");
            return;
        }

        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit().putString(PREF_HOST, host).apply();
        closeClient();

        try {
            String uri = "tcp://" + host + ":" + port;
            String clientId = "support-phone-" + UUID.randomUUID().toString().substring(0, 8);
            final MqttAsyncClient mqtt = new MqttAsyncClient(uri, clientId, new MemoryPersistence());
            client = mqtt;

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(30);
            options.setUserName(username);
            options.setPassword(password.toCharArray());

            mqtt.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    if (client != mqtt) return;
                    try {
                        mqtt.subscribe(ACK_TOPIC, 1, null, new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken token) {
                                runOnUiThread(() -> {
                                    if (client != mqtt) return;
                                    setConnected(true, "Đã kết nối MQTT");
                                    appendLog("Đã Subscribe " + ACK_TOPIC +
                                            (reconnect ? " (kết nối lại)" : ""));
                                });
                            }
                            @Override
                            public void onFailure(IMqttToken token, Throwable error) {
                                runOnUiThread(() -> {
                                    if (client != mqtt) return;
                                    setConnected(false, "Lỗi Subscribe");
                                    appendLog("Không đăng ký được ACK: " + error.getMessage());
                                });
                            }
                        });
                    } catch (MqttException error) {
                        runOnUiThread(() -> {
                            if (client == mqtt) appendLog("Lỗi Subscribe: " + error.getMessage());
                        });
                    }
                }

                @Override
                public void connectionLost(Throwable cause) {
                    runOnUiThread(() -> {
                        if (client != mqtt) return;
                        setConnected(false, "Mất kết nối - đang thử lại");
                        appendLog("Mất kết nối Broker.");
                    });
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    if (!ACK_TOPIC.equals(topic)) return;
                    String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                    runOnUiThread(() -> {
                        if (client == mqtt) handleAck(payload);
                    });
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // A QoS 1 publish acknowledgement is handled by its action listener.
                }
            });

            setConnected(false, "Đang kết nối...");
            btnConnect.setEnabled(false);
            appendLog("Đang kết nối tới " + uri);

            mqtt.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken token) {
                    // Wait for connectComplete(), then subscribe ACK topic.
                    runOnUiThread(() -> {
                        if (client == mqtt) appendLog("Broker đã chấp nhận kết nối.");
                    });
                }
                @Override
                public void onFailure(IMqttToken token, Throwable error) {
                    runOnUiThread(() -> {
                        if (client != mqtt) return;
                        setConnected(false, "Kết nối thất bại");
                        appendLog("Lỗi kết nối: " + error.getMessage());
                    });
                }
            });
        } catch (MqttException ex) {
            setConnected(false, "Lỗi MQTT");
            appendLog("Không khởi tạo được MQTT: " + ex.getMessage());
        }
    }

    private void sendRequest(String type) {
        final MqttAsyncClient mqtt = client;
        if (mqtt == null || !mqtt.isConnected() || !btnSupport.isEnabled()) {
            appendLog("Chưa sẵn sàng kết nối MQTT.");
            return;
        }

        String deviceId = editDeviceId.getText().toString().trim();
        String location = editLocation.getText().toString().trim();
        if (deviceId.isEmpty() || location.isEmpty() ||
                deviceId.length() > 48 || location.length() > 48) {
            appendLog("Điền mã thiết bị và vị trí (tối đa 48 ký tự).");
            return;
        }

        String requestId = "REQ-" + System.currentTimeMillis() + "-" +
                UUID.randomUUID().toString().substring(0, 6);
        try {
            SimpleDateFormat isoTime = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            isoTime.setTimeZone(TimeZone.getTimeZone("UTC"));

            JSONObject request = new JSONObject();
            request.put("requestId", requestId);
            request.put("deviceId", deviceId);
            request.put("from", "android");
            request.put("location", location);
            request.put("type", type);
            request.put("timestamp", isoTime.format(new Date()));

            MqttMessage msg = new MqttMessage(
                    request.toString().getBytes(StandardCharsets.UTF_8));
            msg.setQos(1);
            msg.setRetained(false);

            if (pending.size() >= 30) {
                Iterator<String> iterator = pending.keySet().iterator();
                if (iterator.hasNext()) {
                    iterator.next();
                    iterator.remove();
                }
            }
            pending.put(requestId, deviceId);

            mqtt.publish(REQUEST_TOPIC, msg, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken token) {
                    runOnUiThread(() -> {
                        if (client == mqtt) appendLog("Đã Publish " + type +
                                " (" + location + "); chờ phản hồi ESP32.");
                    });
                }
                @Override
                public void onFailure(IMqttToken token, Throwable error) {
                    runOnUiThread(() -> {
                        if (client != mqtt) return;
                        pending.remove(requestId);
                        appendLog("Lỗi gửi yêu cầu: " + error.getMessage());
                    });
                }
            });

            handler.postDelayed(() -> {
                if (pending.remove(requestId) != null) {
                    appendLog("Chưa nhận ACK từ ESP32 cho " + requestId);
                }
            }, 15000);

        } catch (JSONException | MqttException ex) {
            pending.remove(requestId);
            appendLog("Không gửi được yêu cầu: " + ex.getMessage());
        }
    }

    private void handleAck(String payload) {
        try {
            JSONObject ack = new JSONObject(payload);
            String requestId = ack.optString("requestId");
            String deviceId = ack.optString("deviceId");
            String state = ack.optString("state");

            String expectedDeviceId = pending.get(requestId);
            if (expectedDeviceId == null || !expectedDeviceId.equals(deviceId)) return;
            pending.remove(requestId);

            String description;
            if ("PLAY_STARTED".equals(state)) {
                description = "ESP32 đã ra lệnh phát âm thanh.";
            } else if ("AUDIO_NOT_READY".equals(state)) {
                description = "ESP32 chưa sẵn sàng phát âm thanh.";
            } else if ("DEMO_ONLY".equals(state)) {
                description = "ESP32 đang ở chế độ mô phỏng, chưa phát loa.";
            } else {
                description = "Trạng thái ESP32: " + state;
            }

            appendLog("ACK " + requestId + ": " + description);
        } catch (JSONException error) {
            appendLog("Phản hồi MQTT không phải JSON hợp lệ.");
        }
    }

    private void closeClient() {
        final MqttAsyncClient oldClient = client;
        client = null;
        pending.clear();
        if (oldClient == null) return;
        // Closing can block briefly; do it outside UI thread.
        new Thread(() -> {
            try {
                if (oldClient.isConnected()) oldClient.disconnectForcibly(0, 1000);
            } catch (MqttException ignored) {
            }
            try {
                oldClient.close();
            } catch (MqttException ignored) {
            }
        }, "mqtt-close").start();
    }

    @Override
    protected void onDestroy() {
        closeClient();
        super.onDestroy();
    }
}
