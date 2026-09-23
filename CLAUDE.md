# Betgamof

Suivi personnel de paris sportifs (paris, montantes, bankrolls, stats, discipline). Mono-utilisateur. Implémente le design Claude Design « Pelouse » (piste 1c, tours 2 à 4).

## Stack
- Backend : Kotlin 2.4, JDK 21, Ktor 3.6 (Netty), kotlinx.serialization, Exposed 1.5 (paquets `org.jetbrains.exposed.v1.*`), Flyway 13, SQLite (xerial), Gradle 8.14 (Kotlin DSL, `gradle/libs.versions.toml`).
- Frontend : SvelteKit 2 + Svelte 5 (runes), TypeScript, Vite 8, adapter-node, Vitest, ESLint + Prettier (tabs).
- Déploiement : Dockerfiles multi-stage (`backend/`, `frontend/`), `compose.yaml` (API non exposée, volume `betgamof-data`).

## Commandes
- Backend (dans `backend/`) : lint `./gradlew ktlintCheck detekt` (format : `ktlintFormat`) · tests `./gradlew test` (un test : `--tests 'fr.bergamof.betgamof.domain.MontanteEngineTest'`) · build `./gradlew installDist` · run `BETGAMOF_API_TOKEN=dev BETGAMOF_SEED_DEMO=true ./gradlew run`.
- Frontend (dans `frontend/`) : lint `npm run lint` (format : `npm run format`) · types `npm run check` · tests `npm test` (un fichier : `npx vitest run src/lib/format.spec.ts`) · build `npm run build` · dev `npm run dev` (lit `frontend/.env`, voir `.env.example`).
- Docker : `cp .env.example .env && docker compose up -d --build`.

## Architecture
- `backend/src/main/kotlin/fr/bergamof/betgamof/`
  - `domain/` — logique pure et testée : `Money` (centimes), `Bet`, `Montante*`, `MontantePlanner` (aperçu, cote requise), `MontanteEngine` (rejoue les paris d'une montante), `BankrollLedger` (soldes), `KellyAdvisor`, `Stats`, `DisciplineRule`.
  - `persistence/` — tables Exposed + repositories (chargent tout, le domaine filtre en mémoire) ; schéma dans `src/main/resources/db/migration/`.
  - `service/` — `PortfolioSnapshot` (bankrolls + paris + états de montantes dérivés) puis services ; `Views.kt` = modèles JSON renvoyés.
  - `api/` — routes Ktor (`/api/...`, bearer token), requêtes, gestion d'erreurs ; `/health` public.
  - `events/` — catalogue d'événements **simulé** (`EventSource`) ; `seed/` — données de démo.
- `frontend/src/`
  - `hooks.server.ts` — garde d'authentification (cookie de session HMAC du mot de passe).
  - `lib/server/` — appels à l'API côté serveur (`getJson`), auth. `routes/api/[...path]` — proxy navigateur → API (le jeton ne sort jamais du serveur).
  - `lib/api/` — types TS miroirs de `Views.kt`, client navigateur (`api`, `mutate` = appel + `invalidateAll`).
  - `lib/components/` — UI partagée ; `BetForm.svelte` = formulaire d'ajout (page mobile et panneau desktop).
  - `routes/(app)/` — pages : `/`, `/paris`, `/paris/nouveau`, `/montantes`, `/montantes/nouvelle`, `/montantes/[id]`, `/bankrolls`, `/statistiques`, `/journal`.
- Flux : page `load` (serveur) → `getJson` → API ; mutations navigateur → `/api/*` (proxy) → API → `invalidateAll`.

## Modèle de domaine
- Bankroll — solde initial + stop-loss + fraction de Kelly + mise fixe ; **solde jamais stocké**, calculé par `BankrollLedger`.
- Bet — sélections (1 = simple, ≥2 = combiné/système), cote, mise, statut OPEN/WON/LOST/VOID/CASHOUT ; peut appartenir à une montante (un pari = un palier).
- Montante — config seule en base (+ `closed_at` manuel) ; état (capital, sécurisé, engagé, relances, statut) **dérivé** par `MontanteEngine` en rejouant ses paris par `placed_at`.
- DisciplineRule — MAX_STAKE_PCT, PAUSE_AFTER_LOSSES, SINGLE_ACTIVE_MONTANTE ; évaluée sur 30 jours.

## Décisions techniques
- 2026-09-23 — Ktor + SQLite fichier : léger, rapide, sans serveur de BDD (usage perso).
- 2026-09-23 — Montants en centimes (`Money`, value class), sérialisés en euros (nombre JSON).
- 2026-09-23 — États dérivés (soldes, montantes) plutôt que stockés : une seule source de vérité, pas de désynchronisation.
- 2026-09-23 — Effet d'une montante sur sa bankroll = sécurisé − engagé + (capital si mise non exclue ou montante terminée). Une relance réengage le capital de départ.
- 2026-09-23 — Kelly : proba = 1/cote corrigée par l'historique de la tranche de cote (lissage, poids 10), puis fraction de Kelly de la bankroll.
- 2026-09-23 — Auth : mot de passe unique côté SvelteKit + jeton partagé SvelteKit → API ; l'API n'est pas exposée publiquement.

## Pièges et conventions spécifiques
- Exposed 1.x : imports `org.jetbrains.exposed.v1.core/jdbc`, opérateurs top-level (`import org.jetbrains.exposed.v1.core.eq`).
- Une classe `@Serializable` ne doit pas avoir de `companion object` privé (lookup du serializer cassé) : constantes au niveau fichier.
- detekt : `MaxLineLength` = 140 (aligné sur ktlint) ; `seed/` et `events/` sont des données, exclues de MagicNumber/LongParameterList.
- ESLint Svelte impose `resolve()` de `$app/paths` pour tout `href`/`goto` : `href={resolve('/paris')}`, `href="{resolve('/paris')}?ajout=1"`, `resolve('/(app)/montantes/[id]', { id })`, `goto(resolve(\`/statistiques?${q}\`))`.
- `$state` littéral : typer via générique (`$state<MontanteMode>('OBJECTIVE')`) sinon TS rétrécit le type.
- Props servant de valeur initiale d'un formulaire : lire dans `untrack(...)`.
- npm 10 plante sur `npm install` (bug arborist peer-set) : utiliser `npm ci`, ou npm 11 / Node 24.
- Intl `fr-FR` utilise des espaces insécables fines (U+202F) : les normaliser dans les assertions de test.

## État du projet
- Fait : API complète + tests (domaine + intégration), toutes les pages du design (mobile + desktop), connexion, Docker, CI.
- Prochaines étapes possibles : fournisseur de cotes réel derrière `EventSource`, dépôts/retraits sur bankroll, PWA/offline, édition des sélections d'un pari.

## Licence
AGPL-3.0 — tout nouveau fichier et toute dépendance doivent rester compatibles avec elle.

## Règles de code
- Clean code systématique (nommage explicite, SRP, SOLID, DRY/KISS/YAGNI, gestion d'erreurs explicite, pas de code mort).
- Bonnes pratiques officielles de chaque techno ; rester idiomatique.
- En cas de conflit, la bonne pratique de la techno l'emporte sur la règle générique.
- Tout nouveau code est accompagné de tests.

## Git
- Conventional Commits (anglais, impératif) ; branches `feat/…`, `fix/…`, `chore/…` depuis `main`.
- Noms de branche parlants : kebab-case décrivant précisément le travail en cours (ex. `feat/user-signup-email-validation`) ; jamais `fix/bug`, `wip`, `test` ou identifiant aléatoire. Préfixe imposé par l'outil (ex. `claude/`) conservé, mais suite parlante. Renommer si le périmètre change, avant push/PR.
- Mettre à jour `CHANGELOG.md` (Keep a Changelog) pour tout changement notable.
- Avant chaque commit : lint + tests au vert.

## Maintenance de ce fichier (obligatoire)
- Lire ce fichier avant d'explorer le code ; s'y fier plutôt que tout re-parcourir.
- À la fin de chaque tâche, AVANT le commit, mettre à jour ce fichier si la tâche a changé : stack ou dépendance majeure, commandes, architecture ou emplacement de fichiers clés, modèle de domaine, décision technique, piège découvert, état du projet.
- Inclure la mise à jour dans le même commit que le changement concerné.
- Rester concis (< ~200 lignes) : condenser, déplacer le détail dans `docs/`, supprimer l'obsolète.
