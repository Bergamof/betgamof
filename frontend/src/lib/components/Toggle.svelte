<script lang="ts">
	let {
		checked = $bindable(false),
		label,
		size = 'md',
		disabled = false,
		onchange
	}: {
		checked?: boolean;
		label: string;
		size?: 'md' | 'lg';
		disabled?: boolean;
		onchange?: (checked: boolean) => void;
	} = $props();
</script>

<button
	type="button"
	role="switch"
	class="toggle {size}"
	class:on={checked}
	aria-checked={checked}
	aria-label={label}
	{disabled}
	onclick={() => {
		checked = !checked;
		onchange?.(checked);
	}}
>
	<span class="knob"></span>
</button>

<style>
	.toggle {
		width: 44px;
		height: 26px;
		flex: none;
		border: none;
		border-radius: 999px;
		background: var(--idle);
		display: flex;
		align-items: center;
		padding: 3px;
		transition: background 0.15s;
	}
	.toggle.lg {
		width: 46px;
		height: 28px;
	}
	.knob {
		width: 20px;
		height: 20px;
		border-radius: 50%;
		background: #fff;
		transition: transform 0.15s;
	}
	.lg .knob {
		width: 22px;
		height: 22px;
	}
	.on {
		background: var(--grass);
	}
	.on .knob {
		transform: translateX(18px);
	}
	.toggle:disabled {
		opacity: 1;
		cursor: default;
	}
</style>
