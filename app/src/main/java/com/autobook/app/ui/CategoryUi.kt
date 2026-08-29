package com.autobook.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.autobook.app.data.Categories

object CategoryUi {

    fun colorOf(category: String): Color {
        return when (Categories.primaryOf(category)) {
            Categories.FOOD -> Color(0xFFFF8A3D)
            Categories.TRANSPORT -> Color(0xFF4A90D9)
            Categories.SHOPPING -> Color(0xFFE5539C)
            Categories.ENTERTAINMENT -> Color(0xFF8E6BE0)
            Categories.MEDICAL -> Color(0xFFE5533C)
            Categories.EDUCATION -> Color(0xFF3DB4A8)
            Categories.COMMUNICATION -> Color(0xFF8D9B4A)
            Categories.GIFT -> Color(0xFFD46B3E)
            Categories.BEAUTY -> Color(0xFFE88BB5)
            Categories.INCOME -> Color(0xFF4CAF7D)
            else -> Color(0xFF9E9E9E)
        }
    }

    fun iconOf(category: String): ImageVector {
        return when (Categories.primaryOf(category)) {
            Categories.FOOD -> Icons.Outlined.Restaurant
            Categories.TRANSPORT -> Icons.Outlined.DirectionsBus
            Categories.SHOPPING -> Icons.Outlined.ShoppingCart
            Categories.ENTERTAINMENT -> Icons.Outlined.Movie
            Categories.MEDICAL -> Icons.Outlined.LocalHospital
            Categories.EDUCATION -> Icons.Outlined.MenuBook
            Categories.COMMUNICATION -> Icons.Outlined.PhoneAndroid
            Categories.GIFT -> Icons.Outlined.CardGiftcard
            Categories.BEAUTY -> Icons.Outlined.Face
            Categories.INCOME -> Icons.Outlined.ArrowUpward
            else -> Icons.Outlined.MoreHoriz
        }
    }
}
