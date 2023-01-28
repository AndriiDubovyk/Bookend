package com.artifex.mupdf.mini;

import com.artifex.mupdf.fitz.*;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.FileUriExposedException;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Stack;

public class DocumentActivity extends FragmentActivity
{
	public static final String APP = "MuPDF";

	public final int NAVIGATE_REQUEST = 1;

	protected Worker worker;
	protected SharedPreferences prefs;

	public static Document doc;

	private Context mContext;
	protected String key;
	protected String mimetype;
	protected SeekableInputStream stream;
	protected byte[] buffer;

	protected boolean returnToLibraryActivity;
	protected boolean hasLoaded;
	protected boolean isReflowable;
	protected String title;
	protected ArrayList<OutlineActivity.Item> flatOutline;
	protected float layoutW, layoutH, layoutEm;
	protected float displayDPI;
	protected int canvasW, canvasH;
	protected float pageZoom;

	protected ReaderView readerView;
	protected View currentBar;
	protected View actionBar;
	protected TextView titleLabel;
	protected View searchButton;
	protected View searchBar;
	protected EditText searchText;
	protected View searchCloseButton;
	protected View searchBackwardButton;
	protected View searchForwardButton;
	protected View zoomButton;
	protected View layoutButton;
	protected PopupMenu layoutPopupMenu;
	protected View outlineButton;
	protected View navigationBar;
	protected TextView pageLabel;
	protected SeekBar pageSeekbar;

	protected int pageCount;
	protected int currentPage;
	protected int searchHitPage;
	protected String searchNeedle;
	protected boolean stopSearch;
	protected Stack<Integer> history;
	protected boolean wentBack;
	private DocumentActivity actionListener;

	public String getSearchNeedle() { return searchNeedle; }
	public void setStopSearch(boolean v) { stopSearch=v; }
	public boolean getWentBack() { return wentBack; }
	public float getPageZoom() { return pageZoom; }
	public int getCanvasW() { return canvasW; }
	public int getCanvasH() { return canvasH; }
	public Worker getWorker() {return worker; }

	private String toHex(byte[] digest) {
		StringBuilder builder = new StringBuilder(2 * digest.length);
		for (byte b : digest)
			builder.append(String.format("%02x", b));
		return builder.toString();
	}

	private void openInput(Uri uri, long size, String mimetype) throws IOException {
		ContentResolver cr = getContentResolver();

		Log.i(APP, "Opening document " + uri);

		InputStream is = cr.openInputStream(uri);
		byte[] buf = null;
		int used = -1;
		try {
			final int limit = 8 * 1024 * 1024;
			if (size < 0) { // size is unknown
				buf = new byte[limit];
				used = is.read(buf);
				boolean atEOF = is.read() == -1;
				if (used < 0 || (used == limit && !atEOF)) // no or partial data
					buf = null;
			} else if (size <= limit) { // size is known and below limit
				buf = new byte[(int) size];
				used = is.read(buf);
				if (used < 0 || used < size) // no or partial data
					buf = null;
			}
			if (buf != null && buf.length != used) {
				byte[] newbuf = new byte[used];
				System.arraycopy(buf, 0, newbuf, 0, used);
				buf = newbuf;
			}
		} catch (OutOfMemoryError e) {
			buf = null;
		} finally {
			is.close();
		}

		if (buf != null) {
			Log.i(APP, "  Opening document from memory buffer of size " + buf.length);
			buffer = buf;
		} else {
			Log.i(APP, "  Opening document from stream");
			stream = new ContentInputStream(cr, uri, size);
		}
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		mContext = this;
		actionListener = this;
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		displayDPI = metrics.densityDpi;

		setContentView(R.layout.document_activity);



		actionBar = findViewById(R.id.action_bar);
		searchBar = findViewById(R.id.search_bar);
		navigationBar = findViewById(R.id.navigation_bar);



		currentBar = actionBar;

		Uri uri = getIntent().getData();
		mimetype = getIntent().getType();

		if (uri == null) {
			Toast.makeText(this, "No document uri to open", Toast.LENGTH_SHORT).show();
			return;
		}

		returnToLibraryActivity = getIntent().getIntExtra(getComponentName().getPackageName() + ".ReturnToLibraryActivity", 0) != 0;

		key = uri.toString();

		Log.i(APP, "OPEN URI " + uri.toString());
		Log.i(APP, "  MAGIC (Intent) " + mimetype);

		title = "";
		long size = -1;
		Cursor cursor = null;

		try {
			cursor = getContentResolver().query(uri, null, null, null, null, null);
			if (cursor != null && cursor.moveToFirst()){
				int idx;

				idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
				if (idx >= 0 && cursor.getType(idx) == Cursor.FIELD_TYPE_STRING)
					title = cursor.getString(idx);

				idx = cursor.getColumnIndex(OpenableColumns.SIZE);
				if (idx >= 0 && cursor.getType(idx) == Cursor.FIELD_TYPE_INTEGER)
					size = cursor.getLong(idx);

				if (size == 0)
					size = -1;
			}
		} catch (Exception x) {
			// Ignore any exception and depend on default values for title
			// and size (unless one was decoded
		} finally {
			if (cursor != null)
				cursor.close();
		}

		Log.i(APP, "  NAME " + title);
		Log.i(APP, "  SIZE " + size);

		if (mimetype == null || mimetype.equals("application/octet-stream")) {
			mimetype = getContentResolver().getType(uri);
			Log.i(APP, "  MAGIC (Resolver) " + mimetype);
		}
		if (mimetype == null || mimetype.equals("application/octet-stream")) {
			mimetype = title;
			Log.i(APP, "  MAGIC (Filename) " + mimetype);
		}

		try {
			openInput(uri, size, mimetype);
		} catch (Exception x) {
			Log.e(APP, x.toString());
			Toast.makeText(this, x.getMessage(), Toast.LENGTH_SHORT).show();
		}

		titleLabel = (TextView)findViewById(R.id.title_label);
		titleLabel.setText(title);

		history = new Stack<Integer>();

		worker = new Worker(this);
		worker.start();

		prefs = getPreferences(Context.MODE_PRIVATE);
		layoutEm = prefs.getFloat("layoutEm", 8);
		currentPage = prefs.getInt(key, 0);
		searchHitPage = -1;
		hasLoaded = false;

		pageLabel = (TextView)findViewById(R.id.page_label);
		pageSeekbar = (SeekBar)findViewById(R.id.page_seekbar);
		pageSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public int newProgress = -1;
			public void onProgressChanged(SeekBar seekbar, int progress, boolean fromUser) {
				if (fromUser) {
					newProgress = progress;
					pageLabel.setText((progress+1) + " / " + pageCount);
				}
			}
			public void onStartTrackingTouch(SeekBar seekbar) {}
			public void onStopTrackingTouch(SeekBar seekbar) {
				gotoPage(newProgress);
			}
		});

		searchButton = findViewById(R.id.search_button);
		searchButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showSearch();
			}
		});
		searchText = (EditText)findViewById(R.id.search_text);
		searchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN) {
					search(1);
					return true;
				}
				if (actionId == EditorInfo.IME_ACTION_SEARCH) {
					search(1);
					return true;
				}
				return false;
			}
		});
		searchText.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {}
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				resetSearch();
			}
		});
		searchCloseButton = findViewById(R.id.search_close_button);
		searchCloseButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				hideSearch();
			}
		});
		searchBackwardButton = findViewById(R.id.search_backward_button);
		searchBackwardButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				search(-1);
			}
		});
		searchForwardButton = findViewById(R.id.search_forward_button);
		searchForwardButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				search(1);
			}
		});

		outlineButton = findViewById(R.id.outline_button);
		outlineButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(DocumentActivity.this, OutlineActivity.class);
				Bundle bundle = new Bundle();
				bundle.putInt("POSITION", currentPage);
				bundle.putSerializable("OUTLINE", flatOutline);
				intent.putExtras(bundle);
				startActivityForResult(intent, NAVIGATE_REQUEST);
			}
		});

		layoutButton = findViewById(R.id.layout_button);
		layoutPopupMenu = new PopupMenu(this, layoutButton);
		layoutPopupMenu.getMenuInflater().inflate(R.menu.layout_menu, layoutPopupMenu.getMenu());
		layoutPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				float oldLayoutEm = layoutEm;
				int id = item.getItemId();
				if (id == R.id.action_layout_6pt) layoutEm = 6;
				else if (id == R.id.action_layout_7pt) layoutEm = 7;
				else if (id == R.id.action_layout_8pt) layoutEm = 8;
				else if (id == R.id.action_layout_9pt) layoutEm = 9;
				else if (id == R.id.action_layout_10pt) layoutEm = 10;
				else if (id == R.id.action_layout_11pt) layoutEm = 11;
				else if (id == R.id.action_layout_12pt) layoutEm = 12;
				else if (id == R.id.action_layout_13pt) layoutEm = 13;
				else if (id == R.id.action_layout_14pt) layoutEm = 14;
				else if (id == R.id.action_layout_15pt) layoutEm = 15;
				else if (id == R.id.action_layout_16pt) layoutEm = 16;
				if (oldLayoutEm != layoutEm)
					relayoutDocument();
				return true;
			}
		});
		layoutButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				layoutPopupMenu.show();
			}
		});

		readerView = findViewById(R.id.reader_view);
		readerView.setActionListener(actionListener);
		readerView.setAdapter(new PageAdapter(getSupportFragmentManager(), actionListener));
	}

	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_PAGE_UP:
		case KeyEvent.KEYCODE_COMMA:
		case KeyEvent.KEYCODE_B:
			//goBackward();
			return true;
		case KeyEvent.KEYCODE_PAGE_DOWN:
		case KeyEvent.KEYCODE_PERIOD:
		case KeyEvent.KEYCODE_SPACE:
			//goForward();
			return true;
		case KeyEvent.KEYCODE_M:
			history.push(currentPage);
			return true;
		case KeyEvent.KEYCODE_T:
			if (!history.empty()) {
				currentPage = history.pop();
				loadOrUpdatePage(currentPage);
			}
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	public void onPageViewSizeChanged(int w, int h) {
		pageZoom = 1;
		canvasW = w;
		canvasH = h;
		layoutW = canvasW * 72 / displayDPI;
		layoutH = canvasH * 72 / displayDPI;
		if (!hasLoaded) {
			hasLoaded = true;
			openDocument();
		} else if(!isReflowable) {
			loadOrUpdatePage(currentPage);
		}
	}

	public void onPageViewZoomChanged(float zoom) {
		if (zoom != pageZoom) {
			pageZoom = zoom;
			loadOrUpdatePage(currentPage);
		}
	}

	protected void openDocument() {
		worker.add(new Worker.Task() {
			boolean needsPassword;
			public void work() {
				Log.i(APP, "open document");
				if (buffer != null)
					doc = Document.openDocument(buffer, mimetype);
				else
					doc = Document.openDocument(stream, mimetype);
				needsPassword = doc.needsPassword();
			}
			public void run() {
				if (needsPassword)
					askPassword(R.string.dlog_password_message);
				else
					loadDocument();
			}
		});
	}

	protected void askPassword(int message) {
		final EditText passwordView = new EditText(this);
		passwordView.setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
		passwordView.setTransformationMethod(PasswordTransformationMethod.getInstance());

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.dlog_password_title);
		builder.setMessage(message);
		builder.setView(passwordView);
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				checkPassword(passwordView.getText().toString());
			}
		});
		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				finish();
			}
		});
		builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				finish();
			}
		});
		builder.create().show();
	}

	protected void checkPassword(final String password) {
		worker.add(new Worker.Task() {
			boolean passwordOkay;
			public void work() {
				Log.i(APP, "check password");
				passwordOkay = doc.authenticatePassword(password);
			}
			public void run() {
				if (passwordOkay)
					loadDocument();
				else
					askPassword(R.string.dlog_password_retry);
			}
		});
	}

	public void onPause() {
		super.onPause();
		SharedPreferences.Editor editor = prefs.edit();
		editor.putFloat("layoutEm", layoutEm);
		editor.putInt(key, currentPage);
		editor.apply();
	}

	public void onBackPressed() {
		if (history.empty()) {
			super.onBackPressed();
			if (returnToLibraryActivity) {
				Intent intent = getPackageManager().getLaunchIntentForPackage(getComponentName().getPackageName());
				startActivity(intent);
			}
		} else {
			currentPage = history.pop();
			loadOrUpdatePage(currentPage);
		}
	}

	public void updatePageNumberInfo(int newPageNumber) {
		currentPage = newPageNumber;
		pageLabel.setText((currentPage+1) + " / " + pageCount);
		pageSeekbar.setMax(pageCount - 1);
		pageSeekbar.setProgress(currentPage);
		wentBack = false;
	}

	public void onActivityResult(int request, int result, Intent data) {
		super.onActivityResult(request, result, data);
		if (request == NAVIGATE_REQUEST && result >= RESULT_FIRST_USER)
			gotoPage(result - RESULT_FIRST_USER);
	}

	protected void showKeyboard() {
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null)
			imm.showSoftInput(searchText, 0);
	}

	protected void hideKeyboard() {
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null)
			imm.hideSoftInputFromWindow(searchText.getWindowToken(), 0);
	}

	protected void resetSearch() {
		stopSearch = true;
		searchHitPage = -1;
		searchNeedle = null;
		readerView.updateCachedPages();
		//pageView.resetHits();
	}

	protected void runSearch(final int startPage, final int direction, final String needle) {
		stopSearch = false;
		worker.add(new Worker.Task() {
			int searchPage = startPage;
			public void work() {
				if (stopSearch || needle != searchNeedle)
					return;
				for (int i = 0; i < 9; ++i) {
					Log.i(APP, "search page " + searchPage);
					Page page = doc.loadPage(searchPage);
					Quad[][] hits = page.search(searchNeedle);
					page.destroy();
					if (hits != null && hits.length > 0) {
						searchHitPage = searchPage;
						break;
					}
					searchPage += direction;
					if (searchPage < 0 || searchPage >= pageCount)
						break;
				}
			}
			public void run() {
				if (stopSearch || needle != searchNeedle) {
					pageLabel.setText((currentPage+1) + " / " + pageCount);
				} else if (searchHitPage == currentPage) {
					loadOrUpdatePage(currentPage);
				} else if (searchHitPage >= 0) {
					history.push(currentPage);
					currentPage = searchHitPage;
					loadOrUpdatePage(currentPage);
				} else {
					if (searchPage >= 0 && searchPage < pageCount) {
						pageLabel.setText((searchPage+1) + " / " + pageCount);
						worker.add(this);
					} else {
						pageLabel.setText((currentPage+1) + " / " + pageCount);
						Log.i(APP, "search not found");
						Toast.makeText(DocumentActivity.this, getString(R.string.toast_search_not_found), Toast.LENGTH_SHORT).show();
					}
				}
			}
		});
	}

	protected void search(int direction) {
		hideKeyboard();
		int startPage;
		if (searchHitPage == currentPage)
			startPage = currentPage + direction;
		else
			startPage = currentPage;
		searchHitPage = -1;
		searchNeedle = searchText.getText().toString();
		if (searchNeedle.length() == 0)
			searchNeedle = null;
		if (searchNeedle != null)
			if (startPage >= 0 && startPage < pageCount)
				runSearch(startPage, direction, searchNeedle);
	}

	protected void loadDocument() {
		worker.add(new Worker.Task() {
			public void work() {
				try {
					Log.i(APP, "load document");
					String metaTitle = doc.getMetaData(Document.META_INFO_TITLE);
					if (metaTitle != null && !metaTitle.equals(""))
						title = metaTitle;
					isReflowable = doc.isReflowable();
					if (isReflowable) {
						Log.i(APP, "layout document");
						doc.layout(layoutW, layoutH, layoutEm);
					}
					pageCount = doc.countPages();
				} catch (Throwable x) {
					doc = null;
					pageCount = 1;
					currentPage = 0;
					throw x;
				}
			}
			public void run() {
				readerView.getAdapter().notifyDataSetChanged();
				if (currentPage < 0 || currentPage >= pageCount)
					currentPage = 0;
				titleLabel.setText(title);
				if (isReflowable)
					layoutButton.setVisibility(View.VISIBLE);
				loadOrUpdatePage(currentPage);
				loadOutline();

			}
		});
	}

	protected void relayoutDocument() {
		worker.add(new Worker.Task() {
			public void work() {
				try {
					long mark = doc.makeBookmark(doc.locationFromPageNumber(currentPage));
					Log.i(APP, "relayout document");
					doc.layout(layoutW, layoutH, layoutEm);
					pageCount = doc.countPages();
					currentPage = doc.pageNumberFromLocation(doc.findBookmark(mark));
				} catch (Throwable x) {
					pageCount = 1;
					currentPage = 0;
					throw x;
				}
			}
			public void run() {
				updatePageNumberInfo(currentPage);
				readerView.getAdapter().notifyDataSetChanged();
				loadOrUpdatePage(currentPage);
				Log.i("mytag", "doc count: "+pageCount+"; adapter count: "+readerView.getAdapter().getCount());
				loadOutline();
			}
		});
	}

	private void loadOutline() {
		worker.add(new Worker.Task() {
			private void flattenOutline(Outline[] outline, String indent) {
				for (Outline node : outline) {
					if (node.title != null)
					{
						int outlinePage = doc.pageNumberFromLocation(doc.resolveLink(node));
						flatOutline.add(new OutlineActivity.Item(indent + node.title, node.uri, outlinePage));
					}
					if (node.down != null)
						flattenOutline(node.down, indent + "    ");
				}
			}
			public void work() {
				Log.i(APP, "load outline");
				Outline[] outline = doc.loadOutline();
				if (outline != null) {
					flatOutline = new ArrayList<OutlineActivity.Item>();
					flattenOutline(outline, "");
				} else {
					flatOutline = null;
				}
			}
			public void run() {
				if (flatOutline != null)
					outlineButton.setVisibility(View.VISIBLE);
			}
		});
	}

	protected void showSearch() {
		currentBar = searchBar;
		actionBar.setVisibility(View.GONE);
		searchBar.setVisibility(View.VISIBLE);
		searchBar.requestFocus();
		showKeyboard();
	}

	protected void hideSearch() {
		currentBar = actionBar;
		actionBar.setVisibility(View.VISIBLE);
		searchBar.setVisibility(View.GONE);
		hideKeyboard();
		resetSearch();
	}

	public void toggleUI() {
		if (navigationBar.getVisibility() == View.VISIBLE) {
			currentBar.setVisibility(View.GONE);
			navigationBar.setVisibility(View.GONE);
			if (currentBar == searchBar)
				hideKeyboard();
		} else {
			currentBar.setVisibility(View.VISIBLE);
			navigationBar.setVisibility(View.VISIBLE);
			if (currentBar == searchBar) {
				searchBar.requestFocus();
				showKeyboard();
			}
		}
	}

	/*
	If we have not cached pages - it will be drawn with actual info
	If we have cached pages - we need redraw it to display current state (search hits for example)
	 */
	private void loadOrUpdatePage(int p) {
		readerView.setCurrentItem(p);
		if(Math.abs(currentPage-p)<2)
			readerView.getCurrentPageFragment().updatePage();
		currentPage = p;
	}

	public void gotoPage(int p) {
		if (p >= 0 && p < pageCount && p != currentPage) {
			history.push(currentPage);
			currentPage = p;
			readerView.setCurrentItem(p);
		}
	}

	public void gotoPage(String uri) {
		gotoPage(doc.pageNumberFromLocation(doc.resolveLink(uri)));
	}

	@RequiresApi(api = Build.VERSION_CODES.N)
	public void gotoURI(String uri) {
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET); // FLAG_ACTIVITY_NEW_DOCUMENT in API>=21
		try {
			startActivity(intent);
		} catch (FileUriExposedException x) {
			Log.e(APP, x.toString());
			Toast.makeText(DocumentActivity.this, "Android does not allow following file:// link: " + uri, Toast.LENGTH_LONG).show();
		} catch (Throwable x) {
			Log.e(APP, x.getMessage());
			Toast.makeText(DocumentActivity.this, x.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}
}
