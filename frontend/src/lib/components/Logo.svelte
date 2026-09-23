<script lang="ts">
	let { size = 23 }: { size?: number } = $props();
</script>

<span class="logo display" style:font-size="{size}px">Bet<span class="accent">gamof</span></span>

<style>
	.logo {
		letter-spacing: -0.02em;
	}
	.accent {
		color: var(--grass);
	}
</style>
