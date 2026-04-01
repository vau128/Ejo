import { useCallback, useEffect, useState } from 'react';

export function useApiData(fetcher, deps = [], options = {}) {
  const { immediate = true, intervalMs } = options;
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(immediate);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const result = await fetcher();
      setData(result);
    } catch (err) {
      setError(err.response?.data?.message || err.message || '데이터를 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  }, deps);

  useEffect(() => {
    if (!immediate) {
      return undefined;
    }

    load();
  }, [load, immediate]);

  useEffect(() => {
    if (!intervalMs) {
      return undefined;
    }

    const intervalId = window.setInterval(load, intervalMs);
    return () => window.clearInterval(intervalId);
  }, [intervalMs, load]);

  return { data, loading, error, refetch: load, setData };
}
