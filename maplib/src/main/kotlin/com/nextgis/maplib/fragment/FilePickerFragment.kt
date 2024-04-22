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

package com.nextgis.maplib.fragment

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nextgis.maplib.R
import com.nextgis.maplib.Object
import com.nextgis.maplib.activity.PickerActivity
import com.nextgis.maplib.adapter.FilesAdapter
import com.nextgis.maplib.adapter.OnFileClickListener
import com.nextgis.maplib.databinding.FragmentFilePickerBinding
import com.nextgis.maplib.util.NonNullObservableField
import com.nextgis.maplib.util.runAsync
import java.util.*


/**
 * Fragment with file picker implementation for Object.
 */
open class FilePickerFragment : Fragment(), OnFileClickListener {

    private var _binding: FragmentFilePickerBinding? = null
    private val binding get() = _binding!!

    private var root = ""
    private val stack = Stack<Object>()
    val path = NonNullObservableField("/")
    val current: Object? get() = if (stack.isNotEmpty()) stack.peek() else null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentFilePickerBinding.inflate(LayoutInflater.from(context), null, false)

        binding.apply {
            fragment = this@FilePickerFragment
            addRoot(activity as? PickerActivity)
            list.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        }
        binding.executePendingBindings()
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_picker, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                refresh()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Refresh contents of current directory.
     */
    fun refresh() {
        if (stack.isNotEmpty()) {
            val file = stack.pop()
            file.refresh()
            onFileClick(file)
        } else {
            addRoot(activity as? PickerActivity)
        }
    }



    override fun onFileClick(file: Object) {
        (binding.list.adapter as? FilesAdapter)?.items?.clear()
        val children = arrayListOf<Object>()
        // TODO gpkg/ngrc type
        if (file.type == 501) { // shp type
            (activity as? PickerActivity)?.onLayerSelected(file)
            return
        }
        if (file.type == 507) { // geojson
            (activity as? PickerActivity)?.onLayerSelected(file)
            return
        }
        if (file.type == 55) { // zip type
            (activity as? PickerActivity)?.onLayerSelected(file)
            return
        }
//        if (file.type == 75) { // tracker
//            (activity as? PickerActivity)?.onLayerSelected(file)
//            return
//        }
        if (file.type == 1002) { // tif type
            (activity as? PickerActivity)?.onLayerSelected(file)
            return
        }

        if (file.type == 3007) { // polygon multiple
            (activity as? PickerActivity)?.onLayerSelected(file)
            return
        }

        if (file.type == -999) {
            stack.pop()
            if (stack.size == 0) {
                (activity as? PickerActivity)?.let { addItems(children, it.root()) }
                path.set("/")
            } else {
                addBack(children)
                val parent = stack.peek()
                addItems(children, parent.children().toList())
                path.set(parent.path.replace(root, ""))
            }
        } else {
            addBack(children)
            addItems(children, file.children().toList())
            if (stack.isEmpty() && root.isBlank())
                root = file.path.substringBeforeLast("/")
            stack.add(file)
            path.set(file.path.replace(root, ""))
        }
        (binding.list.adapter as? FilesAdapter)?.items?.addAll(children)
        binding.list.adapter?.notifyDataSetChanged()
    }

    private fun addRoot(parent: PickerActivity?) {
        parent?.let {
            runAsync {
                val items = arrayListOf<Object>()
                addItems(items, it.root())
                activity?.runOnUiThread { binding.list.adapter = FilesAdapter(items, this@FilePickerFragment) }
            }
        }
    }

    private fun addBack(children: ArrayList<Object>) {
        children.add(Object(getString(R.string.back), -999, "", -1))
    }

    private fun addItems(children: ArrayList<Object>, items: List<Object>) {
        children.addAll(items.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }).sortedBy { it.type != 53 || it.type != 74 })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}