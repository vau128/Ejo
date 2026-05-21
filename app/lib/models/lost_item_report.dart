class LostItemReport {
  const LostItemReport({
    required this.id,
    required this.seatNumber,
    required this.detectedAt,
    required this.imageUrl,
    required this.classificationStatus,
  });

  final String id;
  final int seatNumber;
  final DateTime detectedAt;
  final String imageUrl;
  final String classificationStatus;

  factory LostItemReport.fromJson(Map<String, dynamic> json) {
    return LostItemReport(
      id: json['reportId'] as String,
      seatNumber: json['seatNumber'] as int,
      detectedAt: DateTime.parse(json['detectedAt'] as String),
      imageUrl: json['imageAssetPath'] as String,
      classificationStatus: json['classificationStatus'] as String,
    );
  }
}
