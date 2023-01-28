package com.artifex.mupdf.mini;

import com.artifex.mupdf.fitz.*;
import com.artifex.mupdf.fitz.android.AndroidDrawDevice;

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

import java.nio.charset.StandardCharsets;

public class PageView extends View implements
	GestureDetector.OnGestureListener,
	ScaleGestureDetector.OnScaleGestureListener
{
	protected DocumentActivity actionListener;

	protected float pageScale, viewScale, minScale, maxScale;
	protected Bitmap bitmap;
	protected int bitmapW, bitmapH;
	protected int canvasW, canvasH;
	protected int scrollX, scrollY;
	protected Link[] links;
	protected Quad[][] hits;
	protected boolean showLinks;

	protected GestureDetector detector;
	protected ScaleGestureDetector scaleDetector;
	protected Scroller scroller;
	protected boolean error;
	protected Paint errorPaint;
	protected Path errorPath;
	protected Paint linkPaint;
	protected Paint searchHitPaint;

	private static int BACKGROUND_COLOR = 0xFFE7DDD0;
	private static int INK_COLOR = 0xFF302B25;

	private static int ERROR_PAINT_COLOR = 0xffff5050;
	private static int SEARCH_HIT_PAINT_COLOR = 0x20ff0000;
	private static int LINK_PAINT_COLOR = 0x200000ff;

	public PageView(Context ctx, AttributeSet atts) {
		super(ctx, atts);
		setBackgroundColor(BACKGROUND_COLOR);
		scroller = new Scroller(ctx);
		detector = new GestureDetector(ctx, this);
		scaleDetector = new ScaleGestureDetector(ctx, this);

		pageScale = 1;
		viewScale = 1;
		minScale = 1;
		maxScale = 2;

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

	private void setBitmap(Bitmap b, float zoom, boolean wentBack, Link[] ls, Quad[][] hs) {
		if (bitmap != null)
			bitmap.recycle();
		error = false;
		links = ls;
		hits = hs;
		bitmap = b;
		bitmapW = (int)(bitmap.getWidth() * viewScale / zoom);
		bitmapH = (int)(bitmap.getHeight() * viewScale / zoom);
		scroller.forceFinished(true);
		if (pageScale == zoom) {
			scrollX = wentBack ? bitmapW - canvasW : 0;
			scrollY = wentBack ? bitmapH - canvasH : 0;
		}
		pageScale = zoom;
		invalidate();
	}

	protected void setPage(int pageNumber) {
		final float zoom = actionListener.getPageZoom();
		actionListener.setStopSearch(true);
		actionListener.getWorker().add(new Worker.Task() {
			public Bitmap bitmap;
			public Link[] links;
			public Quad[][] hits;
			public void work() {
				try {
					Log.i(DocumentActivity.APP, "load page " + pageNumber);
					Page page = DocumentActivity.doc.loadPage(pageNumber);
					Log.i(DocumentActivity.APP, "draw page " + pageNumber + " zoom=" + zoom);
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
					if (zoom != 1)
						ctm.scale(zoom);
					Pixmap pixmap = page.toPixmap(ctm, ColorSpace.DeviceBGR, true);
					pixmap.tint(INK_COLOR, BACKGROUND_COLOR);
					bitmap = pixmapToBitmap(pixmap);

				} catch (Throwable x) {
					Log.i(DocumentActivity.APP, x.getMessage());
				}
			}
			public void run() {
				if (bitmap != null) {
					setBitmap(bitmap, zoom, actionListener.getWentBack(), links, hits);
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

	public void resetHits() {
		hits = null;
		invalidate();
	}

	public void onSizeChanged(int w, int h, int ow, int oh) {
		canvasW = w;
		canvasH = h;
		if (actionListener != null)
			actionListener.onPageViewSizeChanged(w, h);
	}

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
		switchLinks(); // activte/deactivate link displaying and following
		invalidate();
	}

	private void switchLinks() {
		showLinks = !showLinks;
	}

	@RequiresApi(api = Build.VERSION_CODES.N)
	public boolean onSingleTapUp(MotionEvent e) {
		boolean foundLink = false;
		float x = e.getX();
		float y = e.getY();
		if (showLinks && links != null) {
			float dx = (bitmapW <= canvasW) ? (bitmapW - canvasW) / 2 : scrollX;
			float dy = (bitmapH <= canvasH) ? (bitmapH - canvasH) / 2 : scrollY;
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
			float a = canvasW / 3;
			float b = a * 2;
//			if (x <= a) goBackward();
//			if (x >= b) goForward();
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
		}
		return true;
	}

	public boolean onScaleBegin(ScaleGestureDetector det) {
		return true;
	}

	public boolean onScale(ScaleGestureDetector det) {
		if (bitmap != null) {
			float focusX = det.getFocusX();
			float focusY = det.getFocusY();
			float scaleFactor = det.getScaleFactor();
			float pageFocusX = (focusX + scrollX) / viewScale;
			float pageFocusY = (focusY + scrollY) / viewScale;
			viewScale *= scaleFactor;
			if (viewScale < minScale) viewScale = minScale;
			if (viewScale > maxScale) viewScale = maxScale;
			bitmapW = (int)(bitmap.getWidth() * viewScale / pageScale);
			bitmapH = (int)(bitmap.getHeight() * viewScale / pageScale);
			scrollX = (int)(pageFocusX * viewScale - focusX);
			scrollY = (int)(pageFocusY * viewScale - focusY);
			scroller.forceFinished(true);
			invalidate();
		}
		return true;
	}

	public void onScaleEnd(ScaleGestureDetector det) {
		if (actionListener != null)
			actionListener.onPageViewZoomChanged(viewScale);
	}


	private android.graphics.Rect dst = new android.graphics.Rect();
	private Path path = new Path();

	public void onDraw(Canvas canvas) {
		int x, y;

		if (bitmap == null) {
			if (error) {
				canvas.translate(canvasW / 2, canvasH / 2);
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

		dst.set(x, y, x + bitmapW, y + bitmapH);
		canvas.drawBitmap(bitmap, null, dst, null);

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
