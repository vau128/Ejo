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
    warningCount: 2,
    agreedToPrivacy: true,
  );

  static const AppSettings defaultSettings = AppSettings(
    pushEnabled: true,
    seatAlertEnabled: true,
    warningAlertEnabled: false,
  );

  static const List<Seat> initialSeats = [
    Seat(id: 'seat-1', number: 1, status: SeatStatus.available),
    Seat(id: 'seat-2', number: 2, status: SeatStatus.occupied),
    Seat(id: 'seat-3', number: 3, status: SeatStatus.item),
    Seat(id: 'seat-4', number: 4, status: SeatStatus.reserved),
    Seat(id: 'seat-5', number: 5, status: SeatStatus.available),
    Seat(id: 'seat-6', number: 6, status: SeatStatus.occupied),
  ];

  static final List<LostItemReport> lostItemReports = [
    LostItemReport(
      id: 'report-1',
      seatNumber: 2,
      detectedAt: DateTime(2026, 4, 12, 9, 10),
      imageUrl: 'assets/images/lost_item_1.png',
      classificationStatus: '보류',
    ),
    LostItemReport(
      id: 'report-2',
      seatNumber: 3,
      detectedAt: DateTime(2026, 4, 12, 11, 45),
      imageUrl: 'assets/images/lost_item_2.png',
      classificationStatus: '보류',
    ),
    LostItemReport(
      id: 'report-3',
      seatNumber: 6,
      detectedAt: DateTime(2026, 4, 11, 18, 20),
      imageUrl: 'assets/images/lost_item_3.png',
      classificationStatus: '보류',
    ),
  ];
}
