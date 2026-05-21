import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [form, setForm] = useState({ username: 'admin', password: 'admin123' });
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setError('');

    try {
      await login(form);
      navigate('/overview');
    } catch (err) {
      if (!err.response) {
        setError('백엔드에 연결할 수 없습니다. Amplify 도메인 CORS 허용 또는 HTTPS API 설정을 확인하세요.');
      } else {
        setError(err.response?.data?.message || '로그인에 실패했습니다.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-[linear-gradient(160deg,_#f7f9ff_0%,_#eef3fb_55%,_#e8eef9_100%)] px-4">
      <div className="grid w-full max-w-5xl gap-8 lg:grid-cols-[1.1fr_0.9fr]">
        <div className="hidden rounded-[36px] bg-gradient-to-br from-brand-600 to-brand-700 p-10 text-white shadow-soft lg:block">
          <p className="text-sm uppercase tracking-[0.35em] text-white/75">AIoT Smart Library</p>
          <h1 className="mt-6 text-4xl font-semibold leading-tight">관리자 전용 좌석 모니터링 웹 대시보드</h1>
          <p className="mt-6 text-base leading-7 text-white/80">
            좌석 상태, 알림 이력, 분실물 관리, 시스템 상태까지 한 화면에서 확인할 수 있도록 구성했습니다.
          </p>
        </div>

        <div className="app-card mx-auto w-full max-w-lg p-8 sm:p-10">
          <p className="text-sm font-medium uppercase tracking-[0.25em] text-brand-600">Library Admin Login</p>
          <h2 className="mt-4 text-3xl font-semibold text-slate-800">관리자 로그인</h2>
          <p className="mt-2 text-sm leading-6 text-slate-500">기본 계정은 admin / admin123 입니다.</p>

          <form onSubmit={handleSubmit} className="mt-8 grid gap-4">
            <label className="grid gap-2">
              <span className="text-sm font-medium text-slate-700">관리자 ID</span>
              <input
                name="username"
                value={form.username}
                onChange={handleChange}
                className="input-base"
                placeholder="enter the id"
              />
            </label>
            <label className="grid gap-2">
              <span className="text-sm font-medium text-slate-700">비밀번호</span>
              <input
                type="password"
                name="password"
                value={form.password}
                onChange={handleChange}
                className="input-base"
                placeholder="enter the password"
              />
            </label>

            {error ? <div className="rounded-2xl bg-rose-50 px-4 py-3 text-sm text-rose-700">{error}</div> : null}

            <button disabled={submitting} className="primary-button mt-2 w-full py-3 text-base">
              {submitting ? '로그인 중...' : '로그인'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
