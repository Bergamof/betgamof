<script lang="ts">
	import { resolve } from '$app/paths';
	import type { Rule } from '$lib/api/types';
	import { ruleLabel } from '$lib/labels';

	let { rules, link = 'count' }: { rules: Rule[]; link?: 'count' | 'edit' } = $props();

	const respected = $derived(rules.filter((rule) => rule.respected).length);
</script>

<section class="card rules">
	<div class="head">
		<h2 class="card-title">Journal de discipline</h2>
		<a href={resolve('/journal')} class="link"
			>{link === 'count' ? `${respected} / ${rules.length}` : 'Modifier'}</a
		>
	</div>
	{#if rules.length === 0}
		<p class="empty">
			Aucune règle. <a href={resolve('/journal')} class="link">Fixe-toi des garde-fous</a>
		</p>
	{:else}
		<ul>
			{#each rules as rule (rule.id)}
				<li class:broken={!rule.respected}>
					<span class="box" aria-label={rule.respected ? 'Respectée' : 'Non respectée'}></span>
					<span>{ruleLabel(rule)}</span>
				</li>
			{/each}
		</ul>
	{/if}
</section>

<style>
	.rules {
		padding: 18px;
	}
	.head {
		display: flex;
		justify-content: space-between;
		align-items: baseline;
	}
	.card-title {
		font-size: 16px;
	}
	ul {
		list-style: none;
		margin: 14px 0 0;
		padding: 0;
		display: flex;
		flex-direction: column;
		gap: 10px;
	}
	li {
		display: flex;
		gap: 10px;
		align-items: center;
		font-size: 13px;
		color: var(--text-3);
	}
	.box {
		width: 18px;
		height: 18px;
		border-radius: 6px;
		background: var(--grass);
		flex: none;
	}
	.broken {
		color: var(--muted-2);
	}
	.broken .box {
		background: transparent;
		border: 2px solid var(--idle);
	}
	.empty {
		font-size: 13px;
		color: var(--muted);
		margin: 12px 0 0;
	}
</style>
