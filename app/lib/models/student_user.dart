class StudentUser {
  const StudentUser({
    required this.id,
    required this.name,
    required this.studentId,
    required this.email,
    required this.role,
    required this.warningCount,
    required this.agreedToPrivacy,
    this.createdAt,
    this.photo,
  });

  final String id;
  final String name;
  final String studentId;
  final String email;
  final String role;
  final int warningCount;
  final bool agreedToPrivacy;
  final String? createdAt;
  final String? photo;

  factory StudentUser.fromJson(Map<String, dynamic> json) {
    return StudentUser(
      id: json['id'] as String,
      name: json['name'] as String,
      studentId: json['studentId'] as String,
      email: json['email'] as String,
      role: json['role'] as String? ?? 'USER',
      warningCount: json['warningCount'] as int? ?? 0,
      agreedToPrivacy: json['agreedToPrivacy'] as bool? ?? false,
      createdAt: json['createdAt'] as String?,
      photo: json['photo'] as String?,
    );
  }

  StudentUser copyWith({
    String? id,
    String? name,
    String? studentId,
    String? email,
    String? role,
    int? warningCount,
    bool? agreedToPrivacy,
    String? createdAt,
    String? photo,
  }) {
    return StudentUser(
      id: id ?? this.id,
      name: name ?? this.name,
      studentId: studentId ?? this.studentId,
      email: email ?? this.email,
      role: role ?? this.role,
      warningCount: warningCount ?? this.warningCount,
      agreedToPrivacy: agreedToPrivacy ?? this.agreedToPrivacy,
      createdAt: createdAt ?? this.createdAt,
      photo: photo ?? this.photo,
    );
  }
}
