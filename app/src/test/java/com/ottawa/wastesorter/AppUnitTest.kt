package com.ottawa.wastesorter

import com.ottawa.wastesorter.core.common.ImageProcessor
import com.ottawa.wastesorter.core.network.ApiProvider
import com.ottawa.wastesorter.domain.model.IdentificationMethod
import com.ottawa.wastesorter.domain.usecase.ClassifyManuallyUseCase
import com.ottawa.wastesorter.rules.ClassificationEngine
import com.ottawa.wastesorter.rules.Condition
import com.ottawa.wastesorter.rules.Destination
import com.ottawa.wastesorter.rules.ItemComponent
import com.ottawa.wastesorter.rules.Material
import com.ottawa.wastesorter.rules.ObjectType
import com.ottawa.wastesorter.ui.theme.label
import com.ottawa.wastesorter.ui.theme.style
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppUnitTest {

    @Test
    fun sampleSizeKeepsImageAboveTargetSize() {
        assertEquals(1, ImageProcessor.sampleSizeFor(1000, 800, 1024))
        assertEquals(2, ImageProcessor.sampleSizeFor(4032, 3024, 1024)) // 4032/2 = 2016 ≥ 1024, /4 = 1008 < 1024
        assertEquals(4, ImageProcessor.sampleSizeFor(8000, 6000, 1024))
    }

    @Test
    fun backendUrlIsNormalized() {
        assertEquals("https://api.example.com/", ApiProvider.normalize(" https://api.example.com "))
        assertEquals("https://api.example.com/", ApiProvider.normalize("https://api.example.com/"))
        assertNull(ApiProvider.normalize(""))
        assertNull(ApiProvider.normalize("api.example.com"))
    }

    @Test
    fun manualSortingUsesTheRulesEngineOffline() {
        val useCase = ClassifyManuallyUseCase(ClassificationEngine())
        val outcome = useCase(
            itemName = "Pringles",
            objectType = ObjectType.FOOD_PACKAGING,
            components = listOf(ItemComponent("Tube", Material.PAPER_LAMINATE_CONTAINER, Condition.CLEAN_EMPTY)),
        )
        assertEquals(IdentificationMethod.MANUAL, outcome.method)
        assertEquals(Destination.BLUE_BIN, outcome.classification.components.single().destination)
    }

    @Test
    fun catalogSearchFindsItemsInBothLanguages() {
        assertEquals("pringles", com.ottawa.wastesorter.domain.model.ItemCatalog.search("croustilles").first().id)
        assertEquals("yogurt", com.ottawa.wastesorter.domain.model.ItemCatalog.search("Yogourt").first().id)
        org.junit.Assert.assertTrue(com.ottawa.wastesorter.domain.model.ItemCatalog.search("air fryer").isEmpty())
    }

    @Test
    fun pringlesFromCatalogSplitsIntoBlueAndGreen() {
        val item = com.ottawa.wastesorter.domain.model.ItemCatalog.byId("pringles")!!
        val outcome = ClassifyManuallyUseCase(ClassificationEngine())(item.name.en, item.objectType, item.components("en"))
        assertEquals(listOf(Destination.BLUE_BIN, Destination.BLUE_BIN, Destination.GREEN_BIN), outcome.classification.components.map { it.destination })
    }

    @Test
    fun everyEnumValueHasALabelAndAStyle() {
        Material.entries.forEach { assertNotEquals(0, it.label()) }
        Condition.entries.forEach { assertNotEquals(0, it.label()) }
        ObjectType.entries.forEach { assertNotEquals(0, it.label()) }
        // Les deux « noirs » ne doivent jamais se confondre.
        assertNotEquals(Destination.BLACK_BIN.style().name, Destination.GARBAGE.style().name)
        assertNotEquals(Destination.BLACK_BIN.style().bg, Destination.GARBAGE.style().bg)
        assertNotEquals(Destination.BLACK_BIN.style().icon, Destination.GARBAGE.style().icon)
    }
}
