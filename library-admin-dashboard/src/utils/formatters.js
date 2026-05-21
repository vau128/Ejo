export function formatPercent(value) {
  return `${value}%`;
}

export function statusClass(status) {
  switch (status) {
    case '사용 중':
    case '정상':
    case '활성':
      return 'bg-emerald-50 text-emerald-700';
    case '비어있음':
    case '대기':
      return 'bg-slate-100 text-slate-600';
    case '물품 감지':
    case '주의':
      return 'bg-amber-50 text-amber-700';
    case '장시간 비움':
    case '사석화':
    case '지연':
      return 'bg-rose-50 text-rose-700';
    case '오프라인':
    case '오류':
      return 'bg-rose-100 text-rose-700';
    default:
      return 'bg-brand-50 text-brand-700';
  }
}

export function seatTone(status) {
  switch (status) {
    case 'OCCUPIED':
      return 'bg-emerald-500 text-white shadow-sm';
    case 'AVAILABLE':
      return 'bg-white text-slate-600 ring-1 ring-slate-200';
    case 'OBJECT_ONLY':
    case 'ITEM':
      return 'bg-amber-200 text-amber-900';
    case 'VACANT_LONG':
    case 'RESERVED':
    case 'SQUATTING':
    case 'ABNORMAL':
      return 'bg-rose-200 text-rose-900';
    case 'SENSOR_DELAY':
      return 'bg-violet-200 text-violet-900';
    default:
      return 'bg-slate-100 text-slate-700';
  }
}

export function seatLabel(status) {
  switch (status) {
    case 'OCCUPIED':
      return '사용 중';
    case 'AVAILABLE':
      return '비어있음';
    case 'OBJECT_ONLY':
    case 'ITEM':
      return '물품 감지';
    case 'VACANT_LONG':
    case 'RESERVED':
      return '장시간 비움';
    case 'SQUATTING':
    case 'ABNORMAL':
      return '사석화';
    case 'SENSOR_DELAY':
      return '센서 지연';
    default:
      return status;
  }
}
