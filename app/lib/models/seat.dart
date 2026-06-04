enum SeatStatus {
  available,
  reserved,
  occupied,
  objectOnly,
  vacantLong,
  sensorDelay,
}

class Seat {
  const Seat({
    required this.id,
    required this.number,
    required this.location,
    required this.status,
    required this.checkedIn,
    required this.occupied,
    required this.posture,
    required this.leftPressure,
    required this.rightPressure,
    required this.backPressure,
    this.postureTimestamp,
    this.selectedAt,
    this.selectedByCurrentUser = false,
  });

  final String id;
  final int number;
  final String location;
  final SeatStatus status;
  final bool checkedIn;
  final bool occupied;
  final String posture;
  final int leftPressure;
  final int rightPressure;
  final int backPressure;
  final String? postureTimestamp;
  final String? selectedAt;
  final bool selectedByCurrentUser;

  factory Seat.fromJson(Map<String, dynamic> json) {
    return Seat(
      id: json['seatId'] as String,
      number: json['seatNumber'] as int,
      location: json['location'] as String? ?? '-',
      status: _statusFromApi(json['status'] as String?),
      checkedIn: json['checkedIn'] as bool? ?? false,
      occupied: json['occupied'] as bool? ?? false,
      posture: json['posture'] as String? ?? '정상',
      leftPressure: json['leftPressure'] as int? ?? 0,
      rightPressure: json['rightPressure'] as int? ?? 0,
      backPressure: json['backPressure'] as int? ?? 0,
      postureTimestamp: json['postureTimestamp'] as String?,
      selectedAt: json['selectedAt'] as String?,
      selectedByCurrentUser: json['selectedByCurrentUser'] as bool? ?? false,
    );
  }

  Seat copyWith({
    String? id,
    int? number,
    String? location,
    SeatStatus? status,
    bool? checkedIn,
    bool? occupied,
    String? posture,
    int? leftPressure,
    int? rightPressure,
    int? backPressure,
    String? postureTimestamp,
    String? selectedAt,
    bool? selectedByCurrentUser,
  }) {
    return Seat(
      id: id ?? this.id,
      number: number ?? this.number,
      location: location ?? this.location,
      status: status ?? this.status,
      checkedIn: checkedIn ?? this.checkedIn,
      occupied: occupied ?? this.occupied,
      posture: posture ?? this.posture,
      leftPressure: leftPressure ?? this.leftPressure,
      rightPressure: rightPressure ?? this.rightPressure,
      backPressure: backPressure ?? this.backPressure,
      postureTimestamp: postureTimestamp ?? this.postureTimestamp,
      selectedAt: selectedAt ?? this.selectedAt,
      selectedByCurrentUser:
          selectedByCurrentUser ?? this.selectedByCurrentUser,
    );
  }

  static SeatStatus _statusFromApi(String? status) {
    switch (status) {
      case 'AVAILABLE':
        return SeatStatus.available;
      case 'RESERVED':
        return SeatStatus.reserved;
      case 'OCCUPIED':
        return SeatStatus.occupied;
      case 'OBJECT_ONLY':
        return SeatStatus.objectOnly;
      case 'VACANT_LONG':
      case 'SQUATTING':
      case 'ABNORMAL':
        return SeatStatus.vacantLong;
      case 'SENSOR_DELAY':
        return SeatStatus.sensorDelay;
      default:
        return SeatStatus.available;
    }
  }
}
