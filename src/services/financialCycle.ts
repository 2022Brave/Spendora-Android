import { FinancialCycleInfo } from '../types';

export class FinancialCycleService {
  /**
   * Calculates financial cycle boundaries given the configured cycle start day (1..31)
   * and an optional reference date.
   */
  static getCycleInfo(cycleStartDay: number = 1, referenceDate: Date = new Date()): FinancialCycleInfo {
    const clampedDay = Math.max(1, Math.min(31, cycleStartDay));
    const refYear = referenceDate.getFullYear();
    const refMonth = referenceDate.getMonth(); // 0-indexed
    const refDay = referenceDate.getDate();

    let startYear = refYear;
    let startMonth = refMonth;
    let endYear = refYear;
    let endMonth = refMonth;

    if (clampedDay === 1) {
      // Standard calendar month
      startYear = refYear;
      startMonth = refMonth;
      const lastDayOfCurMonth = new Date(refYear, refMonth + 1, 0).getDate();
      
      const start = new Date(startYear, startMonth, 1);
      const end = new Date(startYear, startMonth, lastDayOfCurMonth, 23, 59, 59, 999);
      
      return this.buildCycleInfo(start, end, clampedDay, referenceDate);
    }

    if (refDay >= clampedDay) {
      // Cycle started this month on clampedDay, ends next month on clampedDay - 1
      startYear = refYear;
      startMonth = refMonth;
      endMonth = refMonth + 1;
      if (endMonth > 11) {
        endMonth = 0;
        endYear = refYear + 1;
      }
    } else {
      // Cycle started previous month on clampedDay, ends this month on clampedDay - 1
      endYear = refYear;
      endMonth = refMonth;
      startMonth = refMonth - 1;
      if (startMonth < 0) {
        startMonth = 11;
        startYear = refYear - 1;
      }
    }

    // Clamp start date to valid days in start month
    const maxStartMonthDays = new Date(startYear, startMonth + 1, 0).getDate();
    const actualStartDay = Math.min(clampedDay, maxStartMonthDays);
    const start = new Date(startYear, startMonth, actualStartDay);

    // End day is clampedDay - 1
    const targetEndDay = clampedDay - 1;
    const maxEndMonthDays = new Date(endYear, endMonth + 1, 0).getDate();
    const actualEndDay = Math.min(targetEndDay, maxEndMonthDays);
    const end = new Date(endYear, endMonth, actualEndDay, 23, 59, 59, 999);

    return this.buildCycleInfo(start, end, clampedDay, referenceDate);
  }

  private static buildCycleInfo(start: Date, end: Date, cycleStartDay: number, referenceDate: Date): FinancialCycleInfo {
    const startStr = this.formatDate(start);
    const endStr = this.formatDate(end);

    // Cycle label is based on the end month/year (the month being budgeted for)
    const endMonthName = end.toLocaleString('default', { month: 'short' });
    const endYear = end.getFullYear();
    const displayLabel = `${endMonthName} ${endYear}`;
    const cycleMonthYear = `${endYear}-${String(end.getMonth() + 1).padStart(2, '0')}`;

    // Days remaining and progress
    const now = referenceDate.getTime();
    const startTime = start.getTime();
    const endTime = end.getTime();
    const totalMs = Math.max(1, endTime - startTime);
    const elapsedMs = Math.max(0, Math.min(totalMs, now - startTime));
    const progressPercent = Math.min(100, Math.round((elapsedMs / totalMs) * 100));

    const oneDayMs = 1000 * 60 * 60 * 24;
    const totalDays = Math.ceil(totalMs / oneDayMs);
    const daysRemaining = Math.max(0, Math.ceil((endTime - now) / oneDayMs));

    return {
      cycleStartDay,
      startDate: startStr,
      endDate: endStr,
      displayLabel,
      cycleMonthYear,
      daysRemaining,
      totalDays,
      progressPercent
    };
  }

  static formatDate(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  /**
   * Determine which cycle month-year a given transaction timestamp belongs to.
   */
  static getCycleMonthYearForTimestamp(timestamp: number, cycleStartDay: number): string {
    const cycle = this.getCycleInfo(cycleStartDay, new Date(timestamp));
    return cycle.cycleMonthYear;
  }
}
