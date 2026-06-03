export default function MetricCard({ label, value, helper, accent = 'brand', compact = false, className = '' }) {
  const tone = {
    brand: 'from-brand-50 to-white text-brand-700',
    emerald: 'from-emerald-50 to-white text-emerald-700',
    amber: 'from-amber-50 to-white text-amber-700',
    rose: 'from-rose-50 to-white text-rose-700',
    violet: 'from-violet-50 to-white text-violet-700',
  }[accent] || 'from-brand-50 to-white text-brand-700';

  return (
    <div className={`metric-card bg-gradient-to-br ${compact ? 'p-4' : ''} ${tone} ${className}`}>
      <p className="text-sm font-medium text-slate-500">{label}</p>
      <p className={`font-semibold tracking-tight text-slate-800 ${compact ? 'mt-2 text-2xl' : 'mt-3 text-3xl'}`}>{value}</p>
      {helper ? <p className={`text-slate-500 ${compact ? 'mt-1 text-xs' : 'mt-2 text-sm'}`}>{helper}</p> : null}
    </div>
  );
}
