enum SeatStatus { available, occupied, squatting, abnormal }

class Seat {
  const Seat({
    required this.id,
    required this.number,
    required this.status,
    required this.checkedIn,
    required this.posture,
    required this.leftPressure,
    required this.rightPressure,
    required this.backPressure,
    this.selectedByCurrentUser = false,
  });

  final String id;
  final int number;
  final SeatStatus status;
  final bool checkedIn;
  final String posture;
  final int leftPressure;
  final int rightPressure;
  final int backPressure;
  final bool selectedByCurrentUser;

  factory Seat.fromJson(Map<String, dynamic> json) {
    return Seat(
      id: json['seatId'] as String,
      number: json['seatNumber'] as int,
      status: _statusFromApi(json['status'] as String?),
      checkedIn: json['checkedIn'] as bool? ?? false,
      posture: json['posture'] as String? ?? '정상',
      leftPressure: json['leftPressure'] as int? ?? 0,
      rightPressure: json['rightPressure'] as int? ?? 0,
      backPressure: json['backPressure'] as int? ?? 0,
      selectedByCurrentUser: json['selectedByCurrentUser'] as bool? ?? false,
    );
  }

  Seat copyWith({
    String? id,
    int? number,
    SeatStatus? status,
    bool? checkedIn,
    String? posture,
    int? leftPressure,
    int? rightPressure,
    int? backPressure,
    bool? selectedByCurrentUser,
  }) {
    return Seat(
      id: id ?? this.id,
      number: number ?? this.number,
      status: status ?? this.status,
      checkedIn: checkedIn ?? this.checkedIn,
      posture: posture ?? this.posture,
      leftPressure: leftPressure ?? this.leftPressure,
      rightPressure: rightPressure ?? this.rightPressure,
      backPressure: backPressure ?? this.backPressure,
      selectedByCurrentUser:
          selectedByCurrentUser ?? this.selectedByCurrentUser,
    );
  }

  static SeatStatus _statusFromApi(String? status) {
    switch (status) {
      case 'AVAILABLE':
        return SeatStatus.available;
      case 'OCCUPIED':
        return SeatStatus.occupied;
      case 'SQUATTING':
        return SeatStatus.squatting;
      case 'ABNORMAL':
        return SeatStatus.abnormal;
      default:
        return SeatStatus.available;
    }
  }
}
