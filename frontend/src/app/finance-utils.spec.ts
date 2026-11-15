import { cents, cumulative, todayISO, validDate, validMonth } from './finance-utils';

describe('valores monetários e calendário financeiro', () => {
  it('converte centavos sem arredondar entradas inválidas', () => {
    expect(cents('0.10')).toBe(10);
    expect(cents('999999999999.99')).toBe(99999999999999);
    for (const invalid of ['', '-1', '1.001', '1,00', '1e3', '1000000000000']) expect(cents(invalid)).toBeNull();
  });
  it('rejeita datas inexistentes e mantém anos bissextos', () => {
    expect(validDate('2024-02-29')).toBe(true);
    expect(validDate('2025-02-29')).toBe(false);
    expect(validDate('2026-04-31')).toBe(false);
    expect(validMonth('2026-13')).toBe(false);
    expect(validMonth('2026-11')).toBe(true);
  });
  it('usa o dia de São Paulo na virada do dia UTC', () => {
    expect(todayISO(new Date('2026-11-12T01:00:00Z'))).toBe('2026-11-11');
  });
  it('acumula cada série em centavos e exclui lançamentos futuros', () => {
    expect(cumulative([{date:'2026-11-01',amount:.1},{date:'2026-11-02',amount:.2},{date:'2026-11-03',amount:99}], '2026-11-02')).toEqual([{date:'2026-11-01',cents:10},{date:'2026-11-02',cents:30}]);
  });
});
