import { AfterViewInit, Component, ElementRef, OnDestroy, ViewChild, computed, input, signal } from '@angular/core';
import { Summary } from './models';
import { cumulative, money, monthName, shortDate, todayISO } from './finance-utils';
import { Icon } from './icon';

const patterns = ['', '6 3', '2 3', '8 3 2 3', '11 4', '4 3 1 3', '1 3', '9 2', '3 2 3 5', '12 3 2 3'];
@Component({
  selector: 'app-spending-charts', imports: [Icon], host: { class: 'ct-charts' },
  template: `
    <section class="ct-panel ct-category-panel">
      <div class="ct-panel-heading"><div><h2>Para onde vai seu dinheiro</h2><p>Gastos por categoria</p></div><app-icon name="pie" /></div>
      <div class="ct-category-chart">
        @if (segments().length) {
          <div class="ct-donut-wrap"><svg class="ct-donut" viewBox="0 0 180 180" role="img" [attr.aria-label]="distributionLabel()"><title>Distribuição dos gastos por categoria</title>
            @for (segment of segments(); track segment.category.id) {
              <circle [attr.data-category-segment]="segment.category.id" cx="90" cy="90" r="65" fill="none" [attr.stroke]="segment.category.color" stroke-width="20" [attr.stroke-dasharray]="segment.dash" [attr.stroke-dashoffset]="segment.offset" transform="rotate(-90 90 90)"><title>{{segment.category.name}}: {{money(segment.amount)}}</title></circle>
            }
          </svg><div class="ct-donut-label"><span>Total do mês</span><strong>{{money(summary()?.totalExpenses ?? 0)}}</strong></div></div>
          <div class="ct-legend">@for (segment of segments(); track segment.category.id) {
            <div class="ct-legend-item"><span class="ct-dot" [style.background]="segment.category.color"></span><span>{{segment.category.name}}</span><strong>{{segment.percent}}%</strong></div>
          }</div>
        } @else { <p class="ct-empty">Nenhum gasto neste mês.</p> }
      </div>
    </section>
    <section class="ct-panel ct-trend-panel">
      <div class="ct-panel-heading"><div><h2>Ao longo do mês</h2><p>Acumulado por categoria, em R$</p></div><app-icon name="chart" /></div>
      <div class="ct-trend-head"><strong>{{money(trendTotal())}}</strong><span>{{cutoffLabel()}}</span></div>
      <div class="ct-trend-chart" #plot>
        @if (series().length) {
          <svg class="ct-line-svg" [attr.viewBox]="'0 0 ' + width() + ' 180'" role="img" [attr.aria-label]="trendLabel()"><title>Gastos acumulados por dia e categoria, em reais</title>
            @for (fraction of [0, .5, 1]; track fraction) {
              <line x1="43" [attr.y1]="13 + 140 * (1-fraction)" [attr.x2]="width()-8" [attr.y2]="13 + 140 * (1-fraction)" stroke="var(--ct-line)" stroke-width="1" />
              <text x="36" [attr.y]="17 + 140 * (1-fraction)" text-anchor="end">{{axis(maximum()*fraction/100)}}</text>
            }
            @for (line of series(); track line.category.id) {
              <g [attr.opacity]="focused() && focused() !== line.category.id ? .2 : 1">
                <path [attr.data-series-id]="line.category.id" [attr.d]="line.path" fill="none" [attr.stroke]="line.category.color" [attr.stroke-width]="focused() === line.category.id ? 3 : 2.2" [attr.stroke-dasharray]="line.dash" stroke-linecap="round" stroke-linejoin="round"><title>{{line.category.name}}: {{money(line.total/100)}} acumulados</title></path>
                @for (point of line.points; track point.date) {
                  <circle [attr.cx]="point.x" [attr.cy]="point.y" [attr.r]="$last ? 3.2 : 4" [attr.fill]="line.category.color" [attr.opacity]="$last ? 1 : 0"><title>{{line.category.name}} · {{shortDate(point.date)}}: {{money(point.cents/100)}} acumulados</title></circle>
                }
              </g>
            }
            @for (tick of ticks(); track tick.day) { <text [attr.x]="tick.x" y="175" [attr.text-anchor]="tick.anchor">{{tick.label}}</text> }
          </svg>
        } @else { <p class="ct-empty">Nenhum gasto até a data atual.</p> }
      </div>
      <div class="ct-series-legend" aria-label="Categorias do gráfico de evolução">@for (line of series(); track line.category.id) {
        <button type="button" class="ct-series-item" [attr.aria-pressed]="focused() === line.category.id" [attr.aria-label]="line.category.name + ', ' + money(line.total/100) + ' acumulados. Destacar linha.'" (click)="focused.set(focused() === line.category.id ? null : line.category.id)">
          <svg viewBox="0 0 25 10" aria-hidden="true"><line x1="1" y1="5" x2="24" y2="5" [attr.stroke]="line.category.color" stroke-width="2.3" [attr.stroke-dasharray]="line.dash" /></svg><span class="ct-series-name">{{line.category.name}}</span><span class="ct-series-total">{{money(line.total/100)}}</span>
        </button>
      }</div>
    </section>
  `,
})
export class SpendingCharts implements AfterViewInit, OnDestroy {
  readonly summary = input<Summary | null>(null);
  readonly width = signal(300);
  readonly focused = signal<number | null>(null);
  @ViewChild('plot', { static: true }) plot!: ElementRef<HTMLElement>;
  private observer?: ResizeObserver;
  readonly money = money; readonly shortDate = shortDate;
  readonly axis = (amount: number) => new Intl.NumberFormat('pt-BR', { notation: 'compact', maximumFractionDigits: 1 }).format(amount);
  readonly segments = computed(() => {
    const summary = this.summary();
    if (!summary?.totalExpenses) return [];
    const circumference = Math.PI * 130;
    let offset = 0;
    return summary.byCategory.map(item => {
      const length = item.amount / summary.totalExpenses * circumference;
      const result = { ...item, dash: `${Math.max(0, length-3)} ${circumference-Math.max(0, length-3)}`, offset: -offset, percent: (item.amount/summary.totalExpenses*100).toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 }) };
      offset += length;
      return result;
    });
  });
  readonly eligible = computed(() => (this.summary()?.dailyByCategory ?? []).map(series => {
    const points = cumulative(series.points, todayISO());
    return { ...series, points, total: points.at(-1)?.cents ?? 0 };
  }).filter(series => series.total > 0));
  readonly maximum = computed(() => Math.max(10000, Math.ceil(Math.max(0, ...this.eligible().map(line => line.total))/50000)*50000));
  readonly trendTotal = computed(() => this.eligible().reduce((total, line) => total + line.total, 0)/100);
  readonly series = computed(() => this.eligible().map(line => ({
    ...line, dash: patterns[(line.category.id-1)%patterns.length],
    points: line.points.map((point, index) => ({ ...point, x: 43 + index / Math.max(line.points.length-1,1) * (this.width()-51), y: 13 + 140 * (1-point.cents/this.maximum()) })),
    path: line.points.map((point, index) => `${index ? 'L' : 'M'}${(43+index/Math.max(line.points.length-1,1)*(this.width()-51)).toFixed(2)},${(13+140*(1-point.cents/this.maximum())).toFixed(2)}`).join(' '),
  })));
  readonly ticks = computed(() => {
    const length = this.series()[0]?.points.length ?? 0;
    return [...new Set([1, Math.ceil(length/2), length])].filter(day => day > 0).map(day => ({ day, x: 43+(day-1)/Math.max(length-1,1)*(this.width()-51), anchor: day === 1 ? 'start' : day === length ? 'end' : 'middle', label: `${String(day).padStart(2,'0')}/${this.summary()?.month.slice(5)}` }));
  });
  readonly cutoffLabel = computed(() => {
    const month = this.summary()?.month;
    if (!month) return '';
    const today = todayISO();
    return month > today.slice(0,7) ? 'Mês ainda não iniciado' : month === today.slice(0,7) ? `Até ${Number(today.slice(8))} de ${monthName(month).split(' de ')[0]}` : monthName(month);
  });
  readonly distributionLabel = computed(() => 'Gastos por categoria: ' + this.segments().map(item => `${item.category.name} ${money(item.amount)}`).join('; '));
  readonly trendLabel = computed(() => 'Gastos acumulados por categoria: ' + this.series().map(item => `${item.category.name} ${money(item.total/100)}`).join('; '));
  ngAfterViewInit(): void {
    this.observer = new ResizeObserver(entries => this.width.set(Math.max(180, entries[0].contentRect.width)));
    this.observer.observe(this.plot.nativeElement);
  }
  ngOnDestroy(): void { this.observer?.disconnect(); }
}
