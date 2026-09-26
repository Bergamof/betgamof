# Betgamof

Suivi personnel de paris sportifs — paris simples, combinés et systèmes, **montantes**, **bankrolls** multiples, statistiques et journal de discipline. Design « Pelouse » (vert gazon), pensé pour le mobile et le desktop.

## Fonctionnalités

- **Paris** : liste par jour (cartes enrichies sur mobile, tableau dense sur desktop), filtres statut / sport / bookmaker / période, recherche, export CSV, règlement en un clic (Gagné / Perdu) et menu « ··· » (cash-out, remboursé, modification, suppression).
- **Ajout d'un pari** : depuis un événement (sélecteur de marchés par catégorie, ticket combiné) ou de zéro ; cote et mise modifiables, raccourcis de mise, **conseil Kelly** avec bouton « Suivre », rattachement à une montante.
- **Montantes** : modes Objectif (x2…x10), Paliers ou Libre ; exclusion de la mise de la bankroll (le solde final y est reporté à la clôture) ; part des gains sécurisée à chaque palier ; relances ; aperçu des paliers et probabilité de succès ; suivi palier par palier avec scénarios gagné / perdu.
- **Bankrolls** : plusieurs bankrolls avec stop-loss, fraction de Kelly et mise fixe ; soldes calculés à partir de l'historique.
- **Statistiques** : profit cumulé, yield, drawdown, séries, rendement par sport et par tranche de cote, bilan des montantes, points à surveiller.
- **Journal de discipline** : règles (mise max, pause après N pertes, une seule montante active) vérifiées sur 30 jours.

Les cotes « depuis un événement » viennent pour l'instant d'un **catalogue simulé** côté API (aucun fournisseur de cotes n'est branché).

## Architecture

| Dossier     | Rôle                                                                                      |
| ----------- | ----------------------------------------------------------------------------------------- |
| `backend/`  | API REST Kotlin 2 en architecture hexagonale (voir ci-dessous), SQLite, protégée par un jeton |
| `frontend/` | SvelteKit 2 / Svelte 5 (adapter-node) : pages, connexion par mot de passe, proxy vers l'API |

Le backend est découpé en modules Gradle dont les dépendances pointent uniquement vers le domaine :

| Module                  | Rôle                                                                            |
| ----------------------- | ------------------------------------------------------------------------------- |
| `domain`                | Toutes les règles métier en Kotlin pur : paris, montantes, soldes, Kelly, stats |
| `application`           | Cas d'usage, ports entrants (use cases, commandes) et sortants (repositories, transactions, cotes) |
| `adapters/http`         | Adaptateur entrant : API REST Ktor, DTO JSON, authentification                  |
| `adapters/demo`         | Adaptateur entrant : données de démonstration créées via les use cases          |
| `adapters/persistence`  | Adaptateur sortant : SQLite via Exposed + migrations Flyway                     |
| `adapters/odds`         | Adaptateur sortant : catalogue d'événements et de cotes (simulé pour l'instant) |
| `app`                   | Racine de composition : configuration, câblage, point d'entrée                  |

Des tests d'architecture (Konsist) vérifient les règles d'import à l'intérieur des modules, et des suites de contrat communes garantissent que les fakes en mémoire et SQLite se comportent de la même façon.

Le navigateur ne parle qu'au serveur SvelteKit ; celui-ci appelle l'API avec le jeton partagé, qui ne quitte jamais le serveur.

## Prérequis

- JDK 21
- Node.js 22+ (24 recommandé) et npm
- Docker + Docker Compose (optionnel, pour le déploiement)

Les versions sont épinglées à la racine et lues automatiquement par les outils usuels (et par la CI) :

| Fichier | Outil | Contenu |
| --- | --- | --- |
| `.sdkmanrc` | [SDKMAN!](https://sdkman.io) (`sdk env install`), mise | JDK Temurin 21 |
| `.nvmrc` | nvm (`nvm use`), fnm, mise, Volta | Node.js 24 |
| `backend/gradle/wrapper/` | `./gradlew` | Gradle 8.14 |

`.gitattributes` force les fins de ligne LF (CRLF pour les `.bat`), `.vscode/extensions.json` recommande les extensions utiles et `.github/dependabot.yml` propose chaque semaine les mises à jour Gradle, npm, Docker et GitHub Actions.

## Lancement local

```bash
# 1. API (port 8080) avec des données de démonstration
cd backend
BETGAMOF_API_TOKEN=dev BETGAMOF_SEED_DEMO=true ./gradlew :app:run

# 2. Frontend (port 5173)
cd frontend
cp .env.example .env   # BETGAMOF_API_TOKEN=dev, BETGAMOF_APP_PASSWORD=…
npm install
npm run dev
```

Ouvre http://localhost:5173 et connecte-toi avec `BETGAMOF_APP_PASSWORD`.

### Variables d'environnement

| Variable                | Service  | Rôle                                                              |
| ----------------------- | -------- | ----------------------------------------------------------------- |
| `BETGAMOF_API_TOKEN`    | les deux | Jeton partagé entre le frontend et l'API (obligatoire)            |
| `BETGAMOF_APP_PASSWORD` | frontend | Mot de passe de la page de connexion (obligatoire)                |
| `BETGAMOF_BACKEND_URL`  | frontend | URL de l'API (défaut `http://localhost:8080`)                     |
| `ORIGIN`                | frontend | URL publique de l'app en production (protection CSRF SvelteKit)   |
| `BETGAMOF_DB_PATH`      | backend  | Fichier SQLite (défaut `data/betgamof.db`)                        |
| `BETGAMOF_SEED_DEMO`    | backend  | `true` : remplit une base vide avec des données de démonstration  |
| `BETGAMOF_TIMEZONE`     | backend  | Fuseau des statistiques (défaut `Europe/Paris`)                   |
| `PORT`                  | les deux | Port d'écoute                                                     |

## Docker

```bash
cp .env.example .env   # renseigne les secrets
docker compose up -d --build
```

L'app est servie sur http://localhost:3000 ; l'API n'est pas exposée hors du réseau Compose et la base SQLite vit dans le volume `betgamof-data`.

## Tests et qualité

```bash
# Backend : ktlint + detekt, tests JUnit 5, build
cd backend && ./gradlew ktlintCheck detekt test :app:installDist

# Frontend : Prettier + ESLint, svelte-check, Vitest, build
cd frontend && npm run lint && npm run check && npm test && npm run build
```

La CI GitHub Actions (`.github/workflows/ci.yml`) exécute tout cela à chaque push et pull request, puis construit les images Docker.

## Licence

[GNU Affero General Public License v3.0](LICENSE) — toute version modifiée hébergée en ligne doit publier ses sources.
