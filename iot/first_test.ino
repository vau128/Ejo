#include <WiFi.h>
#include <PubSubClient.h>

// [1. 네트워크 설정]
const char* ssid = "yerin";
const char* password = "20040821";
const char* mqtt_server = "172.20.10.9"; 

// [2. 좌석 설정] - 구울 때 이 번호만 1~7로 바꾸기
#define SEAT_NUM 1 

// [3. 핀 설정] - 모든 기기가 똑같이 34번 사용
const int fsrPin = 34;
WiFiClient espClient;
PubSubClient client(espClient);

void setup() {
  Serial.begin(115200);
  pinMode(fsrPin, INPUT); // 입력 모드 명시
  setup_wifi();
  client.setServer(mqtt_server, 1883);
}

void setup_wifi() {
  delay(10);
  Serial.print("Connecting to WiFi (Seat ");
  Serial.print(SEAT_NUM);
  Serial.println(")...");
  
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
    
    // 중요: Client ID가 기기마다 다 달라야 서버에서 안 튕김!
    String clientId = "ESP32_Seat_Client_" + String(SEAT_NUM);
    
    if (client.connect(clientId.c_str())) {
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
  
  // 좌석 번호에 따라 토픽을 자동으로 생성 (seat/status/1, seat/status/2...)
  String topic = "seat/status/" + String(SEAT_NUM);
  String payload = String(val);
  
  client.publish(topic.c_str(), payload.c_str());

  Serial.print("[Seat ");
  Serial.print(SEAT_NUM);
  Serial.print("] Pressure Value: ");
  Serial.println(val);
  
  delay(2000); // 전송 주기 2초 (요구사항 F-01 반영)
}