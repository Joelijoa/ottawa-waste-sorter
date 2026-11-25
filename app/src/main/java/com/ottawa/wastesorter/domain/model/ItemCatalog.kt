package com.ottawa.wastesorter.domain.model

import com.ottawa.wastesorter.rules.ClassificationEngine
import com.ottawa.wastesorter.rules.Condition
import com.ottawa.wastesorter.rules.ItemComponent
import com.ottawa.wastesorter.rules.LocalizedText
import com.ottawa.wastesorter.rules.Material
import com.ottawa.wastesorter.rules.ObjectType

/** Un objet courant proposé dans la recherche manuelle. La destination vient toujours du moteur de règles. */
data class CatalogItem(
    val id: String,
    val name: LocalizedText,
    val objectType: ObjectType,
    val parts: List<CatalogPart>,
    /** Mots supplémentaires pour la recherche (anglais et français, sans accents). */
    val aliases: String = "",
) {
    val isMulti: Boolean get() = parts.size > 1
    val primaryMaterial: Material get() = parts.first().material

    fun components(language: String, condition: Condition? = null): List<ItemComponent> = parts.map {
        ItemComponent(it.name.get(language), it.material, condition ?: it.condition)
    }
}

data class CatalogPart(val name: LocalizedText, val material: Material, val condition: Condition = Condition.CLEAN_EMPTY)

object ItemCatalog {

    private fun t(en: String, fr: String) = LocalizedText(en, fr)
    private fun single(id: String, en: String, fr: String, type: ObjectType, material: Material, aliases: String = "", condition: Condition = Condition.CLEAN_EMPTY) =
        CatalogItem(id, t(en, fr), type, listOf(CatalogPart(t(en, fr), material, condition)), aliases)

    val items: List<CatalogItem> = listOf(
        single("biscuit", "Biscuit box", "Boîte de biscuits", ObjectType.FOOD_PACKAGING, Material.PAPERBOARD, "cookie cracker boxboard carton biscuit"),
        single("yogurt", "Yogurt cup", "Pot de yogourt", ObjectType.FOOD_PACKAGING, Material.RIGID_PLASTIC, "tub yoghurt yaourt pot"),
        single("bottle", "Plastic bottle", "Bouteille en plastique", ObjectType.BEVERAGE_PACKAGING, Material.RIGID_PLASTIC, "water drink eau"),
        single("pizza", "Pizza box", "Boîte de pizza", ObjectType.FOOD_PACKAGING, Material.CORRUGATED_CARDBOARD, "carton"),
        single("grounds", "Coffee grounds", "Marc de café", ObjectType.FOOD_SCRAPS, Material.FOOD_WASTE, "filter filtre cafe coffee", Condition.WET),
        single("delivery", "Cardboard delivery box", "Boîte de livraison en carton", ObjectType.DELIVERY_PACKAGING, Material.CORRUGATED_CARDBOARD, "amazon shipping parcel colis carton box boite"),
        CatalogItem(
            "pringles", t("Pringles tube", "Tube de Pringles"), ObjectType.FOOD_PACKAGING,
            listOf(
                CatalogPart(t("Tube with metal bottom", "Tube avec fond en métal"), Material.PAPER_LAMINATE_CONTAINER),
                CatalogPart(t("Plastic lid", "Couvercle en plastique"), Material.RIGID_PLASTIC),
                CatalogPart(t("Leftover chips and crumbs", "Restes de croustilles et miettes"), Material.FOOD_WASTE, Condition.FOOD_RESIDUE),
            ),
            "chips can croustilles spiral tube",
        ),
        single("wrapper", "Plastic food wrapper", "Emballage alimentaire en plastique", ObjectType.FOOD_PACKAGING, Material.FLEXIBLE_PLASTIC, "bag snack chip sachet emballage biscuit"),
        single("banana", "Banana peel", "Pelure de banane", ObjectType.FOOD_SCRAPS, Material.FOOD_WASTE, "fruit peel epluchure", Condition.FOOD_RESIDUE),
        single("cup", "Coffee cup", "Gobelet à café", ObjectType.BEVERAGE_PACKAGING, Material.PAPER_LAMINATE_CONTAINER, "tim hortons cafe tasse gobelet paper cup"),
        single("cereal", "Cereal box", "Boîte de céréales", ObjectType.FOOD_PACKAGING, Material.PAPERBOARD, "cereales"),
        single("eggs", "Egg carton", "Boîte d'œufs", ObjectType.FOOD_PACKAGING, Material.PAPERBOARD, "oeufs eggs"),
        single("paperbag", "Paper bag", "Sac en papier", ObjectType.HOUSEHOLD_PACKAGING, Material.PAPER, "sac"),
        single("newspaper", "Newspaper", "Journal", ObjectType.PRINTED_PAPER, Material.PAPER, "flyer circulaire magazine"),
        single("jar", "Glass jar", "Pot en verre", ObjectType.FOOD_PACKAGING, Material.GLASS, "verre bocal"),
        single("can", "Aluminum can", "Canette en aluminium", ObjectType.BEVERAGE_PACKAGING, Material.METAL, "soda pop conserve tin"),
        single("foil", "Aluminum foil", "Papier d'aluminium", ObjectType.HOUSEHOLD_PACKAGING, Material.ALUMINUM_FOIL, "papier alu"),
        single("milk", "Milk carton", "Carton de lait", ObjectType.BEVERAGE_PACKAGING, Material.CARTON, "juice jus lait"),
        single("foam", "Foam meat tray", "Barquette de viande en mousse", ObjectType.FOOD_PACKAGING, Material.FOAM, "styrofoam polystyrene barquette"),
        single("grocerybag", "Plastic grocery bag", "Sac d'épicerie en plastique", ObjectType.HOUSEHOLD_PACKAGING, Material.FLEXIBLE_PLASTIC, "shopping sac plastique epicerie"),
        single("battery", "Battery", "Pile", ObjectType.OTHER, Material.HAZARDOUS, "batteries piles batterie"),
    )

    /** Les 7 « objets populaires » de l'écran de recherche. */
    val popular: List<CatalogItem> = listOf("biscuit", "yogurt", "bottle", "pizza", "grounds", "delivery", "pringles").map { byId(it)!! }

    fun byId(id: String): CatalogItem? = items.firstOrNull { it.id == id }

    fun search(query: String): List<CatalogItem> {
        val q = ClassificationEngine.normalize(query)
        if (q.isEmpty()) return emptyList()
        return items.filter { item ->
            ClassificationEngine.normalize("${item.name.en} ${item.name.fr} ${item.aliases}").contains(q)
        }
    }
}
