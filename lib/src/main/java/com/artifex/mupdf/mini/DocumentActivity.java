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
import androidx.fragment.app.FragmentTransaction;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Stack;

public class DocumentActivity extends FragmentActivity
{
	public static final String APP = "Reader";

	public final int NAVIGATE_REQUEST = 1;

	protected Worker worker;
	protected SharedPreferences prefs;

	protected Document doc;

	// We must keep all this info for cases with screen size change
	protected String key;
	private static final String CURRENT_PAGE = "CURRENT_PAGE";
	private static final String CURRENT_CHAPTER_PAGE = "CURRENT_CHAPTER_PAGE";
	private static final String CURRENT_CHAPTER = "CURRENT_CHAPTER";
	private static final String CHAPTER_PAGE_COUNT = "CHAPTER_PAGE_COUNT";
	private int oldChapterPageCount;
	private Location loadedLoc;

	private static final String DEFAULT_CHAPTER_NAME = "Section";
	protected String mimetype;
	protected SeekableInputStream stream;
	protected byte[] buffer;

	protected boolean returnToLibraryActivity;
	protected boolean hasLoaded;
	protected boolean isReflowable;
	protected String title;
	protected ArrayList<ContentFragment.Item> flatOutline;
	protected float layoutW, layoutH;
	protected static final float LAYOUT_EM = 1;
	protected float displayDPI;
	protected int canvasW, canvasH;

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
	protected View layoutButton;
	protected PopupMenu layoutPopupMenu;
	protected View outlineButton;
	protected View navigationBar;
	protected TextView pageLabel;
	protected SeekBar pageSeekbar;
	protected TextView chapterLabel;
	protected TextView chapterPageLabel;

	private CSSManager cssManager;
	protected int pageCount;
	protected int currentPage;
	protected int searchHitPage;
	protected String searchNeedle;
	protected boolean stopSearch;
	protected Stack<Integer> history;
	private DocumentActivity actionListener;

	protected ContentFragment contentFragment = new ContentFragment();


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
		actionListener = this;
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		displayDPI = metrics.densityDpi;

		setContentView(R.layout.document_activity);
		cssManager = new CSSManager();


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

		loadPrefs();
		com.artifex.mupdf.fitz.Context.setUserCSS(cssManager.getCSS());
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

		chapterLabel = findViewById(R.id.chapter_label);
		chapterPageLabel = findViewById(R.id.chapter_page);

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
				if(contentFragment.isVisible()) {
					closeContentFragment();
				} else {
					createContentFragment();
				}
			}
		});

		layoutButton = findViewById(R.id.layout_button);
		layoutPopupMenu = new PopupMenu(this, layoutButton);
		layoutPopupMenu.getMenuInflater().inflate(R.menu.layout_menu, layoutPopupMenu.getMenu());
		layoutPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				int oldFontSize = cssManager.fontSize;
				int id = item.getItemId();
				if (id == R.id.font_size_4) cssManager.fontSize=4;
				else if (id == R.id.font_size_9) cssManager.fontSize = 9;
				else if (id == R.id.font_size_10) cssManager.fontSize = 10;
				else if (id == R.id.font_size_12) cssManager.fontSize = 12;
				else if (id == R.id.font_size_13) cssManager.fontSize = 13;
				else if (id == R.id.font_size_14) cssManager.fontSize = 14;
				else if (id == R.id.font_size_15) cssManager.fontSize = 15;
				if (oldFontSize != cssManager.fontSize)
					reopenDocument();
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

	private void loadPrefs() {
		prefs = getPreferences(Context.MODE_PRIVATE);
		cssManager.fontSize = prefs.getInt("fontSize", cssManager.fontSize);
		loadedLoc = new Location(prefs.getInt(key+ CURRENT_CHAPTER, 0), prefs.getInt(key+CURRENT_CHAPTER_PAGE, 0));
		currentPage = prefs.getInt(key+CURRENT_PAGE, 0);
		oldChapterPageCount = prefs.getInt(key+CHAPTER_PAGE_COUNT, 0);
	}

	private void createContentFragment() {
		Bundle bundle = new Bundle();
		bundle.putInt("POSITION", currentPage);
		bundle.putSerializable("OUTLINE", flatOutline);
		contentFragment.setArguments(bundle);
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);;
		ft.setReorderingAllowed(true);
		ft.replace(R.id.side_menu_container, contentFragment);
		ft.commit();
	}

	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_PAGE_UP:
		case KeyEvent.KEYCODE_COMMA:
		case KeyEvent.KEYCODE_B:
			goBackward();
			return true;
		case KeyEvent.KEYCODE_PAGE_DOWN:
		case KeyEvent.KEYCODE_PERIOD:
		case KeyEvent.KEYCODE_SPACE:
			goForward();
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


	private int oldW = 0;
	private int oldH = 0;
	public void onPageViewSizeChanged(int w, int h) {
		canvasW = w;
		canvasH = h;
		layoutW = canvasW * 72 / displayDPI;
		layoutH = canvasH * 72 / displayDPI;
		if (!hasLoaded) {
			hasLoaded = true;
			openDocument();
		} else if(!isReflowable && oldW != w || oldH != h) {
			readerView.setZoomWithoutUpdate(1);
			relayoutDocument();
		}
		oldW = w;
		oldH = h;
	}

	public void onPageViewZoomChanged(float zoom) {
		if(readerView.getZoom()!=zoom)
			readerView.setZoom(zoom);
	}

	public float getCommonZoom() {
		return readerView.getZoom();
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

	protected void relayoutDocument() {
		worker.add(new Worker.Task() {
			public void work() {
				try {
					loadedLoc = doc.locationFromPageNumber(currentPage);
					int oldChapterPages = doc.countPages(loadedLoc.chapter);
					Log.i(APP, "relayout document");
					doc.layout(layoutW, layoutH, LAYOUT_EM);
					pageCount = doc.countPages();
					if(oldChapterPages!=doc.countPages(loadedLoc.chapter)) {
						Location newLoc = getNewLocation(loadedLoc, oldChapterPages);
						currentPage = doc.pageNumberFromLocation(newLoc);
						if(currentPage<0) currentPage = 0;
						else if (currentPage>=pageCount) currentPage=pageCount-1;
					}
				} catch (Throwable x) {
					pageCount = 1;
					currentPage = 0;
					throw x;
				}
			}
			public void run() {
				updatePageNumberInfo(currentPage);
				loadOrUpdatePage(currentPage);
				loadOutline();
			}
		});
	}

	/**
	 * Relayout document after we change css
	 */
	protected void reopenDocument() {
		com.artifex.mupdf.fitz.Context.setUserCSS(cssManager.getCSS());
		Location oldLoc = doc.locationFromPageNumber(currentPage);
		int oldChapterPages = doc.countPages(oldLoc.chapter);
		worker.add(new Worker.Task() {
			boolean needsPassword;
			public void work() {
				Log.i(APP, "open document");
				if (buffer != null)
					doc = Document.openDocument(buffer, mimetype);
				else
					doc = Document.openDocument(stream, mimetype);;
			}
			public void run() {
				reloadDocument(oldLoc, oldChapterPages);
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
		savePrefs();
	}

	private void savePrefs() {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt("fontSize", cssManager.fontSize);
		editor.putInt(key+CURRENT_PAGE, currentPage);
		loadedLoc = doc.locationFromPageNumber(currentPage);
		editor.putInt(key+ CURRENT_CHAPTER, loadedLoc.chapter);
		editor.putInt(key+CURRENT_CHAPTER_PAGE, loadedLoc.page);
		editor.putInt(key+CHAPTER_PAGE_COUNT, doc.countPages(loadedLoc.chapter));
		editor.apply();
	}

	public void onBackPressed() {
		if(contentFragment.isVisible()) {
			closeContentFragment();
		}else if (history.empty()) {
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

	public void closeContentFragment() {
		getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left).remove(contentFragment).commit();
	}

	public void updatePageNumberInfo(int newPageNumber) {
		currentPage = newPageNumber;
		pageLabel.setText((currentPage+1) + " / " + pageCount);
		pageSeekbar.setMax(pageCount - 1);
		pageSeekbar.setProgress(currentPage);

//		String chapterName = DEFAULT_CHAPTER_NAME;
//		int chapterCurrentPage = 0;
//		int chapterMaxPages = 0;
//
//		chapterLabel.setText(chapterName);
//		chapterPageLabel.setText("- "+(chapterCurrentPage+1)+" / "+chapterMaxPages);
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
		if(searchNeedle!=null) {
			searchNeedle = null;
			readerView.updateCachedPages();
		}
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
					readerView.updateCachedPages();
					loadOrUpdatePage(currentPage);
				} else if (searchHitPage >= 0) {
					history.push(currentPage);
					loadOrUpdatePage(searchHitPage);
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
		readerView.setZoom(1);
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

	/**
	 * Load required document data
	 */
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
						doc.layout(layoutW, layoutH, LAYOUT_EM);
					}
					pageCount = doc.countPages();
					if(oldChapterPageCount!=doc.countPages(loadedLoc.chapter)) {
						Location newLoc = getNewLocation(loadedLoc, oldChapterPageCount);
						currentPage = doc.pageNumberFromLocation(newLoc);
						if(currentPage<0) currentPage = 0;
						else if (currentPage>=pageCount) currentPage=pageCount-1;
					}
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
				readerView.setCurrentItem(currentPage, false);
				updatePageNumberInfo(currentPage);
				loadOutline();

			}
		});
	}

	protected void reloadDocument(Location oldLoc, int oldChapterPages) {
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
						doc.layout(layoutW, layoutH, LAYOUT_EM);
					}
					pageCount = doc.countPages();
					if(oldChapterPages!=doc.countPages(oldLoc.chapter)) {
						Location newLoc = getNewLocation(oldLoc, oldChapterPages);
						currentPage = doc.pageNumberFromLocation(newLoc);
						if(currentPage<0) currentPage = 0;
						else if (currentPage>=pageCount) currentPage=pageCount-1;
					}
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
				readerView.setCurrentItem(currentPage, false);
				updatePageNumberInfo(currentPage);
				loadOutline();
			}
		});
	}

	public String getSearchNeedle() { return searchNeedle; }
	public void setStopSearch(boolean v) { stopSearch=v; }
	public int getCanvasW() { return canvasW; }
	public int getCanvasH() { return canvasH; }
	public Worker getWorker() {return worker; }

	private void loadOutline() {
		worker.add(new Worker.Task() {
			private void flattenOutline(Outline[] outline, String indent) {
				for (Outline node : outline) {
					if (node.title != null)
					{
						int outlinePage = doc.pageNumberFromLocation(doc.resolveLink(node));
						flatOutline.add(new ContentFragment.Item(indent + node.title, node.uri, outlinePage));
					}
					if (node.down != null)
						flattenOutline(node.down, indent + "    ");
				}
			}
			public void work() {
				Log.i(APP, "load outline");
				Outline[] outline = doc.loadOutline();
				if (outline != null) {
					flatOutline = new ArrayList<>();
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

	private Location getNewLocation(Location loc, int oldChapterPageCount) {
		int currentChapterPages = doc.countPages(loc.chapter);
		int newPage = Math.round(getReadProgress(loc.page, oldChapterPageCount)*currentChapterPages);
		if(newPage>=currentChapterPages) newPage = currentChapterPages-1;
		return new Location(loc.chapter, newPage);
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
		searchText.setText("");
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
				if(searchText.getText().equals("")) showKeyboard();
			}
		}
	}

	private float getReadProgress(int current, int count) {
		return ((float) current) / (count - 1);
	}

	/*
	If we have not cached pages - it will be drawn with actual info
	If we have cached pages - we need redraw it to display current state (search hits for example)
	 */
	private void loadOrUpdatePage(int p) {
		if(Math.abs(currentPage-p)<2)
			readerView.getCurrentPageFragment().updatePage();
		if (p >= 0 && p < pageCount && p != currentPage) {
			currentPage = p;
			readerView.setCurrentItem(p, false);
		}
	}

	public void gotoPage(int p) {
		if (p >= 0 && p < pageCount && p != currentPage) {
			history.push(currentPage);
			currentPage = p;
			readerView.setCurrentItem(p, false);
		}
	}

	public void gotoPage(String uri) {
		gotoPage(doc.pageNumberFromLocation(doc.resolveLink(uri)));
	}

	public void goBackward() {
		if(currentPage>0) {
			readerView.setCurrentItem(currentPage-1, true);
		}
	}

	public void goForward() {
		if(currentPage<pageCount-1) {
			readerView.setCurrentItem(currentPage+1, true);
		}
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
