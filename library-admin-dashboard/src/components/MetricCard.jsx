export default function MetricCard({ label, value, helper, accent = 'brand' }) {
  const tone = {
    brand: 'from-brand-50 to-white text-brand-700',
    emerald: 'from-emerald-50 to-white text-emerald-700',
    amber: 'from-amber-50 to-white text-amber-700',
    rose: 'from-rose-50 to-white text-rose-700',
    violet: 'from-violet-50 to-white text-violet-700',
  }[accent] || 'from-brand-50 to-white text-brand-700';

  return (
    <div className={`metric-card bg-gradient-to-br ${tone}`}>
      <p className="text-sm font-medium text-slate-500">{label}</p>
      <p className="mt-3 text-3xl font-semibold tracking-tight text-slate-800">{value}</p>
      {helper ? <p className="mt-2 text-sm text-slate-500">{helper}</p> : null}
    </div>
  );
}
