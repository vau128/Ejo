class StudentUser {
  const StudentUser({
    required this.id,
    required this.name,
    required this.studentId,
    required this.email,
    required this.warningCount,
    required this.agreedToPrivacy,
  });

  final String id;
  final String name;
  final String studentId;
  final String email;
  final int warningCount;
  final bool agreedToPrivacy;

  factory StudentUser.fromJson(Map<String, dynamic> json) {
    return StudentUser(
      id: json['id'] as String,
      name: json['name'] as String,
      studentId: json['studentId'] as String,
      email: json['email'] as String,
      warningCount: json['warningCount'] as int? ?? 0,
      agreedToPrivacy: json['agreedToPrivacy'] as bool? ?? false,
    );
  }

  StudentUser copyWith({
    String? id,
    String? name,
    String? studentId,
    String? email,
    int? warningCount,
    bool? agreedToPrivacy,
  }) {
    return StudentUser(
      id: id ?? this.id,
      name: name ?? this.name,
      studentId: studentId ?? this.studentId,
      email: email ?? this.email,
      warningCount: warningCount ?? this.warningCount,
      agreedToPrivacy: agreedToPrivacy ?? this.agreedToPrivacy,
    );
  }
}
