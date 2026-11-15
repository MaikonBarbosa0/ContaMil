import { Component, ElementRef, effect, inject, input } from '@angular/core';
import { createElement, Wallet, Sun, Moon, LayoutDashboard, ReceiptText, Palette, Sprout, Plus, ArrowRight, ArrowUpRight, Scale, Repeat2, X, Check, CalendarCheck2, ChartPie, ChartNoAxesCombined, RefreshCw } from 'lucide';

const icons = {
  wallet: Wallet, sun: Sun, moon: Moon, dashboard: LayoutDashboard, receipt: ReceiptText, palette: Palette,
  sprout: Sprout, plus: Plus, arrow: ArrowRight, up: ArrowUpRight, scale: Scale, repeat: Repeat2, close: X,
  check: Check, calendar: CalendarCheck2, pie: ChartPie, chart: ChartNoAxesCombined, refresh: RefreshCw,
};
@Component({ selector: 'app-icon', template: '', host: { 'aria-hidden': 'true', 'style': 'display:inline-flex;flex-shrink:0;align-items:center;justify-content:center' } })
export class Icon {
  readonly name = input<keyof typeof icons>('wallet');
  private readonly element = inject<ElementRef<HTMLElement>>(ElementRef);
  constructor() {
    effect(() => this.element.nativeElement.replaceChildren(createElement(icons[this.name()], { width: '18', height: '18', 'stroke-width': '1.7', 'aria-hidden': 'true' })));
  }
}
