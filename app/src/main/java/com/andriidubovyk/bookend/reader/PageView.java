package com.andriidubovyk.bookend.reader;

import com.artifex.mupdf.fitz.*;
import com.artifex.mupdf.fitz.android.AndroidDrawDevice;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Scroller;

import androidx.annotation.RequiresApi;

public class PageView extends View implements
	GestureDetector.OnGestureListener,
	ScaleGestureDetector.OnScaleGestureListener
{
	protected DocumentActivity actionListener;

	protected float viewScale;
	protected Bitmap bitmap;
	protected int bitmapW, bitmapH;
	protected int canvasW, canvasH;
	protected int scrollX, scrollY;
	protected Link[] links;
	protected Quad[][] hits;
	protected boolean showLinks = true; // links enabled by default

	protected GestureDetector detector;
	protected ScaleGestureDetector scaleDetector;
	protected Scroller scroller;
	protected boolean error;
	protected Paint errorPaint;
	protected Path errorPath;
	protected Paint linkPaint;
	protected Paint searchHitPaint;
	private int pageNumber = -1;

	private static final float TURN_PAGE_SCREEN_EDGE = 0f;
	private static final float MIN_SCALE = 1;
	private static final float MAX_SCALE = 8;

	public static final int BACKGROUND_COLOR = 0xFFf5dcbd;
	private static final int INK_COLOR = 0xFF58472c;

	private static final int ERROR_PAINT_COLOR = 0xffff5050;
	private static final int SEARCH_HIT_PAINT_COLOR = 0x30ff0000;
	private static final int LINK_PAINT_COLOR = 0x00ffffff; // no background color for active links


	public PageView(Context ctx, AttributeSet atts) {
		super(ctx, atts);
		setBackgroundColor(BACKGROUND_COLOR);
		scroller = new Scroller(ctx);
		detector = new GestureDetector(ctx, this);
		scaleDetector = new ScaleGestureDetector(ctx, this);

		viewScale = 1;

		linkPaint = new Paint();
		linkPaint.setColor(LINK_PAINT_COLOR);

		searchHitPaint = new Paint();
		searchHitPaint.setColor(SEARCH_HIT_PAINT_COLOR);
		searchHitPaint.setStyle(Paint.Style.FILL);

		errorPaint = new Paint();
		errorPaint.setColor(ERROR_PAINT_COLOR);
		errorPaint.setStrokeWidth(5);
		errorPaint.setStyle(Paint.Style.STROKE);

		errorPath = new Path();
		errorPath.moveTo(-100, -100);
		errorPath.lineTo(100, 100);
		errorPath.moveTo(100, -100);
		errorPath.lineTo(-100, 100);
	}



	public void setActionListener(DocumentActivity l) {
		actionListener = l;
	}

	public void setError() {
		if (bitmap != null)
			bitmap.recycle();
		error = true;
		links = null;
		hits = null;
		bitmap = null;
		invalidate();
	}

	private void setBitmap(Bitmap b, Link[] ls, Quad[][] hs) {
		if (bitmap != null)
			bitmap.recycle();
		error = false;
		links = ls;
		hits = hs;
		bitmap = b;
		bitmapW = (int)(bitmap.getWidth() * viewScale);
		bitmapH = (int)(bitmap.getHeight() * viewScale);
		scroller.forceFinished(true);
	}

	private boolean isActivePage() {
		return this.pageNumber==actionListener.readerView.getCurrentItem();
	}

	protected void setPage(int pageNumber) {
		this.pageNumber = pageNumber;
		actionListener.setStopSearch(true);
		actionListener.getWorker().add(new Worker.Task() {
			public Bitmap bitmap;
			public Link[] links;
			public Quad[][] hits;
			public void work() {
				try {
					Log.i(DocumentActivity.APP, "load page " + pageNumber);
					Page page = actionListener.doc.loadPage(pageNumber);
					Log.i(DocumentActivity.APP, "draw page " + pageNumber + " zoom=" + actionListener.getCommonZoom());
					Matrix ctm;
					ctm = AndroidDrawDevice.fitPageWidth(page, actionListener.getCanvasW());
					links = page.getLinks();
					if (links != null)
						for (Link link : links)
							link.bounds.transform(ctm);
					if (actionListener.getSearchNeedle() != null) {
						hits = page.search(actionListener.getSearchNeedle());
						if (hits != null)
							for (Quad[] hit : hits)
								for (Quad chr : hit)
									chr.transform(ctm);
					}
					Pixmap pixmap = page.toPixmap(ctm, ColorSpace.DeviceBGR, true);
					pixmap.tint(INK_COLOR, BACKGROUND_COLOR);
					bitmap = pixmapToBitmap(pixmap);

				} catch (Throwable x) {
					Log.i(DocumentActivity.APP, x.getMessage());
				}
			}
			public void run() {
				if (bitmap != null) {
					setBitmap(bitmap, links, hits);
					setPageZoom(actionListener.getCommonZoom(), false);
					setPageScroll(actionListener.readerView.currentPageScrollX, actionListener.readerView.currentPageScrollY, false);
					invalidate();
				} else {
					setError();
				}
			}
		});
	}

	private Bitmap pixmapToBitmap(Pixmap src) {
		if(src == null) return null;
		int width = src.getWidth();
		int height = src.getHeight();
		Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		result.setPixels(src.getPixels(), 0, width, 0, 0, width, height);
		return result;
	}

	public void onSizeChanged(int w, int h, int ow, int oh) {
		canvasW = w;
		canvasH = h;
	}

	@SuppressLint("ClickableViewAccessibility")
	public boolean onTouchEvent(MotionEvent event) {
		detector.onTouchEvent(event);
		scaleDetector.onTouchEvent(event);
		return true;
	}

	public boolean onDown(MotionEvent e) {
		scroller.forceFinished(true);
		return true;
	}


	public void onShowPress(MotionEvent e) { }

	public void onLongPress(MotionEvent e) {

		invalidate();
	}

	@RequiresApi(api = Build.VERSION_CODES.N)
	public boolean onSingleTapUp(MotionEvent e) {
		actionListener.manageFragmentTransaction(DocumentActivity.FragmentsState.NONE);
		boolean foundLink = false;
		float x = e.getX();
		float y = e.getY();
		if (showLinks && links != null) {
			float dx = (bitmapW <= canvasW) ? (bitmapW - canvasW) / 2f : scrollX;
			float dy = (bitmapH <= canvasH) ? (bitmapH - canvasH) / 2f : scrollY;
			float mx = (x + dx) / viewScale;
			float my = (y + dy) / viewScale;
			for (Link link : links) {
				Rect b = link.bounds;
				if (mx >= b.x0 && mx <= b.x1 && my >= b.y0 && my <= b.y1) {
					if (link.isExternal() && actionListener != null)
						actionListener.gotoURI(link.uri);
					else if (actionListener != null)
						actionListener.gotoPage(link.uri);
					foundLink = true;
					break;
				}
			}
		}
		if (!foundLink) {

			float a = canvasW * TURN_PAGE_SCREEN_EDGE;
			float b = canvasW - canvasW*TURN_PAGE_SCREEN_EDGE;
			if (x < a) actionListener.goBackward();
			if (x > b) actionListener.goForward();
			if (x > a && x < b && actionListener != null) actionListener.toggleUI();
		}
		invalidate();
		return true;
	}

	public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
		if (bitmap != null) {
			scrollX += (int)dx;
			scrollY += (int)dy;
			scroller.forceFinished(true);
			invalidate();
		}
		return true;
	}

	public boolean onFling(MotionEvent e1, MotionEvent e2, float dx, float dy) {
		if (bitmap != null) {
			int maxX = bitmapW > canvasW ? bitmapW - canvasW : 0;
			int maxY = bitmapH > canvasH ? bitmapH - canvasH : 0;
			scroller.forceFinished(true);
			scroller.fling(scrollX, scrollY, (int)-dx, (int)-dy, 0, maxX, 0, maxY);
			invalidate();
			if(isActivePage()) controlPaging();
		}
		return true;
	}

	public void setPageScroll(int scrollX, int scrollY, boolean invalidate) {
		this.scrollX = scrollX;
		this.scrollY = scrollY;
		if(invalidate) invalidate();
	}

	public void setPageScroll(int scrollX, int scrollY) {
		setPageScroll(scrollX, scrollY, true);
	}


	public void controlPaging() {
		if(viewScale <= 1f) {
			actionListener.readerView.setLeftPageScroll(bitmapW-canvasW, scrollY);
			actionListener.readerView.setRightPageScroll(0, scrollY);
			actionListener.readerView.setAllowedSwipeDirection(ReaderView.SwipeDirection.ALL);
		}else if(scrollX<=0) {
			actionListener.readerView.setLeftPageScroll(bitmapW-canvasW, scrollY);
			actionListener.readerView.setAllowedSwipeDirection(ReaderView.SwipeDirection.LEFT);
		} else if (scrollX+canvasW>=bitmapW) {
			actionListener.readerView.setRightPageScroll(0, scrollY);
			actionListener.readerView.setAllowedSwipeDirection(ReaderView.SwipeDirection.RIGHT);
		} else  {
			actionListener.readerView.setAllowedSwipeDirection(ReaderView.SwipeDirection.NONE);
		}
		actionListener.readerView.setCurrentPageScroll(scrollX, scrollY);
	}


	public boolean onScaleBegin(ScaleGestureDetector det) {
		return true;
	}

	public boolean onScale(ScaleGestureDetector det) {
		if (bitmap != null) {
			float scaleFactor = det.getScaleFactor();
			float newViewScale = viewScale * scaleFactor;
			if (newViewScale < MIN_SCALE) newViewScale = MIN_SCALE;
			if (newViewScale > MAX_SCALE) newViewScale = MAX_SCALE;
			setPageZoom(newViewScale);
			invalidate();
		}
		return true;
	}

	public void setPageZoom(float scale, boolean invalidate) {
		if(viewScale==scale) return;
		float pageFocusX = (scaleDetector.getFocusX()+ scrollX) / viewScale;
		float pageFocusY = (scaleDetector.getFocusY() + scrollY) / viewScale;
		viewScale = scale;
		bitmapW = (int)(bitmap.getWidth() * viewScale);
		bitmapH = (int)(bitmap.getHeight() * viewScale);
		scrollX = (int)(pageFocusX * viewScale - scaleDetector.getFocusX());
		scrollY = (int)(pageFocusY * viewScale - scaleDetector.getFocusY());
		scroller.forceFinished(true);
		if(invalidate) invalidate();
	}

	public void setPageZoom(float scale) {
		setPageZoom(scale, true);
	}


	public void onScaleEnd(ScaleGestureDetector det) {
		if (actionListener != null && isActivePage()) {
			controlPaging();
			actionListener.onPageViewZoomChanged(viewScale);
		}

	}


	private final android.graphics.Rect dst = new android.graphics.Rect();
	private final Path path = new Path();

	public void onDraw(Canvas canvas) {
		int x, y;

		if (bitmap == null) {
			if (error) {
				canvas.translate(canvasW / 2f, canvasH / 2f);
				canvas.drawPath(errorPath, errorPaint);
			}
			return;
		}

		if (scroller.computeScrollOffset()) {
			scrollX = scroller.getCurrX();
			scrollY = scroller.getCurrY();
			invalidate(); /* keep animating */
		}

		if (bitmapW <= canvasW) {
			scrollX = 0;
			x = (canvasW - bitmapW) / 2;
		} else {
			if (scrollX < 0) scrollX = 0;
			if (scrollX > bitmapW - canvasW) scrollX = bitmapW - canvasW;
			x = -scrollX;
		}

		if (bitmapH <= canvasH) {
			scrollY = 0;
			y = (canvasH - bitmapH) / 2;
		} else {
			if (scrollY < 0) scrollY = 0;
			if (scrollY > bitmapH - canvasH) scrollY = bitmapH - canvasH;
			y = -scrollY;
		}

		// Draw main page content
		x=x+actionListener.settingsManager.leftMargin-actionListener.settingsManager.rightMargin;
		y=y+actionListener.settingsManager.topMargin-actionListener.settingsManager.botMargin;
		dst.set(x, y, x + bitmapW, y + bitmapH);
		canvas.drawBitmap(bitmap, null, dst, null);

		// Draw links background
		if (showLinks && links != null && links.length > 0) {
			for (Link link : links) {
				Rect b = link.bounds;
				canvas.drawRect(
					x + b.x0 * viewScale,
					y + b.y0 * viewScale,
					x + b.x1 * viewScale,
					y + b.y1 * viewScale,
					linkPaint
				);
			}
		}

		// Draw search hits
		if (hits != null && hits.length > 0) {
			for (Quad[] h : hits)
				for (Quad q : h) {
					path.rewind();
					path.moveTo(x + q.ul_x * viewScale, y + q.ul_y * viewScale);
					path.lineTo(x + q.ll_x * viewScale, y + q.ll_y * viewScale);
					path.lineTo(x + q.lr_x * viewScale, y + q.lr_y * viewScale);
					path.lineTo(x + q.ur_x * viewScale, y + q.ur_y * viewScale);
					path.close();
					canvas.drawPath(path, searchHitPaint);
				}
		}

	}
}
