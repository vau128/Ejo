class WarningAlert {
  const WarningAlert({
    required this.id,
    required this.seatNumber,
    required this.warningType,
    required this.status,
    required this.message,
    required this.warningTime,
  });

  final int id;
  final int seatNumber;
  final String warningType;
  final String status;
  final String message;
  final DateTime warningTime;

  factory WarningAlert.fromJson(Map<String, dynamic> json) {
    return WarningAlert(
      id: json['warning_id'] as int,
      seatNumber: json['seat_num'] as int,
      warningType: json['warningType'] as String? ?? '',
      status: json['status'] as String? ?? '',
      message: json['message'] as String? ?? '',
      warningTime: DateTime.parse(json['warning_time'] as String),
    );
  }
}
