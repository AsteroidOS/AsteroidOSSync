/*
 * AsteroidOSSync
 * Copyright (c) 2024 AsteroidOS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.asteroidos.sync.utils

import android.content.Context
import java.util.Properties

class IconToPackageMapper(val context: Context) {

    private val mapping = Properties()

    private val defaultIcon: String

    init {
        mapping.load(context.assets.open("icons.properties"))
        defaultIcon = mapping.getProperty("default")
    }

    fun iconForPackage(pkg: String): String {
        return mapping.getProperty(pkg, defaultIcon)
    }

}