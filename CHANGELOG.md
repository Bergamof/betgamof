# Changelog

Toutes les évolutions notables de ce projet sont consignées ici.
Le format suit [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/) et le projet respecte le [versionnement sémantique](https://semver.org/lang/fr/).

## [Unreleased]

### Added

- API Kotlin / Ktor : bankrolls, paris (simples, combinés, systèmes ; règlement gagné, perdu, remboursé, cash-out), montantes (objectif, paliers, libre ; exclusion de la mise, gains sécurisés, relances), statistiques, journal de discipline, conseil Kelly, catalogue d'événements simulé, données de démonstration.
- Frontend SvelteKit reprenant le design « Pelouse » : tableau de bord, paris (liste et ajout), montantes (création et suivi), bankrolls, statistiques, journal ; mises en page mobile et desktop.
- Connexion par mot de passe, proxy serveur vers l'API protégée par jeton.
- Dockerfiles multi-stage, `compose.yaml`, CI GitHub Actions.
- Tests de contrat des repositories (fakes et SQLite), tests d'architecture Konsist, tests isolés de l'adaptateur HTTP, tests des use cases de statistiques et d'événements.
- Fichiers d'autoconfiguration : `.sdkmanrc` (JDK 21 Temurin), `.nvmrc` (Node 24), `.gitattributes`, `backend/gradle.properties` (build parallèle et cache), recommandations d'extensions VS Code, Dependabot.

### Changed

- Backend aligné sur l'audit d'architecture hexagonale : règles métier et transitions d'état déplacées dans le domaine (erreurs typées), ports secondaires orientés agrégats avec frontière transactionnelle, commandes en entrée des use cases, identifiants typés, enums de l'API et codes SQL découplés du domaine, libellés produits par l'adaptateur HTTP, données de démo créées via les use cases (module `adapters/demo`). Contrat JSON de l'API inchangé.
- Une `IllegalArgumentException` non prévue renvoie désormais 500 au lieu de 400 : seules les erreurs métier donnent 400/409.
- Backend réorganisé en architecture hexagonale : modules Gradle `domain`, `application` (ports et cas d'usage), `adapters/http`, `adapters/persistence`, `adapters/odds` et `app` ; contrat JSON de l'API inchangé.
- Licence passée de GPL-3.0 à AGPL-3.0.
