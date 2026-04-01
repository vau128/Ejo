import { useEffect, useState } from 'react';
import { getSettings, updateSettings } from '../api/dashboardApi';
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
};

export default function SettingsPage() {
  const { data, loading, error, refetch } = useApiData(getSettings, []);
  const [form, setForm] = useState(defaultSettings);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    if (data?.settings) {
      setForm(data.settings);
    }
  }, [data]);

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

  if (loading && !data) return <div className="app-card p-10 text-center text-sm text-slate-500">설정 데이터를 불러오는 중입니다.</div>;
  if (error && !data) return <div className="app-card p-10 text-center text-sm text-rose-600">{error}</div>;

  return (
    <div>
      <PageHeader
        title="설정"
        description="알림 정책, 좌석 운영 기준, 시스템 갱신 주기를 관리합니다."
        right={message ? <StatusBadge>{message.includes('못') ? '오류' : '정상'}</StatusBadge> : null}
      />

      <div className="grid gap-4 md:grid-cols-3">
        <MetricCard label="활성 자동화" value={data.summary.enabledAutomations} helper="현재 적용 중" accent="emerald" />
        <MetricCard label="알림 채널" value={data.summary.activeChannels} helper="사용 가능한 채널" accent="brand" />
        <MetricCard label="대시보드 갱신" value={`${data.summary.refreshSeconds}초`} helper="자동 새로고침" accent="amber" />
      </div>

      <form onSubmit={handleSubmit} className="mt-6 grid gap-6 xl:grid-cols-3">
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

        <SectionCard title="좌석 운영 기준" subtitle="사석화와 장시간 비움 기준을 조정합니다.">
          <div className="grid gap-4">
            <ToggleRow
              label="자동 좌석 해제"
              description="장시간 비움 상태를 넘기면 좌석을 자동 해제 대상으로 전환합니다."
              checked={form.autoReleaseEnabled}
              onChange={(checked) => updateField('autoReleaseEnabled', checked)}
            />
            <ToggleRow
              label="분실물 자동 등록"
              description="물품 감지 상태가 유지되면 분실물 큐에 자동 등록합니다."
              checked={form.lostItemAutoRegisterEnabled}
              onChange={(checked) => updateField('lostItemAutoRegisterEnabled', checked)}
            />
            <Field label="장시간 비움 기준 (분)">
              <input
                type="number"
                min="5"
                max="120"
                className="input-base"
                value={form.vacantSeatThresholdMinutes}
                onChange={(event) => updateField('vacantSeatThresholdMinutes', Number(event.target.value))}
              />
            </Field>
            <Field label="물품 감지 기준 (분)">
              <input
                type="number"
                min="1"
                max="60"
                className="input-base"
                value={form.objectDetectionThresholdMinutes}
                onChange={(event) => updateField('objectDetectionThresholdMinutes', Number(event.target.value))}
              />
            </Field>
          </div>
        </SectionCard>

        <SectionCard
          title="시스템 운영"
          subtitle="대시보드 반영 속도와 운영 모드를 관리합니다."
          action={
            <button type="submit" className="primary-button px-4 py-2 text-sm" disabled={saving}>
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
              <p className="mt-1">장시간 비움 기준: {form.vacantSeatThresholdMinutes}분</p>
              <p className="mt-1">센서 지연 기준: {form.sensorDelayThresholdSeconds}초</p>
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
