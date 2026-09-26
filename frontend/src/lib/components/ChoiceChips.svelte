<script lang="ts" generics="T extends string | number">
	// Square-ish chips with the lemon "selected" state (x2 / x3 / x5, 10 % / 30 %…).
	let {
		options,
		value,
		onselect,
		label
	}: {
		options: { value: T; label: string }[];
		value: T | null;
		onselect: (value: T) => void;
		label: string;
	} = $props();
</script>

<div class="chips" role="radiogroup" aria-label={label}>
	{#each options as option (option.value)}
		<button
			type="button"
			role="radio"
			aria-checked={option.value === value}
			class:selected={option.value === value}
			onclick={() => onselect(option.value)}
		>
			{option.label}
		</button>
	{/each}
</div>

<style>
	.chips {
		display: flex;
		gap: 7px;
	}
	button {
		flex: 1;
		text-align: center;
		font-size: 13px;
		font-weight: 700;
		padding: 10px 0;
		border-radius: 12px;
		background: var(--bg);
		border: 1px solid var(--border);
	}
	button.selected {
		background: var(--lemon);
		border: 2px solid var(--grass);
		padding: 9px 0;
	}
</style>
