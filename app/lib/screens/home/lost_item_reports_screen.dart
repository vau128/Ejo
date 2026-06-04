import 'package:flutter/material.dart';

import '../../models/lost_item_report.dart';
import '../../services/app_state.dart';

class LostItemReportsScreen extends StatelessWidget {
  const LostItemReportsScreen({super.key, required this.appState});

  final AppState appState;

  @override
  Widget build(BuildContext context) {
    final reports = appState.lostItemReports;

    return Scaffold(
      appBar: AppBar(title: const Text('분실물 리포트')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          if (reports.isEmpty)
            const _EmptyState()
          else
            ...List.generate(reports.length, (index) {
              final report = reports[index];
              return Padding(
                padding: EdgeInsets.only(bottom: index == reports.length - 1 ? 0 : 12),
                child: _ReportCard(report: report),
              );
            }),
        ],
      ),
    );
  }
}

class _EmptyState extends StatelessWidget {
  const _EmptyState();

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 48),
      child: Center(
        child: Text(
          '현재 좌석에 등록된 분실물 리포트가 없습니다.',
          style: Theme.of(context).textTheme.bodyLarge,
        ),
      ),
    );
  }
}

class _ReportCard extends StatelessWidget {
  const _ReportCard({required this.report});

  final LostItemReport report;

  @override
  Widget build(BuildContext context) {
    final imageWidget =
        report.imageUrl.startsWith('http://') ||
            report.imageUrl.startsWith('https://')
        ? Image.network(
            report.imageUrl,
            fit: BoxFit.cover,
            errorBuilder: (_, error, stackTrace) => _imageFallback(context),
          )
        : Image.asset(
            report.imageUrl,
            fit: BoxFit.cover,
            errorBuilder: (_, error, stackTrace) => _imageFallback(context),
          );

    return Card(
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          AspectRatio(aspectRatio: 16 / 9, child: imageWidget),
          Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '좌석 번호 ${report.seatNumber}',
                  style: Theme.of(context).textTheme.titleMedium,
                ),
                const SizedBox(height: 8),
                Text('감지 시간 ${_formatDateTime(report.detectedAt)}'),
                const SizedBox(height: 6),
                Text('종류: ${report.classificationStatus}'),
              ],
            ),
          ),
        ],
      ),
    );
  }

  String _formatDateTime(DateTime dateTime) {
    final month = dateTime.month.toString().padLeft(2, '0');
    final day = dateTime.day.toString().padLeft(2, '0');
    final hour = dateTime.hour.toString().padLeft(2, '0');
    final minute = dateTime.minute.toString().padLeft(2, '0');
    return '$month/$day $hour:$minute';
  }

  Widget _imageFallback(BuildContext context) {
    return Container(
      color: Theme.of(context).colorScheme.surfaceContainerHighest,
      alignment: Alignment.center,
      child: const Icon(Icons.image_not_supported_outlined, size: 40),
    );
  }
}
