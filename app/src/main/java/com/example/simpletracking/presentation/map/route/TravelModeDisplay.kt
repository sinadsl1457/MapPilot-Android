package com.example.simpletracking.presentation.map.route

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Subway
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.simpletracking.R
import com.example.simpletracking.domain.model.TransitLeg
import com.example.simpletracking.domain.model.TravelMode

@get:StringRes
val TravelMode.titleRes: Int
    get() = when (this) {
        TravelMode.TRANSIT -> R.string.travel_mode_transit
        TravelMode.CAR -> R.string.travel_mode_car
        TravelMode.WALK -> R.string.travel_mode_walk
    }

val TravelMode.icon: ImageVector
    get() = when (this) {
        TravelMode.TRANSIT -> Icons.Filled.DirectionsBus
        TravelMode.CAR -> Icons.Filled.DirectionsCar
        TravelMode.WALK -> Icons.AutoMirrored.Filled.DirectionsWalk
    }

val TransitLeg.Kind.icon: ImageVector
    get() = when (this) {
        TransitLeg.Kind.WALK -> Icons.AutoMirrored.Filled.DirectionsWalk
        TransitLeg.Kind.BUS -> Icons.Filled.DirectionsBus
        TransitLeg.Kind.SUBWAY -> Icons.Filled.Subway
    }
