<script lang="ts">
	import type { Snippet } from 'svelte';

	let {
		open = $bindable(false),
		title,
		children
	}: { open?: boolean; title: string; children: Snippet } = $props();

	let dialog: HTMLDialogElement | undefined = $state();

	$effect(() => {
		if (!dialog) return;
		if (open && !dialog.open) dialog.showModal();
		if (!open && dialog.open) dialog.close();
	});
</script>

<dialog
	bind:this={dialog}
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
		{#if open}{@render children()}{/if}
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
	.content {
		padding: 20px;
		display: flex;
		flex-direction: column;
		gap: 14px;
	}
	.head {
		display: flex;
		justify-content: space-between;
		align-items: center;
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
