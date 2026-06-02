import 'dart:convert';

import 'package:http/http.dart' as http;

import '../config/api_config.dart';
import '../models/app_settings.dart';
import '../models/lost_item_report.dart';
import '../models/posture_stats.dart';
import '../models/seat.dart';
import '../models/student_user.dart';
import '../models/warning_alert.dart';

class AppApiException implements Exception {
  const AppApiException(this.message);

  final String message;

  @override
  String toString() => message;
}

class AuthResult {
  const AuthResult({required this.token, required this.user});

  final String token;
  final StudentUser user;
}

class AppApi {
  const AppApi();

  Future<AuthResult> login({
    required String email,
    required String password,
  }) async {
    final json = await _request(
      'POST',
      '/auth/login',
      body: {'email': email.trim(), 'password': password},
    );
    return AuthResult(
      token: json['token'] as String,
      user: StudentUser.fromJson(_map(json['user'])),
    );
  }

  Future<AuthResult> signUp({
    required String name,
    required String studentId,
    required String email,
    required String password,
    required bool agreedToPrivacy,
  }) async {
    final json = await _request(
      'POST',
      '/auth/signup',
      body: {
        'name': name.trim(),
        'studentId': studentId.trim(),
        'email': email.trim(),
        'password': password,
        'agreedToPrivacy': agreedToPrivacy,
      },
    );
    return AuthResult(
      token: json['token'] as String,
      user: StudentUser.fromJson(_map(json['user'])),
    );
  }

  Future<StudentUser> fetchCurrentUser(String token) async {
    final json = await _request('GET', '/me', token: token);
    return StudentUser.fromJson(_map(json['user']));
  }

  Future<List<Seat>> fetchSeats(String token) async {
    final json = await _request('GET', '/seats', token: token);
    return (json['seats'] as List<dynamic>? ?? const [])
        .map((item) => Seat.fromJson(_map(item)))
        .toList();
  }

  Future<Seat?> fetchMySeat(String token) async {
    final json = await _request('GET', '/me/seat', token: token);
    final seat = json['seat'];
    if (seat == null) {
      return null;
    }
    return Seat.fromJson(_map(seat));
  }

  Future<PostureStats> fetchMyPostureStats(String token) async {
    final json = await _request('GET', '/me/posture-stats', token: token);
    return PostureStats.fromJson(json);
  }

  Future<String> toggleSeatSelection(String token, String seatId) async {
    final json = await _request(
      'POST',
      '/seats/$seatId/selection',
      token: token,
    );
    return json['message'] as String? ?? '좌석 상태가 변경되었습니다.';
  }

  Future<List<LostItemReport>> fetchLostItems(String token) async {
    final json = await _request('GET', '/lost-items', token: token);
    return (json['reports'] as List<dynamic>? ?? const [])
        .map((item) => LostItemReport.fromJson(_map(item)))
        .toList();
  }

  Future<AppSettings> fetchSettings(String token) async {
    final json = await _request('GET', '/settings', token: token);
    return AppSettings.fromJson(_map(json['settings']));
  }

  Future<AppSettings> updateSettings(String token, AppSettings settings) async {
    final json = await _request(
      'PATCH',
      '/settings',
      token: token,
      body: {
        'pushEnabled': settings.pushEnabled,
        'seatAlertEnabled': settings.seatAlertEnabled,
        'warningAlertEnabled': settings.warningAlertEnabled,
      },
    );
    return AppSettings.fromJson(_map(json['settings']));
  }

  Future<List<WarningAlert>> fetchWarnings(String token) async {
    final json = await _request('GET', '/me/warnings', token: token);
    return (json['warnings'] as List<dynamic>? ?? const [])
        .map((item) => WarningAlert.fromJson(_map(item)))
        .toList();
  }

  Future<Map<String, dynamic>> _request(
    String method,
    String path, {
    String? token,
    Map<String, dynamic>? body,
  }) async {
    final uri = Uri.parse('${ApiConfig.baseUrl}$path');
    final headers = <String, String>{
      'Accept': 'application/json',
      if (body != null) 'Content-Type': 'application/json',
      ...?(token == null ? null : {'X-Student-Token': token}),
    };

    late final http.Response response;
    try {
      switch (method) {
        case 'GET':
          response = await http.get(uri, headers: headers);
          break;
        case 'POST':
          response = await http.post(
            uri,
            headers: headers,
            body: body == null ? null : jsonEncode(body),
          );
          break;
        case 'PATCH':
          response = await http.patch(
            uri,
            headers: headers,
            body: body == null ? null : jsonEncode(body),
          );
          break;
        default:
          throw const AppApiException('지원하지 않는 요청 방식입니다.');
      }
    } catch (_) {
      throw const AppApiException('백엔드에 연결할 수 없습니다. 서버 주소와 실행 상태를 확인해주세요.');
    }

    final decoded = response.body.isEmpty
        ? <String, dynamic>{}
        : jsonDecode(response.body) as Map<String, dynamic>;

    if (response.statusCode >= 400) {
      final message =
          decoded['message'] as String? ??
          decoded['error'] as String? ??
          '요청 처리에 실패했습니다.';
      throw AppApiException(message);
    }

    return decoded;
  }

  Map<String, dynamic> _map(Object? value) {
    return Map<String, dynamic>.from(value as Map);
  }
}
