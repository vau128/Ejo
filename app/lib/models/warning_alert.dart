class WarningAlert {
  const WarningAlert({
    required this.id,
    required this.seatNumber,
    required this.status,
    required this.warningTime,
  });

  final int id;
  final int seatNumber;
  final String status;
  final DateTime warningTime;

  factory WarningAlert.fromJson(Map<String, dynamic> json) {
    return WarningAlert(
      id: json['warning_id'] as int,
      seatNumber: json['seat_num'] as int,
      status: json['status'] as String? ?? '',
      warningTime: DateTime.parse(json['warning_time'] as String),
    );
  }
}
