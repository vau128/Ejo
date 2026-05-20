START TRANSACTION;

DELETE FROM lost_item;
DELETE FROM warning;
DELETE FROM posture_log;

DELETE FROM seat
WHERE seat_num NOT IN (1, 2, 3, 4);

INSERT INTO seat (
    seat_num,
    location,
    seat_code,
    pressure,
    status,
    checked_in,
    occupied,
    posture,
    left_pressure,
    right_pressure,
    back_pressure,
    posture_timestamp,
    updated_at
)
VALUES
    (1, 'A-1', 'seat-1', 0, 'AVAILABLE', FALSE, FALSE, '정상', 0, 0, 0, NULL, NOW()),
    (2, 'A-2', 'seat-2', 0, 'AVAILABLE', FALSE, FALSE, '정상', 0, 0, 0, NULL, NOW()),
    (3, 'A-3', 'seat-3', 0, 'AVAILABLE', FALSE, FALSE, '정상', 0, 0, 0, NULL, NOW()),
    (4, 'A-4', 'seat-4', 0, 'AVAILABLE', FALSE, FALSE, '정상', 0, 0, 0, NULL, NOW())
ON DUPLICATE KEY UPDATE
    location = VALUES(location),
    seat_code = VALUES(seat_code),
    pressure = 0,
    status = 'AVAILABLE',
    checked_in = FALSE,
    occupied = FALSE,
    posture = '정상',
    left_pressure = 0,
    right_pressure = 0,
    back_pressure = 0,
    posture_timestamp = NULL,
    updated_at = NOW();

COMMIT;
