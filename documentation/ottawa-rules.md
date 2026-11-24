# Règles de tri d'Ottawa — version `ottawa-2026.1`

Fichier : `rules/src/main/kotlin/com/ottawa/wastesorter/rules/OttawaRules2026.kt`
Dernière vérification : 24 novembre 2026.

## Responsabilités en 2026
- **Bac bleu (contenants)** et **bac noir (papier et fibres)** : programme provincial géré par Circular Materials.
- **Bac vert (organiques)** et **ordures** : Ville d'Ottawa.

## Sources
- Circular Materials, Ottawa : https://www.circularmaterials.ca/ottawa/
- Guide de recyclage 2026 : https://www.circularmaterials.ca/wp-content/uploads/2025/12/Ottawa-ON-2026-Recycle-guide-and-calendar-template.pdf
- Ville d'Ottawa, bac vert : https://ottawa.ca/en/garbage-and-recycling/green-bin-and-leaf-and-yard-waste
- Ville d'Ottawa, ordures et recyclage : https://ottawa.ca/en/garbage-and-recycling
- Calendrier de collecte : https://ottawa.ca/CollectionCalendar

## Points à connaître (corrections par rapport au cahier des charges initial)
- Les **tubes spiralés de type Pringles** et les **gobelets en papier** sont des contenants en papier laminé :
  **bac bleu** (videz-les, retirez le couvercle). Ils ne vont pas au bac noir.
- Les **livres** ne sont plus acceptés au recyclage en bordure de rue.
- Les **boîtes de pizza souillées** vont au **bac vert**, les propres au **bac noir**.
- Le carton doit mesurer au plus 60 × 60 cm.

## Règles marquées « À vérifier » (`verified = false`)
| Règle | Pourquoi |
|---|---|
| `black.paper-laminate-packaging` | Article accepté, mais son affectation au bac noir est déduite de la présentation de la page |
| `blue.plastic-shopping-bag` | Les plastiques souples sont acceptés, mais les sacs d'épicerie ne sont pas nommés |
| `garbage.flexible-plastic.soiled` | Les sources exigent un plastique souple propre ; l'envoi aux ordures sinon est une déduction |
| `garbage.ceramic` | La céramique est refusée au recyclage ; la destination « ordures » est déduite |
| `special.books`, `special.textile` | Refusés au recyclage ; la destination (don) n'est pas une consigne officielle |

Point de divergence : la Ville accepte aussi les gobelets à café en papier dans le bac vert, alors que
Circular Materials les accepte au bac bleu. L'application conseille le bac bleu s'ils sont propres et le bac
vert s'ils sont souillés.

Sans règle correspondante, le moteur renvoie `NEEDS_VERIFICATION` : **il n'invente jamais de consigne.**
Pour confirmer un cas : Miller Waste, 1-888-852-2374, area2@millerwaste.ca.

## Mettre à jour les règles
1. Vérifier la consigne sur une source officielle et noter son URL.
2. Modifier ou ajouter la règle (identifiant unique, consignes en anglais et en français, `sourceUrl`).
3. Incrémenter `VERSION` et mettre à jour `VERIFIED_ON`.
4. Ajouter ou ajuster un test dans `ClassificationEngineTest`, puis lancer `./gradlew :rules:test`.
5. Redéployer le backend **et** publier une nouvelle version de l'application, qui embarque les mêmes règles.
