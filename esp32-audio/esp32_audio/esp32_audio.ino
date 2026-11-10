#include <WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>
#include "secrets.h"

// Physical ESP32 + DFPlayer Mini + speaker: 1
// Wokwi serial-only MQTT demo (no real audio): 0
#ifndef USE_DFPLAYER
#define USE_DFPLAYER 1
#endif

#if USE_DFPLAYER
#include <DFRobotDFPlayerMini.h>
#endif

// Hardware pins
#define STATUS_LED_PIN 26
#define DFPLAYER_RX_PIN 16  // ESP32 RX2 <- DFPlayer TX
#define DFPLAYER_TX_PIN 17  // ESP32 TX2 -> 1k resistor -> DFPlayer RX

const char* DEVICE_ID = "esp32-audio-01";
const char* TOPIC_REQUESTS = "support/requests";
const char* TOPIC_ACK = "support/ack";

WiFiClient wifiClient;
PubSubClient mqtt(wifiClient);

#if USE_DFPLAYER
HardwareSerial dfSerial(2);
DFRobotDFPlayerMini player;
#endif

bool audioReady = false;
unsigned long lastWifiRetry = 0;
unsigned long lastMqttRetry = 0;
String lastRequestId = "";
String lastAckState = "";

void publishAck(const String& requestId, const String& location, const char* state) {
  StaticJsonDocument<256> ack;
  ack["requestId"] = requestId;
  ack["deviceId"] = DEVICE_ID;
  ack["location"] = location;
  ack["state"] = state;

  String body;
  serializeJson(ack, body);
  bool ok = mqtt.publish(TOPIC_ACK, body.c_str(), false);
  Serial.printf("ACK %s: %s\n", ok ? "sent" : "failed", body.c_str());
}

void onMqttMessage(char* topic, byte* payload, unsigned int length) {
  if (strcmp(topic, TOPIC_REQUESTS) != 0) return;

  StaticJsonDocument<384> message;
  DeserializationError error = deserializeJson(message, payload, length);
  if (error) {
    Serial.println("Invalid request JSON");
    return;
  }

  String target = message["deviceId"] | "";
  String requestId = message["requestId"] | "";
  String location = message["location"] | "";
  String type = message["type"] | "";

  if (target != DEVICE_ID || requestId.isEmpty() || location.isEmpty()) return;
  if (type != "SUPPORT" && type != "SOS") return;

  // Avoid playing twice when a QoS 1 request is delivered again.
  if (requestId == lastRequestId) {
    publishAck(requestId, location, lastAckState.c_str());
    return;
  }

  Serial.printf("Request: %s | room: %s | id: %s\n",
                type.c_str(), location.c_str(), requestId.c_str());

  digitalWrite(STATUS_LED_PIN, HIGH);
  const char* state = "AUDIO_NOT_READY";

#if USE_DFPLAYER
  if (audioReady) {
    // /mp3/0001.mp3 = normal support; /mp3/0002.mp3 = SOS
    player.playMp3Folder(type == "SOS" ? 2 : 1);
    state = "PLAY_STARTED";
  }
#else
  Serial.println("DEMO: Would play audio notification");
  state = "DEMO_ONLY";
#endif

  lastRequestId = requestId;
  lastAckState = state;
  publishAck(requestId, location, state);

  delay(120);
  digitalWrite(STATUS_LED_PIN, LOW);
}

void connectMqtt() {
  String clientId = String(DEVICE_ID) + "-" +
                    String((uint32_t)(ESP.getEfuseMac() & 0xFFFFFF), HEX);
  Serial.print("Connecting to MQTT ... ");
  if (mqtt.connect(clientId.c_str(), MQTT_USERNAME, MQTT_PASSWORD)) {
    Serial.println("connected");
    if (mqtt.subscribe(TOPIC_REQUESTS, 1)) {
      Serial.println("Subscribed: support/requests");
    } else {
      Serial.println("Subscribe failed");
    }
  } else {
    Serial.printf("failed (state %d)\n", mqtt.state());
  }
}

void setup() {
  Serial.begin(115200);
  pinMode(STATUS_LED_PIN, OUTPUT);
  digitalWrite(STATUS_LED_PIN, LOW);

#if USE_DFPLAYER
  dfSerial.begin(9600, SERIAL_8N1, DFPLAYER_RX_PIN, DFPLAYER_TX_PIN);
  audioReady = player.begin(dfSerial, true, true);
  if (audioReady) {
    player.volume(23);  // 0..30
    Serial.println("DFPlayer ready");
  } else {
    Serial.println("DFPlayer not ready; check 5V, ground, serial and microSD");
  }
#else
  Serial.println("Audio simulation mode: no physical sound");
#endif

  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  mqtt.setServer(MQTT_HOST, MQTT_PORT);
  mqtt.setCallback(onMqttMessage);
  mqtt.setBufferSize(512);
  mqtt.setKeepAlive(30);
  Serial.println("ESP32 audio receiver started");
}

void loop() {
  if (WiFi.status() != WL_CONNECTED) {
    if (millis() - lastWifiRetry >= 5000) {
      lastWifiRetry = millis();
      Serial.println("Connecting WiFi...");
      WiFi.reconnect();
    }
    delay(20);
    return;
  }

  if (!mqtt.connected()) {
    if (millis() - lastMqttRetry >= 3000) {
      lastMqttRetry = millis();
      connectMqtt();
    }
  } else {
    mqtt.loop();
  }

  delay(10); // Keep WiFi stack responsive
}
