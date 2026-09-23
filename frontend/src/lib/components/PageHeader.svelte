<script lang="ts">
	import { resolve } from '$app/paths';
	import type { Pathname } from '$app/types';
	import type { Snippet } from 'svelte';
	import Avatar from './Avatar.svelte';

	// Desktop: white 72px top bar. Mobile: large title row (or centered title with a back link).
	let {
		title,
		subtitle,
		back,
		actions,
		mobileActions,
		showAvatar = true,
		mobile = true
	}: {
		title: string;
		subtitle?: string;
		back?: Pathname;
		actions?: Snippet;
		mobileActions?: Snippet;
		showAvatar?: boolean;
		/** False when the page draws its own mobile header. */
		mobile?: boolean;
	} = $props();
</script>

<header class="desktop">
	<div>
		<h1>{title}</h1>
		{#if subtitle}<div class="subtitle">{subtitle}</div>{/if}
	</div>
	<div class="actions">
		{@render actions?.()}
		{#if showAvatar}<Avatar />{/if}
	</div>
</header>

{#if mobile}
	<header class="mobile">
		{#if back}
			<a href={resolve(back)} class="square-button" aria-label="Retour">‹</a>
			<h1 class="display small">{title}</h1>
			<div class="mobile-actions">{@render mobileActions?.()}</div>
		{:else}
			<h1 class="display">{title}</h1>
			<div class="mobile-actions">{@render mobileActions?.()}</div>
		{/if}
	</header>
{/if}

<style>
	h1 {
		margin: 0;
	}
	.desktop {
		height: var(--header-height);
		flex: none;
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 0 28px;
		background: var(--surface);
		border-bottom: 1px solid var(--border);
		position: sticky;
		top: 0;
		z-index: 10;
	}
	.desktop h1 {
		font-size: 17px;
		font-weight: 700;
	}
	.subtitle {
		font-size: 12px;
		color: var(--muted);
		margin-top: 2px;
	}
	.actions {
		display: flex;
		align-items: center;
		gap: 12px;
	}
	.mobile {
		display: none;
		padding: calc(16px + env(safe-area-inset-top)) 24px 14px;
		justify-content: space-between;
		align-items: center;
		gap: 12px;
	}
	.mobile h1 {
		font-size: 26px;
	}
	.mobile h1.small {
		font-size: 18px;
		text-align: center;
		flex: 1;
		min-width: 0;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}
	.mobile-actions {
		display: flex;
		gap: 8px;
		min-width: 34px;
		justify-content: flex-end;
	}
	.square-button {
		width: 34px;
		height: 34px;
		flex: none;
		border-radius: 11px;
		background: var(--surface);
		border: 1px solid var(--border);
		display: grid;
		place-items: center;
		font-size: 15px;
		font-weight: 700;
	}
	@media (max-width: 1023px) {
		.desktop {
			display: none;
		}
		.mobile {
			display: flex;
		}
	}
</style>
