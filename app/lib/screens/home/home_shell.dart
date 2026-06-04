import 'package:flutter/material.dart';
import 'dart:async';

import '../../services/app_state.dart';
import 'lost_item_reports_screen.dart';
import 'my_seat_screen.dart';
import 'seat_selection_screen.dart';
import 'settings_screen.dart';
import 'warnings_screen.dart';

class HomeShell extends StatefulWidget {
  const HomeShell({super.key, required this.appState});

  final AppState appState;

  @override
  State<HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends State<HomeShell> {
  int _currentIndex = 0;
  Timer? _warningPollTimer;

  @override
  void initState() {
    super.initState();
    _warningPollTimer = Timer.periodic(const Duration(seconds: 5), (_) async {
      if (!mounted) {
        return;
      }
      final settings = widget.appState.settings;
      if (!settings.pushEnabled || !settings.warningAlertEnabled) {
        return;
      }

      final warning = await widget.appState.pollWarningAlerts();
      if (!mounted || warning == null) {
        return;
      }

      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(
          SnackBar(
            content: Text(_snackBarMessage(warning)),
            duration: const Duration(seconds: 4),
          ),
        );
    });
  }

  @override
  void dispose() {
    _warningPollTimer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final screens = [
      SeatSelectionScreen(appState: widget.appState),
      MySeatScreen(appState: widget.appState),
      WarningsScreen(appState: widget.appState),
      LostItemReportsScreen(appState: widget.appState),
      SettingsScreen(appState: widget.appState),
    ];

    return Scaffold(
      body: SafeArea(child: screens[_currentIndex]),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _currentIndex,
        onDestinationSelected: (index) {
          setState(() {
            _currentIndex = index;
          });
        },
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.event_seat_outlined),
            selectedIcon: Icon(Icons.event_seat),
            label: '좌석 선택',
          ),
          NavigationDestination(
            icon: Icon(Icons.person_pin_circle_outlined),
            selectedIcon: Icon(Icons.person_pin_circle),
            label: '나의 자리',
          ),
          NavigationDestination(
            icon: Icon(Icons.warning_amber_outlined),
            selectedIcon: Icon(Icons.warning_amber),
            label: '경고 알림',
          ),
          NavigationDestination(
            icon: Icon(Icons.inventory_2_outlined),
            selectedIcon: Icon(Icons.inventory_2),
            label: '분실물 리포트',
          ),
          NavigationDestination(
            icon: Icon(Icons.settings_outlined),
            selectedIcon: Icon(Icons.settings),
            label: '앱 설정',
          ),
        ],
      ),
    );
  }

  String _snackBarMessage(dynamic warning) {
    final type = '${warning.warningType}'.toLowerCase();
    final status = '${warning.status}'.toLowerCase();
    if (type == 'admin_warning' || status == 'admin_warning') {
      return '관리자 경고: ${warning.seatNumber}번 좌석 ${warning.message}';
    }
    return '${warning.seatNumber}번 좌석 ${warning.message}';
  }
}
