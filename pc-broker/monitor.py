"""Optional PC-side MQTT event logger. Writes request/ack payloads to SQLite."""
import datetime
import json
import os
import sqlite3
import uuid

import paho.mqtt.client as mqtt

BROKER_HOST = os.getenv("MQTT_HOST", "127.0.0.1")
BROKER_PORT = int(os.getenv("MQTT_PORT", "1883"))
MQTT_USER = os.getenv("MQTT_USER", "support")
MQTT_PASSWORD = os.getenv("MQTT_PASSWORD")
DB_FILE = os.path.join(os.path.dirname(__file__), "events.db")
TOPICS = [("support/requests", 1), ("support/ack", 1)]

if not MQTT_PASSWORD:
    raise SystemExit("Set MQTT_PASSWORD environment variable before running.")

db = sqlite3.connect(DB_FILE)
db.execute("""CREATE TABLE IF NOT EXISTS mqtt_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    received_at TEXT NOT NULL,
    topic TEXT NOT NULL,
    payload TEXT NOT NULL
)""")
db.commit()


def on_connect(client, userdata, flags, reason_code, properties):
    if reason_code == 0:
        print("MQTT connected:", BROKER_HOST)
        client.subscribe(TOPICS)
    else:
        print("MQTT connect failed:", reason_code)


def on_message(client, userdata, message):
    body = message.payload.decode("utf-8", errors="replace")
    timestamp = datetime.datetime.now(datetime.timezone.utc).isoformat()
    try:
        decoded = json.loads(body)
        print(timestamp, message.topic, json.dumps(decoded, ensure_ascii=False))
    except json.JSONDecodeError:
        print(timestamp, message.topic, body)
    db.execute("INSERT INTO mqtt_events (received_at, topic, payload) VALUES (?, ?, ?)",
               (timestamp, message.topic, body))
    db.commit()


client = mqtt.Client(
    callback_api_version=mqtt.CallbackAPIVersion.VERSION2,
    client_id="support-pc-monitor-" + uuid.uuid4().hex[:8],
)
client.username_pw_set(MQTT_USER, MQTT_PASSWORD)
client.on_connect = on_connect
client.on_message = on_message
try:
    client.connect(BROKER_HOST, BROKER_PORT, keepalive=60)
    print("Logging to:", DB_FILE)
    client.loop_forever()
finally:
    db.close()
