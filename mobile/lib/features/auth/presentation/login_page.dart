import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../shared/widgets/app_widgets.dart';
import '../domain/auth_controller.dart';

class LoginPage extends ConsumerStatefulWidget {
  const LoginPage({
    super.key,
    this.registrationPending = false,
    this.initialEmail = '',
  });

  final bool registrationPending;
  final String initialEmail;

  @override
  ConsumerState<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends ConsumerState<LoginPage> {
  final _formKey = GlobalKey<FormState>();
  final _email = TextEditingController();
  final _password = TextEditingController();
  bool _resending = false;

  @override
  void initState() {
    super.initState();
    _email.text = widget.initialEmail;
  }

  @override
  void dispose() {
    _email.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;

    FocusScope.of(context).unfocus();

    final controller = ref.read(authControllerProvider.notifier);

    final success = await controller.login(
      email: _email.text,
      password: _password.text,
    );

    if (!success || !mounted) return;

    final fingerprintAvailable = await controller.canEnableFingerprint();

    if (fingerprintAvailable && mounted) {
      final enable = await showDialog<bool>(
        context: context,
        builder: (context) => AlertDialog(
          title: const Text('Activar acceso con huella'),
          content: const Text(
            '¿Quieres usar tu huella para ingresar más rápido la próxima vez?',
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context, false),
              child: const Text('Ahora no'),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(context, true),
              child: const Text('Activar'),
            ),
          ],
        ),
      );

      if (enable == true) {
        await controller.enableFingerprint();
      }
    }

    await controller.completeLogin();

    if (mounted) context.go('/');
  }

  Future<void> _loginWithFingerprint() async {
    final success = await ref
        .read(authControllerProvider.notifier)
        .loginWithFingerprint();
    if (success && mounted) context.go('/');
  }

  Future<void> _resendVerification() async {
    final email = _email.text.trim();

    if (email.isEmpty || !email.contains('@')) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Escribe un correo electrónico válido.')),
      );
      return;
    }

    setState(() => _resending = true);

    final message = await ref
        .read(authControllerProvider.notifier)
        .resendVerification(email);

    if (!mounted) return;

    setState(() => _resending = false);

    if (message != null) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(message)));
    }
  }

  @override
  Widget build(BuildContext context) {
    final auth = ref.watch(authControllerProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Iniciar sesión')),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 480),
              child: Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Icon(
                      Icons.bloodtype,
                      size: 52,
                      color: Theme.of(context).colorScheme.primary,
                    ),
                    const SizedBox(height: 12),
                    Text(
                      'Bienvenido a BloodConnect RD',
                      style: Theme.of(context).textTheme.headlineSmall
                          ?.copyWith(fontWeight: FontWeight.w800),
                    ),
                    const SizedBox(height: 8),
                    const Text(
                      'Conecta con personas que necesitan sangre en República Dominicana.',
                    ),
                    if (widget.registrationPending) ...[
                      const SizedBox(height: 20),
                      Container(
                        width: double.infinity,
                        padding: const EdgeInsets.all(16),
                        decoration: BoxDecoration(
                          color: Colors.green.shade50,
                          border: Border.all(color: Colors.green.shade200),
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text(
                              'Cuenta creada correctamente',
                              style: TextStyle(fontWeight: FontWeight.bold),
                            ),
                            const SizedBox(height: 4),
                            const Text(
                              'Revisa tu correo y confirma tu cuenta antes de iniciar sesión.',
                            ),
                            const SizedBox(height: 8),
                            TextButton(
                              onPressed: _resending
                                  ? null
                                  : _resendVerification,
                              child: Text(
                                _resending
                                    ? 'Reenviando…'
                                    : 'Reenviar correo de verificación',
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                    const SizedBox(height: 28),
                    const SizedBox(height: 28),
                    AppTextField(
                      controller: _email,
                      label: 'Correo electrónico',
                      keyboardType: TextInputType.emailAddress,
                      textInputAction: TextInputAction.next,
                      prefixIcon: Icons.email_outlined,
                      validator: (value) {
                        final text = value?.trim() ?? '';
                        if (text.isEmpty) {
                          return 'Ingresa tu correo electrónico';
                        }
                        if (!text.contains('@')) {
                          return 'Ingresa un correo válido';
                        }
                        return null;
                      },
                    ),
                    const SizedBox(height: 16),
                    AppTextField(
                      controller: _password,
                      label: 'Contraseña',
                      obscureText: true,
                      textInputAction: TextInputAction.done,
                      prefixIcon: Icons.lock_outline,
                      validator: (value) => (value?.isEmpty ?? true)
                          ? 'Ingresa tu contraseña'
                          : null,
                    ),
                    if (auth.errorMessage != null) ...[
                      const SizedBox(height: 12),
                      Text(
                        auth.errorMessage!,
                        style: TextStyle(
                          color: Theme.of(context).colorScheme.error,
                        ),
                      ),
                    ],
                    const SizedBox(height: 20),
                    PrimaryButton(
                      label: 'Iniciar sesión',
                      isLoading: auth.isSubmitting,
                      onPressed: _submit,
                    ),
                    if (auth.fingerprintLoginAvailable) ...[
                      const SizedBox(height: 12),
                      SizedBox(
                        width: double.infinity,
                        child: OutlinedButton.icon(
                          onPressed: auth.isSubmitting
                              ? null
                              : _loginWithFingerprint,
                          icon: const Icon(Icons.fingerprint),
                          label: const Text('Ingresar con huella'),
                        ),
                      ),
                    ],
                    const SizedBox(height: 8),
                    Center(
                      child: TextButton(
                        onPressed: auth.isSubmitting
                            ? null
                            : () {
                          ref
                              .read(authControllerProvider.notifier)
                              .clearError();
                          context.go('/registro');
                        },
                        child: const Text('¿No tienes cuenta? Regístrate'),
                      ),
                    ),
                    Center(
                      child: TextButton(
                        onPressed: auth.isSubmitting
                            ? null
                            : () {
                          ref
                              .read(authControllerProvider.notifier)
                              .clearError();

                          context.go('/recuperar-contrasena');
                        },
                        child: const Text('¿Olvidaste tu contraseña?'),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
