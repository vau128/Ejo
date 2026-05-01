#include <WiFi.h>
#include <PubSubClient.h>

// 1. 설정
const char* ssid = "yerin";
const char* password = "20040821";
const char* mqtt_server = "172.20.10.9"; // 라즈베리 파이 IP (hostname -I로 확인한 것)

const int fsrPin = 34;
WiFiClient espClient;
PubSubClient client(espClient);

void setup() {
  Serial.begin(115200);
  setup_wifi();
  client.setServer(mqtt_server, 1883);
}

void setup_wifi() {
  delay(10);
  Serial.println("Connecting to WiFi...");
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nWiFi connected");
}

void reconnect() {
  while (!client.connected()) {
    Serial.print("Attempting MQTT connection...");
    if (client.connect("ESP32_Seat_Sensor")) {
      Serial.println("connected");
    } else {
      Serial.print("failed, rc=");
      Serial.print(client.state());
      delay(2000);
    }
  }
}

void loop() {
  if (!client.connected()) reconnect();
  client.loop();

  int val = analogRead(fsrPin);
  // 라파 main_system.py가 기다리는 토픽과 값을 맞추기
  String payload = String(val);
  client.publish("seat/status", payload.c_str());

  Serial.print("Pressure Value: ");
  Serial.println(val);
  delay(2000); // 전송 주기 2초 (요구사항 F-01 반영)
}