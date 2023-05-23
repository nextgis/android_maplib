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

package com.nextgis.maplib.fragment

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nextgis.maplib.Instance
import com.nextgis.maplib.R
import com.nextgis.maplib.activity.AddInstanceActivity
import com.nextgis.maplib.adapter.OnInstanceClickListener
import com.nextgis.maplib.adapter.getInstances
import com.nextgis.maplib.adapter.replaceInstances
import com.nextgis.maplib.databinding.DialogSelectInstanceBinding


/**
 * Fragment with all saved [Instances][Instance] and link to create a new one.
 */
class SelectInstanceDialog : DialogFragment(), OnInstanceClickListener {
    override fun onInstanceClick(instance: Instance) {
        listener?.onInstanceClick(instance)
        dismiss()
    }

    private var _binding: DialogSelectInstanceBinding? = null
    private val binding get() = _binding!!

    private var listener: OnInstanceClickListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext(), androidx.appcompat.R.style.Base_ThemeOverlay_AppCompat_Dialog_Alert)
        _binding = DialogSelectInstanceBinding.inflate(LayoutInflater.from(context), null, false)
        dialog.setContentView(binding.root)
        binding.fragment = this
        binding.list.adapter = getInstances(this, false)
        binding.list.layoutManager = LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false)
        return dialog
    }

    /**
     * Show this fragment and attach listener.
     *
     * @param activity parent activity.
     * @param listener listener for instances callback.
     */
    fun show(activity: FragmentActivity, listener: OnInstanceClickListener) {
        this.listener = listener
        show(activity.supportFragmentManager, TAG)
    }

    /**
     * Start activity to add a new instance connection.
     */
    fun addAccount() {
        context?.let {
            val intent = Intent(it, AddInstanceActivity::class.java)
            startActivityForResult(intent, AddInstanceActivity.ADD_INSTANCE_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            AddInstanceActivity.ADD_INSTANCE_REQUEST -> {
                if (resultCode == Activity.RESULT_OK)
                    replaceInstances(binding.list.adapter, false)
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        const val TAG = "SelectInstanceDialog"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}