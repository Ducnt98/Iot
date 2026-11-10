import mqtt from "mqtt";
import "./styles.css";

const $ = (selector) => document.querySelector(selector);
let client = null;
const pendingRequests = new Map();

const storedHost = localStorage.getItem("iot-broker-host");
if (storedHost) $("#host").value = storedHost;

function log(text) {
  const list = $("#events");
  if (list.children.length === 1 && list.firstElementChild.textContent === "Chưa có sự kiện.") {
    list.replaceChildren();
  }
  const item = document.createElement("li");
  item.textContent = new Date().toLocaleTimeString("vi-VN") + " — " + text;
  list.prepend(item);
  while (list.children.length > 30) list.lastElementChild.remove();
}

function setConnected(connected, label) {
  $("#status").textContent = label;
  $("#status").classList.toggle("connected", connected);
  $("#connect").disabled = connected;
  $("#disconnect").disabled = !connected;
  $("#support").disabled = !connected;
  $("#sos").disabled = !connected;
}

function makeId() {
  return "REQ-" + Date.now().toString(36) + "-" + Math.random().toString(36).slice(2, 8);
}

$("#connect").addEventListener("click", () => {
  const host = $("#host").value.trim();
  const port = Number($("#port").value);
  const username = $("#username").value.trim();
  const password = $("#password").value;
  if (!/^[a-zA-Z0-9.-]+$/.test(host) || !Number.isInteger(port) || port < 1 || port > 65535) {
    log("IP hoặc tên Broker không hợp lệ."); return;
  }
  if (!username || !password) { log("Hãy nhập tài khoản và mật khẩu MQTT."); return; }
  localStorage.setItem("iot-broker-host", host); // Never save password
  if (client) client.end(true);
  setConnected(false, "Đang kết nối...");
  $("#connect").disabled = true;

  client = mqtt.connect("ws://" + host + ":" + port, {
    clientId: "phone-" + makeId().replace(/-/g, "").slice(-19),
    username, password,
    protocolVersion: 4,
    reconnectPeriod: 3000,
    connectTimeout: 7000,
    clean: true
  });

  client.on("connect", () => {
    setConnected(true, "Đã kết nối");
    log("Đã kết nối MQTT Broker.");
    client.subscribe("support/ack", { qos: 1 }, (err) => {
      if (err) log("Không Subscribe được support/ack: " + err.message);
    });
  });
  client.on("reconnect", () => setConnected(false, "Đang kết nối lại..."));
  client.on("close", () => setConnected(false, "Mất kết nối"));
  client.on("error", (error) => log("Lỗi MQTT: " + error.message));
  client.on("message", (topic, payload) => {
    if (topic !== "support/ack") return;
    try {
      const reply = JSON.parse(payload.toString());
      if (!pendingRequests.has(reply.requestId)) return;
      const request = pendingRequests.get(reply.requestId);
      if (request.deviceId !== reply.deviceId) return;
      const state = reply.state || "UNKNOWN";
      log("Yêu cầu " + reply.requestId + ": ESP32 phản hồi " + state);
      pendingRequests.delete(reply.requestId);
    } catch { log("Nhận ACK không hợp lệ."); }
  });
});

$("#disconnect").addEventListener("click", () => {
  if (client) client.end(true);
  client = null;
  setConnected(false, "Đã ngắt");
  log("Đã ngắt kết nối.");
});

function sendRequest(type) {
  if (!client || !client.connected) { log("Chưa kết nối Broker."); return; }
  const deviceId = $("#deviceId").value.trim();
  const location = $("#location").value.trim();
  if (!deviceId || !location || deviceId.length > 48 || location.length > 48) {
    log("Cần mã ESP32 và vị trí (tối đa 48 ký tự)."); return;
  }
  const request = {
    requestId: makeId(),
    deviceId,
    from: "phone",
    location,
    type,
    timestamp: new Date().toISOString()
  };
  pendingRequests.set(request.requestId, request);
  client.publish("support/requests", JSON.stringify(request), { qos: 1, retain: false }, (err) => {
    if (err) {
      log("Lỗi gửi " + type + ": " + err.message);
      pendingRequests.delete(request.requestId);
    } else {
      log("Đã gửi " + type + " từ " + location + "; chờ ACK của ESP32...");
    }
  });
}

$("#support").addEventListener("click", () => sendRequest("SUPPORT"));
$("#sos").addEventListener("click", () => sendRequest("SOS"));
