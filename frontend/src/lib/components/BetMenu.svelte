<script lang="ts">
	import { api, mutate } from '$lib/api/client';
	import type { Bet } from '$lib/api/types';
	import { amount, parseNumber } from '$lib/format';
	import Dialog from './Dialog.svelte';

	// Secondary actions behind "···": cash-out, refund, edit, delete.
	let { bet, open = $bindable(false) }: { bet: Bet; open?: boolean } = $props();

	type View = 'menu' | 'cashout' | 'edit' | 'delete';
	let view: View = $state('menu');
	let cashout = $state('');
	let stake = $state('');
	let odds = $state('');
	let bookmaker = $state('');
	let error: string | null = $state(null);
	let busy = $state(false);

	$effect(() => {
		if (open) {
			view = 'menu';
			error = null;
			cashout = '';
			stake = String(bet.stake);
			odds = bet.odds.toFixed(2);
			bookmaker = bet.bookmaker;
		}
	});

	async function run(action: () => Promise<unknown>) {
		busy = true;
		error = await mutate(action);
		busy = false;
		if (!error) open = false;
	}

	const settle = (status: 'VOID' | 'CASHOUT') =>
		run(() =>
			api.post(`/bets/${bet.id}/settlement`, {
				status,
				cashout: status === 'CASHOUT' ? parseNumber(cashout) : null
			})
		);

	const save = () =>
		run(() =>
			api.put(`/bets/${bet.id}`, { stake: parseNumber(stake), odds: parseNumber(odds), bookmaker })
		);

	const remove = () => run(() => api.delete(`/bets/${bet.id}`));
</script>

<Dialog bind:open title={bet.label}>
	{#if view === 'menu'}
		<div class="menu">
			<button type="button" onclick={() => (view = 'cashout')}>Cash-out</button>
			<button type="button" onclick={() => settle('VOID')} disabled={busy}>
				Remboursé (mise rendue)
			</button>
			<button type="button" onclick={() => (view = 'edit')}>Modifier la mise ou la cote</button>
			<button type="button" class="danger" onclick={() => (view = 'delete')}>Supprimer</button>
		</div>
	{:else if view === 'cashout'}
		<label class="field">
			<span>Montant encaissé</span>
			<input class="big" inputmode="decimal" bind:value={cashout} placeholder="0" />
		</label>
		<div class="muted small">
			Mise {amount(bet.stake)} · gain potentiel {amount(bet.potentialReturn)}
		</div>
		<button class="btn" type="button" onclick={() => settle('CASHOUT')} disabled={busy || !cashout}>
			Valider le cash-out
		</button>
	{:else if view === 'edit'}
		<div class="row">
			<label class="field">
				<span>Mise</span>
				<input
					class="big"
					inputmode="decimal"
					bind:value={stake}
					disabled={bet.montanteId !== null}
				/>
			</label>
			<label class="field">
				<span>Cote</span>
				<input class="big" inputmode="decimal" bind:value={odds} />
			</label>
		</div>
		<label class="field">
			<span>Bookmaker</span>
			<input bind:value={bookmaker} />
		</label>
		<button class="btn" type="button" onclick={save} disabled={busy}>Enregistrer</button>
	{:else}
		<p class="confirm">Supprimer définitivement ce pari ?</p>
		<button class="btn danger" type="button" onclick={remove} disabled={busy}>Supprimer</button>
	{/if}
	{#if error}<div class="form-error">{error}</div>{/if}
</Dialog>

<style>
	.menu {
		display: flex;
		flex-direction: column;
		gap: 8px;
	}
	.menu button {
		text-align: left;
		padding: 13px 15px;
		border-radius: 14px;
		border: 1px solid var(--border);
		background: var(--surface);
		font-size: 14px;
		font-weight: 600;
	}
	.menu .danger {
		color: var(--loss);
	}
	.row {
		display: flex;
		gap: 10px;
	}
	.row .field {
		flex: 1;
	}
	.small {
		font-size: 12px;
	}
	.confirm {
		margin: 0;
		font-size: 14px;
	}
</style>
