import 'package:flutter/material.dart';

import '../../models/posture_stats.dart';
import '../../models/seat.dart';
import '../../services/app_state.dart';
import '../../widgets/empty_state_view.dart';
import 'posture_stats_screen.dart';

class MySeatScreen extends StatelessWidget {
  const MySeatScreen({super.key, required this.appState});

  final AppState appState;

  @override
  Widget build(BuildContext context) {
    final selectedSeat = appState.mySeat;
    final postureStats = appState.postureStats;

    return Scaffold(
      appBar: AppBar(
        title: const Text('마이페이지'),
        actions: [
          IconButton(
            onPressed: () => _refresh(context),
            icon: const Icon(Icons.refresh),
            tooltip: '새로고침',
          ),
        ],
      ),
      body: selectedSeat == null
          ? const Padding(
              padding: EdgeInsets.all(16),
              child: EmptyStateView(
                icon: Icons.event_seat_outlined,
                title: '현재 발권한 좌석이 없습니다',
                message: '좌석 선택 화면에서 발권한 뒤 본인 좌석 상태와 자세를 확인할 수 있습니다.',
              ),
            )
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                _SeatSummaryCard(
                  seat: selectedSeat,
                  studentName: appState.currentUser?.name ?? '학생',
                  studentId: appState.currentUser?.studentId ?? '-',
                ),
                const SizedBox(height: 16),
                _HealthcareSummaryCard(seat: selectedSeat),
                const SizedBox(height: 16),
                _StatsSummaryCard(stats: postureStats),
                const SizedBox(height: 16),
                _PressurePanel(seat: selectedSeat),
                const SizedBox(height: 16),
                _GuideCard(seat: selectedSeat),
              ],
            ),
    );
  }

  Future<void> _refresh(BuildContext context) async {
    final message = await appState.refreshSeatStatuses();
    if (!context.mounted) {
      return;
    }

    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message ?? '마이페이지를 새로고침했습니다.')));
  }
}

class _SeatSummaryCard extends StatelessWidget {
  const _SeatSummaryCard({
    required this.seat,
    required this.studentName,
    required this.studentId,
  });

  final Seat seat;
  final String studentName;
  final String studentId;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('나의 좌석', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 16),
            Text('$studentName · $studentId'),
            const SizedBox(height: 8),
            Text('${seat.number}번 좌석 (${seat.location})'),
            const SizedBox(height: 8),
            Text(seat.checkedIn ? '발권 상태: 사용 중' : '발권 상태: 미사용'),
            if (seat.selectedAt != null) ...[
              const SizedBox(height: 8),
              Text('발권 시각: ${seat.selectedAt}'),
            ],
          ],
        ),
      ),
    );
  }
}

class _HealthcareSummaryCard extends StatelessWidget {
  const _HealthcareSummaryCard({required this.seat});

  final Seat seat;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('현재 자세 상태', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 12),
            Text(
              seat.posture,
              style: Theme.of(context).textTheme.headlineSmall,
            ),
            const SizedBox(height: 8),
            Text(seat.occupied ? '실제 착석 감지됨' : '현재는 미착석 상태입니다.'),
            if (seat.postureTimestamp != null) ...[
              const SizedBox(height: 8),
              Text('최근 센서 시각: ${seat.postureTimestamp}'),
            ],
          ],
        ),
      ),
    );
  }
}

class _StatsSummaryCard extends StatelessWidget {
  const _StatsSummaryCard({required this.stats});

  final PostureStats? stats;

  @override
  Widget build(BuildContext context) {
    final summary = stats;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('자세 통계', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 12),
            Text(
              summary == null
                  ? '자세 통계를 불러오는 중입니다.'
                  : '최근 1주일 측정 ${summary.totalSamples}회',
            ),
            const SizedBox(height: 8),
            Text(
              summary == null
                  ? '앱 새로고침 후 다시 확인해주세요.'
                  : '가장 많이 나온 자세: ${summary.mostFrequentPosture}',
            ),
            const SizedBox(height: 16),
            FilledButton.tonalIcon(
              onPressed: summary == null
                  ? null
                  : () {
                      Navigator.of(context).push(
                        MaterialPageRoute(
                          builder: (_) => PostureStatsScreen(stats: summary),
                        ),
                      );
                    },
              icon: const Icon(Icons.insights_outlined),
              label: const Text('자세 통계 페이지 보기'),
            ),
          ],
        ),
      ),
    );
  }
}

class _PressurePanel extends StatelessWidget {
  const _PressurePanel({required this.seat});

  final Seat seat;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('압력 센서 상태', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: _PressureCard(
                    label: '왼쪽',
                    value: seat.leftPressure,
                    color: const Color(0xFF42A5F5),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: _PressureCard(
                    label: '오른쪽',
                    value: seat.rightPressure,
                    color: const Color(0xFFEF5350),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: _PressureCard(
                    label: '등받이',
                    value: seat.backPressure,
                    color: const Color(0xFF66BB6A),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _PressureCard extends StatelessWidget {
  const _PressureCard({
    required this.label,
    required this.value,
    required this.color,
  });

  final String label;
  final int value;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        children: [
          Text(label),
          const SizedBox(height: 8),
          Text('$value', style: Theme.of(context).textTheme.headlineSmall),
        ],
      ),
    );
  }
}

class _GuideCard extends StatelessWidget {
  const _GuideCard({required this.seat});

  final Seat seat;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('자세 안내', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 12),
            Text(
              _actionGuide(seat.posture),
              style: Theme.of(context).textTheme.bodyLarge,
            ),
            const SizedBox(height: 14),
            Text(
              '이 화면은 본인 좌석 데이터만 표시합니다.',
              style: Theme.of(
                context,
              ).textTheme.bodyMedium?.copyWith(color: Colors.grey.shade700),
            ),
          ],
        ),
      ),
    );
  }

  String _actionGuide(String posture) {
    if (posture.contains('허리') || posture.contains('숙임')) {
      return '모니터를 눈높이에 맞추고 허리를 세워 앉아주세요.';
    }
    if (posture.contains('왼')) {
      return '왼쪽으로 기울어져 있습니다. 엉덩이와 어깨 중심을 맞춰주세요.';
    }
    if (posture.contains('오른')) {
      return '오른쪽으로 기울어져 있습니다. 의자 중앙에 앉아 균형을 맞춰주세요.';
    }
    return '현재 자세가 안정적입니다. 같은 자세가 오래 지속되지 않게 중간중간 스트레칭해주세요.';
  }
}
