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

package com.nextgis.maplib.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.nextgis.maplib.API
import com.nextgis.maplib.Instance
import com.nextgis.maplib.databinding.ItemWebInstanceBinding


interface OnInstanceClickListener {
    fun onInstanceClick(instance: Instance)
}

class InstanceAdapter(val items: ArrayList<Instance>, val listener: OnInstanceClickListener) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemWebInstanceBinding.inflate(layoutInflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
            (holder as ViewHolder).bind(items[position], listener)

    override fun getItemCount(): Int = items.size

    class ViewHolder(private var binding: ItemWebInstanceBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(repo: Instance, listener: OnInstanceClickListener) {
            binding.instance = repo
            val root = binding.root as LinearLayout
            if (!repo.more)
                root.getChildAt(root.childCount - 1).visibility = View.GONE
            root.setOnClickListener { listener.onInstanceClick(repo) }
            binding.executePendingBindings()
        }
    }
}

fun replaceInstances(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>?, more: Boolean = true) {
    (adapter as? InstanceAdapter)?.items?.let {
        it.clear()
        addInstances(it, more)
        (adapter as? InstanceAdapter)?.notifyDataSetChanged()
    }
}

private fun addInstances(list: ArrayList<Instance>, more: Boolean) {
    API.getCatalog()?.let {
        for (child in it.children()) {
            if (child.type == 72) {
                val items = child.children()
                for (item in items)
                    list.add(Instance(item.name.replace(".wconn", ""), item.path, "", "", "", more))
                break
            }
        }
    }
}

fun getInstances(listener: OnInstanceClickListener, more: Boolean = true): InstanceAdapter {
    val list = arrayListOf<Instance>()
    addInstances(list, more)
    return InstanceAdapter(list, listener)
}