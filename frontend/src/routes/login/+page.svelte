<script lang="ts">
	import { enhance } from '$app/forms';
	import Logo from '$lib/components/Logo.svelte';

	let { form } = $props();
</script>

<svelte:head><title>Connexion · Betgamof</title></svelte:head>

<main>
	<section class="panel">
		<div class="stripes"></div>
		<div class="body">
			<Logo size={34} />
			<p class="tagline">Tes paris, tes montantes, tes bankrolls. Au vert.</p>
			<form method="POST" use:enhance>
				<label class="field">
					<span>Mot de passe</span>
					<!-- svelte-ignore a11y_autofocus -->
					<input
						type="password"
						name="password"
						autocomplete="current-password"
						required
						autofocus
					/>
				</label>
				{#if form?.invalid}<div class="form-error">Mot de passe incorrect.</div>{/if}
				<button class="btn">Entrer sur la pelouse</button>
			</form>
		</div>
	</section>
</main>

<style>
	main {
		min-height: 100vh;
		display: grid;
		place-items: center;
		padding: 20px;
	}
	.panel {
		position: relative;
		overflow: hidden;
		width: min(420px, 100%);
		border-radius: 28px;
		background: var(--surface);
		border: 1px solid var(--border);
		box-shadow: 0 24px 50px rgba(18, 24, 14, 0.12);
	}
	.stripes {
		height: 90px;
		background: var(--mowing), var(--grass);
	}
	.body {
		padding: 24px;
		display: flex;
		flex-direction: column;
		gap: 10px;
	}
	.tagline {
		margin: 0 0 8px;
		color: var(--muted);
		font-size: 14px;
	}
	form {
		display: flex;
		flex-direction: column;
		gap: 12px;
	}
</style>
