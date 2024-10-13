package org.telegram.ui.Components.QuickShare;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

public class QuickShareBtnAttributes {
    public Paint paint = new Paint();
    public RectF rectF = new RectF();
    public int radius;
    public Drawable icon;
    public int buttonParentTopY;
    public boolean drawDisabled;
    public int maxParentX;
    public int maxParentY;
    public boolean bottomDirection = false;
    public Bitmap shaderBitmap;
    public float shaderOffset;
}
