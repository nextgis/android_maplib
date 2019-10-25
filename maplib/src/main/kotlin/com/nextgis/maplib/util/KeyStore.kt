/*
 * Project: NextGIS Mobile SDK
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 *
 * Created by Stanislav Petriakov on 11.10.19
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

package com.nextgis.maplib.util

import android.content.Context
import com.kazakago.cryptore.CipherAlgorithm
import com.kazakago.cryptore.Cryptore


@Throws(Exception::class)
private fun getCryptore(context: Context, alias: String): Cryptore {
    val builder = Cryptore.Builder(alias, CipherAlgorithm.RSA)
    builder.context = context
    return builder.build()
}

/**
 * Encrypt String with key for given alias from Android KeyStore.
 *
 * @param context required Context to init KeyStore.
 * @param plainStr String to encrypt.
 * @param alias optional alias for key to use from KeyStore.
 */
@Throws(Exception::class)
fun encrypt(context: Context, plainStr: String, alias: String = "nextgis"): String {
    val plainByte = plainStr.toByteArray()
    val result = getCryptore(context, alias).encrypt(plainByte)
    return android.util.Base64.encodeToString(result.bytes, android.util.Base64.DEFAULT)
}

/**
 * Decrypt encrypted String with key for given alias from Android KeyStore.
 *
 * @param context required Context to init KeyStore.
 * @param encryptedStr encrypted String to decrypt.
 * @param alias optional alias for key to use from KeyStore.
 */
@Throws(Exception::class)
fun decrypt(context: Context, encryptedStr: String, alias: String = "nextgis"): String {
    val encryptedByte = android.util.Base64.decode(encryptedStr, android.util.Base64.DEFAULT)
    val result = getCryptore(context, alias).decrypt(encryptedByte, null)
    return String(result.bytes)
}