import 'package:flutter/material.dart';

import '../models/seat.dart';

class SeatCard extends StatelessWidget {
  const SeatCard({
    super.key,
    required this.seat,
    required this.statusLabel,
    required this.backgroundColor,
    required this.onTap,
  });

  final Seat seat;
  final String statusLabel;
  final Color backgroundColor;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final foregroundColor =
        ThemeData.estimateBrightnessForColor(backgroundColor) == Brightness.dark
        ? Colors.white
        : const Color(0xFF1F1F1F);

    return Material(
      color: backgroundColor,
      borderRadius: BorderRadius.circular(20),
      child: InkWell(
        borderRadius: BorderRadius.circular(20),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Icon(Icons.event_seat, color: foregroundColor, size: 24),
              const Spacer(),
              Text(
                '${seat.number}번 좌석',
                style: Theme.of(context).textTheme.titleMedium?.copyWith(
                  color: foregroundColor,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                statusLabel,
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: foregroundColor.withValues(alpha: 0.9),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
