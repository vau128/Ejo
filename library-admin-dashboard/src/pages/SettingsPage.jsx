import { useEffect, useState } from 'react';
import { getSettings, getSquattingThreshold, resetAllSeats, updateSettings, updateSquattingThreshold } from '../api/dashboardApi';
import MetricCard from '../components/MetricCard';
import PageHeader from '../components/PageHeader';
import SectionCard from '../components/SectionCard';
import StatusBadge from '../components/StatusBadge';
import { useApiData } from '../hooks/useApiData';

const defaultSettings = {
  pushAlertsEnabled: false,
  smsAlertsEnabled: false,
  quietHoursStart: '22:00',
  quietHoursEnd: '07:00',
  autoReleaseEnabled: false,
  lostItemAutoRegisterEnabled: false,
  vacantSeatThresholdMinutes: 15,
  objectDetectionThresholdMinutes: 10,
  dashboardRefreshSeconds: 10,
  sensorDelayThresholdSeconds: 5,
  libraryMode: 'NORMAL',
  squattingThresholdMinutes: 60,
};

const thresholdOptions = [
  { value: 10, label: '10초' },
  { value: 30, label: '30분' },
  { value: 60, label: '1시간' },
  { value: 120, label: '2시간' },
  { value: 240, label: '4시간' },
];

export default function SettingsPage() {
  const { data, loading, error, refetch } = useApiData(getSettings, []);
  const [form, setForm] = useState(defaultSettings);
  const [thresholdMinutes, setThresholdMinutes] = useState(60);
  const [saving, setSaving] = useState(false);
  const [thresholdSaving, setThresholdSaving] = useState(false);
  const [resetting, setResetting] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    if (data?.settings) {
      setForm((current) => ({ ...current, ...data.settings }));
    }
  }, [data]);

  useEffect(() => {
    let cancelled = false;

    async function loadThreshold() {
      try {
        const result = await getSquattingThreshold();
        if (!cancelled) {
          setThresholdMinutes(result.thresholdMinutes ?? result.threshold_minutes ?? 60);
        }
      } catch {
      }
    }

    loadThreshold();
    return () => {
      cancelled = true;
    };
  }, []);

  const updateField = (key, value) => {
    setForm((current) => ({ ...current, [key]: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setMessage('');

    try {
      const result = await updateSettings(form);
      setMessage(result.message || '설정을 저장했습니다.');
      await refetch();
    } catch (err) {
      setMessage(err.response?.data?.message || '설정을 저장하지 못했습니다.');
    } finally {
      setSaving(false);
    }
  };

  const handleThresholdSave = async () => {
    setThresholdSaving(true);
    setMessage('');

    try {
      console.log('selected thresholdMinutes', thresholdMinutes);
      const result = await updateSquattingThreshold(thresholdMinutes);
      const refreshed = await getSquattingThreshold();
      setThresholdMinutes(refreshed.thresholdMinutes ?? refreshed.threshold_minutes ?? thresholdMinutes);
      setMessage(result.message || '사석화 기준 시간을 저장했습니다.');
    } catch (err) {
      setMessage(err.response?.data?.message || '사석화 기준 시간을 저장하지 못했습니다.');
    } finally {
      setThresholdSaving(false);
    }
  };

  const handleResetSeats = async () => {
    const confirmed = window.confirm('모든 좌석 상태와 학생 발권 정보를 초기화합니다. 계속할까요?');
    if (!confirmed) {
      return;
    }

    setResetting(true);
    setMessage('');

    try {
      const result = await resetAllSeats();
      await refetch();
      setMessage(result.message || '전체 좌석을 초기화했습니다.');
    } catch (err) {
      setMessage(err.response?.data?.message || '전체 좌석을 초기화하지 못했습니다.');
    } finally {
      setResetting(false);
    }
  };

  if (loading && !data) return <div className="app-card p-10 text-center text-sm text-slate-500">설정 데이터를 불러오는 중입니다.</div>;
  if (error && !data) return <div className="app-card p-10 text-center text-sm text-rose-600">{error}</div>;

  return (
    <div>
      <PageHeader
        title="설정"
        description="사석화 판정 시간과 운영 설정을 관리합니다."
        right={message ? <StatusBadge>{message.includes('못') ? '오류' : '정상'}</StatusBadge> : null}
      />

      <div className="grid gap-4 md:grid-cols-3">
        <MetricCard label="사석화 기준" value={thresholdLabel(thresholdMinutes)} helper="관리자 설정값" accent="rose" />
        <MetricCard label="활성 자동화" value={data.summary.enabledAutomations} helper="현재 적용 중" accent="emerald" />
        <MetricCard label="대시보드 갱신" value={`${data.summary.refreshSeconds}초`} helper="자동 새로고침" accent="amber" />
      </div>

      <form onSubmit={handleSubmit} className="mt-6 grid gap-6 xl:grid-cols-3">
        <SectionCard
          title="사석화 판정 시간"
          subtitle="발권 상태인데 사람이 없는 시간이 이 기준을 넘으면 사석화로 판단합니다."
          action={
            <button type="button" className="primary-button px-4 py-2 text-sm" disabled={thresholdSaving} onClick={handleThresholdSave}>
              {thresholdSaving ? '저장 중' : '기준 저장'}
            </button>
          }
        >
          <div className="grid gap-4">
            <Field label="판정 기준">
              <select
                className="input-base"
                value={thresholdMinutes}
                onChange={(event) => setThresholdMinutes(Number(event.target.value))}
              >
                {thresholdOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </Field>
            <div className="rounded-2xl border border-rose-100 bg-rose-50 p-4 text-sm text-rose-800">
              현재 기준: 발권 상태 + 사람 미감지 + {thresholdLabel(thresholdMinutes)} 이상 지속
            </div>
          </div>
        </SectionCard>

        <SectionCard title="알림 설정" subtitle="관리자 알림 채널과 발송 제한 시간을 제어합니다.">
          <div className="grid gap-4">
            <ToggleRow
              label="앱 푸시 알림"
              description="비정상 좌석 감지 시 관리자 앱 푸시를 전송합니다."
              checked={form.pushAlertsEnabled}
              onChange={(checked) => updateField('pushAlertsEnabled', checked)}
            />
            <ToggleRow
              label="SMS 알림"
              description="긴급 상태는 문자 채널로도 전송합니다."
              checked={form.smsAlertsEnabled}
              onChange={(checked) => updateField('smsAlertsEnabled', checked)}
            />
            <div className="grid gap-3 sm:grid-cols-2">
              <Field label="방해 금지 시작">
                <input
                  type="time"
                  className="input-base"
                  value={form.quietHoursStart}
                  onChange={(event) => updateField('quietHoursStart', event.target.value)}
                />
              </Field>
              <Field label="방해 금지 종료">
                <input
                  type="time"
                  className="input-base"
                  value={form.quietHoursEnd}
                  onChange={(event) => updateField('quietHoursEnd', event.target.value)}
                />
              </Field>
            </div>
          </div>
        </SectionCard>

        <SectionCard
          title="시스템 운영"
          subtitle="기존 운영 설정은 유지하면서 센서 반영 주기를 조정합니다."
          action={
            <button type="submit" className="primary-button px-4 py-2 text-sm" disabled={saving || resetting}>
              {saving ? '저장 중' : '설정 저장'}
            </button>
          }
        >
          <div className="grid gap-4">
            <Field label="대시보드 갱신 주기 (초)">
              <input
                type="number"
                min="5"
                max="120"
                className="input-base"
                value={form.dashboardRefreshSeconds}
                onChange={(event) => updateField('dashboardRefreshSeconds', Number(event.target.value))}
              />
            </Field>
            <Field label="센서 지연 기준 (초)">
              <input
                type="number"
                min="1"
                max="60"
                className="input-base"
                value={form.sensorDelayThresholdSeconds}
                onChange={(event) => updateField('sensorDelayThresholdSeconds', Number(event.target.value))}
              />
            </Field>
            <Field label="운영 모드">
              <select
                className="input-base"
                value={form.libraryMode}
                onChange={(event) => updateField('libraryMode', event.target.value)}
              >
                <option value="NORMAL">정상 운영</option>
                <option value="EXAM">시험 기간</option>
                <option value="MAINTENANCE">점검 모드</option>
              </select>
            </Field>
            <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600">
              <p className="font-semibold text-slate-800">현재 적용 요약</p>
              <p className="mt-2">운영 모드: {modeLabel(form.libraryMode)}</p>
              <p className="mt-1">사석화 기준: {thresholdLabel(thresholdMinutes)}</p>
              <p className="mt-1">센서 지연 기준: {form.sensorDelayThresholdSeconds}초</p>
            </div>
            <div className="rounded-2xl border border-rose-200 bg-rose-50 p-4">
              <p className="text-sm font-semibold text-rose-800">운영 긴급 조치</p>
              <p className="mt-2 text-sm text-rose-700">
                웹 대시보드, 학생앱, IoT 백엔드가 공유하는 현재 좌석 상태를 한 번에 초기화합니다.
              </p>
              <button
                type="button"
                className="mt-4 rounded-xl bg-rose-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-rose-700 disabled:cursor-not-allowed disabled:bg-rose-300"
                disabled={resetting || saving || thresholdSaving}
                onClick={handleResetSeats}
              >
                {resetting ? '초기화 중' : '전체 좌석 초기화'}
              </button>
            </div>
          </div>
        </SectionCard>
      </form>
    </div>
  );
}

function Field({ label, children }) {
  return (
    <label className="grid gap-2 text-sm font-medium text-slate-700">
      <span>{label}</span>
      {children}
    </label>
  );
}

function ToggleRow({ label, description, checked, onChange }) {
  return (
    <label className="flex items-start justify-between gap-4 rounded-2xl border border-slate-200 p-4">
      <div>
        <p className="font-medium text-slate-800">{label}</p>
        <p className="mt-1 text-sm text-slate-500">{description}</p>
      </div>
      <input type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} className="mt-1 h-5 w-5" />
    </label>
  );
}

function modeLabel(value) {
  switch (value) {
    case 'EXAM':
      return '시험 기간';
    case 'MAINTENANCE':
      return '점검 모드';
    default:
      return '정상 운영';
  }
}

function thresholdLabel(value) {
  const option = thresholdOptions.find((item) => item.value === value);
  if (option) {
    return option.label;
  }
  return value === 10 ? '10초' : `${value}분`;
}
