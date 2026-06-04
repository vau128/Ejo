import 'package:flutter/material.dart';

import '../models/app_settings.dart';
import '../models/lost_item_report.dart';
import '../models/posture_stats.dart';
import '../models/seat.dart';
import '../models/student_user.dart';
import '../models/warning_alert.dart';
import 'app_api.dart';

class AppState extends ChangeNotifier {
  final AppApi _api = const AppApi();

  StudentUser? _currentUser;
  String? _authToken;
  bool _isLoggedIn = false;
  bool _isBusy = false;
  List<Seat> _seats = const [];
  Seat? _mySeat;
  List<LostItemReport> _lostItemReports = const [];
  List<WarningAlert> _warningAlerts = const [];
  PostureStats? _postureStats;
  AppSettings _settings = const AppSettings(
    pushEnabled: true,
    seatAlertEnabled: true,
    warningAlertEnabled: true,
  );
  String? _authErrorMessage;

  StudentUser? get currentUser => _currentUser;
  bool get isLoggedIn => _isLoggedIn;
  bool get isBusy => _isBusy;
  List<Seat> get seats => List<Seat>.unmodifiable(_seats);
  Seat? get mySeat => _mySeat;
  List<LostItemReport> get lostItemReports =>
      List<LostItemReport>.unmodifiable(_lostItemReports);
  List<WarningAlert> get warningAlerts =>
      List<WarningAlert>.unmodifiable(_warningAlerts);
  PostureStats? get postureStats => _postureStats;
  AppSettings get settings => _settings;
  String? get authErrorMessage => _authErrorMessage;
  int get warningCount => _currentUser?.warningCount ?? 0;

  Seat? get selectedSeat => _mySeat;

  void clearAuthError() {
    _authErrorMessage = null;
    notifyListeners();
  }

  Future<bool> login({required String email, required String password}) async {
    _setBusy(true);
    try {
      final result = await _api.login(email: email, password: password);
      _authToken = result.token;
      _currentUser = result.user;
      await _loadAppData();
      _isLoggedIn = true;
      _authErrorMessage = null;
      notifyListeners();
      return true;
    } on AppApiException catch (error) {
      _authErrorMessage = error.message;
      notifyListeners();
      return false;
    } finally {
      _setBusy(false);
    }
  }

  Future<bool> signUp({
    required String name,
    required String studentId,
    required String email,
    required String password,
    required bool agreedToPrivacy,
  }) async {
    if (!agreedToPrivacy) {
      _authErrorMessage = '정보 동의 후 회원가입이 가능합니다.';
      notifyListeners();
      return false;
    }

    _setBusy(true);
    try {
      final result = await _api.signUp(
        name: name,
        studentId: studentId,
        email: email,
        password: password,
        agreedToPrivacy: agreedToPrivacy,
      );
      _authToken = result.token;
      _currentUser = result.user;
      await _loadAppData();
      _isLoggedIn = true;
      _authErrorMessage = null;
      notifyListeners();
      return true;
    } on AppApiException catch (error) {
      _authErrorMessage = error.message;
      notifyListeners();
      return false;
    } finally {
      _setBusy(false);
    }
  }

  void logout() {
    _isLoggedIn = false;
    _currentUser = null;
    _authToken = null;
    _authErrorMessage = null;
    _seats = const [];
    _mySeat = null;
    _lostItemReports = const [];
    _warningAlerts = const [];
    _postureStats = null;
    _settings = const AppSettings(
      pushEnabled: true,
      seatAlertEnabled: true,
      warningAlertEnabled: true,
    );
    notifyListeners();
  }

  String seatStatusLabel(SeatStatus status) {
    switch (status) {
      case SeatStatus.available:
        return '빈 좌석';
      case SeatStatus.reserved:
        return '발권됨';
      case SeatStatus.occupied:
        return '사용 중';
      case SeatStatus.objectOnly:
        return '물품 감지';
      case SeatStatus.vacantLong:
        return '장시간 비움';
      case SeatStatus.sensorDelay:
        return '센서 지연';
    }
  }

  Color seatStatusColor(Seat seat) {
    if (seat.selectedByCurrentUser) {
      return const Color(0xFF1E88E5);
    }

    switch (seat.status) {
      case SeatStatus.available:
        return const Color(0xFFFBC02D);
      case SeatStatus.reserved:
        return const Color(0xFF1E88E5);
      case SeatStatus.occupied:
        return const Color(0xFF757575);
      case SeatStatus.objectOnly:
        return const Color(0xFFFB8C00);
      case SeatStatus.vacantLong:
        return const Color(0xFFD32F2F);
      case SeatStatus.sensorDelay:
        return const Color(0xFF8E24AA);
    }
  }

  Future<String?> toggleSeatSelection(String seatId) async {
    final token = _authToken;
    if (token == null) {
      return '로그인이 필요합니다.';
    }

    try {
      final message = await _api.toggleSeatSelection(token, seatId);
      await _refreshSeatRelatedData(token);
      notifyListeners();
      return message;
    } on AppApiException catch (error) {
      return error.message;
    }
  }

  Future<String?> refreshSeatStatuses() async {
    final token = _authToken;
    if (token == null) {
      return '로그인이 필요합니다.';
    }

    try {
      await _refreshSeatRelatedData(token);
      notifyListeners();
      return null;
    } on AppApiException catch (error) {
      return error.message;
    }
  }

  Future<String?> refreshWarningAlerts() async {
    final token = _authToken;
    if (token == null) {
      return '로그인이 필요합니다.';
    }

    try {
      _warningAlerts = await _api.fetchWarnings(token);
      _currentUser = _currentUser?.copyWith(
        warningCount: _warningAlerts.length,
      );
      notifyListeners();
      return null;
    } on AppApiException catch (error) {
      return error.message;
    }
  }

  Future<String?> refreshPostureStats() async {
    final token = _authToken;
    if (token == null) {
      return '로그인이 필요합니다.';
    }

    try {
      _postureStats = await _api.fetchMyPostureStats(token);
      notifyListeners();
      return null;
    } on AppApiException catch (error) {
      return error.message;
    }
  }

  Future<WarningAlert?> pollWarningAlerts() async {
    final token = _authToken;
    if (token == null) {
      return null;
    }

    try {
      final previousTopId = _warningAlerts.isEmpty
          ? null
          : _warningAlerts.first.id;
      final nextWarnings = await _api.fetchWarnings(token);
      _warningAlerts = nextWarnings;
      _currentUser = _currentUser?.copyWith(
        warningCount: _warningAlerts.length,
      );
      notifyListeners();

      if (nextWarnings.isEmpty) {
        return null;
      }

      if (previousTopId == null || nextWarnings.first.id != previousTopId) {
        return nextWarnings.first;
      }
      return null;
    } on AppApiException {
      return null;
    }
  }

  Future<String?> updatePushEnabled(bool value) async {
    return _saveSettings(_settings.copyWith(pushEnabled: value));
  }

  Future<String?> updateSeatAlertEnabled(bool value) async {
    return _saveSettings(_settings.copyWith(seatAlertEnabled: value));
  }

  Future<String?> updateWarningAlertEnabled(bool value) async {
    return _saveSettings(_settings.copyWith(warningAlertEnabled: value));
  }

  Future<void> _loadAppData() async {
    final token = _authToken;
    if (token == null) {
      return;
    }

    _currentUser = await _api.fetchCurrentUser(token);
    await _refreshSeatRelatedData(token);
    _lostItemReports = await _api.fetchLostItems(token);
    _warningAlerts = await _api.fetchWarnings(token);
    _currentUser = _currentUser?.copyWith(warningCount: _warningAlerts.length);
    _settings = await _api.fetchSettings(token);
  }

  Future<void> _refreshSeatRelatedData(String token) async {
    _seats = await _api.fetchSeats(token);
    _mySeat = await _api.fetchMySeat(token);
    _postureStats = await _api.fetchMyPostureStats(token);
  }

  Future<String?> _saveSettings(AppSettings nextSettings) async {
    final token = _authToken;
    if (token == null) {
      return '로그인이 필요합니다.';
    }

    try {
      _settings = await _api.updateSettings(token, nextSettings);
      notifyListeners();
      return null;
    } on AppApiException catch (error) {
      return error.message;
    }
  }

  void _setBusy(bool value) {
    _isBusy = value;
    notifyListeners();
  }
}
