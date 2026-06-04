import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../models/warning_alert.dart';
import '../../services/app_state.dart';

class WarningsScreen extends StatefulWidget {
  const WarningsScreen({super.key, required this.appState});

  final AppState appState;

  @override
  State<WarningsScreen> createState() => _WarningsScreenState();
}

class _WarningsScreenState extends State<WarningsScreen> {
  bool _loading = true;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _loadWarnings();
  }

  Future<void> _loadWarnings() async {
    setState(() {
      _loading = true;
      _errorMessage = null;
    });

    final message = await widget.appState.refreshWarningAlerts();

    if (!mounted) {
      return;
    }

    setState(() {
      _loading = false;
      _errorMessage = message;
    });
  }

  @override
  Widget build(BuildContext context) {
    final warnings = widget.appState.warningAlerts;

    return Scaffold(
      appBar: AppBar(
        title: const Text('경고 알림'),
        actions: [
          IconButton(
            onPressed: _loadWarnings,
            icon: const Icon(Icons.refresh),
            tooltip: '새로고침',
          ),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: _loading
            ? const Center(child: CircularProgressIndicator())
            : _errorMessage != null
            ? _ErrorState(message: _errorMessage!, onRetry: _loadWarnings)
            : warnings.isEmpty
            ? const _EmptyState()
            : ListView.separated(
                itemCount: warnings.length,
                separatorBuilder: (_, _) => const SizedBox(height: 12),
                itemBuilder: (context, index) {
                  final warning = warnings[index];
                  return _WarningCard(warning: warning);
                },
              ),
      ),
    );
  }
}

class _WarningCard extends StatelessWidget {
  const _WarningCard({required this.warning});

  final WarningAlert warning;

  @override
  Widget build(BuildContext context) {
    final formatter = DateFormat('yyyy-MM-dd HH:mm');

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Icon(
                  Icons.warning_amber_rounded,
                  color: Theme.of(context).colorScheme.error,
                  size: 28,
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    '⚠ ${warning.seatNumber}번 좌석 ${_titleForStatus(warning.status)}',
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Text(
              warning.message.isNotEmpty
                  ? warning.message
                  : _descriptionForStatus(warning.status),
              style: Theme.of(context).textTheme.bodyMedium,
            ),
            const SizedBox(height: 12),
            Text(
              '발생 시간: ${formatter.format(warning.warningTime.toLocal())}',
              style: Theme.of(context).textTheme.bodySmall,
            ),
          ],
        ),
      ),
    );
  }
}

class _EmptyState extends StatelessWidget {
  const _EmptyState();

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Text(
        '현재 경고 알림이 없습니다.',
        style: Theme.of(context).textTheme.bodyLarge,
      ),
    );
  }
}

class _ErrorState extends StatelessWidget {
  const _ErrorState({required this.message, required this.onRetry});

  final String message;
  final Future<void> Function() onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(message, style: Theme.of(context).textTheme.bodyLarge),
          const SizedBox(height: 12),
          FilledButton.icon(
            onPressed: onRetry,
            icon: const Icon(Icons.refresh),
            label: const Text('새로고침'),
          ),
        ],
      ),
    );
  }
}

String _titleForStatus(String status) {
  switch (status.toLowerCase()) {
    case 'squatting':
      return '사석화 경고';
    case 'abnormal':
      return '비정상 좌석 경고';
    case 'admin_warning':
      return '관리자 경고';
    case 'vacant_long':
      return '장시간 비움 경고';
    default:
      return '경고';
  }
}

String _descriptionForStatus(String status) {
  switch (status.toLowerCase()) {
    case 'squatting':
      return '좌석을 비운 지 설정 시간이 지나 경고가 발생했습니다.';
    case 'abnormal':
      return '좌석 상태가 비정상으로 감지되어 확인이 필요합니다.';
    case 'admin_warning':
      return '관리자가 좌석 상태 이상을 확인하고 경고를 발송했습니다.';
    case 'vacant_long':
      return '발권된 좌석이 장시간 비어 있어 확인이 필요합니다.';
    default:
      return '좌석 상태 이상이 감지되었습니다.';
  }
}
