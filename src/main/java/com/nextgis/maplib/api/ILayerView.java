/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2015. NextGIS, info@nextgis.com
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
 * Interface to manage layer visibility sates and etc.
 * @author Dmitry Baryshnikov <dmitry.baryshnikov@nextgis.com>
 */
public interface ILayerView
{
    /**
     * @return If layer visible or not
     */
    boolean isVisible();

    /**
     * Set if layer visible or not
     * @param visible true if layer must be visible, or false
     */
    void setVisible(boolean visible);

    /**
     * Return max zoom for layer
     * @return maximum zoom
     */
    float getMaxZoom();

    /**
     * Set max zoom for layer
     * @param maxZoom The max zoom value
     */
    void setMaxZoom(float maxZoom);

    /**
     * Return min zoom for layer
     * @return minimum zoom value
     */
    float getMinZoom();

    /**
     * Set min zoom for layer
     * @param minZoom The min zoom value
     */
    void setMinZoom(float minZoom);

    /**
     * Executed then layer draw finished or indicating draw process
     * @param id The layer identificator
     * @param percent The draw progress percent
     */
    void onDrawFinished(
            int id,
            float percent);

    /**
     * Update the display size in pixels. Executed by map then screen resized (or rotated)
     * @param w width
     * @param h height
     */
    void setViewSize(
            int w,
            int h);

    /**
     * @return Renderer connected with this layer
     */
    IRenderer getRenderer();

    /**
     * Set renderer for this layer
     * @param renderer The renderer object
     */
    void setRenderer(IRenderer renderer);
}
