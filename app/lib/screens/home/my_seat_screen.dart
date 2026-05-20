import 'package:flutter/material.dart';

import '../../models/seat.dart';
import '../../services/app_state.dart';
import '../../widgets/empty_state_view.dart';

class MySeatScreen extends StatelessWidget {
  const MySeatScreen({super.key, required this.appState});

  final AppState appState;

  @override
  Widget build(BuildContext context) {
    final selectedSeat = appState.selectedSeat;

    return Scaffold(
      appBar: AppBar(title: const Text('나의 자리')),
      body: selectedSeat == null
          ? const Padding(
              padding: EdgeInsets.all(16),
              child: EmptyStateView(
                icon: Icons.event_seat_outlined,
                title: '선택된 좌석이 없습니다',
                message: '좌석 선택 화면에서 1번부터 4번 좌석 중 하나를 선택해주세요.',
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
                _PressurePanel(seat: selectedSeat),
                const SizedBox(height: 16),
                _GuideCard(seat: selectedSeat),
              ],
            ),
    );
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
            Text('현재 이용 좌석', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 14),
            Row(
              children: [
                Container(
                  width: 64,
                  height: 64,
                  decoration: BoxDecoration(
                    color: const Color(0xFFE3F2FD),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: const Icon(Icons.event_seat, color: Color(0xFF1565C0)),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '${seat.number}번 좌석',
                        style: Theme.of(context).textTheme.headlineSmall
                            ?.copyWith(fontWeight: FontWeight.bold),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        seat.checkedIn
                            ? '체크인 상태로 이용 중입니다.'
                            : '아직 체크인되지 않은 좌석입니다.',
                        style: Theme.of(context).textTheme.bodyMedium,
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 18),
            Row(
              children: [
                Expanded(
                  child: _InfoTile(label: '이용자', value: studentName),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: _InfoTile(label: '학번', value: studentId),
                ),
              ],
            ),
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
    final status = _healthStatus(seat);
    final statusColor = _statusColor(status);

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('학생 헬스케어', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 14),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              decoration: BoxDecoration(
                color: statusColor.withValues(alpha: 0.12),
                borderRadius: BorderRadius.circular(999),
              ),
              child: Text(
                status,
                style: Theme.of(context).textTheme.labelLarge?.copyWith(
                  color: statusColor,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
            const SizedBox(height: 16),
            _DetailRow(label: '현재 자세', value: seat.posture),
            _DetailRow(label: '상태 메시지', value: _postureMessage(seat.posture)),
            _DetailRow(
              label: '헬스케어 해석',
              value: _healthcareInterpretation(seat),
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
              '통합 테스트 중에는 압력 센서 값이 들어오면 이 화면이 실시간 자세 상태로 갱신됩니다.',
              style: Theme.of(
                context,
              ).textTheme.bodyMedium?.copyWith(color: Colors.grey.shade700),
            ),
          ],
        ),
      ),
    );
  }
}

class _InfoTile extends StatelessWidget {
  const _InfoTile({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFFF7F9FC),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: Theme.of(
              context,
            ).textTheme.bodySmall?.copyWith(color: Colors.grey.shade600),
          ),
          const SizedBox(height: 6),
          Text(value, style: Theme.of(context).textTheme.titleMedium),
        ],
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
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: Theme.of(
              context,
            ).textTheme.bodySmall?.copyWith(color: color),
          ),
          const SizedBox(height: 8),
          Text(
            '$value',
            style: Theme.of(context).textTheme.headlineSmall?.copyWith(
              fontWeight: FontWeight.bold,
              color: color,
            ),
          ),
        ],
      ),
    );
  }
}

class _DetailRow extends StatelessWidget {
  const _DetailRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 10),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 88,
            child: Text(
              label,
              style: Theme.of(
                context,
              ).textTheme.bodyMedium?.copyWith(color: Colors.grey.shade600),
            ),
          ),
          Expanded(child: Text(value)),
        ],
      ),
    );
  }
}

String _healthStatus(Seat seat) {
  if (seat.posture == '정상') {
    return '바른 자세 유지 중';
  }
  if (seat.backPressure == 0 &&
      (seat.leftPressure > 0 || seat.rightPressure > 0)) {
    return '허리 지지 부족';
  }
  return '자세 교정 필요';
}

Color _statusColor(String status) {
  switch (status) {
    case '바른 자세 유지 중':
      return const Color(0xFF2E7D32);
    case '허리 지지 부족':
      return const Color(0xFFEF6C00);
    default:
      return const Color(0xFFC62828);
  }
}

String _postureMessage(String posture) {
  switch (posture) {
    case '정상':
      return '바른 자세를 유지하고 있습니다.';
    case '거북목/허리 숙임':
      return '상체가 앞으로 숙여져 있습니다.';
    case '왼쪽으로 기울어짐':
      return '왼쪽으로 체중이 쏠려 있습니다.';
    case '오른쪽으로 기울어짐(다리 꼬기)':
      return '오른쪽으로 체중이 쏠려 있습니다.';
    case '등받이에 기대지 않음':
      return '등받이 지지가 부족합니다.';
    default:
      return posture;
  }
}

String _healthcareInterpretation(Seat seat) {
  if (seat.posture == '정상') {
    return '현재 자세가 안정적입니다. 그대로 유지하면 됩니다.';
  }
  if (seat.backPressure == 0 &&
      (seat.leftPressure > 0 || seat.rightPressure > 0)) {
    return '엉덩이 압력은 감지되지만 등받이 지지가 없습니다. 허리를 기대는 자세가 필요합니다.';
  }
  return '좌우 압력 불균형이 감지되었습니다. 체중을 중앙으로 맞춰 앉는 것이 좋습니다.';
}

String _actionGuide(String posture) {
  switch (posture) {
    case '정상':
      return '바른 자세 유지 중입니다.';
    case '거북목/허리 숙임':
      return '고개를 들고 허리를 펴서 화면과 눈높이를 맞춰주세요.';
    case '왼쪽으로 기울어짐':
      return '왼쪽으로 기울어진 자세를 바로잡고 중심을 맞춰주세요.';
    case '오른쪽으로 기울어짐(다리 꼬기)':
      return '다리 꼬기를 풀고 양발을 바닥에 안정적으로 두세요.';
    case '등받이에 기대지 않음':
      return '등을 등받이에 기대고 허리를 지지해주세요.';
    default:
      return posture;
  }
}
