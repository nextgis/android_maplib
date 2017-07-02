/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2016 NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.maplib.api;

/**
 * Interface for progress indication
 */
public interface IProgressor {
    /**
     * Set maximum value of progress
     * @param maxValue maximum value
     */
    void setMax(int maxValue);

    /**
     * The process should execute this method if it should be canceled.
     * @return true if progress must be canceled, or false
     */
    boolean isCanceled();

    /**
     * Set current progress.
     * @param value current progress
     */
    void setValue(int value);

    /**
     * If progress cannot determinate the max value and current value, it can be set as indeterminate.
     * @param indeterminate true to set indeterminate state, or false
     */
    void setIndeterminate(boolean indeterminate);

    /**
     * Set some message to show in progress dialog or somewhere else.
     * @param message a message
     */
    void setMessage(String message);
}
