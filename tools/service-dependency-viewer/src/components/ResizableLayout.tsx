import { useRef, useState, type ReactNode } from 'react';
export function ResizableLayout({ graph, details, connections }: { graph: ReactNode; details: ReactNode; connections: ReactNode }) {
  const root = useRef<HTMLDivElement>(null), [left, setLeft] = useState(70), [top, setTop] = useState(65);
  function separator(vertical: boolean) {
    const value = vertical ? left : top, change = vertical ? setLeft : setTop;
    const clamp = (n: number) => Math.max(25, Math.min(80, n));
    return <div className={`splitter ${vertical ? 'vertical' : 'horizontal'}`} role="separator" tabIndex={0} aria-label={vertical ? 'Resize graph and details' : 'Resize upper and lower panels'} aria-orientation={vertical ? 'vertical' : 'horizontal'} aria-valuemin={25} aria-valuemax={80} aria-valuenow={value}
      onPointerDown={e => { e.currentTarget.setPointerCapture(e.pointerId); }} onPointerMove={e => { if (!e.currentTarget.hasPointerCapture(e.pointerId)) return; const r = root.current!.getBoundingClientRect(); change(clamp(vertical ? (e.clientX - r.left) / r.width * 100 : (e.clientY - r.top) / r.height * 100)); }} onPointerUp={e => e.currentTarget.releasePointerCapture(e.pointerId)}
      onKeyDown={e => { if (['ArrowLeft', 'ArrowUp', 'ArrowRight', 'ArrowDown', 'Home', 'End'].includes(e.key)) { e.preventDefault(); change(e.key === 'Home' ? 25 : e.key === 'End' ? 80 : clamp(value + (e.key === 'ArrowLeft' || e.key === 'ArrowUp' ? -2 : 2))); } }}/ >;
  }
  return <div className="workspace" ref={root} style={{ gridTemplateRows: `minmax(0, ${top}fr) 10px minmax(0, ${100 - top}fr)` }}><div className="upper" style={{ gridTemplateColumns: `minmax(0, ${left}fr) 10px minmax(0, ${100 - left}fr)` }}>{graph}{separator(true)}{details}</div>{separator(false)}{connections}</div>;
}
