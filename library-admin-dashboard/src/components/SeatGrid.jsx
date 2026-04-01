import { seatLabel, seatTone } from '../utils/formatters';

export default function SeatGrid({ seats, selectedSeatId, onSelect }) {
  return (
    <div className="grid grid-cols-4 gap-3 sm:grid-cols-6 xl:grid-cols-8">
      {seats.map((seat) => {
        const selected = selectedSeatId === seat.seatId;

        return (
          <button
            key={seat.seatId}
            onClick={() => onSelect(seat)}
            className={`rounded-2xl px-3 py-4 text-left transition ${seatTone(seat.status)} ${
              selected ? 'ring-4 ring-brand-100' : ''
            }`}
          >
            <div className="text-sm font-semibold">{seat.seatId}</div>
            <div className="mt-2 text-xs opacity-85">{seatLabel(seat.status)}</div>
            <div className="mt-1 text-xs opacity-70">{seat.lastUpdated}</div>
          </button>
        );
      })}
    </div>
  );
}
