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
  bool _agreedToPrivacy = false;

  @override
  void dispose() {
    _nameController.dispose();
    _studentIdController.dispose();
    _emailController.dispose();
    _passwordController.dispose();
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
                TextFormField(
                  controller: _nameController,
                  decoration: const InputDecoration(labelText: '이름'),
                  validator: (value) => value == null || value.trim().isEmpty
                      ? '이름을 입력해주세요.'
                      : null,
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _studentIdController,
                  keyboardType: TextInputType.number,
                  decoration: const InputDecoration(labelText: '학번'),
                  validator: (value) => value == null || value.trim().isEmpty
                      ? '학번을 입력해주세요.'
                      : null,
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _emailController,
                  keyboardType: TextInputType.emailAddress,
                  decoration: const InputDecoration(labelText: '이메일'),
                  validator: (value) => value == null || value.trim().isEmpty
                      ? '이메일을 입력해주세요.'
                      : null,
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _passwordController,
                  obscureText: true,
                  decoration: const InputDecoration(labelText: '비밀번호'),
                  validator: (value) {
                    if (value == null || value.trim().length < 6) {
                      return '비밀번호는 6자 이상 입력해주세요.';
                    }
                    return null;
                  },
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
