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
}
