export default function DoughnutChart({ segments }) {
  const total = segments.reduce((sum, segment) => sum + segment.value, 0) || 1;
  let current = 0;
  const colors = ['#4b7bff', '#7bd3c8', '#ffc26b', '#ff8f8f', '#a78bfa'];
  const stops = segments
    .map((segment, index) => {
      const start = (current / total) * 100;
      current += segment.value;
      const end = (current / total) * 100;
      return `${colors[index % colors.length]} ${start}% ${end}%`;
    })
    .join(', ');

  return (
    <div className="flex flex-col gap-5 lg:flex-row lg:items-center">
      <div
        className="h-44 w-44 rounded-full"
        style={{
          background: `conic-gradient(${stops})`,
          mask: 'radial-gradient(circle at center, transparent 39%, black 40%)',
          WebkitMask: 'radial-gradient(circle at center, transparent 39%, black 40%)',
        }}
      />
      <div className="grid gap-3">
        {segments.map((segment, index) => (
          <div key={segment.label} className="flex items-center gap-3 text-sm text-slate-600">
            <span className="h-3 w-3 rounded-full" style={{ backgroundColor: colors[index % colors.length] }} />
            <span className="min-w-32">{segment.label}</span>
            <span className="font-semibold text-slate-800">{segment.value}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
