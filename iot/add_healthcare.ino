#include <WiFi.h>
#include <PubSubClient.h>

// [1. 네트워크 설정]
const char* ssid = "yerin";
const char* password = "20040821";
const char* mqtt_server = "0.tcp.jp.ngrok.io";
const int mqtt_port = 11606;

// [2. 좌석 및 센서 핀 설정]
#define SEAT_NUM 2
const int fsrLeftPin = 34;  // 좌측 엉덩이 (비교기와 연결된 메인 기상 핀)
const int fsrRightPin = 35; // 우측 엉덩이
const int fsrBackPin = 32;  // 등받이

const int sit_threshold = 1500; // 착석으로 인정할 최소 압력값

// [3. 딥슬립 시간 설정]
#define uS_TO_S_FACTOR 1000000ULL  // 마이크로초 -> 초 변환
#define TIME_TO_SLEEP  5          // 1분(60초)마다 깨어나기 -> 현재는 테스트용으로 5초로 설정해놓음.

WiFiClient espClient;
PubSubClient client(espClient);

void setup_wifi() {
  Serial.print("Connecting to WiFi (Seat ");
  Serial.print(SEAT_NUM);
  Serial.println(")...");
  
  WiFi.begin(ssid, password);
  int retries = 0;
  while (WiFi.status() != WL_CONNECTED && retries < 20) {
    delay(500);
    Serial.print(".");
    retries++;
  }
  Serial.println("\nWiFi connected!");
}

void reconnect() {
  int retries = 0;
  while (!client.connected() && retries < 5) {
    Serial.print("Attempting MQTT connection...");
    String clientId = "ESP32_Seat_Client_" + String(SEAT_NUM);
    
    if (client.connect(clientId.c_str())) {
      Serial.println("connected");
    } else {
      Serial.print("failed, rc=");
      Serial.print(client.state());
      delay(1000);
      retries++;
    }
  }
}

void setup() {
  Serial.begin(115200);
  delay(500); // 보드 안정화 대기

  // 1. 깨어난 이유 확인
  esp_sleep_wakeup_cause_t wakeup_reason = esp_sleep_get_wakeup_cause();
  
  // 2. 3개의 압력 센서값 모두 읽기
  int left_val = analogRead(fsrLeftPin);
  int right_val = analogRead(fsrRightPin);
  int back_val = analogRead(fsrBackPin);
  
  Serial.print("Wakeup Reason: "); Serial.println(wakeup_reason);
  Serial.printf("Pressure -> L: %d, R: %d, Back: %d\n", left_val, right_val, back_val);

  // ---------------------------------------------------------
  // [상태 A] 누군가 앉아 있는 경우 (좌측이나 우측 엉덩이에 압력이 있음)
  // ---------------------------------------------------------
  if (left_val >= sit_threshold || right_val >= sit_threshold) {
    Serial.println("Seat Occupied. Analyzing posture...");
    
    // ⭐️ [헬스케어 자세 판별 로직] ⭐️
    String posture = "정상"; // 기본값
    
    // 1) 좌/우 밸런스 붕괴 (다리 꼬기 또는 짝다리) - 차이가 1000 이상 날 때
    if (left_val > right_val + 1000) {
        posture = "왼쪽으로 기울어짐(다리 꼬기)";
    } 
    else if (right_val > left_val + 1000) {
        posture = "오른쪽으로 기울어짐(다리 꼬기)";
    } 
    // 2) 앞/뒤 밸런스 붕괴 (등받이 안 닿음)
    else if (back_val < 500) {
        posture = "거북목/허리 숙임";
    }

    Serial.println("Detected Posture: " + posture);

    // 와이파이 & MQTT 연결
    setup_wifi();
    client.setServer(mqtt_server, mqtt_port);
    if (!client.connected()) reconnect();

    // 서버로 보낼 데이터를 JSON 형태로 예쁘게 포장
    String topic = "seat/status/" + String(SEAT_NUM);
    String payload = "{\"seat_num\":" + String(SEAT_NUM) + 
                     ", \"left\":" + String(left_val) + 
                     ", \"right\":" + String(right_val) + 
                     ", \"back\":" + String(back_val) + 
                     ", \"posture\":\"" + posture + "\"}";
                     
    client.publish(topic.c_str(), payload.c_str());
    
    client.loop();
    delay(500); 
    
    // Wi-Fi 종료
    WiFi.disconnect(true);
    WiFi.mode(WIFI_OFF);
    
    Serial.println("Data sent: " + payload);

    // 1분 뒤에 다시 깨어나서 상태 확인
    Serial.println("💤 Sleeping for 1 minute...");
    esp_sleep_enable_timer_wakeup(TIME_TO_SLEEP * uS_TO_S_FACTOR);
    esp_deep_sleep_start();
  } 
  
  // ---------------------------------------------------------
  // [상태 B] 자리가 비어 있는 경우 (센서 모두 값이 낮음)
  // ---------------------------------------------------------
  else {
    if (wakeup_reason == ESP_SLEEP_WAKEUP_TIMER) {
       Serial.println("User left the seat! Sending empty status...");
       
       setup_wifi();
       client.setServer(mqtt_server, mqtt_port);
       if (!client.connected()) reconnect();

       String topic = "seat/status/" + String(SEAT_NUM);
       // 공석일 때는 status를 0으로 명시해서 JSON 전송
       String empty_payload = "{\"seat_num\":" + String(SEAT_NUM) + ", \"status\": 0}";
       client.publish(topic.c_str(), empty_payload.c_str());
       
       client.loop(); 
       delay(500); 
       WiFi.disconnect(true);
       WiFi.mode(WIFI_OFF);
    }

    Serial.println("💤 Seat is empty. Sleeping indefinitely until someone sits...");
    
    // 메인 센서(34번 핀-비교기 연결)에 압력이 가해지면 깨어남
    esp_sleep_enable_ext0_wakeup(GPIO_NUM_33, 1);
    esp_deep_sleep_start(); 
  }
}

void loop() {
}
