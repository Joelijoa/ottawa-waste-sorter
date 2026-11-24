package com.ottawa.wastesorter.rules

import kotlinx.serialization.Serializable

/** Destination finale d'un objet ou d'un composant. */
@Serializable
enum class Destination {
    /** Bac bleu : contenants (verre, métal, plastique, mousse, contenants en papier laminé). */
    BLUE_BIN,
    /** Bac noir : papier et fibres (carton propre, papier). À ne pas confondre avec le sac d'ordures. */
    BLACK_BIN,
    /** Bac vert : matières organiques et papier souillé. */
    GREEN_BIN,
    /** Ordures ménagères (sac noir / poubelle). */
    GARBAGE,
    /** Dépôt spécial (déchets dangereux, piles, dons…). */
    SPECIAL_DROP_OFF,
    /** Aucune règle vérifiée : l'application ne devine pas. */
    NEEDS_VERIFICATION,
}

/** Matériaux reconnus. Les mêmes valeurs servent à l'IA, au tri manuel et au moteur de règles. */
@Serializable
enum class Material {
    PAPER,
    PAPERBOARD,
    CORRUGATED_CARDBOARD,
    /** Contenants papier laminé : tubes spiralés (Pringles), gobelets en papier. */
    PAPER_LAMINATE_CONTAINER,
    /** Emballages papier laminé non rigides : sacs de nourriture pour animaux, sacs de restauration. */
    PAPER_LAMINATE_PACKAGING,
    /** Cartons de lait, de jus, de soupe. */
    CARTON,
    RIGID_PLASTIC,
    FLEXIBLE_PLASTIC,
    FOAM,
    METAL,
    ALUMINUM_FOIL,
    GLASS,
    FOOD_WASTE,
    CERAMIC,
    TEXTILE,
    HAZARDOUS,
    MIXED,
    OTHER,
    UNKNOWN,
}

@Serializable
enum class ObjectType {
    FOOD_PACKAGING,
    BEVERAGE_PACKAGING,
    DELIVERY_PACKAGING,
    HOUSEHOLD_PACKAGING,
    PERSONAL_CARE_PACKAGING,
    PRINTED_PAPER,
    FOOD_SCRAPS,
    OTHER,
}

@Serializable
enum class Condition {
    CLEAN_EMPTY,
    DIRTY,
    FOOD_RESIDUE,
    WET,
    UNKNOWN;

    val isSoiled: Boolean get() = this == DIRTY || this == FOOD_RESIDUE || this == WET
}

/** Texte bilingue. Les consignes de tri sont toujours fournies en anglais et en français. */
@Serializable
data class LocalizedText(val en: String, val fr: String) {
    fun get(language: String): String = if (language.lowercase().startsWith("fr")) fr else en
}

/** Un composant séparable d'un objet (ex. tube en carton + couvercle en plastique). */
@Serializable
data class ItemComponent(
    val name: String,
    val material: Material,
    val condition: Condition = Condition.UNKNOWN,
    val separable: Boolean = true,
)

@Serializable
data class ClassificationRequest(
    val itemName: String,
    val objectType: ObjectType = ObjectType.OTHER,
    val components: List<ItemComponent>,
)

/** Une règle de tri officielle, versionnée et sourcée. */
@Serializable
data class SortingRule(
    val id: String,
    val materials: Set<Material>,
    /** Vide = s'applique quel que soit l'état. */
    val conditions: Set<Condition> = emptySet(),
    /** Mots-clés (sans accents, minuscules) qui rendent la règle plus spécifique. */
    val keywords: Set<String> = emptySet(),
    val destination: Destination,
    val examples: LocalizedText,
    val instructions: List<LocalizedText>,
    val restrictions: List<LocalizedText> = emptyList(),
    val sourceUrl: String,
    val effectiveDate: String,
    val lastVerified: String,
    /** false = consigne déduite ou ambiguë dans les sources : affichée comme « à vérifier ». */
    val verified: Boolean = true,
)

@Serializable
data class RulesDataset(
    val municipality: String,
    val version: String,
    val effectiveDate: String,
    val rules: List<SortingRule>,
)

@Serializable
data class ComponentResult(
    val componentName: String,
    val material: Material,
    val condition: Condition,
    val destination: Destination,
    val ruleId: String?,
    val instructions: List<LocalizedText>,
    val notes: List<LocalizedText>,
    val requiresVerification: Boolean,
    val sourceUrl: String?,
)

@Serializable
data class ClassificationResult(
    val itemName: String,
    val municipality: String,
    val rulesVersion: String,
    val components: List<ComponentResult>,
) {
    val requiresVerification: Boolean get() = components.any { it.requiresVerification }

    /** Destinations distinctes, dans l'ordre d'apparition. */
    val destinations: List<Destination> get() = components.map { it.destination }.distinct()
}
