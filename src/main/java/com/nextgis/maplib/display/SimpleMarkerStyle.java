/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2012-2018 NextGIS, info@nextgis.com
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

package com.nextgis.maplib.display;

import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.text.TextUtils;

import com.nextgis.maplib.api.ITextStyle;
import com.nextgis.maplib.datasource.GeoGeometry;
import com.nextgis.maplib.datasource.GeoMultiPoint;
import com.nextgis.maplib.datasource.GeoPoint;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

import static com.nextgis.maplib.util.Constants.*;
import static com.nextgis.maplib.util.GeoConstants.GTMultiPoint;
import static com.nextgis.maplib.util.GeoConstants.GTPoint;

public class SimpleMarkerStyle extends Style implements ITextStyle {
    public final static int MarkerStylePoint = 1;
    public final static int MarkerStyleCircle = 2;
    public final static int MarkerStyleDiamond = 3;
    public final static int MarkerStyleCross = 4;
    public final static int MarkerStyleTriangle = 5;
    public final static int MarkerStyleBox = 6;
    public final static int MarkerEditStyleCircle = 7;
    public final static int MarkerStyleCrossedBox = 8;

    public final static float SIZE_SMALL = 3;
    public final static float SIZE_MEDIUM = 6;
    public final static float SIZE_BIG = 10;
    public final static ArrayList<Float> SIZES = new ArrayList<>(Arrays.asList(new Float[]{SIZE_SMALL, SIZE_MEDIUM, SIZE_BIG}));

    public final static int ALIGN_TOP = 0;
    public final static int ALIGN_TOP_RIGHT = 1;
    public final static int ALIGN_RIGHT = 2;
    public final static int ALIGN_BOTTOM_RIGHT = 3;
    public final static int ALIGN_BOTTOM = 4;
    public final static int ALIGN_BOTTOM_LEFT = 5;
    public final static int ALIGN_LEFT = 6;
    public final static int ALIGN_TOP_LEFT = 7;
    public final static ArrayList<Integer> ALIGNMENTS = new ArrayList<>(Arrays.asList(
            new Integer[]{ALIGN_TOP, ALIGN_TOP_RIGHT, ALIGN_RIGHT, ALIGN_BOTTOM_RIGHT, ALIGN_BOTTOM, ALIGN_BOTTOM_LEFT, ALIGN_LEFT, ALIGN_TOP_LEFT}));

    protected int mType;
    protected float mSize, mTextSize = 3;
    protected Paint mOutPaint;
    protected Paint mFillPaint;
    protected String mField;
    protected String mText;
    protected int mTextAlignment;
    protected int mTextColor = Color.BLACK;

    public SimpleMarkerStyle() {
        super();
        initPaints();
    }

    public SimpleMarkerStyle(int fillColor, int outColor, float size, int type) {
        super(fillColor, outColor);

        mType = type;
        mSize = size;
        mWidth = 1;

        initPaints();
        setPaintsColors();
    }

    @Override
    public SimpleMarkerStyle clone() throws CloneNotSupportedException {
        SimpleMarkerStyle obj = (SimpleMarkerStyle) super.clone();
        obj.mType = mType;
        obj.mSize = mSize;
        obj.mTextSize = mTextSize;
        obj.mTextAlignment = mTextAlignment;
        obj.mTextColor = mTextColor;
        obj.mText = mText;
        obj.mField = mField;
        return obj;
    }

    protected void initPaints() {
        mFillPaint = new Paint();
        mFillPaint.setStrokeCap(Paint.Cap.ROUND);

        mOutPaint = new Paint();
        mOutPaint.setStyle(Paint.Style.STROKE);
        mOutPaint.setAntiAlias(true);
    }

    protected void setPaintsColors() {
        mFillPaint.setColor(mColor);
        mOutPaint.setColor(mOutColor);
    }

    protected void onDraw(GeoPoint pt, GISDisplay display) {
        if (null == pt) {
            return;
        }

        mColor = Color.argb(mInnerAlpha, Color.red(mColor), Color.green(mColor), Color.blue(mColor));
        mOutColor = Color.argb(mOuterAlpha, Color.red(mOutColor), Color.green(mOutColor), Color.blue(mOutColor));
        mFillPaint.setColor(mColor);
        mOutPaint.setColor(mOutColor);
        float scaledSize = (float) (mSize / display.getScale());
        float width = (float) (mWidth / display.getScale());
        switch (mType) {
            case MarkerStylePoint:
                drawPointMarker(scaledSize, pt, display);
                break;
            case MarkerStyleCircle:
                drawCircleMarker(scaledSize, width, pt, display);
                break;
            case MarkerStyleDiamond:
                drawDiamondMarker(scaledSize, width, pt, display);
                break;
            case MarkerStyleCross:
                drawCrossMarker(scaledSize, width, pt, display);
                break;
            case MarkerStyleTriangle:
                drawTriangleMarker(scaledSize, width, pt, display);
                break;
            case MarkerStyleBox:
                drawBoxMarker(scaledSize, width, pt, display);
                break;
            case MarkerStyleCrossedBox:
                drawCrossedBoxMarker(scaledSize, width, pt, display);
                break;
        }

//        drawText(scaledSize - width, pt, display);
        drawText(scaledSize, mTextSize, pt, display);
    }

    protected void drawText(float radius, float size, GeoPoint pt, GISDisplay display) {
        if (TextUtils.isEmpty(mText))
            return;

        Paint textPaint = new Paint();
        textPaint.setColor(mTextColor);
        textPaint.setAntiAlias(true);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setStrokeCap(Paint.Cap.ROUND);

        float gap = (float) (1 / display.getScale());
        float innerRadius = radius - gap;
        float textSize = size * innerRadius; // initial text size

        Rect textRect = new Rect();
        textPaint.setTextSize(size);
        textPaint.getTextBounds(mText, 0, mText.length(), textRect);
        textPaint.setTextSize(textSize);

        float halfW = textRect.width() * innerRadius / 2;
        float halfH = textRect.height() * innerRadius / 2;
//        float outerTextRadius = (float) Math.sqrt(halfH * halfH + halfW * halfW);
//        float textScale = innerRadius / outerTextRadius;

        float textX = (float) (pt.getX() - halfW - radius / 2);
        float textY = (float) (pt.getY() + halfH - radius / 2);

        switch (mTextAlignment) {
            case ALIGN_TOP:
                textY = textY - halfH * 2;
                break;
            case ALIGN_TOP_RIGHT:
                textX = textX + halfW * 2 - halfW * .5f;
                textY = textY - halfH * 2;
                break;
            case ALIGN_RIGHT:
                textX = textX + halfW * 2 - halfW * 0.25f;
                break;
            case ALIGN_BOTTOM_RIGHT:
                textX = textX + halfW * 2 - halfW * .5f;
                textY = textY + halfH * 2;
                break;
            case ALIGN_BOTTOM:
                textY = textY + halfH * 2;
                break;
            case ALIGN_BOTTOM_LEFT:
                textX = textX - halfW * 2 + halfW * .5f;
                textY = textY + halfH * 2;
                break;
            case ALIGN_LEFT:
                textX = textX - halfW * 2 + halfW * 0.25f;
                break;
            case ALIGN_TOP_LEFT:
                textX = textX - halfW * 2 + halfW * .5f;
                textY = textY - halfH * 2;
                break;
        }
        Path textPath = new Path();
        textPaint.getTextPath(mText, 0, mText.length(), textX, textY, textPath);
        textPath.close();

        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.setScale(1, -1, (float) pt.getX(), (float) pt.getY());
        textPath.transform(matrix);

        display.drawPath(textPath, textPaint);
    }

    @Override
    public void onDraw(GeoGeometry geoGeometry, GISDisplay display) {
        switch (geoGeometry.getType()) {
            case GTPoint:
                GeoPoint pt = (GeoPoint) geoGeometry;
                onDraw(pt, display);
                break;
            case GTMultiPoint:
                GeoMultiPoint multiPoint = (GeoMultiPoint) geoGeometry;
                for (int i = 0; i < multiPoint.size(); i++) {
                    onDraw(multiPoint.get(i), display);
                }
                break;

            //throw new IllegalArgumentException(
            //        "The input geometry type is not support by this style");
        }
    }

    protected void drawPointMarker(float scaledSize, GeoPoint pt, GISDisplay display) {
        mOutPaint.setColor(mColor);
        mOutPaint.setStrokeWidth(scaledSize);
        display.drawPoint((float) pt.getX(), (float) pt.getY(), mOutPaint);
        mOutPaint.setColor(mOutColor);
    }

    protected void drawCircleMarker(float scaledSize, float width, GeoPoint pt, GISDisplay display) {
        if (scaledSize < 2) {
            mOutPaint.setColor(mColor);
            mOutPaint.setStrokeWidth(scaledSize);
            display.drawCircle((float) pt.getX(), (float) pt.getY(), mOutPaint);
        } else
            display.drawCircle((float) pt.getX(), (float) pt.getY(), scaledSize, mFillPaint);

        mOutPaint.setStrokeWidth(width);
        if (scaledSize >= 2) {
            mOutPaint.setColor(mOutColor);
            display.drawCircle((float) pt.getX(), (float) pt.getY(), scaledSize, mOutPaint);
        }
    }

    protected void drawDiamondMarker(float scaledSize, float width, GeoPoint pt, GISDisplay display) {
        Path path = new Path();
        path.moveTo((float) pt.getX() + scaledSize, (float) pt.getY());
        path.lineTo((float) pt.getX(), (float) pt.getY() + scaledSize);
        path.lineTo((float) pt.getX() - scaledSize, (float) pt.getY());
        path.lineTo((float) pt.getX(), (float) pt.getY() - scaledSize);
        path.close();

        drawPath(width, path, display);
    }

    protected void drawTriangleMarker(float scaledSize, float width, GeoPoint pt, GISDisplay display) {
        Path path = new Path();
        path.moveTo((float) pt.getX() + scaledSize, (float) pt.getY() - scaledSize);
        path.lineTo((float) pt.getX(), (float) pt.getY() + scaledSize);
        path.lineTo((float) pt.getX() - scaledSize, (float) pt.getY() - scaledSize);
        path.close();

        drawPath(width, path, display);
    }

    protected void drawPath(float width, Path path, GISDisplay display) {
        display.drawPath(path, mFillPaint);
        mOutPaint.setStrokeWidth(width);
        display.drawPath(path, mOutPaint);
    }

    protected void drawBoxMarker(float scaledSize, float width, GeoPoint pt, GISDisplay display) {
        display.drawBox((float) pt.getX(), (float) pt.getY(), scaledSize, mFillPaint);
        mOutPaint.setStrokeWidth(width);
        display.drawBox((float) pt.getX(), (float) pt.getY(), scaledSize, mOutPaint);
    }

    protected void drawCrossMarker(float scaledSize, float width, GeoPoint pt, GISDisplay display) {
        mOutPaint.setStrokeWidth(width);
        display.drawCross((float) pt.getX(), (float) pt.getY(), scaledSize, mOutPaint);
    }

    protected void drawCrossedBoxMarker(float scaledSize, float width, GeoPoint pt, GISDisplay display) {
        display.drawBox((float) pt.getX(), (float) pt.getY(), scaledSize, mFillPaint);
        mOutPaint.setStrokeWidth(width);
        display.drawCrossedBox((float) pt.getX(), (float) pt.getY(), scaledSize, mOutPaint);
    }

    public int getType() {
        return mType;
    }

    public void setType(int type) {
        mType = type;
    }

    public float getSize() {
        return mSize;
    }

    public void setSize(float size) {
        mSize = size;
    }

    public float getTextSize() {
        return mTextSize;
    }

    public void setTextSize(float size) {
        mTextSize = size;
    }

    public int getTextAlignment() {
        return mTextAlignment;
    }

    public void setTextAlignment(int alignment) {
        mTextAlignment = alignment;
    }

    public int getTextColor() {
        return mTextColor;
    }

    public void setTextColor(int color) {
        mTextColor = color;
    }

    @Override
    public void setColor(int color) {
        super.setColor(color);
        setPaintsColors();
    }

    public String getField() {
        return mField;
    }

    public void setField(String field) {
        mField = field;
    }

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        if (!TextUtils.isEmpty(text))
            mText = text;
        else
            mText = null;
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject rootConfig = super.toJSON();
        rootConfig.put(JSON_NAME_KEY, "SimpleMarkerStyle");
        rootConfig.put(JSON_TYPE_KEY, mType);
        rootConfig.put(JSON_SIZE_KEY, mSize);
        rootConfig.put(JSON_TEXT_SIZE_KEY, mTextSize);
        rootConfig.put(JSON_TEXT_ALIGN_KEY, mTextAlignment);
        rootConfig.put(JSON_TEXT_COLOR_KEY, mTextColor);

        if (null != mText) {
            rootConfig.put(JSON_DISPLAY_NAME, mText);
        }
        if (null != mField) {
            rootConfig.put(JSON_VALUE_KEY, mField);
        }

        return rootConfig;
    }

    @Override
    public void fromJSON(JSONObject jsonObject) throws JSONException {
        super.fromJSON(jsonObject);
        mType = jsonObject.getInt(JSON_TYPE_KEY);
        mSize = (float) jsonObject.getDouble(JSON_SIZE_KEY);
        mTextSize = (float) jsonObject.getDouble(JSON_TEXT_SIZE_KEY);
        mTextAlignment = jsonObject.getInt(JSON_TEXT_ALIGN_KEY);
        mTextColor = jsonObject.getInt(JSON_TEXT_COLOR_KEY);

        if (jsonObject.has(JSON_DISPLAY_NAME)) {
            mText = jsonObject.getString(JSON_DISPLAY_NAME);
        }
        if (jsonObject.has(JSON_VALUE_KEY)) {
            mField = jsonObject.getString(JSON_VALUE_KEY);
        }

        setPaintsColors();
    }
}
