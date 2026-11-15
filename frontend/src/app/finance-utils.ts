const currency = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
export const money = (value: number): string => currency.format(value);
export const shortDate = (value: string): string => value ? `${value.slice(8, 10)}/${value.slice(5, 7)}` : '—';
export const fullDate = (value: string): string => value ? `${shortDate(value)}/${value.slice(0, 4)}` : '—';
export function todayISO(now = new Date()): string {
  const parts = new Intl.DateTimeFormat('en', { timeZone: 'America/Sao_Paulo', year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(now);
  const part = (name: Intl.DateTimeFormatPartTypes) => parts.find(item => item.type === name)!.value;
  return `${part('year')}-${part('month')}-${part('day')}`;
}
export function monthName(month: string): string {
  return new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric', timeZone: 'UTC' }).format(new Date(`${month}-01T12:00:00Z`));
}
export function cents(value: number | string | null): number | null {
  if (value === null || value === '') return null;
  const text = String(value).trim();
  if (!/^\d{1,12}(\.\d{1,2})?$/.test(text)) return null;
  const [integer, decimal = ''] = text.split('.');
  return Number(integer) * 100 + Number(decimal.padEnd(2, '0'));
}
export function validMonth(value: string): boolean { return /^\d{4}-(0[1-9]|1[0-2])$/.test(value) && Number(value.slice(0, 4)) > 0; }
export function validDate(value: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value) || Number(value.slice(0, 4)) < 1) return false;
  const parsed = new Date(`${value}T12:00:00Z`);
  return !Number.isNaN(parsed.getTime()) && parsed.toISOString().slice(0, 10) === value;
}
export const isColor = (value: string): boolean => /^#[0-9a-f]{6}$/i.test(value);
export function cumulative(points: { date: string; amount: number }[], cutoff: string) {
  let total = 0;
  return points.filter(point => point.date <= cutoff).map(point => {
    total += Math.round(point.amount * 100);
    return { date: point.date, cents: total };
  });
}
