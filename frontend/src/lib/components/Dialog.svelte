<script lang="ts">
	import type { Snippet } from 'svelte';

	// `modal`: small window centred on screen. `sheet`: wide panel resting on the bottom edge,
	// centred horizontally, for forms that need room (adding or editing a bet).
	type Variant = 'modal' | 'sheet';

	let {
		open = $bindable(false),
		title,
		variant = 'modal',
		children
	}: { open?: boolean; title: string; variant?: Variant; children: Snippet } = $props();

	let dialog: HTMLDialogElement | undefined = $state();

	$effect(() => {
		if (!dialog) return;
		if (open && !dialog.open) dialog.showModal();
		if (!open && dialog.open) dialog.close();
	});
</script>

<dialog
	bind:this={dialog}
	class={variant}
	onclose={() => (open = false)}
	onclick={(event) => {
		if (event.target === dialog) open = false;
	}}
>
	<div class="content">
		<div class="head">
			<h2 class="card-title">{title}</h2>
			<button type="button" class="close" aria-label="Fermer" onclick={() => (open = false)}
				>×</button
			>
		</div>
		<div class="body">
			{#if open}{@render children()}{/if}
		</div>
	</div>
</dialog>

<style>
	dialog {
		border: none;
		padding: 0;
		border-radius: 24px;
		width: min(440px, calc(100vw - 32px));
		background: var(--bg);
		color: var(--text);
		box-shadow: 0 24px 50px rgba(18, 24, 14, 0.25);
	}
	dialog::backdrop {
		background: rgba(27, 36, 23, 0.35);
	}
	.sheet {
		margin: auto auto 0;
		width: min(1040px, calc(100vw - 48px));
		max-width: none;
		border-radius: 28px 28px 0 0;
		box-shadow: 0 -18px 50px rgba(18, 24, 14, 0.25);
		overflow: hidden;
	}
	.sheet[open] {
		animation: rise 0.22s ease-out;
	}
	@keyframes rise {
		from {
			transform: translateY(40px);
			opacity: 0;
		}
	}
	@media (prefers-reduced-motion: reduce) {
		.sheet[open] {
			animation: none;
		}
	}
	@media (max-width: 600px) {
		.sheet {
			width: 100vw;
		}
	}
	.content,
	.body {
		display: flex;
		flex-direction: column;
		gap: 14px;
	}
	.content {
		padding: 20px;
	}
	.head {
		display: flex;
		justify-content: space-between;
		align-items: center;
	}
	/* Only the body scrolls: the header stays put and the scrollbar keeps clear of the rounded corners. */
	.sheet .content {
		padding: 0;
		gap: 0;
		max-height: min(92dvh, calc(100dvh - 24px));
	}
	.sheet .head {
		padding: 20px 24px 12px;
	}
	.sheet .body {
		min-height: 0;
		overflow-y: auto;
		overscroll-behavior: contain;
		scrollbar-width: thin;
		scrollbar-color: var(--border) transparent;
		padding: 2px 24px calc(24px + env(safe-area-inset-bottom));
	}
	.close {
		width: 30px;
		height: 30px;
		border-radius: 9px;
		border: 1px solid var(--border);
		background: var(--surface);
		font-weight: 700;
		color: var(--muted);
	}
</style>
