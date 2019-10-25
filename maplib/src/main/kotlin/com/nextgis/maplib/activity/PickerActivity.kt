/*
 * Project: NextGIS Mobile SDK
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 *
 * Created by Stanislav Petriakov on 19.09.19
 * Copyright © 2019 NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplib.activity

import com.nextgis.maplib.Object


/**
 * Interface defining an Activity which supports file picker with [FilePickerFragment].
 */
interface PickerActivity {
    /**
     * Returns a list of Objects which are shown as root directory
     *
     * @return list of Objects shown as root.
     */
    fun root(): List<Object>

    /**
     * Fired when user selects an Object from supported file types.
     *
     * @param file Object selected by user.
     */
    fun onLayerSelected(file: Object?)
}