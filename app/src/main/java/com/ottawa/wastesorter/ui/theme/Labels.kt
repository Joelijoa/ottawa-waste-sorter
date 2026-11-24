package com.ottawa.wastesorter.ui.theme

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.ottawa.wastesorter.R
import com.ottawa.wastesorter.rules.Condition
import com.ottawa.wastesorter.rules.Destination
import com.ottawa.wastesorter.rules.Material
import com.ottawa.wastesorter.rules.ObjectType

/**
 * Présentation d'une destination. La couleur n'est jamais le seul indice :
 * chaque destination a aussi un nom et une icône.
 */
data class DestinationStyle(
    @StringRes val label: Int,
    @StringRes val description: Int,
    val icon: ImageVector,
    val color: Color,
    val onColor: Color,
)

fun Destination.style(): DestinationStyle = when (this) {
    Destination.BLUE_BIN -> DestinationStyle(
        R.string.dest_blue, R.string.dest_blue_desc, Icons.Filled.LocalDrink, Color(0xFF1E5AA8), Color.White,
    )
    Destination.BLACK_BIN -> DestinationStyle(
        R.string.dest_black, R.string.dest_black_desc, Icons.Filled.Description, Color(0xFF2B2D2F), Color.White,
    )
    Destination.GREEN_BIN -> DestinationStyle(
        R.string.dest_green, R.string.dest_green_desc, Icons.Filled.Eco, Color(0xFF2E7D32), Color.White,
    )
    Destination.GARBAGE -> DestinationStyle(
        R.string.dest_garbage, R.string.dest_garbage_desc, Icons.Filled.Delete, Color(0xFF8A8D91), Color.White,
    )
    Destination.SPECIAL_DROP_OFF -> DestinationStyle(
        R.string.dest_special, R.string.dest_special_desc, Icons.Filled.Place, Color(0xFF8D5A00), Color.White,
    )
    Destination.NEEDS_VERIFICATION -> DestinationStyle(
        R.string.dest_verify, R.string.dest_verify_desc, Icons.Filled.Warning, Color(0xFFB3261E), Color.White,
    )
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

/** Matériaux proposés dans le tri manuel (ordre du cahier des charges). */
val MANUAL_MATERIALS = listOf(
    Material.PAPERBOARD,
    Material.PAPER,
    Material.CORRUGATED_CARDBOARD,
    Material.PAPER_LAMINATE_CONTAINER,
    Material.PAPER_LAMINATE_PACKAGING,
    Material.CARTON,
    Material.RIGID_PLASTIC,
    Material.FLEXIBLE_PLASTIC,
    Material.METAL,
    Material.ALUMINUM_FOIL,
    Material.GLASS,
    Material.FOAM,
    Material.FOOD_WASTE,
    Material.MIXED,
    Material.CERAMIC,
    Material.TEXTILE,
    Material.HAZARDOUS,
    Material.OTHER,
)
