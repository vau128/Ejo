import 'package:flutter/material.dart';

import '../../services/app_state.dart';

class SignupScreen extends StatefulWidget {
  const SignupScreen({super.key, required this.appState});

  final AppState appState;

  @override
  State<SignupScreen> createState() => _SignupScreenState();
}

class _SignupScreenState extends State<SignupScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _studentIdController = TextEditingController();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();
  bool _agreedToPrivacy = false;

  String? _validateName(String? value) {
    final text = value?.trim() ?? '';
    if (text.isEmpty) {
      return '이름을 입력해주세요.';
    }
    if (text.length < 2) {
      return '이름은 2자 이상 입력해주세요.';
    }
    return null;
  }

  String? _validateStudentId(String? value) {
    final text = value?.trim() ?? '';
    if (text.isEmpty) {
      return '학번을 입력해주세요.';
    }
    if (!RegExp(r'^\d{8,10}$').hasMatch(text)) {
      return '학번은 숫자 8~10자리로 입력해주세요.';
    }
    return null;
  }

  String? _validateEmail(String? value) {
    final text = value?.trim() ?? '';
    if (text.isEmpty) {
      return '이메일을 입력해주세요.';
    }
    if (!RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(text)) {
      return '올바른 이메일 형식을 입력해주세요.';
    }
    return null;
  }

  String? _validatePassword(String? value) {
    final text = value ?? '';
    if (text.length < 8) {
      return '비밀번호는 8자 이상 입력해주세요.';
    }
    if (!RegExp(r'^(?=.*[A-Za-z])(?=.*\d)').hasMatch(text)) {
      return '영문과 숫자를 모두 포함해주세요.';
    }
    return null;
  }

  @override
  void dispose() {
    _nameController.dispose();
    _studentIdController.dispose();
    _emailController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }

    final success = await widget.appState.signUp(
      name: _nameController.text,
      studentId: _studentIdController.text,
      email: _emailController.text,
      password: _passwordController.text,
      agreedToPrivacy: _agreedToPrivacy,
    );

    if (!mounted) {
      return;
    }

    if (success) {
      Navigator.of(context).pop();
      return;
    }

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(widget.appState.authErrorMessage ?? '회원가입에 실패했습니다.'),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isBusy = widget.appState.isBusy;

    return Scaffold(
      appBar: AppBar(title: const Text('학생 회원가입')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Form(
            key: _formKey,
            child: Column(
              children: [
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: const Color(0xFFF3F7FF),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: const Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '학생 회원가입',
                        style: TextStyle(
                          fontSize: 20,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      SizedBox(height: 8),
                      Text('학번, 학교 이메일, 비밀번호를 정확히 입력하면 바로 좌석 서비스를 사용할 수 있습니다.'),
                    ],
                  ),
                ),
                const SizedBox(height: 24),
                TextFormField(
                  controller: _nameController,
                  textInputAction: TextInputAction.next,
                  decoration: const InputDecoration(
                    labelText: '이름',
                    hintText: '홍길동',
                    helperText: '실명 기준으로 입력해주세요.',
                  ),
                  validator: _validateName,
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _studentIdController,
                  keyboardType: TextInputType.number,
                  textInputAction: TextInputAction.next,
                  decoration: const InputDecoration(
                    labelText: '학번',
                    hintText: '20261234',
                    helperText: '숫자만 입력해주세요.',
                  ),
                  validator: _validateStudentId,
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _emailController,
                  keyboardType: TextInputType.emailAddress,
                  textInputAction: TextInputAction.next,
                  decoration: const InputDecoration(
                    labelText: '이메일',
                    hintText: 'student@university.ac.kr',
                    helperText: '학교 이메일 사용을 권장합니다.',
                  ),
                  validator: _validateEmail,
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _passwordController,
                  obscureText: true,
                  textInputAction: TextInputAction.next,
                  decoration: const InputDecoration(
                    labelText: '비밀번호',
                    hintText: '8자 이상, 영문+숫자 조합',
                    helperText: '영문과 숫자를 포함한 8자 이상을 권장합니다.',
                  ),
                  validator: _validatePassword,
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _confirmPasswordController,
                  obscureText: true,
                  decoration: const InputDecoration(
                    labelText: '비밀번호 확인',
                    helperText: '위 비밀번호와 동일하게 입력해주세요.',
                  ),
                  validator: (value) {
                    if (value != _passwordController.text) {
                      return '비밀번호가 일치하지 않습니다.';
                    }
                    return null;
                  },
                ),
                const SizedBox(height: 8),
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: const Color(0xFFF8FAFC),
                    borderRadius: BorderRadius.circular(18),
                    border: Border.all(color: const Color(0xFFE2E8F0)),
                  ),
                  child: const Text(
                    '개인정보 동의 시 좌석 이용 기록, 경고 이력, 분실물 알림을 계정과 연결해 관리합니다.',
                    style: TextStyle(height: 1.5),
                  ),
                ),
                const SizedBox(height: 8),
                CheckboxListTile(
                  contentPadding: EdgeInsets.zero,
                  value: _agreedToPrivacy,
                  title: const Text('정보 동의'),
                  onChanged: isBusy
                      ? null
                      : (value) {
                          setState(() {
                            _agreedToPrivacy = value ?? false;
                          });
                        },
                ),
                const SizedBox(height: 16),
                FilledButton(
                  onPressed: isBusy ? null : _submit,
                  child: isBusy
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Text('학생 회원가입'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
