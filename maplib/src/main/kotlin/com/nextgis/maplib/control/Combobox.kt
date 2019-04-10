/*
 * Project:  NextGIS Tracker
 * Purpose:  Software tracker for nextgis.com cloud
 * Author:   Dmitry Baryshnikov <dmitry.baryshnikov@nextgis.com>
 * ****************************************************************************
 * Copyright (c) 2018-2019 NextGIS <info@nextgis.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplib.control

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.TextView
import com.nextgis.maplib.R

/**
 * Special control with border, description text and combobox.
 */
class Combobox : RelativeLayout {

    constructor(context: Context?) : super(context) {
        initializeViews(context)
    }

    constructor(context: Context?, attrs: AttributeSet) : super(context, attrs) {
        initializeViews(context, attrs)
    }

    constructor(context: Context?, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        initializeViews(context, attrs)
    }

    private var mAltTextView: TextView? = null
    private var mSpinner: Spinner? = null
    private var mSpinnerEntries: Array<CharSequence>? = null
    private var mSpinnerValues: IntArray? = null
    private var mAltText: String? = null
    private var mDefaultEntry: String = ""
    private var mDefaultValue: Int = -1

    private fun initializeViews(context: Context?, attrs: AttributeSet) {
        val typedArray = context?.obtainStyledAttributes(attrs, R.styleable.Combobox)
        mAltText = typedArray?.getString(R.styleable.Combobox_alt_text)
        val intArrayId = typedArray?.getResourceId(R.styleable.Combobox_values, 0)
        if(intArrayId != 0) {
            mSpinnerValues = context?.resources?.getIntArray(intArrayId!!)
        }
        mSpinnerEntries = typedArray?.getTextArray(R.styleable.Combobox_entries)

        // Size must be equal
        if(mSpinnerEntries?.size != mSpinnerValues?.size) {
            mSpinnerValues = null
        }

        mDefaultEntry = typedArray?.getString(R.styleable.Combobox_default_entry) ?: ""
        mDefaultValue = typedArray?.getInteger(R.styleable.Combobox_default_value, -1) ?: -1

        typedArray?.recycle()

        initializeViews(context)
    }

    private fun initializeViews(context: Context?) {
        val inflater: LayoutInflater = context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.combobox, this)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        mAltTextView = findViewById(R.id.combobox_alt)
        mSpinner = findViewById(R.id.combobox_spinner)

        if(mAltText?.isEmpty() == false) {
            mAltTextView?.text = mAltText
            mAltTextView?.visibility = View.VISIBLE
        }

        if(mSpinnerEntries != null) {
            val adapter = ArrayAdapter<CharSequence>(this.context, android.R.layout.simple_list_item_1, mSpinnerEntries!!)
            mSpinner?.adapter = adapter
        }

        if( !mDefaultEntry.isEmpty() ) {
            stringValue = mDefaultEntry
        }
        else {
            intValue = mDefaultValue
        }
    }

    var stringValue: String get() = mSpinner?.selectedItem as String
    set (newValue) {
        val pos = mSpinnerEntries?.indexOf(newValue)
        if(pos != null && pos != -1) {
            mSpinner?.setSelection(pos)
        }
    }

    var intValue: Int
        get() {
            return if(mSpinnerValues != null) {
                val pos = mSpinner?.selectedItemPosition ?: 0
                mSpinnerValues!![pos]
            } else {
                0
            }
    }
    set(newValue) {
        val pos = mSpinnerValues?.indexOf(newValue)
        if(pos != null && pos != -1) {
            mSpinner?.setSelection(pos)
        }
    }
}