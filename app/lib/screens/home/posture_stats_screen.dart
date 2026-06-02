import 'package:flutter/material.dart';

import '../../models/posture_stats.dart';

class PostureStatsScreen extends StatelessWidget {
  const PostureStatsScreen({super.key, required this.stats});

  final PostureStats stats;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('자세 통계')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _SummaryCard(stats: stats),
          const SizedBox(height: 16),
          _BreakdownCard(stats: stats),
          const SizedBox(height: 16),
          _DailyTrendCard(stats: stats),
        ],
      ),
    );
  }
}

class _SummaryCard extends StatelessWidget {
  const _SummaryCard({required this.stats});

  final PostureStats stats;

  @override
  Widget build(BuildContext context) {
    final normalRate = (stats.normalRate * 100).round();

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('최근 1주일 요약', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 8),
            Text('${stats.rangeStart} ~ ${stats.rangeEnd}'),
            const SizedBox(height: 16),
            Wrap(
              spacing: 12,
              runSpacing: 12,
              children: [
                _MetricChip(label: '측정 수', value: '${stats.totalSamples}회'),
                _MetricChip(label: '정상 비율', value: '$normalRate%'),
                _MetricChip(label: '이상 자세', value: '${stats.abnormalSamples}회'),
                _MetricChip(
                  label: '가장 많은 자세',
                  value: stats.mostFrequentPosture,
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _BreakdownCard extends StatelessWidget {
  const _BreakdownCard({required this.stats});

  final PostureStats stats;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('자세 유형 비율', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 12),
            if (stats.breakdown.isEmpty)
              const Text('아직 수집된 자세 데이터가 없습니다.')
            else
              ...stats.breakdown.map((item) => _BreakdownRow(item: item)),
          ],
        ),
      ),
    );
  }
}

class _DailyTrendCard extends StatelessWidget {
  const _DailyTrendCard({required this.stats});

  final PostureStats stats;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('일자별 자세 기록', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 12),
            ...stats.daily.map(
              (item) => ListTile(
                contentPadding: EdgeInsets.zero,
                title: Text('${item.label}  ${item.dominantPosture}'),
                subtitle: Text(
                  '정상 ${item.normalCount}회 · 이상 ${item.abnormalCount}회',
                ),
                trailing: Text('${item.totalCount}회'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _MetricChip extends StatelessWidget {
  const _MetricChip({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(14),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(label, style: Theme.of(context).textTheme.bodySmall),
          const SizedBox(height: 4),
          Text(value, style: Theme.of(context).textTheme.titleMedium),
        ],
      ),
    );
  }
}

class _BreakdownRow extends StatelessWidget {
  const _BreakdownRow({required this.item});

  final PostureBreakdownItem item;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(item.label),
              Text('${item.count}회 · ${item.percentage}%'),
            ],
          ),
          const SizedBox(height: 6),
          LinearProgressIndicator(value: item.percentage / 100),
        ],
      ),
    );
  }
}
