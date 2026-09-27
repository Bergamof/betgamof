# Betgamof

Suivi personnel de paris sportifs (paris, montantes, bankrolls, stats, discipline). Mono-utilisateur. Implémente le design Claude Design « Pelouse » (piste 1c, tours 2 à 4).

## Stack
- Backend : Kotlin 2.4, JDK 21, **architecture hexagonale en modules Gradle**, Ktor 3.6 (Netty), kotlinx.serialization, Exposed 1.5 (paquets `org.jetbrains.exposed.v1.*`), Flyway 13, SQLite (xerial), Gradle 8.14 (Kotlin DSL, `gradle/libs.versions.toml` avec `[plugins]`, config commune dans `backend/build.gradle.kts`), Kover 0.9 (couverture), MockK (tests REST).
- Frontend : SvelteKit 2 + Svelte 5 (runes), TypeScript, Vite 8, adapter-node, Vitest, ESLint + Prettier (tabs).
- Déploiement : Dockerfiles multi-stage (`backend/`, `frontend/`), `compose.yaml` (API non exposée, volume `betgamof-data`).

## Commandes
- Backend (dans `backend/`) : lint `./gradlew ktlintCheck detekt` (format : `ktlintFormat`) · tests + couverture `./gradlew check` (unitaires `test`, composant `:application:componentTest`, `koverVerify` ≥ 80 % par module, détail `koverLog`/`koverHtmlReport` ; un module : `./gradlew :business:test`, un test : `./gradlew :business:test --tests '*MontanteEngineTest'`) · build `./gradlew :application:installDist` · run `BETGAMOF_API_TOKEN=dev BETGAMOF_SEED_DEMO=true ./gradlew :application:run`.
- Frontend (dans `frontend/`) : lint `npm run lint` (format : `npm run format`) · types `npm run check` · tests `npm test` (un fichier : `npx vitest run src/lib/format.spec.ts`) · build `npm run build` · dev `npm run dev` (lit `frontend/.env`, voir `.env.example`).
- Docker : `cp .env.example .env && docker compose up -d --build`.

## Architecture
- `backend/` — hexagonale, dépendances uniquement vers `business` (garanties par les modules Gradle) :
  - `business/` — Kotlin pur, **aucune dépendance** (paquets `fr.bergamof.betgamof.business.*`) :
    - `domain/` — objets et calculs : `Money` (centimes), `Bet`/`NewBet`/`BetChange`/`Settlement`, `Montante*`, `MontantePlanner` (aperçu, cote requise), `MontanteEngine` (rejoue les paris d'une montante), `BankrollLedger` (soldes), `KellyAdvisor`, `Stats`, `DisciplineRule`/`NewRule`, `SportEvent`.
    - `port/inbound/UseCases.kt` (ports entrants : `BankrollUseCases`, `BetUseCases`, `MontanteUseCases`, `InsightUseCases`, `EventUseCases`), `port/outbound/` (ports sortants : repositories, `BetCriteria` = critères de sélection des paris dont `matches()` fait référence, `EventCatalog`), `service/` (logique métier ; `Portfolio` = lectures ciblées : état d'une montante rejoué avec ses seuls paris, soldes à partir de totaux agrégés `BetTotals`), `model/Views.kt` (read models), `Errors.kt`.
    - `src/testFixtures/` — fakes en mémoire des ports sortants (`InMemoryRepositories.kt`), réutilisés par `application`.
  - `inbound/rest/` — adaptateur entrant Ktor : `HttpApi.kt` (`betgamofApi(useCases, token)`), routes, DTO requêtes (`Requests.kt`, `toXxx()` → objets du domaine) et réponses `*Json.kt` + mappers `toJson()`, auth bearer, erreurs → HTTP. Seul module qui connaît kotlinx.serialization.
  - `outbound/persistence/sqlite/` — adaptateur sortant Exposed/SQLite (`outbound/persistence/` ne fait que regrouper un sous-module par base : une autre base en prod = un module frère implémentant les mêmes ports) : `SqlitePersistence` (façade publique), repositories et tables `internal` (mapping lignes ↔ domaine uniquement ici) ; migrations Flyway dans `src/main/resources/db/migration/`.
  - `outbound/odds/` — adaptateur sortant : catalogue d'événements **simulé** (`SimulatedEventCatalog` implémente `EventCatalog`).
  - `application/` — démarrage et câblage : `Application.kt` (adaptateurs → services → API), `config/AppConfig.kt`, `seed/DemoSeeder.kt`, `logback.xml` ; tests unitaires (`src/test`) + tests composant HTTP de bout en bout sur SQLite (`src/componentTest`, suite Gradle `componentTest`).
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
- Lecture des paris — jamais tout l'historique « par défaut » : chaque cas d'usage charge ce qu'il lui faut via `BetCriteria` (période `Period`, bankroll, montantes, statut, page…). Liste des paris paginable (`limit`/`offset`), soldes via `directTotalsByBankroll()` (lignes parcourues sans être gardées). Seules les statistiques d'une période chargent ses paris.
- Montante — config seule en base (+ `closed_at` manuel) ; état (capital, sécurisé, engagé, relances, statut) **dérivé** par `MontanteEngine` en rejouant ses paris par `placed_at`.
- DisciplineRule — MAX_STAKE_PCT, PAUSE_AFTER_LOSSES, SINGLE_ACTIVE_MONTANTE ; évaluée sur 30 jours.

## Décisions techniques
- 2026-09-23 — Ktor + SQLite fichier : léger, rapide, sans serveur de BDD (usage perso).
- 2026-09-23 — Montants en centimes (`Money`, value class), sérialisés en euros (nombre JSON).
- 2026-09-23 — États dérivés (soldes, montantes) plutôt que stockés : une seule source de vérité, pas de désynchronisation.
- 2026-09-23 — Effet d'une montante sur sa bankroll = sécurisé − engagé + (capital si mise non exclue ou montante terminée). Une relance réengage le capital de départ.
- 2026-09-23 — Kelly : proba = 1/cote corrigée par l'historique de la tranche de cote (lissage, poids 10), puis fraction de Kelly de la bankroll.
- 2026-09-26 — Architecture hexagonale en modules Gradle `business` / `inbound/*` / `outbound/*` (persistance : `outbound/persistence/<techno>`) / `application` : le compilateur interdit les dépendances vers l'extérieur ; `business` ne connaît ni Ktor, ni Exposed, ni la sérialisation. Les adaptateurs mappent vers/depuis les objets du domaine chez eux.
- 2026-09-26 — Pas de `buildSrc` : plugins déclarés `apply false` à la racine et appliqués via `configure(subprojects.filter { it.buildFile.exists() })` (les modules de regroupement n'ont pas de build script) ; seuil Kover 80 % de lignes par module, vérifié par `check`.
- 2026-09-27 — Lectures ciblées plutôt qu'un instantané global (`PortfolioSnapshot` supprimé) : mémoire et temps par requête bornés par ce que la requête affiche. La règle de gain reste dans le domaine (`profitOf`, `BetTotals.add`) ; l'adaptateur SQL filtre/trie/pagine mais ne recalcule rien. Un test de contrat (`BetQueriesTest`) vérifie que la traduction SQL de chaque critère = `BetCriteria.matches`.
- 2026-09-23 — Versions d'outils épinglées dans `.sdkmanrc` (JDK) et `.nvmrc` (Node) ; la CI les lit (`java-version-file`, `node-version-file`) : changer de version = modifier ces fichiers, plus les images des Dockerfiles.
- 2026-09-23 — Auth : mot de passe unique côté SvelteKit + jeton partagé SvelteKit → API ; l'API n'est pas exposée publiquement.

## Pièges et conventions spécifiques
- SQLite : instants stockés en texte ISO de longueur variable (fractions de seconde) → comparer/trier via `julianday(...)`, jamais en texte. `LIKE`/`lower()` n'ignorent la casse que pour l'ASCII (« é » ≠ « É » en SQL, contrairement à `BetCriteria.matches`). Jokers `%`/`_` échappés.
- Exposed 1.x : imports `org.jetbrains.exposed.v1.core/jdbc`, opérateurs top-level (`import org.jetbrains.exposed.v1.core.eq`).
- Une classe `@Serializable` ne doit pas avoir de `companion object` privé (lookup du serializer cassé) : constantes au niveau fichier.
- Nouveau champ exposé par l'API : l'ajouter au read model (`business/.../model/Views.kt`), au DTO `inbound/rest/*Json.kt` + son mapper `toJson()`, puis à `frontend/src/lib/api/types.ts`.
- Tests des services : fakes en mémoire (`business/src/testFixtures`), `ServiceFixture` câble tous les services. Tests REST : ports entrants mockés (MockK), `RestTest.api { }` + `JsonObject.at("a.0.b")`. Invariants du domaine : `ValidationTest` (message attendu → objet invalide).
- Sous-projets : le plugin Kotlin étant déjà sur le classpath racine, utiliser `id("org.jetbrains.kotlin.plugin.serialization")` **sans version** (pas `alias(...)`).
- detekt : `MaxLineLength` = 140 (aligné sur ktlint) ; `seed/` et `outbound/odds/` sont des données, exclues de MagicNumber/LongParameterList.
- ESLint Svelte impose `resolve()` de `$app/paths` pour tout `href`/`goto` : `href={resolve('/paris')}`, `href="{resolve('/paris')}?ajout=1"`, `resolve('/(app)/montantes/[id]', { id })`, `goto(resolve(\`/statistiques?${q}\`))`.
- `$state` littéral : typer via générique (`$state<MontanteMode>('OBJECTIVE')`) sinon TS rétrécit le type.
- Props servant de valeur initiale d'un formulaire : lire dans `untrack(...)`.
- npm 10 plante sur `npm install` (bug arborist peer-set) : utiliser `npm ci`, ou npm 11 / Node 24.
- Intl `fr-FR` utilise des espaces insécables fines (U+202F) : les normaliser dans les assertions de test.

## État du projet
- Fait : API complète en architecture hexagonale (`business` / `inbound` / `outbound` / `application`) + tests unitaires ≥ 80 % par module et tests composant HTTP, toutes les pages du design (mobile + desktop), connexion, Docker, CI.
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
- Noms de branche parlants : kebab-case décrivant précisément le travail en cours (ex. `feat/user-signup-email-validation`) ; jamais `fix/bug`, `wip`, `test` ou identifiant aléatoire. Préfixe imposé par l'outil (ex. `claude/`) conservé, mais suite parlante (ex. `claude/perf/scoped-repository-queries`). Renommer si le périmètre change, avant push/PR. **Impératif** : si l'outil impose une branche au nom non parlant, le signaler et demander avant le premier push, jamais après.
- Mettre à jour `CHANGELOG.md` (Keep a Changelog) pour tout changement notable.
- Avant chaque commit : lint + tests au vert.

## Maintenance de ce fichier (obligatoire)
- Lire ce fichier avant d'explorer le code ; s'y fier plutôt que tout re-parcourir.
- À la fin de chaque tâche, AVANT le commit, mettre à jour ce fichier si la tâche a changé : stack ou dépendance majeure, commandes, architecture ou emplacement de fichiers clés, modèle de domaine, décision technique, piège découvert, état du projet.
- Inclure la mise à jour dans le même commit que le changement concerné.
- Rester concis (< ~200 lignes) : condenser, déplacer le détail dans `docs/`, supprimer l'obsolète.
