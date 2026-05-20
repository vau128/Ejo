import '../models/app_settings.dart';
import '../models/lost_item_report.dart';
import '../models/seat.dart';
import '../models/student_user.dart';

class MockData {
  static const String mockPassword = 'password123';

  static const StudentUser currentStudent = StudentUser(
    id: 'student-001',
    name: '김도서',
    studentId: '20240001',
    email: 'student@library.com',
    warningCount: 0,
    agreedToPrivacy: true,
  );

  static const AppSettings defaultSettings = AppSettings(
    pushEnabled: true,
    seatAlertEnabled: true,
    warningAlertEnabled: false,
  );

  static const List<Seat> initialSeats = [
    Seat(id: 'seat-1', number: 1, status: SeatStatus.available, checkedIn: false, posture: '정상', leftPressure: 0, rightPressure: 0, backPressure: 0),
    Seat(id: 'seat-2', number: 2, status: SeatStatus.available, checkedIn: false, posture: '정상', leftPressure: 0, rightPressure: 0, backPressure: 0),
    Seat(id: 'seat-3', number: 3, status: SeatStatus.available, checkedIn: false, posture: '정상', leftPressure: 0, rightPressure: 0, backPressure: 0),
    Seat(id: 'seat-4', number: 4, status: SeatStatus.available, checkedIn: false, posture: '정상', leftPressure: 0, rightPressure: 0, backPressure: 0),
  ];

  static final List<LostItemReport> lostItemReports = [];
}
