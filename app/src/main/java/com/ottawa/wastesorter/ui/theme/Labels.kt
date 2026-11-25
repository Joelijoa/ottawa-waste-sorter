package com.ottawa.wastesorter.ui.theme

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Recycling
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.TakeoutDining
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WineBar
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.ottawa.wastesorter.R
import com.ottawa.wastesorter.rules.Condition
import com.ottawa.wastesorter.rules.Destination
import com.ottawa.wastesorter.rules.Material
import com.ottawa.wastesorter.rules.ObjectType

/**
 * Identité d'un bac : couleur + icône + nom, toujours ensemble.
 * La couleur n'est jamais le seul indice (règle « Bin identity » du design).
 */
data class BinStyle(
    @StringRes val name: Int,
    @StringRes val sub: Int,
    val icon: ImageVector,
    val bg: Color,
    val fg: Color,
    val chip: Color,
)

fun Destination.style(): BinStyle = when (this) {
    Destination.GARBAGE -> BinStyle(
        R.string.dest_garbage, R.string.dest_garbage_desc, Icons.Rounded.Delete,
        Ows.GarbageGrey, Ows.Charcoal, Ows.Charcoal.copy(alpha = 0.08f),
    )
    Destination.BLACK_BIN -> BinStyle(
        R.string.dest_black, R.string.dest_black_desc, Icons.Rounded.Recycling,
        Ows.Charcoal, Color.White, Color.White.copy(alpha = 0.14f),
    )
    Destination.BLUE_BIN -> BinStyle(
        R.string.dest_blue, R.string.dest_blue_desc, Icons.Rounded.Recycling,
        Ows.Blue, Color.White, Color.White.copy(alpha = 0.18f),
    )
    Destination.GREEN_BIN -> BinStyle(
        R.string.dest_green, R.string.dest_green_desc, Icons.Rounded.Eco,
        Ows.Green, Color.White, Color.White.copy(alpha = 0.16f),
    )
    Destination.SPECIAL_DROP_OFF -> BinStyle(
        R.string.dest_special, R.string.dest_special_desc, Icons.Rounded.Place,
        Ows.WarningSoft, Ows.WarningInk, Ows.WarningIcon.copy(alpha = 0.14f),
    )
    Destination.NEEDS_VERIFICATION -> BinStyle(
        R.string.dest_verify, R.string.dest_verify_desc, Icons.Rounded.Warning,
        Ows.WarningSoft, Ows.WarningInk, Ows.WarningIcon.copy(alpha = 0.14f),
    )
}

/** Les quatre flux de collecte en bordure de rue, dans l'ordre du design. */
val CURBSIDE_BINS = listOf(Destination.GARBAGE, Destination.BLACK_BIN, Destination.BLUE_BIN, Destination.GREEN_BIN)

/** Vignette d'un objet : icône + teinte, déduites du matériau principal. */
data class ItemLook(val icon: ImageVector, val bg: Color, val fg: Color)

fun Material.look(): ItemLook = when (this) {
    Material.PAPERBOARD, Material.CORRUGATED_CARDBOARD -> ItemLook(Icons.Rounded.Inventory2, Color(0xFFF3E8D8), Color(0xFF8A5A2B))
    Material.PAPER -> ItemLook(Icons.Rounded.Description, Color(0xFFEFE4D2), Color(0xFF8A5A2B))
    Material.PAPER_LAMINATE_CONTAINER -> ItemLook(Icons.Rounded.LocalCafe, Color(0xFFEFE7DF), Color(0xFF6B4A33))
    Material.PAPER_LAMINATE_PACKAGING -> ItemLook(Icons.Rounded.ShoppingBag, Color(0xFFEFE4D2), Color(0xFF8A5A2B))
    Material.CARTON -> ItemLook(Icons.Rounded.LocalDrink, Color(0xFFE3EEFB), Color(0xFF2F65AE))
    Material.RIGID_PLASTIC -> ItemLook(Icons.Rounded.LocalDrink, Color(0xFFE3EEFB), Color(0xFF2F65AE))
    Material.FLEXIBLE_PLASTIC -> ItemLook(Icons.Rounded.ShoppingBag, Color(0xFFF2ECF6), Color(0xFF6C4A86))
    Material.FOAM -> ItemLook(Icons.Rounded.TakeoutDining, Color(0xFFF6E1DA), Color(0xFFA2412A))
    Material.METAL -> ItemLook(Icons.Rounded.Kitchen, Color(0xFFE8EBEE), Color(0xFF4A5560))
    Material.ALUMINUM_FOIL -> ItemLook(Icons.Rounded.Layers, Color(0xFFE8EBEE), Color(0xFF4A5560))
    Material.GLASS -> ItemLook(Icons.Rounded.WineBar, Color(0xFFE3F1EE), Color(0xFF2E6B5E))
    Material.FOOD_WASTE -> ItemLook(Icons.Rounded.Restaurant, Color(0xFFFBF0C9), Color(0xFF8A6D0B))
    Material.CERAMIC -> ItemLook(Icons.Rounded.LocalCafe, Color(0xFFECEEEA), Color(0xFF5C635F))
    Material.TEXTILE -> ItemLook(Icons.Rounded.Checkroom, Color(0xFFF2ECF6), Color(0xFF6C4A86))
    Material.HAZARDOUS -> ItemLook(Icons.Rounded.BatteryAlert, Color(0xFFFBF1E0), Color(0xFFB07A1F))
    Material.MIXED -> ItemLook(Icons.Rounded.Category, Color(0xFFECEEEA), Color(0xFF5C635F))
    Material.OTHER, Material.UNKNOWN -> ItemLook(Icons.AutoMirrored.Rounded.HelpOutline, Color(0xFFECEEEA), Color(0xFF5C635F))
}

@StringRes
fun Material.label(): Int = when (this) {
    Material.PAPER -> R.string.mat_paper
    Material.PAPERBOARD -> R.string.mat_paperboard
    Material.CORRUGATED_CARDBOARD -> R.string.mat_corrugated
    Material.PAPER_LAMINATE_CONTAINER -> R.string.mat_laminate_container
    Material.PAPER_LAMINATE_PACKAGING -> R.string.mat_laminate_packaging
    Material.CARTON -> R.string.mat_carton
    Material.RIGID_PLASTIC -> R.string.mat_rigid_plastic
    Material.FLEXIBLE_PLASTIC -> R.string.mat_flexible_plastic
    Material.FOAM -> R.string.mat_foam
    Material.METAL -> R.string.mat_metal
    Material.ALUMINUM_FOIL -> R.string.mat_foil
    Material.GLASS -> R.string.mat_glass
    Material.FOOD_WASTE -> R.string.mat_food
    Material.CERAMIC -> R.string.mat_ceramic
    Material.TEXTILE -> R.string.mat_textile
    Material.HAZARDOUS -> R.string.mat_hazardous
    Material.MIXED -> R.string.mat_mixed
    Material.OTHER -> R.string.mat_other
    Material.UNKNOWN -> R.string.mat_unknown
}

@StringRes
fun Condition.label(): Int = when (this) {
    Condition.CLEAN_EMPTY -> R.string.cond_clean
    Condition.DIRTY -> R.string.cond_dirty
    Condition.FOOD_RESIDUE -> R.string.cond_food
    Condition.WET -> R.string.cond_wet
    Condition.UNKNOWN -> R.string.cond_unknown
}

@StringRes
fun Condition.hint(): Int = when (this) {
    Condition.CLEAN_EMPTY -> R.string.cond_clean_hint
    Condition.DIRTY -> R.string.cond_dirty_hint
    Condition.FOOD_RESIDUE -> R.string.cond_food_hint
    Condition.WET -> R.string.cond_wet_hint
    Condition.UNKNOWN -> R.string.cond_unknown_hint
}

@StringRes
fun ObjectType.label(): Int = when (this) {
    ObjectType.FOOD_PACKAGING -> R.string.type_food
    ObjectType.BEVERAGE_PACKAGING -> R.string.type_beverage
    ObjectType.DELIVERY_PACKAGING -> R.string.type_delivery
    ObjectType.HOUSEHOLD_PACKAGING -> R.string.type_household
    ObjectType.PERSONAL_CARE_PACKAGING -> R.string.type_personal_care
    ObjectType.PRINTED_PAPER -> R.string.type_printed
    ObjectType.FOOD_SCRAPS -> R.string.type_food_scraps
    ObjectType.OTHER -> R.string.type_other
}

/** Les 7 matériaux principaux de l'écran « Item details » du design. */
val PRIMARY_MATERIALS = listOf(
    Material.PAPERBOARD,
    Material.PAPER,
    Material.RIGID_PLASTIC,
    Material.METAL,
    Material.GLASS,
    Material.FLEXIBLE_PLASTIC,
    Material.MIXED,
)

/** Matériaux supplémentaires (« Autres matériaux ») : indispensables pour les règles 2026. */
val MORE_MATERIALS = listOf(
    Material.CORRUGATED_CARDBOARD,
    Material.PAPER_LAMINATE_CONTAINER,
    Material.PAPER_LAMINATE_PACKAGING,
    Material.CARTON,
    Material.FOAM,
    Material.ALUMINUM_FOIL,
    Material.FOOD_WASTE,
    Material.CERAMIC,
    Material.TEXTILE,
    Material.HAZARDOUS,
)

/** Recherche par matériau (6 raccourcis de l'écran de recherche). */
val BROWSE_MATERIALS = listOf(
    Material.PAPERBOARD,
    Material.PAPER,
    Material.RIGID_PLASTIC,
    Material.METAL,
    Material.GLASS,
    Material.FLEXIBLE_PLASTIC,
)

/** Ordre d'affichage des états dans l'écran de détails (le design met « Pas sûr » en dernier). */
val CONDITION_ORDER = listOf(Condition.CLEAN_EMPTY, Condition.DIRTY, Condition.FOOD_RESIDUE, Condition.WET, Condition.UNKNOWN)
