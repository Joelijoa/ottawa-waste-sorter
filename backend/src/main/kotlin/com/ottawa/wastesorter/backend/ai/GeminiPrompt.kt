package com.ottawa.wastesorter.backend.ai

import com.ottawa.wastesorter.rules.Condition
import com.ottawa.wastesorter.rules.Material
import com.ottawa.wastesorter.rules.ObjectType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** Consigne et schéma de réponse imposés à Gemini (sortie JSON structurée uniquement). */
object GeminiPrompt {

    fun instructions(language: String): String {
        val lang = if (language.lowercase().startsWith("fr")) "French" else "English"
        return """
            You identify household waste items from a single photo for a recycling app in Ottawa, Canada.
            Identify the object and list each physically separable component.
            You must NOT decide which bin to use: only describe objects, materials and condition.

            Material definitions:
            - PAPER: newspapers, office paper, paper bags.
            - PAPERBOARD: thin boxboard such as cereal, biscuit or cracker boxes, paper egg cartons.
            - CORRUGATED_CARDBOARD: shipping boxes and trays with a wavy middle layer, pizza boxes.
            - PAPER_LAMINATE_CONTAINER: spiral-wound cans (e.g. Pringles, frozen juice) and paper cups.
            - PAPER_LAMINATE_PACKAGING: multi-layer paper bags such as pet food or food-service bags.
            - CARTON: milk, juice or soup cartons.
            - RIGID_PLASTIC: bottles, tubs, clamshells, lids.
            - FLEXIBLE_PLASTIC: bags, wrappers, chip bags, film, bubble wrap.
            - FOAM: polystyrene foam trays, cups and containers.
            - METAL: cans, aluminum trays, metal lids.
            - ALUMINUM_FOIL: aluminum foil sheets.
            - GLASS: glass bottles and jars.
            - FOOD_WASTE: food scraps and leftovers.
            - CERAMIC, TEXTILE, HAZARDOUS (batteries, paint, chemicals), MIXED, OTHER, UNKNOWN.

            Rules:
            - Use UNKNOWN rather than guessing. Lower the confidence when unsure.
            - Set separable to false only if a component cannot be removed by hand.
            - Condition: CLEAN_EMPTY, DIRTY, FOOD_RESIDUE, WET, or UNKNOWN if not visible.
            - If information is missing (for example the material of a part cannot be seen), describe it in clarificationNeeded.
            - Write objectName, component names and clarificationNeeded in $lang.
            - Return only JSON matching the response schema.
        """.trimIndent()
    }

    private fun enumSchema(values: List<String>) = buildJsonObject {
        put("type", "STRING")
        put("enum", JsonArray(values.map { JsonPrimitive(it) }))
    }

    val responseSchema: JsonObject = buildJsonObject {
        put("type", "OBJECT")
        putJsonObject("properties") {
            putJsonObject("objectName") { put("type", "STRING") }
            put("objectType", enumSchema(ObjectType.entries.map { it.name }))
            put("primaryMaterial", enumSchema(Material.entries.map { it.name }))
            putJsonObject("components") {
                put("type", "ARRAY")
                putJsonObject("items") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("name") { put("type", "STRING") }
                        put("material", enumSchema(Material.entries.map { it.name }))
                        put("condition", enumSchema(Condition.entries.map { it.name }))
                        putJsonObject("separable") { put("type", "BOOLEAN") }
                    }
                    putJsonArray("required") {
                        add(JsonPrimitive("name")); add(JsonPrimitive("material"))
                        add(JsonPrimitive("condition")); add(JsonPrimitive("separable"))
                    }
                }
            }
            putJsonObject("visibleContamination") { put("type", "BOOLEAN") }
            putJsonObject("confidence") { put("type", "NUMBER") }
            putJsonObject("clarificationNeeded") {
                put("type", "STRING")
                put("nullable", true)
            }
        }
        putJsonArray("required") {
            listOf("objectName", "objectType", "primaryMaterial", "components", "visibleContamination", "confidence")
                .forEach { add(JsonPrimitive(it)) }
        }
    }
}
