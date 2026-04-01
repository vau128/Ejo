import { statusClass } from '../utils/formatters';

export default function StatusBadge({ children }) {
  return <span className={`badge ${statusClass(children)}`}>{children}</span>;
}
