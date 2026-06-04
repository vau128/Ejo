class PostureBreakdownItem {
  const PostureBreakdownItem({
    required this.label,
    required this.count,
    required this.percentage,
  });

  final String label;
  final int count;
  final int percentage;

  factory PostureBreakdownItem.fromJson(Map<String, dynamic> json) {
    return PostureBreakdownItem(
      label: json['label'] as String? ?? '-',
      count: json['count'] as int? ?? 0,
      percentage: json['percentage'] as int? ?? 0,
    );
  }
}

class DailyPostureStat {
  const DailyPostureStat({
    required this.date,
    required this.label,
    required this.totalCount,
    required this.normalCount,
    required this.abnormalCount,
    required this.dominantPosture,
  });

  final String date;
  final String label;
  final int totalCount;
  final int normalCount;
  final int abnormalCount;
  final String dominantPosture;

  factory DailyPostureStat.fromJson(Map<String, dynamic> json) {
    return DailyPostureStat(
      date: json['date'] as String? ?? '',
      label: json['label'] as String? ?? '',
      totalCount: json['totalCount'] as int? ?? 0,
      normalCount: json['normalCount'] as int? ?? 0,
      abnormalCount: json['abnormalCount'] as int? ?? 0,
      dominantPosture: json['dominantPosture'] as String? ?? '-',
    );
  }
}

class PostureStats {
  const PostureStats({
    required this.rangeStart,
    required this.rangeEnd,
    required this.totalSamples,
    required this.normalSamples,
    required this.abnormalSamples,
    required this.mostFrequentPosture,
    required this.breakdown,
    required this.daily,
    this.currentSeatNumber,
  });

  final String rangeStart;
  final String rangeEnd;
  final int totalSamples;
  final int normalSamples;
  final int abnormalSamples;
  final String mostFrequentPosture;
  final List<PostureBreakdownItem> breakdown;
  final List<DailyPostureStat> daily;
  final int? currentSeatNumber;

  factory PostureStats.fromJson(Map<String, dynamic> json) {
    return PostureStats(
      rangeStart: json['rangeStart'] as String? ?? '',
      rangeEnd: json['rangeEnd'] as String? ?? '',
      totalSamples: json['totalSamples'] as int? ?? 0,
      normalSamples: json['normalSamples'] as int? ?? 0,
      abnormalSamples: json['abnormalSamples'] as int? ?? 0,
      mostFrequentPosture: json['mostFrequentPosture'] as String? ?? '데이터 없음',
      currentSeatNumber: json['currentSeatNumber'] as int?,
      breakdown: (json['breakdown'] as List<dynamic>? ?? const [])
          .map(
            (item) => PostureBreakdownItem.fromJson(
              Map<String, dynamic>.from(item as Map),
            ),
          )
          .toList(),
      daily: (json['daily'] as List<dynamic>? ?? const [])
          .map(
            (item) => DailyPostureStat.fromJson(
              Map<String, dynamic>.from(item as Map),
            ),
          )
          .toList(),
    );
  }

  double get normalRate {
    if (totalSamples == 0) {
      return 0;
    }
    return normalSamples / totalSamples;
  }
}
