import 'package:flutter/material.dart';

import 'screens/auth/login_screen.dart';
import 'screens/home/home_shell.dart';
import 'services/app_state.dart';
import 'theme/app_theme.dart';

class SeatAppRoot extends StatefulWidget {
  const SeatAppRoot({super.key});

  @override
  State<SeatAppRoot> createState() => _SeatAppRootState();
}

class _SeatAppRootState extends State<SeatAppRoot> {
  final AppState _appState = AppState();

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _appState,
      builder: (context, _) {
        return MaterialApp(
          title: '도서관 좌석 관리',
          debugShowCheckedModeBanner: false,
          theme: AppTheme.lightTheme,
          home: _appState.isLoggedIn
              ? HomeShell(appState: _appState)
              : LoginScreen(appState: _appState),
        );
      },
    );
  }
}
