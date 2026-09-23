<script lang="ts">
	import Sidebar from '$lib/components/Sidebar.svelte';
	import TabBar from '$lib/components/TabBar.svelte';

	let { data, children } = $props();
</script>

<div class="shell">
	<div class="sidebar-slot"><Sidebar bankroll={data.bankroll} /></div>
	<main>{@render children()}</main>
	<div class="tabbar-slot"><TabBar /></div>
</div>

<style>
	.shell {
		display: flex;
		min-height: 100vh;
	}
	main {
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
	}
	.tabbar-slot {
		display: none;
	}
	@media (max-width: 1023px) {
		.sidebar-slot {
			display: none;
		}
		.tabbar-slot {
			display: block;
		}
		main {
			padding-bottom: calc(var(--tabbar-height) + 24px);
		}
	}
</style>
