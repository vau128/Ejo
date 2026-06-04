import { seatLabel, seatTone } from '../utils/formatters';

export default function SeatGrid({ seats, selectedSeatId, onSelect, variant = 'overview' }) {
  const isDetailed = variant === 'detailed';

  return (
    <div className={isDetailed ? 'grid gap-4 md:grid-cols-2 2xl:grid-cols-4' : 'grid gap-4 sm:grid-cols-2 xl:grid-cols-4'}>
      {seats.map((seat) => {
        const selected = selectedSeatId === seat.seatId;

        return (
          <button
            key={seat.seatId}
            onClick={() => onSelect(seat)}
            className={`rounded-3xl text-left transition ${isDetailed ? 'min-h-[180px] px-5 py-5' : 'px-4 py-4'} ${seatTone(seat.status)} ${
              selected ? 'ring-4 ring-brand-100' : ''
            }`}
          >
            <div className="flex items-start justify-between gap-3">
              <div className={isDetailed ? 'text-lg font-semibold' : 'text-sm font-semibold'}>{seat.seatId}</div>
              <span className="rounded-full bg-white/70 px-2.5 py-1 text-[11px] font-medium text-slate-700">
                {seatLabel(seat.status)}
              </span>
            </div>
            {seat.location ? <div className="mt-3 text-sm font-medium opacity-90">{seat.location}</div> : null}
            <div className={`opacity-80 ${isDetailed ? 'mt-3 text-sm leading-6' : 'mt-2 text-xs'}`}>
              {seat.lastUpdated}
            </div>
          </button>
        );
      })}
    </div>
  );
}
