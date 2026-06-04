import 'package:flutter/material.dart';

import '../../services/app_state.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key, required this.appState});

  final AppState appState;

  @override
  Widget build(BuildContext context) {
    final settings = appState.settings;

    return Scaffold(
      appBar: AppBar(
        title: const Text('앱 설정'),
        actions: [
          IconButton(
            onPressed: appState.logout,
            icon: const Icon(Icons.logout),
            tooltip: '로그아웃',
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            child: Column(
              children: [
                SwitchListTile(
                  value: settings.pushEnabled,
                  title: const Text('푸시 알림 수신 여부'),
                  onChanged: (value) => _updateSetting(
                    context,
                    () => appState.updatePushEnabled(value),
                  ),
                ),
                const Divider(height: 1),
                SwitchListTile(
                  value: settings.seatAlertEnabled,
                  title: const Text('좌석 알림 수신 여부'),
                  onChanged: (value) => _updateSetting(
                    context,
                    () => appState.updateSeatAlertEnabled(value),
                  ),
                ),
                const Divider(height: 1),
                SwitchListTile(
                  value: settings.warningAlertEnabled,
                  title: const Text('경고 알림 수신 여부'),
                  onChanged: (value) => _updateSetting(
                    context,
                    () => appState.updateWarningAlertEnabled(value),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _updateSetting(
    BuildContext context,
    Future<String?> Function() action,
  ) async {
    final message = await action();
    if (!context.mounted || message == null) {
      return;
    }

    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message)));
  }
}
