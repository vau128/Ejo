export default function SearchField({ value, onChange, placeholder = '검색어를 입력하세요' }) {
  return (
    <div className="relative w-full max-w-sm">
      <span className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-slate-400">⌕</span>
      <input
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="input-base pl-10"
        placeholder={placeholder}
      />
    </div>
  );
}
