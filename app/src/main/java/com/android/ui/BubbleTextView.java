package com.android.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.TextView;

import com.android.data.AppInfo;

import com.android.asustore.R;
import com.android.data.AppStoreSettings;

/**
 * TextView that draws a bubble behind the text. We cannot use a LineBackgroundSpan
 * because we want to make the bubble taller than the text and TextView's clip is
 * too aggressive.
 */
public class BubbleTextView extends TextView {

    private static SparseArray<Theme> sPreloaderThemes = new SparseArray<Theme>(2);

    private static final float SHADOW_LARGE_RADIUS = 4.0f;
    private static final float SHADOW_SMALL_RADIUS = 1.75f;
    private static final float SHADOW_Y_OFFSET = 2.0f;
    private static final int SHADOW_LARGE_COLOUR = 0xDD000000;
    private static final int SHADOW_SMALL_COLOUR = 0xCC000000;

    private static final int DISPLAY_WORKSPACE = 0;
    private static final int DISPLAY_ALL_APPS = 1;

    private Drawable mIcon;
    private final Drawable mBackground;

    private boolean mBackgroundSizeChanged;

    private Bitmap mPressedBackground;

    private float mSlop;

    private final boolean mDeferShadowGenerationOnTouch;
    private final boolean mCustomShadowsEnabled;
    private final boolean mLayoutHorizontal;
    private final int mIconSize;
    private int mTextColor;

    private boolean mStayPressed;
    private boolean mIgnorePressedStateChange;
    private boolean mDisableRelayout = false;


    public BubbleTextView(Context context) {
        this(context, null, 0);
    }

    public BubbleTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.BubbleTextView, defStyle, 0);
        mCustomShadowsEnabled = a.getBoolean(R.styleable.BubbleTextView_customShadows, true);
        mLayoutHorizontal = a.getBoolean(R.styleable.BubbleTextView_layoutHorizontal, false);
        mDeferShadowGenerationOnTouch =
                a.getBoolean(R.styleable.BubbleTextView_deferShadowGeneration, false);

        int display = a.getInteger(R.styleable.BubbleTextView_iconDisplay, DISPLAY_WORKSPACE);
        int defaultIconSize = 96;
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.4f);

        mIconSize = a.getDimensionPixelSize(R.styleable.BubbleTextView_iconSizeOverride,
                defaultIconSize);

        a.recycle();

        if (mCustomShadowsEnabled) {
            // Draw the background itself as the parent is drawn twice.
            mBackground = getBackground();
            setBackground(null);
        } else {
            mBackground = null;
        }

        if (mCustomShadowsEnabled) {
            setShadowLayer(SHADOW_LARGE_RADIUS, 0.0f, SHADOW_Y_OFFSET, SHADOW_LARGE_COLOUR);
        }
    }
    public FastBitmapDrawable createIconDrawable(Bitmap icon) {
        FastBitmapDrawable d = new FastBitmapDrawable(icon);
        d.setFilterBitmap(true);
        resizeIconDrawable(d);
        return d;
    }
    public Drawable resizeIconDrawable(Drawable icon) {
        icon.setBounds(0, 0, 80, 80);
        return icon;
    }
    public void applyFromApplicationInfo(AppInfo info) {
        FastBitmapDrawable iconDrawable = createIconDrawable(info.iconBitmap);
        if (info.isDisabled()) {
            iconDrawable.setState(FastBitmapDrawable.State.DISABLED);
        }
        setIcon(iconDrawable, mIconSize);
        if (info.status == AppStoreSettings.APKs.STATUS_TYPE_DOWNLOADING) {
            setText(R.string.downloading);
        } else if (info.status == AppStoreSettings.APKs.STATUS_TYPE_DOWNLOADED){
            setText(R.string.install);
        } else {
            setText(info.title);
        }
        if (info.contentDescription != null) {
            setContentDescription(info.contentDescription);
        }
        // We don't need to check the info since it's not a ShortcutInfo
        setTag(info);

        // Verify high res immediately
        verifyHighRes();
    }

    @Override
    protected boolean setFrame(int left, int top, int right, int bottom) {
        if (getLeft() != left || getRight() != right || getTop() != top || getBottom() != bottom) {
            mBackgroundSizeChanged = true;
        }
        return super.setFrame(left, top, right, bottom);
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == mBackground || super.verifyDrawable(who);
    }

    @Override
    public void setTag(Object tag) {
        super.setTag(tag);
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);

        if (!mIgnorePressedStateChange) {
            updateIconState();
        }
    }

    /** Returns the icon for this view. */
    public Drawable getIcon() {
        return mIcon;
    }

    /** Returns whether the layout is horizontal. */
    public boolean isLayoutHorizontal() {
        return mLayoutHorizontal;
    }

    private void updateIconState() {
        if (mIcon instanceof FastBitmapDrawable) {
            FastBitmapDrawable d = (FastBitmapDrawable) mIcon;
            if (getTag() instanceof AppInfo
                    && ((AppInfo) getTag()).isDisabled()) {
                d.animateState(FastBitmapDrawable.State.DISABLED);
            } else if (isPressed() || mStayPressed) {
                d.animateState(FastBitmapDrawable.State.PRESSED);
            } else {
                d.animateState(FastBitmapDrawable.State.NORMAL);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Call the superclass onTouchEvent first, because sometimes it changes the state to
        // isPressed() on an ACTION_UP
        boolean result = super.onTouchEvent(event);


        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // So that the pressed outline is visible immediately on setStayPressed(),
                // we pre-create it on ACTION_DOWN (it takes a small but perceptible amount of time
                // to create it)
                /*if (!mDeferShadowGenerationOnTouch && mPressedBackground == null) {
                    mPressedBackground = mOutlineHelper.createMediumDropShadow(this);
                }*/

                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                // If we've touched down and up on an item, and it's still not "pressed", then
                // destroy the pressed outline
                if (!isPressed()) {
                    mPressedBackground = null;
                }

                break;
            case MotionEvent.ACTION_MOVE:
                break;
        }
        return result;
    }


    @Override
    public void draw(Canvas canvas) {
        if (!mCustomShadowsEnabled) {
            super.draw(canvas);
            return;
        }

        final Drawable background = mBackground;
        if (background != null) {
            final int scrollX = getScrollX();
            final int scrollY = getScrollY();

            if (mBackgroundSizeChanged) {
                background.setBounds(0, 0,  getRight() - getLeft(), getBottom() - getTop());
                mBackgroundSizeChanged = false;
            }

            if ((scrollX | scrollY) == 0) {
                background.draw(canvas);
            } else {
                canvas.translate(scrollX, scrollY);
                background.draw(canvas);
                canvas.translate(-scrollX, -scrollY);
            }
        }

        // If text is transparent, don't draw any shadow
        if (getCurrentTextColor() == getResources().getColor(android.R.color.transparent)) {
            getPaint().clearShadowLayer();
            super.draw(canvas);
            return;
        }

        // We enhance the shadow by drawing the shadow twice
        getPaint().setShadowLayer(SHADOW_LARGE_RADIUS, 0.0f, SHADOW_Y_OFFSET, SHADOW_LARGE_COLOUR);
        super.draw(canvas);
        canvas.save(Canvas.CLIP_SAVE_FLAG);
        canvas.clipRect(getScrollX(), getScrollY() + getExtendedPaddingTop(),
                getScrollX() + getWidth(),
                getScrollY() + getHeight(), Region.Op.INTERSECT);
        getPaint().setShadowLayer(SHADOW_SMALL_RADIUS, 0.0f, 0.0f, SHADOW_SMALL_COLOUR);
        super.draw(canvas);
        canvas.restore();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (mBackground != null) mBackground.setCallback(this);

        if (mIcon instanceof PreloadIconDrawable) {
            ((PreloadIconDrawable) mIcon).applyPreloaderTheme(getPreloaderTheme());
        }
        mSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mBackground != null) mBackground.setCallback(null);
    }

    @Override
    public void setTextColor(int color) {
        mTextColor = color;
        super.setTextColor(color);
    }

    @Override
    public void setTextColor(ColorStateList colors) {
        mTextColor = colors.getDefaultColor();
        super.setTextColor(colors);
    }

    public void setTextVisibility(boolean visible) {
        Resources res = getResources();
        if (visible) {
            super.setTextColor(mTextColor);
        } else {
            super.setTextColor(res.getColor(android.R.color.transparent));
        }
    }



    private Theme getPreloaderTheme() {
        Object tag = getTag();
        int style = R.style.PreloadIcon;
        Theme theme = sPreloaderThemes.get(style);
        if (theme == null) {
            theme = getResources().newTheme();
            theme.applyStyle(style, true);
            sPreloaderThemes.put(style, theme);
        }
        return theme;
    }

    /**
     * Sets the icon for this view based on the layout direction.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private Drawable setIcon(Drawable icon, int iconSize) {
        mIcon = icon;
        if (iconSize != -1) {
            mIcon.setBounds(0, 0, iconSize, iconSize);
        }
        if (mLayoutHorizontal) {
            setCompoundDrawablesRelative(mIcon, null, null, null);
        } else {
            setCompoundDrawables(null, mIcon, null, null);
        }
        return icon;
    }

    @Override
    public void requestLayout() {
        if (!mDisableRelayout) {
            super.requestLayout();
        }
    }

    /**
     * Verifies that the current icon is high-res otherwise posts a request to load the icon.
     */
    public void verifyHighRes() {
        /*if (mIconLoadRequest != null) {
            mIconLoadRequest.cancel();
            mIconLoadRequest = null;
        }
        if (getTag() instanceof AppInfo) {
            AppInfo info = (AppInfo) getTag();
            if (info.usingLowResIcon) {
                mIconLoadRequest = LauncherAppState.getInstance().getIconCache()
                        .updateIconInBackground(BubbleTextView.this, info);
            }
        } else if (getTag() instanceof ShortcutInfo) {
            ShortcutInfo info = (ShortcutInfo) getTag();
            if (info.usingLowResIcon) {
                mIconLoadRequest = LauncherAppState.getInstance().getIconCache()
                        .updateIconInBackground(BubbleTextView.this, info);
            }
        } else if (getTag() instanceof PackageItemInfo) {
            PackageItemInfo info = (PackageItemInfo) getTag();
            if (info.usingLowResIcon) {
                mIconLoadRequest = LauncherAppState.getInstance().getIconCache()
                        .updateIconInBackground(BubbleTextView.this, info);
            }
        }*/
    }

    /**
     * Returns the start delay when animating between certain {@link FastBitmapDrawable} states.
     */
    private static int getStartDelayForStateChange(final FastBitmapDrawable.State fromState,
                                                   final FastBitmapDrawable.State toState) {
        switch (toState) {
            case NORMAL:
                switch (fromState) {
                    case FAST_SCROLL_HIGHLIGHTED:
                        return FastBitmapDrawable.FAST_SCROLL_INACTIVE_DURATION / 4;
                }
        }
        return 0;
    }

    /**
     * Interface to be implemented by the grand parent to allow click shadow effect.
     */
    public interface BubbleTextShadowHandler {
        void setPressedIcon(BubbleTextView icon, Bitmap background);
    }
}
