<script lang="ts">
	import { OWNER } from '$lib/labels';
</script>

<form method="POST" action="/logout" class="avatar-form">
	<button class="avatar" title="Se déconnecter ({OWNER.name})" aria-label="Se déconnecter">
		{OWNER.initials}
	</button>
</form>

<style>
	.avatar-form {
		display: contents;
	}
	.avatar {
		width: 38px;
		height: 38px;
		flex: none;
		border: none;
		border-radius: 50%;
		background: var(--grass);
		color: #fff;
		display: grid;
		place-items: center;
		font-size: 13px;
		font-weight: 700;
	}
</style>
