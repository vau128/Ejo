import 'package:flutter/material.dart';

import '../../models/seat.dart';
import '../../services/app_state.dart';
import '../../widgets/seat_card.dart';
import '../../widgets/status_legend.dart';
import 'dashboard_screen.dart';

class SeatSelectionScreen extends StatelessWidget {
  const SeatSelectionScreen({super.key, required this.appState});

  final AppState appState;

  @override
  Widget build(BuildContext context) {
    final seats = appState.seats;

    return Scaffold(
      appBar: AppBar(
        title: const Text('좌석 선택'),
        actions: [
          IconButton(
            onPressed: () {
              appState.refreshSeatStatuses();
              ScaffoldMessenger.of(
                context,
              ).showSnackBar(const SnackBar(content: Text('상태가 새로고침되었습니다.')));
            },
            icon: const Icon(Icons.refresh),
            tooltip: '상태 새로고침',
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('좌석 현황', style: Theme.of(context).textTheme.titleLarge),
                  const SizedBox(height: 8),
                  Align(
                    alignment: Alignment.centerLeft,
                    child: FilledButton.icon(
                      onPressed: () {
                        appState.refreshSeatStatuses();
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(content: Text('상태가 새로고침되었습니다.')),
                        );
                      },
                      icon: const Icon(Icons.refresh),
                      label: const Text('상태 새로고침'),
                    ),
                  ),
                  const SizedBox(height: 16),
                  const StatusLegend(),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
          GridView.builder(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            itemCount: seats.length,
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 2,
              crossAxisSpacing: 12,
              mainAxisSpacing: 12,
              childAspectRatio: 1.38,
            ),
            itemBuilder: (context, index) {
              final seat = seats[index];
              return SeatCard(
                seat: seat,
                statusLabel: seat.selectedByCurrentUser
                    ? '현재 선택됨'
                    : appState.seatStatusLabel(seat.status),
                backgroundColor: appState.seatStatusColor(seat),
                onTap: () => _handleSeatTap(context, seat),
              );
            },
          ),
          const SizedBox(height: 20),
          const DashboardScreen(),
        ],
      ),
    );
  }

  void _handleSeatTap(BuildContext context, Seat seat) {
    final message = appState.toggleSeatSelection(seat.id);

    if (message == null) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('${seat.number}번 좌석이 선택되었습니다.')));
      return;
    }

    final isBlockedStatus =
        seat.status == SeatStatus.occupied ||
        seat.status == SeatStatus.item ||
        seat.status == SeatStatus.reserved;

    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message)));

    if (isBlockedStatus) {
      return;
    }
  }
}
