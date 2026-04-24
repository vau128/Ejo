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
}
