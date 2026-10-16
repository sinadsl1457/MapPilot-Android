package com.example.simpletracking.presentation.map.search

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalConvenienceStore
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.simpletracking.R
import com.example.simpletracking.domain.model.PlaceCategory

@get:StringRes
val PlaceCategory.titleRes: Int
    get() = when (this) {
        PlaceCategory.RESTAURANT -> R.string.category_restaurant
        PlaceCategory.CAFE -> R.string.category_cafe
        PlaceCategory.CONVENIENCE_STORE -> R.string.category_convenience_store
        PlaceCategory.GAS_STATION -> R.string.category_gas_station
        PlaceCategory.PHARMACY -> R.string.category_pharmacy
        PlaceCategory.PARKING -> R.string.category_parking
    }

val PlaceCategory.icon: ImageVector
    get() = when (this) {
        PlaceCategory.RESTAURANT -> Icons.Filled.Restaurant
        PlaceCategory.CAFE -> Icons.Filled.LocalCafe
        PlaceCategory.CONVENIENCE_STORE -> Icons.Filled.LocalConvenienceStore
        PlaceCategory.GAS_STATION -> Icons.Filled.LocalGasStation
        PlaceCategory.PHARMACY -> Icons.Filled.LocalPharmacy
        PlaceCategory.PARKING -> Icons.Filled.LocalParking
    }
