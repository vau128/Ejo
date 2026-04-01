function createPath(points, width, height) {
  if (!points.length) return '';

  const max = Math.max(...points, 1);
  const min = Math.min(...points, 0);
  const xStep = points.length > 1 ? width / (points.length - 1) : width;

  return points
    .map((point, index) => {
      const x = index * xStep;
      const y = height - ((point - min) / (max - min || 1)) * height;
      return `${index === 0 ? 'M' : 'L'} ${x} ${y}`;
    })
    .join(' ');
}

export default function MiniLineChart({ labels, values }) {
  const path = createPath(values, 280, 120);

  return (
    <div>
      <svg viewBox="0 0 280 140" className="h-36 w-full">
        <path d="M 0 130 L 280 130" stroke="#e2e8f0" strokeWidth="1" fill="none" />
        <path d="M 0 90 L 280 90" stroke="#eef2ff" strokeWidth="1" fill="none" />
        <path d="M 0 50 L 280 50" stroke="#eef2ff" strokeWidth="1" fill="none" />
        <path d={path} stroke="#4b7bff" strokeWidth="4" fill="none" strokeLinecap="round" />
      </svg>
      <div className="mt-2 flex justify-between gap-2 text-xs text-slate-400">
        {labels.map((label) => (
          <span key={label}>{label}</span>
        ))}
      </div>
    </div>
  );
}
