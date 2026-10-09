# Changelog

Toutes les évolutions notables de ce projet sont consignées ici.
Le format suit [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/) et le projet respecte le [versionnement sémantique](https://semver.org/lang/fr/).

## [Unreleased]

### Added

- API Kotlin / Ktor : bankrolls, paris (simples, combinés, systèmes ; règlement gagné, perdu, remboursé, cash-out), montantes (objectif, paliers, libre ; exclusion de la mise, gains sécurisés, relances), statistiques, journal de discipline, conseil Kelly, catalogue d'événements simulé, données de démonstration.
- Frontend SvelteKit reprenant le design « Pelouse » : tableau de bord, paris (liste et ajout), montantes (création et suivi), bankrolls, statistiques, journal ; mises en page mobile et desktop.
- Connexion par mot de passe, proxy serveur vers l'API protégée par jeton.
- Dockerfiles multi-stage, `compose.yaml`, CI GitHub Actions.
- Mise en pourcentage de la bankroll : à l'ajout d'un pari, la mise se saisit en euros ou en % du solde de la bankroll (bascule € / %, montant équivalent affiché, mises rapides adaptées : 1 / 2 / 5 / 10 % en mode %, et en mode € des montants ronds proportionnels au solde, par ex. 10 / 25 / 50 / 100 € pour ~1 200 €) ; le pari est enregistré avec son montant en euros.
- Fichiers d'autoconfiguration : `.sdkmanrc` (JDK 21 Temurin), `.nvmrc` (Node 24), `.gitattributes`, `backend/gradle.properties` (build parallèle et cache), recommandations d'extensions VS Code, Dependabot.

### Changed

- Bankroll : la « mise fixe » devient une mise par défaut en euros ou en pourcentage du solde ; elle préremplit la mise du formulaire de pari. API : `fixedStake` (nombre) remplacé par `defaultStake` (`{ "unit": "EUR" | "PERCENT", "value": … }` ou `null`) ; migration SQLite `V2`.
- Ajout et modification d'un pari dans un grand panneau posé sur le bas de l'écran et centré (variante `sheet` de `Dialog`) : le formulaire d'ajout passe sur deux colonnes (marchés | ticket) quand la place le permet ; il remplace le panneau latéral de la page Paris et s'ouvre via `?ajout=1`.
- Formulaire d'ajout d'un pari : aucun événement n'est présélectionné, il s'ouvre sur la liste des événements.
- Backend : chaque requête ne lit plus que les paris dont elle a besoin (critères `BetCriteria` traduits en SQL, période, pagination) au lieu de tout l'historique ; soldes calculés à partir de totaux agrégés. `GET /api/bets` accepte `limit` et `offset` (facultatifs, sans eux tout est renvoyé comme avant).
- Backend restructuré en modules `business` (domaine, ports et logique métier, sans dépendance), `inbound/rest`, `outbound/persistence/sqlite`, `outbound/odds` et `application` (démarrage et câblage) ; `domain`, `adapters/*`, `app` et `buildSrc` supprimés, configuration commune dans `backend/build.gradle.kts`. Contrat JSON de l'API inchangé.
- Objets du domaine `BetChange` et `NewRule` : la modification d'un pari et l'ajout d'une règle passent eux aussi par des objets du domaine validés.
- Tests unitaires dans chaque module avec un seuil de couverture de 80 % (Kover, vérifié par `check` et la CI) ; tests composant de bout en bout dans `application` (`componentTest`).
- Backend réorganisé en architecture hexagonale : modules Gradle `domain`, `application` (ports et cas d'usage), `adapters/http`, `adapters/persistence`, `adapters/odds` et `app` ; contrat JSON de l'API inchangé.
- Licence passée de GPL-3.0 à AGPL-3.0.
