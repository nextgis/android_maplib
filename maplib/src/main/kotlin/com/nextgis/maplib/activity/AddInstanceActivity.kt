/*
 * Project: NextGIS Mobile SDK
 * Author:  Stanislav Petriakov, becomeglory@gmail.com
 *
 * Created by Stanislav Petriakov on 07.10.19
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

import android.app.Activity
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.nextgis.maplib.*
import com.nextgis.maplib.databinding.ActivityAddInstanceBinding
import com.nextgis.maplib.util.NonNullObservableField
import com.nextgis.maplib.util.tint
import kotlinx.android.synthetic.main.activity_add_instance.*


class AddInstanceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddInstanceBinding
    val instance = NonNullObservableField(Instance("", "", "", "", ""))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_add_instance)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        binding.apply {
            activity = this@AddInstanceActivity
            fab.tint(R.color.white)
        }

        binding.executePendingBindings()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun save() {
        if (instance.get().url.isBlank() || instance.get().login.isBlank() || instance.get().password.isBlank()) {
            Toast.makeText(this, R.string.empty_field, Toast.LENGTH_SHORT).show()
            return
        }

        API.getCatalog()?.let {
            val connection = NGWConnectionDescription(instance.get().url, instance.get().login, instance.get().password)
            val status = connection.check()
            if (status) {
                it.createConnection(instance.get().url, connection)
                setResult(Activity.RESULT_OK)
                finish()
            } else {
                Toast.makeText(this, R.string.connection_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val ADD_INSTANCE_REQUEST = 145
    }
}