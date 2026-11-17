import { useEffect, useState, type RefObject } from 'react';

export function useFullscreen(target: RefObject<HTMLElement | null>) {
  const [active, setActive] = useState(false);
  const [supported, setSupported] = useState(false);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    setSupported(Boolean(document.fullscreenEnabled && target.current?.requestFullscreen));
    const sync = () => setActive(document.fullscreenElement === target.current);
    document.addEventListener('fullscreenchange', sync);
    sync();
    return () => document.removeEventListener('fullscreenchange', sync);
  }, [target]);

  async function toggle() {
    if (!target.current || pending) return;
    setError(''); setPending(true);
    try {
      if (document.fullscreenElement === target.current) await document.exitFullscreen();
      else await target.current.requestFullscreen();
    } catch {
      setError('Full screen could not be changed. Check your browser’s full-screen permissions and try again.');
    } finally { setPending(false); }
  }

  return { active, supported, pending, error, toggle };
}
