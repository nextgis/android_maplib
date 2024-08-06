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

package com.nextgis.maplib.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.nextgis.maplib.R
import com.nextgis.maplib.Object
import com.nextgis.maplib.databinding.ItemFileBinding


/**
 * Interface defining callback for operations with files.
 */
interface OnFileClickListener {
    /**
     * Fired when user selects an Object.
     *
     * @param file Object selected by user.
     */
    fun onFileClick(file: Object)
}

/**
 * Adapter for Object files.
 */
class FilesAdapter(val items: ArrayList<Object>, val listener: OnFileClickListener)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemFileBinding.inflate(layoutInflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
        (holder as ViewHolder).bind(items[position], listener)

    override fun getItemCount(): Int = items.size

    class ViewHolder(private var binding: ItemFileBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(repo: Object, listener: OnFileClickListener) {
            binding.file = repo
            val context = binding.root.context
            val view = (binding.root as? TextView)
            val drawable = ContextCompat.getDrawable(context, typeIcon(repo.type))
            view?.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
            binding.root.setOnClickListener {
                listener.onFileClick(repo) }
            binding.executePendingBindings()
        }

        private fun typeIcon(type: Int): Int {
            return when (type) {
                51 -> R.drawable.ic_sd
                52 -> R.drawable.ic_home
                53, 74, 3001, 3002 -> R.drawable.ic_folder
                55 -> R.drawable.ic_zip
                75, 3010, 3017 -> R.drawable.ic_map_marker_path
                -999 -> R.drawable.ic_keyboard_return
                3007 -> R.drawable.ic_file_document_outline
                else -> R.drawable.ic_file_document_outline
            }
        }
    }
}