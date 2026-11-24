package com.ottawa.wastesorter.rules

import java.text.Normalizer

/**
 * Moteur de classification déterministe.
 *
 * Il ne dépend d'aucune IA : il reçoit des composants (matériau + état), issus de Gemini ou
 * d'une sélection manuelle, et applique les règles versionnées. Même entrée ⇒ même résultat.
 *
 * Priorité des règles pour un composant :
 *  1. règle dont un mot-clé correspond au nom du composant ou de l'objet ;
 *  2. règle sans mot-clé, spécifique à l'état (propre / souillé) ;
 *  3. règle sans mot-clé valable pour tous les états.
 * Aucune règle ⇒ [Destination.NEEDS_VERIFICATION] : le moteur n'invente jamais de consigne.
 */
class ClassificationEngine(private val dataset: RulesDataset = OttawaRules2026.dataset) {

    fun classify(request: ClassificationRequest): ClassificationResult {
        val components = request.components.ifEmpty {
            listOf(ItemComponent(request.itemName, Material.UNKNOWN, Condition.UNKNOWN))
        }
        val results = components.map { classifyComponent(it, request.itemName) }

        val mixedNonSeparable = components.any { !it.separable } &&
            results.map { it.destination }.distinct().size > 1

        val finalResults = if (mixedNonSeparable) {
            listOf(nonSeparableResult(request.itemName, components))
        } else {
            results
        }

        return ClassificationResult(
            itemName = request.itemName,
            municipality = dataset.municipality,
            rulesVersion = dataset.version,
            components = finalResults,
        )
    }

    private fun classifyComponent(component: ItemComponent, itemName: String): ComponentResult {
        val text = normalize("${component.name} $itemName")

        if (component.condition == Condition.UNKNOWN) {
            // État inconnu : on applique la consigne « propre et vide » et on signale l'alternative
            // si le même objet souillé change de destination.
            val clean = findRule(component.material, Condition.CLEAN_EMPTY, text)
            val soiled = findRule(component.material, Condition.FOOD_RESIDUE, text)
            val base = toResult(component, clean)
            return if (soiled != null && soiled.destination != base.destination) {
                base.copy(
                    notes = base.notes + LocalizedText(
                        en = "If it is soiled with food: ${DestinationLabels.of(soiled.destination).en}.",
                        fr = "S'il est souillé par des aliments : ${DestinationLabels.of(soiled.destination).fr}.",
                    ),
                )
            } else {
                base
            }
        }
        return toResult(component, findRule(component.material, component.condition, text))
    }

    private fun findRule(material: Material, condition: Condition, normalizedText: String): SortingRule? {
        val candidates = dataset.rules.filter { rule ->
            material in rule.materials && (rule.conditions.isEmpty() || condition in rule.conditions)
        }
        return candidates.firstOrNull { rule -> rule.keywords.any { matchesKeyword(normalizedText, it) } }
            ?: candidates.firstOrNull { it.keywords.isEmpty() && it.conditions.isNotEmpty() }
            ?: candidates.firstOrNull { it.keywords.isEmpty() }
    }

    private fun toResult(component: ItemComponent, rule: SortingRule?): ComponentResult =
        if (rule == null) {
            ComponentResult(
                componentName = component.name,
                material = component.material,
                condition = component.condition,
                destination = Destination.NEEDS_VERIFICATION,
                ruleId = null,
                instructions = listOf(NO_RULE_INSTRUCTION),
                notes = emptyList(),
                requiresVerification = true,
                sourceUrl = OttawaRules2026.SRC_CITY,
            )
        } else {
            ComponentResult(
                componentName = component.name,
                material = component.material,
                condition = component.condition,
                destination = rule.destination,
                ruleId = rule.id,
                instructions = rule.instructions,
                notes = rule.restrictions,
                requiresVerification = !rule.verified,
                sourceUrl = rule.sourceUrl,
            )
        }

    private fun nonSeparableResult(itemName: String, components: List<ItemComponent>) = ComponentResult(
        componentName = itemName,
        material = Material.MIXED,
        condition = components.first().condition,
        destination = Destination.NEEDS_VERIFICATION,
        ruleId = null,
        instructions = listOf(
            LocalizedText(
                en = "This item combines materials that cannot be separated. Check the City of Ottawa Waste Explorer.",
                fr = "Cet objet combine des matériaux impossibles à séparer. Consultez l'outil Waste Explorer de la Ville d'Ottawa.",
            ),
        ),
        notes = emptyList(),
        requiresVerification = true,
        sourceUrl = OttawaRules2026.SRC_CITY,
    )

    companion object {
        val NO_RULE_INSTRUCTION = LocalizedText(
            en = "No verified Ottawa rule matches this item. Check the City of Ottawa Waste Explorer before disposing of it.",
            fr = "Aucune règle d'Ottawa vérifiée ne correspond à cet objet. Consultez l'outil Waste Explorer de la Ville avant de le jeter.",
        )

        /** Minuscules, sans accents, ponctuation remplacée par des espaces. */
        fun normalize(input: String): String =
            Normalizer.normalize(input, Normalizer.Form.NFD)
                .replace(Regex("\\p{M}+"), "")
                .lowercase()
                .replace(Regex("[^a-z0-9]+"), " ")
                .trim()

        /** Le mot-clé doit commencer un mot (« livre » correspond à « livres », pas à « delivre »). */
        internal fun matchesKeyword(normalizedText: String, keyword: String): Boolean {
            val k = normalize(keyword)
            if (k.isEmpty()) return false
            return Regex("(^| )" + Regex.escape(k)).containsMatchIn(normalizedText)
        }
    }
}

/** Libellés officiels des destinations, réutilisés par l'application et le backend. */
object DestinationLabels {
    fun of(destination: Destination): LocalizedText = when (destination) {
        Destination.BLUE_BIN -> LocalizedText("Blue recycling bin — containers", "Bac bleu — contenants")
        Destination.BLACK_BIN -> LocalizedText("Black recycling bin — paper and fibre", "Bac noir — papier et fibres")
        Destination.GREEN_BIN -> LocalizedText("Green bin — organics", "Bac vert — matières organiques")
        Destination.GARBAGE -> LocalizedText("Garbage (black garbage bag)", "Ordures ménagères (sac noir)")
        Destination.SPECIAL_DROP_OFF -> LocalizedText("Special drop-off", "Dépôt spécial")
        Destination.NEEDS_VERIFICATION -> LocalizedText("To be verified", "À vérifier")
    }
}
