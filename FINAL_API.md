# Final API Notes

## IoT Team URLs

- `POST http://13.209.33.104/api/seat/posture`
- `POST http://13.209.33.104/api/seat/squatting`
- `POST http://13.209.33.104/api/seat/lost-item`
- `GET http://13.209.33.104/api/seat/check-in-status/{seat_num}`

## Admin Web URLs

- `POST http://13.209.33.104/api/admin/lost-item-scan`
- `GET http://13.209.33.104/api/seats`
- `GET http://13.209.33.104/api/lost-items`
- `GET http://13.209.33.104/api/warnings`
- `GET http://13.209.33.104/api/settings/squatting-threshold`
- `PUT http://13.209.33.104/api/settings/squatting-threshold`

## SQL

```sql
ALTER TABLE seat
    ADD COLUMN checked_in BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN occupied BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN posture VARCHAR(100) NULL,
    ADD COLUMN left_pressure INT NULL,
    ADD COLUMN right_pressure INT NULL,
    ADD COLUMN back_pressure INT NULL,
    ADD COLUMN posture_timestamp DATETIME NULL;

ALTER TABLE lost_item
    ADD COLUMN seat_num INT NULL,
    ADD COLUMN category VARCHAR(50) NULL,
    ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'FOUND',
    ADD COLUMN detected_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE warning
    ADD COLUMN seat_num INT NULL,
    ADD COLUMN warning_time DATETIME NULL;

CREATE TABLE IF NOT EXISTS posture_log (
    posture_log_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    seat_num INT NOT NULL,
    posture VARCHAR(100) NULL,
    left_pressure INT NULL,
    right_pressure INT NULL,
    back_pressure INT NULL,
    sensor_timestamp DATETIME NULL,
    created_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS system_setting (
    setting_key VARCHAR(100) NOT NULL PRIMARY KEY,
    setting_value VARCHAR(255) NOT NULL,
    updated_at DATETIME NOT NULL
);
```

## curl Tests

```bash
curl -X POST http://13.209.33.104/api/seat/posture \
  -H "Content-Type: application/json" \
  -d '{
    "seat_num": 4,
    "posture": "오른쪽으로 기울어짐(다리 꼬기)",
    "left_pressure": 0,
    "right_pressure": 4095,
    "back_pressure": 0,
    "timestamp": "2026-05-18T15:00:00Z"
  }'

curl -X POST http://13.209.33.104/api/seat/squatting \
  -H "Content-Type: application/json" \
  -d '{
    "seat_num": 4,
    "status": "squatting"
  }'

curl -X POST http://13.209.33.104/api/seat/lost-item \
  -H "Content-Type: application/json" \
  -d '{
    "seat_num": 3,
    "image_url": "https://example.com/lost_item.jpg",
    "category": "laptop"
  }'

curl http://13.209.33.104/api/seat/check-in-status/4

curl http://13.209.33.104/api/seats

curl http://13.209.33.104/api/lost-items

curl -X PUT http://13.209.33.104/api/settings/squatting-threshold \
  -H "Content-Type: application/json" \
  -d '{
    "threshold_minutes": 30
  }'
```
