import 'package:flutter/material.dart';

import '../../widgets/section_card.dart';

class DashboardScreen extends StatelessWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const Column(
      children: [
        SectionCard(
          title: '시설 예약',
          subtitle: '준비 중',
          description: '스터디룸, 열람실 예약 기능은 추후 제공 예정입니다.',
          icon: Icons.meeting_room_outlined,
        ),
        SizedBox(height: 12),
        SectionCard(
          title: '도서 검색',
          subtitle: '준비 중',
          description: '도서 검색과 상세 정보 조회 기능은 추후 제공 예정입니다.',
          icon: Icons.menu_book_outlined,
        ),
        SizedBox(height: 12),
        SectionCard(
          title: '대출 이력 조회',
          subtitle: '준비 중',
          description: '개인 대출 이력 조회 기능은 추후 제공 예정입니다.',
          icon: Icons.history_edu_outlined,
        ),
      ],
    );
  }
}
