import 'package:flutter/material.dart';

import '../mock/mock_data.dart';
import '../models/app_settings.dart';
import '../models/lost_item_report.dart';
import '../models/seat.dart';
import '../models/student_user.dart';

class AppState extends ChangeNotifier {
  StudentUser? _currentUser;
  StudentUser? _registeredUser;
  String? _registeredPassword;
  bool _isLoggedIn = false;
  List<Seat> _seats = List<Seat>.from(MockData.initialSeats);
  late List<LostItemReport> _lostItemReports;
  AppSettings _settings = MockData.defaultSettings;
  String? _authErrorMessage;

  AppState() {
    _lostItemReports = List<LostItemReport>.from(MockData.lostItemReports);
  }

  StudentUser? get currentUser => _currentUser;
  bool get isLoggedIn => _isLoggedIn;
  List<Seat> get seats => List<Seat>.unmodifiable(_seats);
  List<LostItemReport> get lostItemReports =>
      List<LostItemReport>.unmodifiable(_lostItemReports);
  AppSettings get settings => _settings;
  String? get authErrorMessage => _authErrorMessage;
  int get warningCount => _currentUser?.warningCount ?? 0;

  Seat? get selectedSeat {
    for (final seat in _seats) {
      if (seat.selectedByCurrentUser) {
        return seat;
      }
    }
    return null;
  }

  void clearAuthError() {
    _authErrorMessage = null;
    notifyListeners();
  }

  bool login({required String email, required String password}) {
    final matchesMockUser =
        email.trim() == MockData.currentStudent.email &&
        password == MockData.mockPassword;
    final matchesSignedUpUser =
        _registeredUser != null &&
        email.trim() == _registeredUser!.email &&
        password == _registeredPassword;

    if (matchesMockUser) {
      _currentUser = MockData.currentStudent;
      _isLoggedIn = true;
      _authErrorMessage = null;
      notifyListeners();
      return true;
    }

    if (matchesSignedUpUser && password.isNotEmpty) {
      _currentUser = _registeredUser;
      _isLoggedIn = true;
      _authErrorMessage = null;
      notifyListeners();
      return true;
    }

    _authErrorMessage = '이메일 또는 비밀번호를 확인해주세요.';
    notifyListeners();
    return false;
  }

  bool signUp({
    required String name,
    required String studentId,
    required String email,
    required String password,
    required bool agreedToPrivacy,
  }) {
    if (!agreedToPrivacy) {
      _authErrorMessage = '정보 동의 후 회원가입이 가능합니다.';
      notifyListeners();
      return false;
    }

    if (password.trim().length < 6) {
      _authErrorMessage = '비밀번호는 6자 이상 입력해주세요.';
      notifyListeners();
      return false;
    }

    _currentUser = StudentUser(
      id: 'student-local-${DateTime.now().millisecondsSinceEpoch}',
      name: name.trim(),
      studentId: studentId.trim(),
      email: email.trim(),
      warningCount: 1,
      agreedToPrivacy: agreedToPrivacy,
    );
    _registeredUser = _currentUser;
    _registeredPassword = password;
    _isLoggedIn = true;
    _authErrorMessage = null;
    _seats = List<Seat>.from(MockData.initialSeats);
    notifyListeners();
    return true;
  }

  void logout() {
    _isLoggedIn = false;
    _currentUser = null;
    _authErrorMessage = null;
    _seats = List<Seat>.from(MockData.initialSeats);
    _settings = MockData.defaultSettings;
    notifyListeners();
  }

  String seatStatusLabel(SeatStatus status) {
    switch (status) {
      case SeatStatus.available:
        return '빈 좌석';
      case SeatStatus.occupied:
        return '사용 중';
      case SeatStatus.item:
        return '물품';
      case SeatStatus.reserved:
        return '사유석 의심';
    }
  }

  Color seatStatusColor(Seat seat) {
    if (seat.selectedByCurrentUser) {
      return const Color(0xFF1E88E5);
    }

    switch (seat.status) {
      case SeatStatus.available:
        return const Color(0xFFFBC02D);
      case SeatStatus.occupied:
        return const Color(0xFF757575);
      case SeatStatus.item:
        return const Color(0xFFD32F2F);
      case SeatStatus.reserved:
        return const Color(0xFFD32F2F);
    }
  }

  String? toggleSeatSelection(String seatId) {
    final targetSeat = _seats.firstWhere((seat) => seat.id == seatId);

    if (targetSeat.selectedByCurrentUser) {
      _seats = _seats
          .map(
            (seat) => seat.id == seatId
                ? seat.copyWith(selectedByCurrentUser: false)
                : seat,
          )
          .toList();
      notifyListeners();
      return '좌석 선택이 취소되었습니다.';
    }

    if (targetSeat.status != SeatStatus.available) {
      return '${targetSeat.number}번 좌석은 ${seatStatusLabel(targetSeat.status)} 상태입니다.';
    }

    _seats = _seats
        .map((seat) => seat.copyWith(selectedByCurrentUser: seat.id == seatId))
        .toList();
    notifyListeners();
    return null;
  }

  void refreshSeatStatuses() {
    final selectedSeatId = selectedSeat?.id;
    final rotatedStatuses = [
      SeatStatus.available,
      SeatStatus.occupied,
      SeatStatus.item,
      SeatStatus.reserved,
      SeatStatus.available,
      SeatStatus.occupied,
    ];

    _seats = _seats.asMap().entries.map((entry) {
      final index = entry.key;
      final seat = entry.value;
      final nextStatus =
          rotatedStatuses[(index + DateTime.now().second) %
              rotatedStatuses.length];
      final isSelected =
          seat.id == selectedSeatId && nextStatus == SeatStatus.available;

      return seat.copyWith(
        status: nextStatus,
        selectedByCurrentUser: isSelected,
      );
    }).toList();

    notifyListeners();
  }

  void updatePushEnabled(bool value) {
    _settings = _settings.copyWith(pushEnabled: value);
    notifyListeners();
  }

  void updateSeatAlertEnabled(bool value) {
    _settings = _settings.copyWith(seatAlertEnabled: value);
    notifyListeners();
  }

  void updateWarningAlertEnabled(bool value) {
    _settings = _settings.copyWith(warningAlertEnabled: value);
    notifyListeners();
  }
}
