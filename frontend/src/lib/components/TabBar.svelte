<script lang="ts">
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import NavIcon from './NavIcon.svelte';
	import { NAV_ITEMS, isActive } from './nav';

	const items = NAV_ITEMS.filter((item) => item.mobile);
	const left = items.slice(0, 2);
	const right = items.slice(2);
</script>

{#snippet tab(item: (typeof items)[number])}
	{@const active = isActive(item.href, page.url.pathname)}
	<a href={resolve(item.href)} class="tab" class:active aria-current={active ? 'page' : undefined}>
		<NavIcon shape={item.shape} {active} size={16} />
		<span>{item.label}</span>
	</a>
{/snippet}

<nav class="tabbar" aria-label="Navigation principale">
	{#each left as item (item.href)}{@render tab(item)}{/each}
	<a href={resolve('/paris/nouveau')} class="fab" aria-label="Nouveau pari">+</a>
	{#each right as item (item.href)}{@render tab(item)}{/each}
</nav>

<style>
	.tabbar {
		position: fixed;
		inset: auto 0 0 0;
		z-index: 20;
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 12px 26px calc(22px + env(safe-area-inset-bottom));
		background: var(--surface);
		border-top: 1px solid var(--border);
	}
	.tab {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 5px;
		width: 56px;
		font-size: 10px;
		color: var(--muted);
	}
	.tab.active {
		font-weight: 700;
		color: var(--grass-strong);
	}
	.fab {
		width: 54px;
		height: 54px;
		border-radius: 50%;
		background: var(--grass);
		color: #fff;
		display: grid;
		place-items: center;
		font-size: 28px;
		font-weight: 700;
		margin-top: -24px;
		box-shadow: 0 10px 22px var(--grass-shadow);
	}
</style>
