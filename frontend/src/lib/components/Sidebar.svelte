<script lang="ts">
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import type { Bankroll } from '$lib/api/types';
	import { money } from '$lib/format';
	import Logo from './Logo.svelte';
	import Meter from './Meter.svelte';
	import NavIcon from './NavIcon.svelte';
	import { NAV_ITEMS, isActive } from './nav';

	let { bankroll }: { bankroll: Bankroll | null } = $props();
</script>

<aside class="sidebar">
	<a href={resolve('/')} class="brand"><Logo /></a>
	<nav>
		{#each NAV_ITEMS as item (item.href)}
			{@const active = isActive(item.href, page.url.pathname)}
			<a
				href={resolve(item.href)}
				class="item"
				class:active
				aria-current={active ? 'page' : undefined}
			>
				<NavIcon shape={item.shape} {active} />
				{item.label}
			</a>
		{/each}
	</nav>
	<div class="spacer"></div>
	{#if bankroll}
		<a href={resolve('/bankrolls')} class="bankroll">
			<div class="caption">Bankroll active</div>
			<div class="name">{bankroll.name}</div>
			<div class="balance num">{money(bankroll.balance)}</div>
			{#if bankroll.stopLoss !== null && bankroll.stopLossMargin !== null}
				<Meter
					value={bankroll.balance > 0 ? 1 - bankroll.stopLoss / bankroll.balance : 0}
					height={6}
					track="#E4E9DF"
				/>
				<div class="hint">
					Stop-loss {money(bankroll.stopLoss)} · marge {money(Math.max(0, bankroll.stopLossMargin))}
				</div>
			{/if}
		</a>
	{/if}
	<a href="{resolve('/paris')}?ajout=1" class="cta">Nouveau pari</a>
</aside>

<style>
	.sidebar {
		width: var(--sidebar-width);
		flex: none;
		background: var(--surface);
		border-right: 1px solid var(--border);
		display: flex;
		flex-direction: column;
		padding: 22px 16px;
		position: sticky;
		top: 0;
		height: 100vh;
	}
	.brand {
		padding: 0 10px 24px;
	}
	nav {
		display: flex;
		flex-direction: column;
		gap: 4px;
	}
	.item {
		display: flex;
		gap: 11px;
		align-items: center;
		padding: 11px 12px;
		border-radius: 12px;
		font-size: 14px;
		font-weight: 600;
		color: var(--text-2);
	}
	.item:hover {
		background: var(--bg);
	}
	.item.active {
		font-weight: 700;
		background: var(--grass-soft);
		color: var(--grass-deep);
	}
	.spacer {
		flex: 1;
	}
	.bankroll {
		display: block;
		border-radius: 16px;
		padding: 14px;
		background: var(--bg);
		border: 1px solid var(--border);
	}
	.caption {
		font-size: 11px;
		color: var(--muted);
	}
	.name {
		font-size: 15px;
		font-weight: 700;
		margin-top: 3px;
	}
	.balance {
		font-size: 20px;
		margin: 4px 0 10px;
	}
	.hint {
		font-size: 10px;
		color: var(--muted);
		margin-top: 6px;
	}
	.cta {
		margin-top: 12px;
		height: 44px;
		border-radius: 14px;
		background: var(--grass);
		color: #fff;
		display: grid;
		place-items: center;
		font-size: 14px;
		font-weight: 700;
	}
</style>
