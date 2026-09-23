<script lang="ts" generics="T extends string">
	// Rounded pill switcher ("Tous / En cours / Gagnés / Perdus").
	let {
		options,
		value = $bindable(),
		onchange,
		stretch = false,
		size = 'md',
		label
	}: {
		options: { value: T; label: string }[];
		value: T;
		onchange?: (value: T) => void;
		stretch?: boolean;
		size?: 'sm' | 'md';
		label: string;
	} = $props();

	function select(next: T) {
		value = next;
		onchange?.(next);
	}
</script>

<div class="segmented {size}" class:stretch role="radiogroup" aria-label={label}>
	{#each options as option (option.value)}
		<button
			type="button"
			role="radio"
			aria-checked={option.value === value}
			class:selected={option.value === value}
			onclick={() => select(option.value)}
		>
			{option.label}
		</button>
	{/each}
</div>

<style>
	.segmented {
		display: flex;
		gap: 7px;
		padding: 4px;
		border-radius: 999px;
		background: var(--surface);
		border: 1px solid var(--border);
	}
	button {
		border: none;
		background: transparent;
		border-radius: 999px;
		font-size: 12px;
		font-weight: 600;
		color: var(--muted);
		padding: 9px 18px;
		white-space: nowrap;
	}
	.sm button {
		padding: 7px 14px;
	}
	.stretch button {
		flex: 1;
		padding-left: 0;
		padding-right: 0;
	}
	button.selected {
		background: var(--grass);
		color: #fff;
		font-weight: 700;
	}
</style>
