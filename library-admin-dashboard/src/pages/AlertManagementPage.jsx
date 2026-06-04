import { useState } from 'react';
import { getAlertManagement, updateAlertRule } from '../api/dashboardApi';
import DataTable from '../components/DataTable';
import MetricCard from '../components/MetricCard';
import PageHeader from '../components/PageHeader';
import SectionCard from '../components/SectionCard';
import StatusBadge from '../components/StatusBadge';
import { useApiData } from '../hooks/useApiData';

export default function AlertManagementPage() {
  const { data, loading, error, refetch } = useApiData(getAlertManagement, []);
  const [savingRuleId, setSavingRuleId] = useState('');
  const rules = (data?.rules ?? []).filter((rule) => rule.name !== '물품 장기 방치 알림');

  const toggleRule = async (rule) => {
    try {
      setSavingRuleId(rule.ruleId);
      await updateAlertRule(rule.ruleId, !rule.enabled);
      await refetch();
    } finally {
      setSavingRuleId('');
    }
  };

  if (loading && !data) return <div className="app-card p-10 text-center text-sm text-slate-500">알림 관리 데이터를 불러오는 중입니다.</div>;
  if (error && !data) return <div className="app-card p-10 text-center text-sm text-rose-600">{error}</div>;

  return (
    <div>
      <PageHeader title="알림 관리" description="자동 알림 규칙과 현재 트리거 대상을 관리합니다." />

      <div className="grid gap-4 md:grid-cols-3">
        <MetricCard label="활성 규칙" value={data.summary.enabledRules} helper="자동 실행 중" accent="emerald" />
        <MetricCard label="비활성 규칙" value={data.summary.disabledRules} helper="수동 점검 필요" accent="amber" />
        <MetricCard label="현재 대상" value={data.summary.pendingTargets} helper="조치 대기" accent="rose" />
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-[1fr_1.2fr]">
        <SectionCard title="알림 규칙" subtitle="자동 알림 기준과 채널을 제어합니다.">
          <div className="grid gap-4">
            {rules.map((rule) => (
              <div key={rule.ruleId} className="rounded-2xl border border-slate-200 p-5">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <p className="font-semibold text-slate-800">{rule.name}</p>
                    <p className="mt-1 text-sm text-slate-500">{rule.condition}</p>
                    <p className="mt-2 text-sm text-slate-600">기준: {rule.thresholdMinutes}분 · 채널: {rule.channel}</p>
                  </div>
                  <button onClick={() => toggleRule(rule)} className="secondary-button px-3 py-2 text-xs">
                    {savingRuleId === rule.ruleId ? '저장 중' : rule.enabled ? '비활성화' : '활성화'}
                  </button>
                </div>
                <div className="mt-4 flex items-center justify-between">
                  <StatusBadge>{rule.enabled ? '활성' : '대기'}</StatusBadge>
                  <span className="text-sm text-slate-500">대상: {rule.targetType}</span>
                </div>
              </div>
            ))}
          </div>
        </SectionCard>

        <SectionCard title="현재 알림 대상" subtitle="지금 자동화 규칙에 의해 추적 중인 좌석입니다.">
          <DataTable
            columns={[
              { key: 'seatId', label: '좌석' },
              { key: 'currentStatus', label: '현재 상태' },
              { key: 'triggerAt', label: '트리거 시각' },
              { key: 'recommendedAction', label: '추천 조치' },
              { key: 'severity', label: '중요도', render: (row) => <StatusBadge>{row.severity}</StatusBadge> },
            ]}
            rows={data.targets}
          />
        </SectionCard>
      </div>
    </div>
  );
}
