package com.ottawa.wastesorter.rules

import com.ottawa.wastesorter.rules.Condition.CLEAN_EMPTY
import com.ottawa.wastesorter.rules.Condition.DIRTY
import com.ottawa.wastesorter.rules.Condition.FOOD_RESIDUE
import com.ottawa.wastesorter.rules.Condition.WET
import com.ottawa.wastesorter.rules.Material.*

/**
 * Règles officielles de tri pour la collecte résidentielle en bordure de rue à Ottawa — programme 2026.
 *
 * Répartition des responsabilités en 2026 :
 *  - Bac bleu (contenants) et bac noir (papier et fibres) : programme provincial géré par Circular Materials.
 *  - Bac vert (organiques) et ordures : Ville d'Ottawa.
 *
 * Toute règle marquée `verified = false` est déduite ou ambiguë dans les sources :
 * l'application l'affiche avec la mention « À vérifier ». Ne jamais ajouter de règle sans source.
 *
 * Mettre à jour [VERSION] et `lastVerified` à chaque révision (voir documentation/ottawa-rules.md).
 */
object OttawaRules2026 {

    const val MUNICIPALITY = "Ottawa, ON"
    const val VERSION = "ottawa-2026.1"
    private const val EFFECTIVE = "2026-01-01"
    private const val VERIFIED_ON = "2026-11-24"

    const val SRC_CIRCULAR = "https://www.circularmaterials.ca/ottawa/"
    const val SRC_CIRCULAR_GUIDE =
        "https://www.circularmaterials.ca/wp-content/uploads/2025/12/Ottawa-ON-2026-Recycle-guide-and-calendar-template.pdf"
    const val SRC_CITY_GREEN = "https://ottawa.ca/en/garbage-and-recycling/green-bin-and-leaf-and-yard-waste"
    const val SRC_CITY = "https://ottawa.ca/en/garbage-and-recycling"

    private val SOILED = setOf(DIRTY, FOOD_RESIDUE, WET)
    private val CLEAN = setOf(CLEAN_EMPTY)

    private fun t(en: String, fr: String) = LocalizedText(en, fr)

    private fun rule(
        id: String,
        materials: Set<Material>,
        destination: Destination,
        examples: LocalizedText,
        instructions: List<LocalizedText>,
        source: String,
        conditions: Set<Condition> = emptySet(),
        keywords: Set<String> = emptySet(),
        restrictions: List<LocalizedText> = emptyList(),
        verified: Boolean = true,
    ) = SortingRule(
        id = id,
        materials = materials,
        conditions = conditions,
        keywords = keywords,
        destination = destination,
        examples = examples,
        instructions = instructions,
        restrictions = restrictions,
        sourceUrl = source,
        effectiveDate = EFFECTIVE,
        lastVerified = VERIFIED_ON,
        verified = verified,
    )

    private val EMPTY_IT = t("Empty it completely.", "Videz-le complètement.")
    private val FLATTEN = t("Flatten it.", "Aplatissez-le.")

    val rules: List<SortingRule> = listOf(
        // ───────────── Bac noir : papier et fibres (Circular Materials) ─────────────
        rule(
            id = "black.pizza-box.clean",
            materials = setOf(PAPERBOARD, CORRUGATED_CARDBOARD),
            conditions = CLEAN,
            keywords = setOf("pizza"),
            destination = Destination.BLACK_BIN,
            examples = t("Clean pizza boxes", "Boîtes de pizza propres"),
            instructions = listOf(
                t("Remove any leftover food.", "Retirez tout reste de nourriture."),
                FLATTEN,
                t("Keep it no larger than 60 × 60 cm.", "Maximum 60 × 60 cm."),
            ),
            source = SRC_CITY_GREEN,
        ),
        rule(
            id = "black.paper",
            materials = setOf(PAPER),
            conditions = CLEAN,
            destination = Destination.BLACK_BIN,
            examples = t(
                "Newspapers, flyers, paper of any colour, paper bags, envelopes",
                "Journaux, circulaires, papier de toutes couleurs, sacs en papier, enveloppes",
            ),
            instructions = listOf(
                t("Keep it clean and dry.", "Gardez-le propre et sec."),
                t(
                    "Put shredded paper in a tied clear plastic bag.",
                    "Mettez le papier déchiqueté dans un sac de plastique transparent fermé.",
                ),
                t("Remove elastic bands.", "Retirez les élastiques."),
            ),
            source = SRC_CIRCULAR,
        ),
        rule(
            id = "black.boxboard",
            materials = setOf(PAPERBOARD),
            conditions = CLEAN,
            destination = Destination.BLACK_BIN,
            examples = t(
                "Biscuit, cereal and cracker boxes, paper egg cartons, tissue boxes",
                "Boîtes de biscuits, de céréales et de craquelins, boîtes d'œufs en carton, boîtes de mouchoirs",
            ),
            instructions = listOf(
                EMPTY_IT,
                t(
                    "Remove the plastic inner bag or liner (it goes in the blue bin).",
                    "Retirez le sachet ou la doublure en plastique (il va dans le bac bleu).",
                ),
                FLATTEN,
            ),
            source = SRC_CIRCULAR,
        ),
        rule(
            id = "black.corrugated",
            materials = setOf(CORRUGATED_CARDBOARD),
            conditions = CLEAN,
            destination = Destination.BLACK_BIN,
            examples = t(
                "Shipping and delivery boxes, cardboard delivery trays",
                "Boîtes d'expédition et de livraison, plateaux de livraison en carton",
            ),
            instructions = listOf(
                t("Remove packing material and plastic.", "Retirez les matériaux d'emballage et le plastique."),
                FLATTEN,
                t(
                    "Cut or fold it to no larger than 60 × 60 cm.",
                    "Coupez-le ou pliez-le à 60 × 60 cm maximum.",
                ),
            ),
            restrictions = listOf(
                t(
                    "Oversized cardboard (larger than 60 × 60 cm) may be left behind with an \"oops\" sticker.",
                    "Le carton trop grand (plus de 60 × 60 cm) peut être refusé avec un autocollant « oups ».",
                ),
            ),
            source = SRC_CIRCULAR,
        ),
        rule(
            id = "black.paper-laminate-packaging",
            materials = setOf(PAPER_LAMINATE_PACKAGING),
            conditions = CLEAN,
            destination = Destination.BLACK_BIN,
            examples = t(
                "Pet food bags, food-service paper bags, paper plates",
                "Sacs de nourriture pour animaux, sacs en papier de restauration, assiettes en papier",
            ),
            instructions = listOf(
                EMPTY_IT,
                t("It must be free of food residue.", "Il ne doit plus contenir de résidus alimentaires."),
            ),
            source = SRC_CIRCULAR,
            // Le site de Circular Materials accepte ces articles, mais l'affectation au bac noir est déduite
            // de la présentation de la page.
            verified = false,
        ),
        rule(
            id = "special.books",
            materials = setOf(PAPER, PAPERBOARD),
            keywords = setOf("livre", "book", "roman", "novel", "manuel"),
            destination = Destination.SPECIAL_DROP_OFF,
            examples = t("Hardcover and softcover books", "Livres à couverture rigide ou souple"),
            instructions = listOf(
                t(
                    "Books are not accepted in curbside recycling in 2026. Donate them if they are in good condition.",
                    "Les livres ne sont plus acceptés au recyclage en bordure de rue en 2026. Donnez-les s'ils sont en bon état.",
                ),
            ),
            source = SRC_CIRCULAR,
            verified = false,
        ),

        // ───────────── Bac vert : organiques (Ville d'Ottawa) ─────────────
        rule(
            id = "green.pizza-box.soiled",
            materials = setOf(PAPERBOARD, CORRUGATED_CARDBOARD),
            conditions = SOILED,
            keywords = setOf("pizza"),
            destination = Destination.GREEN_BIN,
            examples = t("Food-soiled pizza boxes", "Boîtes de pizza souillées"),
            instructions = listOf(
                t(
                    "Put the food-soiled pizza box in the green bin.",
                    "Mettez la boîte de pizza souillée dans le bac vert.",
                ),
                t(
                    "Clean parts can be torn off and put in the black bin.",
                    "Les parties propres peuvent être déchirées et mises dans le bac noir.",
                ),
            ),
            source = SRC_CITY_GREEN,
        ),
        rule(
            id = "green.paper-cup.soiled",
            materials = setOf(PAPER_LAMINATE_CONTAINER),
            conditions = SOILED,
            keywords = setOf("cup", "gobelet", "tasse", "verre en carton", "coffee", "cafe"),
            destination = Destination.GREEN_BIN,
            examples = t("Paper coffee cups", "Gobelets à café en papier"),
            instructions = listOf(
                t("Remove the plastic lid (blue bin).", "Retirez le couvercle en plastique (bac bleu)."),
            ),
            source = SRC_CITY_GREEN,
        ),
        rule(
            id = "green.soiled-paper",
            materials = setOf(PAPER, PAPERBOARD, CORRUGATED_CARDBOARD),
            conditions = SOILED,
            destination = Destination.GREEN_BIN,
            examples = t(
                "Soiled paper, boxboard and cardboard, paper towels, napkins, muffin wrappers",
                "Papier, carton mince et carton souillés, essuie-tout, serviettes, moules à muffins",
            ),
            instructions = listOf(
                t("Put it in the green bin.", "Mettez-le dans le bac vert."),
            ),
            source = SRC_CITY_GREEN,
        ),
        rule(
            id = "green.food",
            materials = setOf(FOOD_WASTE),
            destination = Destination.GREEN_BIN,
            examples = t(
                "Fruit and vegetable scraps, leftovers, meat and bones, coffee grounds, tea bags",
                "Épluchures, restes de repas, viande et os, marc de café, sachets de thé",
            ),
            instructions = listOf(
                t(
                    "Any plastic bag can be used to wrap organic waste in the green bin.",
                    "Vous pouvez emballer les déchets organiques dans n'importe quel sac de plastique.",
                ),
            ),
            source = SRC_CITY_GREEN,
        ),

        // ───────────── Bac bleu : contenants (Circular Materials) ─────────────
        rule(
            id = "blue.paper-laminate-container",
            materials = setOf(PAPER_LAMINATE_CONTAINER),
            destination = Destination.BLUE_BIN,
            examples = t(
                "Spiral-wound cans (chips such as Pringles, nuts, frozen juice), hot and cold paper cups",
                "Tubes spiralés (croustilles type Pringles, noix, jus congelé), gobelets en papier chauds et froids",
            ),
            instructions = listOf(
                EMPTY_IT,
                t("Remove the lid.", "Retirez le couvercle."),
            ),
            source = SRC_CIRCULAR,
        ),
        rule(
            id = "blue.carton",
            materials = setOf(CARTON),
            destination = Destination.BLUE_BIN,
            examples = t(
                "Milk, juice, soup and coffee cartons",
                "Cartons de lait, de jus, de soupe et de café",
            ),
            instructions = listOf(
                t("Rinse and dry it.", "Rincez-le et laissez-le sécher."),
                t("Leave lids and caps on.", "Laissez les bouchons et couvercles."),
            ),
            source = SRC_CIRCULAR,
        ),
        rule(
            id = "blue.rigid-plastic",
            materials = setOf(RIGID_PLASTIC),
            destination = Destination.BLUE_BIN,
            examples = t(
                "Plastic bottles, yogurt and food containers, clamshells, plastic egg containers, lids",
                "Bouteilles en plastique, pots de yogourt, contenants alimentaires, barquettes, boîtes d'œufs en plastique, couvercles",
            ),
            instructions = listOf(
                EMPTY_IT,
                t("Rinse off food residue.", "Rincez les résidus alimentaires."),
                t("Leave lids and caps on.", "Laissez les bouchons et couvercles."),
            ),
            source = SRC_CIRCULAR,
        ),
        rule(
            id = "blue.plastic-shopping-bag",
            materials = setOf(FLEXIBLE_PLASTIC),
            conditions = CLEAN,
            keywords = setOf("shopping", "grocery", "epicerie", "magasin", "retail", "plastique d'emplette", "sac de caisse"),
            destination = Destination.BLUE_BIN,
            examples = t("Plastic grocery and shopping bags", "Sacs d'épicerie et sacs de magasin en plastique"),
            instructions = listOf(
                EMPTY_IT,
            ),
            restrictions = listOf(
                t(
                    "Flexible plastics are accepted, but grocery bags are not listed explicitly. You can also reuse them to wrap green bin waste.",
                    "Les plastiques souples sont acceptés, mais les sacs d'épicerie ne sont pas nommés explicitement. Vous pouvez aussi les réutiliser pour emballer les déchets du bac vert.",
                ),
            ),
            source = SRC_CIRCULAR,
            verified = false,
        ),
        rule(
            id = "blue.flexible-plastic",
            materials = setOf(FLEXIBLE_PLASTIC),
            conditions = CLEAN,
            destination = Destination.BLUE_BIN,
            examples = t(
                "Chip and biscuit bags, bread bags, cereal liners, bubble wrap, newspaper bags",
                "Sacs de croustilles et de biscuits, sacs à pain, sachets de céréales, papier bulle, sacs de journaux",
            ),
            instructions = listOf(
                EMPTY_IT,
                t("It must be free of food residue.", "Il ne doit plus contenir de résidus alimentaires."),
            ),
            source = SRC_CIRCULAR,
        ),
        rule(
            id = "garbage.flexible-plastic.soiled",
            materials = setOf(FLEXIBLE_PLASTIC),
            conditions = SOILED,
            destination = Destination.GARBAGE,
            examples = t("Food-soiled flexible plastic", "Plastique souple souillé"),
            instructions = listOf(
                t(
                    "If you can empty and clean it, put it in the blue bin instead.",
                    "Si vous pouvez le vider et le nettoyer, mettez-le plutôt dans le bac bleu.",
                ),
            ),
            source = SRC_CIRCULAR,
            // Les sources exigent un plastique souple sans résidus ; l'envoi aux ordures sinon est déduit.
            verified = false,
        ),
        rule(
            id = "blue.foam",
            materials = setOf(FOAM),
            destination = Destination.BLUE_BIN,
            examples = t(
                "Foam meat trays, takeout containers, cups, plates and bowls",
                "Barquettes de viande en mousse, contenants pour emporter, verres, assiettes et bols en mousse",
            ),
            instructions = listOf(
                EMPTY_IT,
                t("Remove all food residue.", "Retirez tous les résidus alimentaires."),
                t(
                    "Remove film wrap and absorbent pads from meat trays.",
                    "Retirez la pellicule et le tampon absorbant des barquettes de viande.",
                ),
            ),
            source = SRC_CIRCULAR,
        ),
        rule(
            id = "blue.metal",
            materials = setOf(METAL),
            destination = Destination.BLUE_BIN,
            examples = t(
                "Food and drink cans, aluminum trays and containers, metal lids, empty food aerosols",
                "Boîtes de conserve et canettes, barquettes en aluminium, couvercles en métal, aérosols alimentaires vides",
            ),
            instructions = listOf(
                EMPTY_IT,
                t("Rinse it.", "Rincez-le."),
            ),
            restrictions = listOf(
                t(
                    "Aerosols with toxic, corrosive or flammable symbols are not accepted (except cooking oil sprays and hair, cosmetic and skin products).",
                    "Les aérosols portant un symbole toxique, corrosif ou inflammable sont refusés (sauf huile de cuisson et produits capillaires, cosmétiques et pour la peau).",
                ),
                t("Pots and pans are not accepted.", "Les casseroles et poêles sont refusées."),
            ),
            source = SRC_CIRCULAR,
        ),
        rule(
            id = "blue.aluminum-foil",
            materials = setOf(ALUMINUM_FOIL),
            destination = Destination.BLUE_BIN,
            examples = t("Aluminum foil", "Papier d'aluminium"),
            instructions = listOf(
                t("Rinse and dry it.", "Rincez-le et séchez-le."),
                t("Crumple it into a ball.", "Faites-en une boule."),
            ),
            source = SRC_CIRCULAR,
        ),
        rule(
            id = "blue.glass",
            materials = setOf(GLASS),
            destination = Destination.BLUE_BIN,
            examples = t("Glass bottles and jars", "Bouteilles et pots en verre"),
            instructions = listOf(
                EMPTY_IT,
                t("Rinse it.", "Rincez-le."),
            ),
            restrictions = listOf(
                t("Only glass containers are accepted. Ceramics are not.", "Seuls les contenants en verre sont acceptés, pas la céramique."),
            ),
            source = SRC_CIRCULAR_GUIDE,
        ),

        // ───────────── Ordures et dépôts spéciaux ─────────────
        rule(
            id = "garbage.diapers",
            materials = setOf(Material.OTHER, MIXED, TEXTILE, PAPER),
            keywords = setOf("diaper", "couche", "sanitary", "hygienique", "serviette sanitaire", "tampon"),
            destination = Destination.GARBAGE,
            examples = t("Diapers and sanitary products", "Couches et produits sanitaires"),
            instructions = listOf(
                t(
                    "Not accepted in recycling or in the green bin.",
                    "Refusés au recyclage et dans le bac vert.",
                ),
            ),
            source = SRC_CITY_GREEN,
        ),
        rule(
            id = "garbage.ceramic",
            materials = setOf(CERAMIC),
            destination = Destination.GARBAGE,
            examples = t("Broken dishes, mugs, ceramics", "Vaisselle cassée, tasses, céramique"),
            instructions = listOf(
                t("Wrap sharp pieces before disposal.", "Emballez les morceaux coupants."),
            ),
            source = SRC_CIRCULAR_GUIDE,
            // Le guide 2026 refuse la céramique au recyclage ; la destination « ordures » est déduite.
            verified = false,
        ),
        rule(
            id = "special.hazardous",
            materials = setOf(HAZARDOUS),
            destination = Destination.SPECIAL_DROP_OFF,
            examples = t(
                "Batteries, paint, solvents, aerosols with hazard symbols",
                "Piles, peinture, solvants, aérosols portant un symbole de danger",
            ),
            instructions = listOf(
                t(
                    "Never put it in the bins: bring it to a household hazardous waste depot or a take-back location.",
                    "Ne le mettez jamais dans les bacs : apportez-le à un dépôt de déchets dangereux ou à un point de reprise.",
                ),
            ),
            source = SRC_CITY,
        ),
        rule(
            id = "special.textile",
            materials = setOf(TEXTILE),
            destination = Destination.SPECIAL_DROP_OFF,
            examples = t("Clothes, shoes, bedding", "Vêtements, chaussures, literie"),
            instructions = listOf(
                t("Donate usable textiles.", "Donnez les textiles réutilisables."),
            ),
            source = SRC_CITY,
            verified = false,
        ),
    )

    val dataset: RulesDataset = RulesDataset(
        municipality = MUNICIPALITY,
        version = VERSION,
        effectiveDate = EFFECTIVE,
        rules = rules,
    )
}
