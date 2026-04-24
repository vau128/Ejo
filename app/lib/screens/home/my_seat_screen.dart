import 'package:flutter/material.dart';

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
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: selectedSeat == null
            ? const EmptyStateView(
                icon: Icons.event_seat_outlined,
                title: '선택된 좌석이 없습니다',
                message: '좌석 선택 화면에서 사용 가능한 좌석을 선택해주세요.',
              )
            : Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(20),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            '현재 선택된 좌석',
                            style: Theme.of(context).textTheme.titleMedium,
                          ),
                          const SizedBox(height: 16),
                          Text(
                            '${selectedSeat.number}번 좌석',
                            style: Theme.of(context).textTheme.headlineMedium
                                ?.copyWith(fontWeight: FontWeight.bold),
                          ),
                          const SizedBox(height: 12),
                          Text(
                            '현재 좌석은 모바일 앱에서 선택된 상태로 유지됩니다.',
                            style: Theme.of(context).textTheme.bodyMedium,
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                  Card(
                    child: ListTile(
                      leading: const CircleAvatar(
                        child: Icon(Icons.person_outline),
                      ),
                      title: Text(appState.currentUser?.name ?? '학생'),
                      subtitle: Text(
                        '학번 ${appState.currentUser?.studentId ?? '-'}',
                      ),
                      trailing: const Icon(Icons.chevron_right),
                    ),
                  ),
                ],
              ),
      ),
    );
  }
}
