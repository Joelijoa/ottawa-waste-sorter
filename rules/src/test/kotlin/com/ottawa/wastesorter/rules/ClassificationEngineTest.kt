package com.ottawa.wastesorter.rules

import com.ottawa.wastesorter.rules.Condition.CLEAN_EMPTY
import com.ottawa.wastesorter.rules.Condition.FOOD_RESIDUE
import com.ottawa.wastesorter.rules.Condition.UNKNOWN
import com.ottawa.wastesorter.rules.Destination.BLACK_BIN
import com.ottawa.wastesorter.rules.Destination.BLUE_BIN
import com.ottawa.wastesorter.rules.Destination.GARBAGE
import com.ottawa.wastesorter.rules.Destination.GREEN_BIN
import com.ottawa.wastesorter.rules.Destination.NEEDS_VERIFICATION
import com.ottawa.wastesorter.rules.Material.*
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Jeu de référence du cahier des charges (15 objets), vérifié contre les sources officielles
 * citées dans [OttawaRules2026].
 */
class ClassificationEngineTest {

    private val engine = ClassificationEngine()

    private fun classify(item: String, vararg components: ItemComponent) =
        engine.classify(ClassificationRequest(itemName = item, components = components.toList()))

    private fun c(name: String, material: Material, condition: Condition = CLEAN_EMPTY, separable: Boolean = true) =
        ItemComponent(name, material, condition, separable)

    private fun ClassificationResult.dest(i: Int = 0) = components[i].destination

    // ── 15 objets de référence ──

    @Test fun `01 biscuit cardboard box goes to black bin, inner wrapper to blue bin`() {
        val r = classify("Biscuit box", c("Cardboard box", PAPERBOARD), c("Plastic inner wrapper", FLEXIBLE_PLASTIC))
        assertEquals(BLACK_BIN, r.dest(0))
        assertEquals(BLUE_BIN, r.dest(1))
        assertFalse(r.requiresVerification)
    }

    @Test fun `02 cereal box`() {
        val r = classify("Cereal box", c("Box", PAPERBOARD), c("Liner bag", FLEXIBLE_PLASTIC))
        assertEquals(listOf(BLACK_BIN, BLUE_BIN), r.components.map { it.destination })
    }

    @Test fun `03 pringles tube is a container - blue bin`() {
        val r = classify("Pringles tube", c("Spiral-wound tube with metal bottom", PAPER_LAMINATE_CONTAINER), c("Lid", RIGID_PLASTIC))
        assertEquals(BLUE_BIN, r.dest(0))
        assertEquals(BLUE_BIN, r.dest(1))
        assertEquals("blue.paper-laminate-container", r.components[0].ruleId)
    }

    @Test fun `04 yogurt plastic container`() =
        assertEquals(BLUE_BIN, classify("Yogurt container", c("Tub", RIGID_PLASTIC, FOOD_RESIDUE)).dest())

    @Test fun `05 flexible biscuit wrapper`() =
        assertEquals(BLUE_BIN, classify("Biscuit wrapper", c("Wrapper", FLEXIBLE_PLASTIC)).dest())

    @Test fun `06 plastic shopping bag is blue bin but flagged for verification`() {
        val r = classify("Plastic grocery bag", c("Bag", FLEXIBLE_PLASTIC))
        assertEquals(BLUE_BIN, r.dest())
        assertEquals("blue.plastic-shopping-bag", r.components[0].ruleId)
        assertTrue(r.requiresVerification)
    }

    @Test fun `07 cardboard delivery tray`() =
        assertEquals(BLACK_BIN, classify("Delivery tray", c("Tray", CORRUGATED_CARDBOARD)).dest())

    @Test fun `08 pizza box with food residue goes to green bin`() {
        val r = classify("Pizza box", c("Box", CORRUGATED_CARDBOARD, FOOD_RESIDUE))
        assertEquals(GREEN_BIN, r.dest())
        assertEquals("green.pizza-box.soiled", r.components[0].ruleId)
    }

    @Test fun `08b clean pizza box goes to black bin`() =
        assertEquals(BLACK_BIN, classify("Boîte de pizza", c("Boîte", CORRUGATED_CARDBOARD)).dest())

    @Test fun `09 glass jar`() = assertEquals(BLUE_BIN, classify("Glass jar", c("Jar", GLASS)).dest())

    @Test fun `10 aluminum can`() = assertEquals(BLUE_BIN, classify("Aluminum can", c("Can", METAL)).dest())

    @Test fun `11 plastic bottle`() = assertEquals(BLUE_BIN, classify("Plastic bottle", c("Bottle", RIGID_PLASTIC)).dest())

    @Test fun `12 paper egg carton goes to black bin`() =
        assertEquals(BLACK_BIN, classify("Egg carton", c("Carton", PAPERBOARD)).dest())

    @Test fun `13 paper bag`() = assertEquals(BLACK_BIN, classify("Paper bag", c("Bag", PAPER)).dest())

    @Test fun `14 foam food tray`() {
        val r = classify("Foam meat tray", c("Tray", FOAM, FOOD_RESIDUE))
        assertEquals(BLUE_BIN, r.dest())
        assertTrue(r.components[0].instructions.any { it.en.contains("absorbent") })
    }

    @Test fun `15 mixed separable cardboard and plastic packaging gets one instruction per component`() {
        val r = classify("Toy packaging", c("Cardboard backing", PAPERBOARD), c("Plastic blister", RIGID_PLASTIC))
        assertEquals(listOf(BLACK_BIN, BLUE_BIN), r.components.map { it.destination })
    }

    // ── Cas limites ──

    @Test fun `non separable mixed materials require verification`() {
        val r = classify(
            "Laminated pouch",
            c("Paper layer", PAPER, separable = false),
            c("Plastic layer", FLEXIBLE_PLASTIC, separable = false),
        )
        assertEquals(1, r.components.size)
        assertEquals(NEEDS_VERIFICATION, r.dest())
    }

    @Test fun `unknown material is never guessed`() {
        val r = classify("Strange object", c("Object", UNKNOWN_MATERIAL))
        assertEquals(NEEDS_VERIFICATION, r.dest())
        assertTrue(r.requiresVerification)
    }

    @Test fun `empty component list is treated as unknown`() {
        val r = engine.classify(ClassificationRequest("Nothing", components = emptyList()))
        assertEquals(NEEDS_VERIFICATION, r.dest())
    }

    @Test fun `unknown condition uses clean rule and warns about soiled alternative`() {
        val r = classify("Cardboard box", c("Box", PAPERBOARD, UNKNOWN))
        assertEquals(BLACK_BIN, r.dest())
        assertTrue(r.components[0].notes.any { it.fr.contains("Bac vert") })
    }

    @Test fun `soiled flexible plastic goes to garbage and is flagged`() {
        val r = classify("Chip bag", c("Bag", FLEXIBLE_PLASTIC, FOOD_RESIDUE))
        assertEquals(GARBAGE, r.dest())
        assertTrue(r.requiresVerification)
    }

    @Test fun `food scraps go to green bin`() =
        assertEquals(GREEN_BIN, classify("Banana peel", c("Peel", FOOD_WASTE, FOOD_RESIDUE)).dest())

    @Test fun `soiled paper coffee cup goes to green bin`() =
        assertEquals(GREEN_BIN, classify("Gobelet de café", c("Gobelet", PAPER_LAMINATE_CONTAINER, FOOD_RESIDUE)).dest())

    @Test fun `books are not curbside recycling`() =
        assertEquals(Destination.SPECIAL_DROP_OFF, classify("Old books", c("Book", PAPER)).dest())

    @Test fun `keyword must start a word`() {
        assertTrue(ClassificationEngine.matchesKeyword("vieux livres", "livre"))
        assertFalse(ClassificationEngine.matchesKeyword("delivre", "livre"))
        assertEquals("boite d oeufs", ClassificationEngine.normalize("Boîte d'œufs"))
    }

    // ── Intégrité du jeu de règles ──

    @Test fun `every rule has a unique id, a source and bilingual instructions`() {
        val rules = OttawaRules2026.rules
        assertEquals(rules.size, rules.map { it.id }.toSet().size)
        rules.forEach { rule ->
            assertTrue(rule.sourceUrl.startsWith("https://"), rule.id)
            assertTrue(rule.instructions.isNotEmpty(), rule.id)
            assertTrue(rule.materials.isNotEmpty(), rule.id)
            rule.instructions.forEach { assertTrue(it.en.isNotBlank() && it.fr.isNotBlank(), rule.id) }
        }
    }

    @Test fun `dataset survives a JSON round trip`() {
        val json = Json.encodeToString(RulesDataset.serializer(), OttawaRules2026.dataset)
        assertEquals(OttawaRules2026.dataset, Json.decodeFromString(RulesDataset.serializer(), json))
    }

    private companion object {
        val UNKNOWN_MATERIAL = Material.UNKNOWN
    }
}
