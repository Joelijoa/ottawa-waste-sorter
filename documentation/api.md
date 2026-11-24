# API REST — v1

Base : `https://<service>.run.app/api/v1`. Réponses en JSON. Les modèles sont définis dans
`rules/src/main/kotlin/com/ottawa/wastesorter/rules/api/ApiModels.kt`.

## `GET /health`
```json
{ "status": "ok", "rulesVersion": "ottawa-2026.1", "aiConfigured": true }
```

## `GET /rules`
Jeu de règles complet (`RulesDataset`) : municipalité, version, et pour chaque règle les matériaux,
états, destination, exemples, consignes, restrictions, URL source, date d'effet, date de dernière vérification
et indicateur `verified`.

## `POST /identify` (multipart/form-data)
| Champ | Obligatoire | Description |
|---|---|---|
| `image` | oui | JPEG, PNG ou WebP, 4 Mo maximum (`MAX_UPLOAD_BYTES`) |
| `language` | non | `en` (défaut) ou `fr` |
| `material` | non | Valeur de `Material` choisie par l'utilisateur ; remplace le matériau du composant principal |
| `condition` | non | Valeur de `Condition` ; appliquée à tous les composants |

Réponse `200` :
```json
{
  "identification": {
    "objectName": "Boîte de biscuits", "objectType": "FOOD_PACKAGING", "primaryMaterial": "PAPERBOARD",
    "components": [
      { "name": "Boîte", "material": "PAPERBOARD", "condition": "CLEAN_EMPTY", "separable": true },
      { "name": "Sachet", "material": "FLEXIBLE_PLASTIC", "condition": "CLEAN_EMPTY", "separable": true }
    ],
    "visibleContamination": false, "confidence": 0.93, "clarificationNeeded": null
  },
  "classification": {
    "itemName": "Boîte de biscuits", "municipality": "Ottawa, ON", "rulesVersion": "ottawa-2026.1",
    "components": [
      { "componentName": "Boîte", "destination": "BLACK_BIN", "ruleId": "black.boxboard", "instructions": [ { "en": "…", "fr": "…" } ], "notes": [], "requiresVerification": false, "sourceUrl": "https://www.circularmaterials.ca/ottawa/", "material": "PAPERBOARD", "condition": "CLEAN_EMPTY" },
      { "componentName": "Sachet", "destination": "BLUE_BIN", "ruleId": "blue.flexible-plastic", "…": "…" }
    ]
  },
  "needsUserInput": false,
  "message": null
}
```

## Erreurs
Corps : `{ "code": "...", "message": "..." }`

| HTTP | Code | Cause |
|---|---|---|
| 400 | `IMAGE_MISSING`, `INVALID_UPLOAD` | Requête incomplète ou illisible |
| 413 | `IMAGE_TOO_LARGE` | Image trop lourde |
| 415 | `UNSUPPORTED_IMAGE` | Type non accepté ou contenu ne correspondant pas au type annoncé |
| 429 | — | Plus de `RATE_LIMIT_PER_MINUTE` requêtes par minute et par adresse IP |
| 502 | `AI_INVALID_RESPONSE` | Réponse de Gemini hors schéma |
| 503 | `AI_NOT_CONFIGURED`, `AI_UNAVAILABLE` | Clé absente, ou Gemini injoignable / quota dépassé |
| 500 | `INTERNAL_ERROR` | Erreur inattendue |

Les journaux contiennent uniquement la méthode, le chemin et le statut : jamais l'image ni la clé.
