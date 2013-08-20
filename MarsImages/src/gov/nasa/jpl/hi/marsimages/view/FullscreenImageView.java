package gov.nasa.jpl.hi.marsimages.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

/**
 * Touch-enabled full-screen interactive image view. Drag one finger to pan,
 * pinch to zoom. Supports red-blue stereo anaglyph mode for 3D viewing. Based
 * originally on Android TouchExample. TODO it would be cool if a two-finger
 * drag could also move the view
 */
public class FullscreenImageView extends View {
	private static final int INVALID_POINTER_ID = -1;
	public static final String INTENT_ACTION_TOUCH_VIEW = "gov.nasa.jpl.hi.marsimages.view.TOUCH_VIEW";

	private static final ColorMatrix redMatrix = new ColorMatrix(new float[] {
			1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
			0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f });
	private static final ColorMatrixColorFilter redFilter = new ColorMatrixColorFilter(
			redMatrix);
	private static final ColorMatrix blueMatrix = new ColorMatrix(new float[] {
			0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f,
			0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f });
	private static final ColorMatrixColorFilter blueFilter = new ColorMatrixColorFilter(
			blueMatrix);

	private BitmapDrawable image1;
	private BitmapDrawable image2;
	private BitmapDrawable image3D;
	private ScaleGestureDetector mScaleDetector;
	private PorterDuffXfermode mXferMode = new PorterDuffXfermode(Mode.LIGHTEN);
	private float mScaleFactor = 1.0f;
	private float mPosX;
	private float mPosY;
	private float mLastTouchX;
	private float mLastTouchY;
	private int mActivePointerId = INVALID_POINTER_ID;
	private Paint paint = new Paint();
	private Rect newImageBoundsRect = new Rect();

	private int mImagePadding = 0;

	private boolean mUpdate3DImage = false;

	public FullscreenImageView(Context context) {
		this(context, null, 0);
	}

	public FullscreenImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public FullscreenImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
	}

	public void setImage(Bitmap image) {
		mUpdate3DImage = true;
		if (image == null) {
			image1 = null;
			return;
		}
		image1 = new BitmapDrawable(getContext().getResources(), image);
		image1.setBounds(0, 0, image.getWidth(), image.getHeight());
	}

	public void setImages(Bitmap leftImage, Bitmap rightImage) {
		mUpdate3DImage = true;

		if (leftImage == null) {
			image1 = null;
		} else {
			image1 = new BitmapDrawable(getContext().getResources(), leftImage);
			image1.setBounds(0, 0, leftImage.getWidth(), leftImage.getHeight());
		}

		if (rightImage == null) {
			image2 = null;
		} else {
			image2 = new BitmapDrawable(getContext().getResources(), rightImage);
			image2.setBounds(0, 0, rightImage.getWidth(),
					rightImage.getHeight());
		}
	}

	public void setImage2(Bitmap rightImage) {
		mUpdate3DImage = true;

		if (rightImage == null) {
			image2 = null;
			return;
		}
		image2 = new BitmapDrawable(getContext().getResources(), rightImage);
		image2.setBounds(0, 0, rightImage.getWidth(), rightImage.getHeight());
	}

	public BitmapDrawable getImage2() {
		return image2;
	}

	public Bitmap overlayImages(BitmapDrawable left, BitmapDrawable right) {
		Bitmap bmOverlay = Bitmap.createBitmap(left.getBitmap().getWidth(),
				left.getBitmap().getHeight(), left.getBitmap().getConfig());

		Canvas canvas = new Canvas(bmOverlay);

		paint.setColorFilter(redFilter);
		canvas.drawBitmap(
				left.getBitmap().copy(left.getBitmap().getConfig(), true), 0,
				0, paint);

		paint.setColorFilter(blueFilter);
		paint.setXfermode(mXferMode);

		right.setAlpha(100);
		canvas.drawBitmap(
				right.getBitmap().copy(right.getBitmap().getConfig(), true), 0,
				0, paint);

		return bmOverlay;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		// Let the ScaleGestureDetector inspect all events.
		mScaleDetector.onTouchEvent(ev);
		final int action = ev.getAction();
		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN: {
			final float x = ev.getX();
			final float y = ev.getY();

			mLastTouchX = x;
			mLastTouchY = y;
			mActivePointerId = ev.getPointerId(0);

			break;
		}

		case MotionEvent.ACTION_MOVE: {
			if (image1 == null)
				break;

			final int pointerIndex = ev.findPointerIndex(mActivePointerId);
			final float x = ev.getX(pointerIndex);
			final float y = ev.getY(pointerIndex);
			// Only move if the ScaleGestureDetector isn't processing a gesture.
			if (!mScaleDetector.isInProgress()) {
				float dx = x - mLastTouchX;
				float dy = y - mLastTouchY;

				if ((((newImageBoundsRect.left >= mImagePadding)
						&& (newImageBoundsRect.left + dx) < mImagePadding && dx < 0) && (newImageBoundsRect.right <= getWidth()))
						|| ((newImageBoundsRect.left + dx) > mImagePadding && dx > 0)
						&& (newImageBoundsRect.right > getWidth())) {
					dx = mImagePadding - newImageBoundsRect.left;
				} else if ((((newImageBoundsRect.right <= (getWidth() - mImagePadding))
						&& (newImageBoundsRect.right + dx) > (getWidth() - mImagePadding) && dx > 0) && (newImageBoundsRect.left >= mImagePadding))
						|| ((newImageBoundsRect.right + dx) < (getWidth() - mImagePadding) && dx < 0)
						&& (newImageBoundsRect.left < mImagePadding)) {
					dx = (getWidth() - mImagePadding)
							- newImageBoundsRect.right;
				}

				if ((((newImageBoundsRect.top >= mImagePadding)
						&& (newImageBoundsRect.top + dy) < mImagePadding && dy < 0) && (newImageBoundsRect.bottom <= getHeight()))
						|| ((newImageBoundsRect.top + dy) > mImagePadding && dy > 0)
						&& (newImageBoundsRect.bottom > getHeight())) {
					dy = mImagePadding - newImageBoundsRect.top;
				} else if ((((newImageBoundsRect.bottom <= (getHeight() - mImagePadding))
						&& (newImageBoundsRect.bottom + dy) > (getHeight() - mImagePadding) && dy > 0) && (newImageBoundsRect.top >= mImagePadding))
						|| ((newImageBoundsRect.bottom + dy) < (getHeight() - mImagePadding) && dy < 0)
						&& (newImageBoundsRect.top < mImagePadding)) {
					dy = (getHeight() - mImagePadding)
							- newImageBoundsRect.bottom;
				}

				// Check whether this move pushed the image too far off the
				// screen before moving it
				newImageBoundsRect.set(image1.getBounds());
				// Log.d(VIEW_LOG_TAG, "image bounds: "+image1.getBounds());
				newImageBoundsRect.offset((int) (mPosX + dx),
						(int) (mPosY + dy));
				newImageBoundsRect
						.set(newImageBoundsRect.left,
								newImageBoundsRect.top,
								(int) ((newImageBoundsRect.right - newImageBoundsRect.left)
										* mScaleFactor + newImageBoundsRect.left),
								(int) ((newImageBoundsRect.bottom - newImageBoundsRect.top)
										* mScaleFactor + newImageBoundsRect.top));

				mPosX += dx;
				mPosY += dy;

				invalidate();
			}

			mLastTouchX = x;
			mLastTouchY = y;

			break;
		}

		case MotionEvent.ACTION_UP: {
			mActivePointerId = INVALID_POINTER_ID;
			break;
		}

		case MotionEvent.ACTION_CANCEL: {
			mActivePointerId = INVALID_POINTER_ID;
			break;
		}

		case MotionEvent.ACTION_POINTER_UP: {
			final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
			final int pointerId = ev.getPointerId(pointerIndex);
			if (pointerId == mActivePointerId) {
				// This was our active pointer going up. Choose a new
				// active pointer and adjust accordingly.
				final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
				mLastTouchX = ev.getX(newPointerIndex);
				mLastTouchY = ev.getY(newPointerIndex);
				mActivePointerId = ev.getPointerId(newPointerIndex);
			}
			break;
		}
		}

		return true;
	}

	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (image1 == null) {
			return;
		}

		canvas.save();

		if (image1 != null && image2 != null && mUpdate3DImage) {
			mUpdate3DImage = false;

			image3D = new BitmapDrawable(getContext().getResources(),
					overlayImages(image1, image2).copy(Config.ARGB_8888, false));
		}
		System.out.println(mScaleFactor);
		canvas.translate(mPosX, mPosY);
		canvas.scale(mScaleFactor, mScaleFactor);

		if (image2 == null) {
			// draw 2D
			image1.setColorFilter(null);
			canvas.drawBitmap(image1.getBitmap(), 0, 0, null);
		} else {
			// draw 3D
			image3D.setColorFilter(null);
			canvas.drawBitmap(image3D.getBitmap(), 0, 0, null);
		}
		canvas.restore();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		if (image1 == null) {
			return;
		}

		float width = MeasureSpec.getSize(widthMeasureSpec);
		float height = MeasureSpec.getSize(heightMeasureSpec);
		int imageWidth = image1.getBitmap().getWidth();
		int imageHeight = image1.getBitmap().getHeight();
		if (imageWidth > width || imageHeight > height) { // scale down to fit
															// screen
			mScaleFactor = Math.min(width / imageWidth, height / imageHeight);
		}
		if (mScaleFactor * imageWidth < width) { // center horizontally
			mPosX = (width - mScaleFactor * imageWidth) / 2;
		}
		if (mScaleFactor * imageHeight < height) { // center vertically
			mPosY = (height - mScaleFactor * imageHeight) / 2;
		}
	}

	private class ScaleListener extends	ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			float oldScaleFactor = mScaleFactor;
			mScaleFactor *= detector.getScaleFactor();

			// Don't let the object get too small or too large.
			mScaleFactor = Math.max(.5f, Math.min(mScaleFactor, 5.0f));
			float deltaScale = mScaleFactor / oldScaleFactor;

			float focusX = detector.getFocusX();
			float focusY = detector.getFocusY();
			mPosX = (mPosX - focusX) * deltaScale + focusX;
			mPosY = (mPosY - focusY) * deltaScale + focusY;

			invalidate();
			return true;
		}
	}
}