<script lang="ts">
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import PageHeader from '$lib/components/PageHeader.svelte';
</script>

<PageHeader title="Oups" />
<div class="content">
	<div class="card box">
		<p class="code num">{page.status}</p>
		<p>{page.error?.message}</p>
		<a class="btn" href={resolve('/')}>Retour à l'accueil</a>
	</div>
</div>

<style>
	.content {
		padding: 24px;
	}
	.box {
		padding: 24px;
		max-width: 480px;
	}
	.code {
		font-size: 38px;
		margin: 0;
	}
</style>
