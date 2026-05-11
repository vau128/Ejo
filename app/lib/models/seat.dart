enum SeatStatus { available, occupied, item, reserved }

class Seat {
  const Seat({
    required this.id,
    required this.number,
    required this.status,
    this.selectedByCurrentUser = false,
  });

  final String id;
  final int number;
  final SeatStatus status;
  final bool selectedByCurrentUser;

  factory Seat.fromJson(Map<String, dynamic> json) {
    return Seat(
      id: json['seatId'] as String,
      number: json['seatNumber'] as int,
      status: _statusFromApi(json['status'] as String?),
      selectedByCurrentUser: json['selectedByCurrentUser'] as bool? ?? false,
    );
  }

  Seat copyWith({
    String? id,
    int? number,
    SeatStatus? status,
    bool? selectedByCurrentUser,
  }) {
    return Seat(
      id: id ?? this.id,
      number: number ?? this.number,
      status: status ?? this.status,
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
      case 'ITEM':
        return SeatStatus.item;
      case 'RESERVED':
        return SeatStatus.reserved;
      default:
        return SeatStatus.available;
    }
  }
}
