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
