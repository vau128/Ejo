class AppSettings {
  const AppSettings({
    required this.pushEnabled,
    required this.seatAlertEnabled,
    required this.warningAlertEnabled,
  });

  final bool pushEnabled;
  final bool seatAlertEnabled;
  final bool warningAlertEnabled;

  factory AppSettings.fromJson(Map<String, dynamic> json) {
    return AppSettings(
      pushEnabled: json['pushEnabled'] as bool? ?? false,
      seatAlertEnabled: json['seatAlertEnabled'] as bool? ?? false,
      warningAlertEnabled: json['warningAlertEnabled'] as bool? ?? false,
    );
  }

  AppSettings copyWith({
    bool? pushEnabled,
    bool? seatAlertEnabled,
    bool? warningAlertEnabled,
  }) {
    return AppSettings(
      pushEnabled: pushEnabled ?? this.pushEnabled,
      seatAlertEnabled: seatAlertEnabled ?? this.seatAlertEnabled,
      warningAlertEnabled: warningAlertEnabled ?? this.warningAlertEnabled,
    );
  }
}
