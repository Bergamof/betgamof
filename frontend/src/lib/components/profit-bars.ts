export interface CurvePoint {
	at: string;
	cumulativeProfit: number;
}

/**
 * Samples a cumulative profit curve into `count` bars (value at the end of each time slice),
 * then scales them between 8 % and 100 % of the chart height.
 */
export function sampleCurve(points: CurvePoint[], count: number, from?: Date): number[] {
	if (points.length === 0) return Array(count).fill(0.2);
	const start = (from ?? new Date(points[0].at)).getTime();
	const end = Math.max(new Date(points[points.length - 1].at).getTime(), start + 1);
	const slice = (end - start) / count;
	const values: number[] = [];
	let index = 0;
	let current = 0;
	for (let bar = 1; bar <= count; bar++) {
		const limit = start + slice * bar;
		while (index < points.length && new Date(points[index].at).getTime() <= limit) {
			current = points[index].cumulativeProfit;
			index++;
		}
		values.push(current);
	}
	const min = Math.min(...values);
	const max = Math.max(...values);
	const range = max - min || 1;
	return values.map((value) => 0.08 + ((value - min) / range) * 0.92);
}
