package com.mojang.minecraftpe;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import android.app.NativeActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;

import org.fmod.FMOD;

import com.byteandahalf.genericlauncher.*; // import ALL the things
import com.byteandahalf.genericlauncher.NativeHandler;


public class MainActivity extends NativeActivity {

	public static final int UI_HOVER_BUTTON_TEST = 0;

	PackageInfo packageInfo;
	ApplicationInfo appInfo;
	String libraryDir;
	String libraryLocation;
	boolean canAccessAssets = false;
	Context apkContext = null;
	DisplayMetrics metrics;

	boolean mcpePackage = false;

	public static ByteBuffer minecraftLibBuffer = null;
	public static MainActivity activity = null;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		activity = this;

		try {
			packageInfo = getPackageManager().getPackageInfo(
					"com.mojang.minecraftpe", 0);
			appInfo = packageInfo.applicationInfo;
			libraryDir = appInfo.nativeLibraryDir;
			libraryLocation = libraryDir + "/libminecraftpe.so";
			System.out.println("libminecraftpe.so is located at " + libraryDir);
			canAccessAssets = !appInfo.sourceDir
					.equals(appInfo.publicSourceDir);
			// int minecraftVersionCode = packageInfo.versionCode;

			if (this.getPackageName().equals("com.mojang.minecraftpe")) {
				apkContext = this;
			} else {
				apkContext = createPackageContext("com.mojang.minecraftpe",
						Context.CONTEXT_IGNORE_SECURITY);
			}
			Log.d("GenericLauncher","Load library fmod !");
			System.load(libraryDir+"/libfmod.so");
			Log.d("GenericLauncher","Load minecraftpe !");
			System.load(libraryLocation);
			
			FMOD.init(this);
			if(!FMOD.checkInit()) {
				Log.e("GenericLauncher","Failed to init fmod");
			} else {
			//Log.d("GenericLauncher","fmod init sucessful");
			}

			metrics = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(metrics);
			setVolumeControlStream(AudioManager.STREAM_MUSIC);
			addLibraryDirToPath(libraryDir);
			mcpePackage = true;
			super.onCreate(savedInstanceState);

			try {

				NativeHandler.init();

			} catch(Exception e) {
				e.printStackTrace();
			}
			
			getWindow().getDecorView().post(new Runnable() {
				public void run() {
					setupTestButton();
				}
			});

			nativeRegisterThis();
			mcpePackage = false;

			int flag = Build.VERSION.SDK_INT >= 19 ? 0x40000000 : 0x08000000; // FLAG_NEEDS_MENU_KEY
			getWindow().addFlags(flag);
		} catch (Exception e) {
			e.printStackTrace();
			finish();
		}

	}

	public String getExternalStoragePath() {
		return Environment.getExternalStorageDirectory().getAbsolutePath();
	}

	public String getLocale() {
		return HardwareInformation.getLocale();
	}

	public int getAndroidVersion() {
		return Build.VERSION.SDK_INT;
	}

	boolean hasHardwareChanged() {
		return false;
	}

	boolean isTablet() {
		return (getResources().getConfiguration().screenLayout & 0xF) ==4;
	}

	public boolean isFirstSnooperStart() {
		return PreferenceManager.getDefaultSharedPreferences(this).getString("snooperId","").isEmpty();
	}

	public String createUUID() {
		return UUID.randomUUID().toString().replaceAll("-","");
	}

	public String getDeviceId() {
		SharedPreferences sharedPreferences = 
			PreferenceManager.getDefaultSharedPreferences(this);
		String id = sharedPreferences.getString("snooperId", "");
		if (id.isEmpty()) {
			id = createUUID();
			SharedPreferences.Editor edit=sharedPreferences.edit();
			edit.putString("snooperId",id);
			edit.commit();
		}
		return id;
	}


	public String getDeviceModel() {
		String str1 = Build.MANUFACTURER;
		String str2 = Build.MODEL;
		if (str2.startsWith(str1))
		return str2.toUpperCase();
		return str1.toUpperCase() + " " + str2;
	}

	private File[] addToFileList(File[] files, File toAdd) {
		for (File f : files) {
			if (f.equals(toAdd)) {
				// System.out.println("Already added path to list");
				return files;
			}
		}
		File[] retval = new File[files.length + 1];
		System.arraycopy(files, 0, retval, 1, files.length);
		retval[0] = toAdd;
		return retval;
	}

	public static Field getDeclaredFieldRecursive(Class<?> clazz, String name) {
		if (clazz == null)
			return null;
		try {
			return clazz.getDeclaredField(name);
		} catch (NoSuchFieldException nsfe) {
			return getDeclaredFieldRecursive(clazz.getSuperclass(), name);
		}
	}

	private void addLibraryDirToPath(String path) {
		try {
			ClassLoader classLoader = getClassLoader();
			Class<? extends ClassLoader> clazz = classLoader.getClass();
			Field field = getDeclaredFieldRecursive(clazz, "pathList");
			field.setAccessible(true);
			Object pathListObj = field.get(classLoader);
			Class<? extends Object> pathListClass = pathListObj.getClass();
			Field natfield = getDeclaredFieldRecursive(pathListClass,
					"nativeLibraryDirectories");
			natfield.setAccessible(true);
			File[] fileList = (File[]) natfield.get(pathListObj);
			File[] newList = addToFileList(fileList, new File(path));
			if (fileList != newList)
				natfield.set(pathListObj, newList);
			// check
			// System.out.println("Class loader shenanigans: " +
			// ((PathClassLoader) getClassLoader()).findLibrary("minecraftpe"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public PackageManager getPackageManager() {
		if (mcpePackage) {
			return new RedirectPackageManager(super.getPackageManager(), libraryDir);
		}
		return super.getPackageManager();
	}

	public native void nativeRegisterThis();

	public native void nativeUnregisterThis();

	public native void nativeTypeCharacter(String character);

	public native void nativeSuspend();

	public native void nativeSetTextboxText(String text);

	public native void nativeBackPressed();

	public native void nativeBackSpacePressed();

	public native void nativeReturnKeyPressed();

	public void buyGame() {
	}

	public int checkLicense() {
		return 0;
	}

	public void onBackPressed() {}

	public void displayDialog(int dialogId) {
		Log.d("GenericLauncher", "[displayDialog] Dialog ID:" + dialogId);
	}

	public String getDateString(int time) {
		System.out.println("getDateString: " + time);
		return DateFormat.getDateInstance(DateFormat.SHORT, Locale.US).format(
				new Date(((long) time) * 1000));
	}

	public byte[] getFileDataBytes(String name) {
		System.out.println("Get file data: " + name);
		try {
			if (name.isEmpty())
				return null;
			InputStream is = null;
			if (name.charAt(0) == '/')
				is = new FileInputStream(new File(name));
			else 
				is = getInputStreamForAsset(name);
			if (is == null) {
				Log.e("GenericLauncher", 
					"FILE IS NULL!");
				return null;
			}
			// can't always find length - use the method from
			// http://www.velocityreviews.com/forums/t136788-store-whole-inputstream-in-a-string.html
			// instead
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			while (true) {
				int len = is.read(buffer);
				if (len < 0) {
					break;
				}
				bout.write(buffer, 0, len);
			}
			byte[] retval = bout.toByteArray();

			return retval;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	//public ArrayList<TexturePack> texturePacks = new ArrayList<TexturePack>();

	protected InputStream getInputStreamForAsset(String name) {
		try {

			/*for (int i = 0; i < texturePacks.size(); i++) {
				try {
					InputStream is = texturePacks.get(i).getInputStream(name);
					if (is != null)
						return is;
				} catch (IOException e) {
				}
			}*/

			return getLocalInputStreamForAsset(name);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	protected InputStream getLocalInputStreamForAsset(String name) {
		//Log.d("GenericLauncher","getStreamAsset "+name);
		InputStream is = null;
		try {
			/*
			 * if (forceFallback) { return getAssets().open(name); }
			 */
			try {
				if (name.charAt(0) == '/')
					is =new FileInputStream(new File(name));
				else 
					is = apkContext.getAssets().open(name);
			} catch (Exception e) {
				e.printStackTrace();
				// System.out.println("Attempting to load fallback");
				is = getAssets().open(name);
			}
			if (is == null) {
				System.out.println("Can't find it in the APK");
				is = getAssets().open(name);
			}
			return is;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public int[] getImageData(String name) {
		//System.out.println("Get image data: " + name);
		try {
			InputStream is = getInputStreamForAsset(name);
			if (is == null) {
				Log.e("GenericLauncher", "IMAGE IS NULL!");
				return null;
			}
			Bitmap bmp = BitmapFactory.decodeStream(is);
			int[] retval = new int[(bmp.getWidth() * bmp.getHeight()) + 2];
			retval[0] = bmp.getWidth();
			retval[1] = bmp.getHeight();
			bmp.getPixels(retval, 2, bmp.getWidth(), 0, 0, bmp.getWidth(),
					bmp.getHeight());
			is.close();
			bmp.recycle();

			return retval;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		/* format: width, height, each integer a pixel */
		/* 0 = white, full transparent */
	}

	public String[] getOptionStrings() {
		Log.e("GenericLauncher", "OptionStrings");
		return new String[] {};
	}

	public void launchUri(String uri) {
		startActivity(new Intent("android.intent.action.View",
			Uri.parse(uri)));
	}

	void pickImage(long paramLong) {
		Log.e("GenericLauncher","pickImage");
	}

	public float getPixelsPerMillimeter() {
		float val = ((float) metrics.densityDpi) / 25.4f;
		System.out.println("Pixels per mm: " + val);
		return val;

	}

	public String getPlatformStringVar(int a) {
		System.out.println("getPlatformStringVar: " + a);
		return "";
	}

	public float getKeyboardHeight() {
		Log.w("GenericLauncher","Bad implemented getKeyboardHeight");
		return (float) getScreenHeight()/2;
	}

	public int getScreenHeight() {
		return metrics.heightPixels;
	}

	public int getScreenWidth() {
		return metrics.widthPixels;
	}

	public int getUserInputStatus() {
		Log.e("GenericLauncher", "User input status");
		return 0;
	}

	public String[] getUserInputString() {
		Log.e("GenericLauncher", "User input string");
		return new String[] {};
	}

	public boolean hasBuyButtonWhenInvalidLicense() {
		return false;
	}

	/** Seems to be called whenever displayDialog is called. Not on UI thread. */
	public void initiateUserInput(int a) {
		System.out.println("initiateUserInput: " + a);
	}

	public boolean isNetworkEnabled(boolean a) {
		System.out.println("Network?:" + a);
		return true;
	}

	public boolean isTouchscreen() {
		return true;
	}

	public void postScreenshotToFacebook(String name, int firstInt,
			int secondInt, int[] thatArray) {
	}

	public void quit() {
		finish();
	}

	public void setIsPowerVR(boolean powerVR) {
		System.out.println("PowerVR: " + powerVR);
	}

	public void tick() {
	}

	public void vibrate(int duration) {
		((Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE))
				.vibrate(duration);
	}

	public boolean supportsNonTouchscreen() {
		boolean xperia = false;
		boolean play = false;
		String[] data = new String[3];
		data[0] = Build.MODEL.toLowerCase(Locale.ENGLISH);
		data[1] = Build.DEVICE.toLowerCase(Locale.ENGLISH);
		data[2] = Build.PRODUCT.toLowerCase(Locale.ENGLISH);
		for (String s : data) {
			if (s.indexOf("xperia") >= 0)
				xperia = true;
			if (s.indexOf("play") >= 0)
				play = true;
		}
		return xperia && play;
	}

	public int getKeyFromKeyCode(int keyCode, int metaState, int deviceId) {
		KeyCharacterMap characterMap = KeyCharacterMap.load(deviceId);
		return characterMap.get(keyCode, metaState);
	}

	public static void saveScreenshot(String name, int firstInt, int secondInt,
			int[] thatArray) {
	}

	public void showKeyboard(final String mystr, final int maxLength,
			final boolean mybool,final boolean isNumber) {
		this.runOnUiThread(new Runnable() {
			public void run() {
				showHiddenTextbox(mystr, maxLength, mybool,isNumber);
			}
		});
	}

	public void hideKeyboard() {
		this.runOnUiThread(new Runnable() {
			public void run() {
				dismissHiddenTextbox();
			}
		});
	}

	public void updateTextboxText(final String text) {
		if (hiddenTextView == null)
			return;
		hiddenTextView.post(new Runnable() {
			public void run() {
				hiddenTextView.setText(text);
			}
		});
	}

	PopupWindow hiddenTextWindow;
	EditText hiddenTextView;
	Boolean hiddenTextDismissAfterOneLine = false;

	private class PopupTextWatcher implements TextWatcher,
			TextView.OnEditorActionListener {
		public void afterTextChanged(Editable e) {
			nativeSetTextboxText(e.toString());

		}

		public void beforeTextChanged(CharSequence c, int start, int count,
				int after) {
		}

		public void onTextChanged(CharSequence c, int start, int count,
				int after) {
		}

		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			if (hiddenTextDismissAfterOneLine) {
				hiddenTextWindow.dismiss();
			} else {
				nativeReturnKeyPressed();
			}
			return true;
		}
	}

	public void showHiddenTextbox(String text, int maxLength,
			boolean dismissAfterOneLine,boolean isNumber) {
		int IME_FLAG_NO_FULLSCREEN = 0x02000000;
		if (hiddenTextWindow == null) {
			hiddenTextView = new EditText(this);
			PopupTextWatcher whoWatchesTheWatcher = new PopupTextWatcher();
			hiddenTextView.addTextChangedListener(whoWatchesTheWatcher);
			hiddenTextView.setOnEditorActionListener(whoWatchesTheWatcher);
			hiddenTextView.setSingleLine(true);
			hiddenTextView.setImeOptions(EditorInfo.IME_ACTION_NEXT
					| EditorInfo.IME_FLAG_NO_EXTRACT_UI
					| IME_FLAG_NO_FULLSCREEN);
			hiddenTextView.setInputType(InputType.TYPE_CLASS_TEXT);
			LinearLayout linearLayout = new LinearLayout(this);
			linearLayout.addView(hiddenTextView);
			hiddenTextWindow = new PopupWindow(linearLayout);
			hiddenTextWindow.setWindowLayoutMode(
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
			hiddenTextWindow.setFocusable(true);
			hiddenTextWindow
					.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
			hiddenTextWindow.setBackgroundDrawable(new ColorDrawable());
			// To get back button handling for free
			hiddenTextWindow.setClippingEnabled(false);
			hiddenTextWindow.setTouchable(false);
			hiddenTextWindow.setOutsideTouchable(true);
			// These flags were taken from a dumpsys window output of Mojang's
			// window
			hiddenTextWindow
					.setOnDismissListener(new PopupWindow.OnDismissListener() {
						public void onDismiss() {
							nativeBackPressed();
						}
					});
		}

		if (isNumber) {
		    hiddenTextView.setInputType(InputType.TYPE_CLASS_NUMBER);
		}
		hiddenTextView.setText(text);
		Selection.setSelection((Spannable) hiddenTextView.getText(),
				text.length());
		this.hiddenTextDismissAfterOneLine = dismissAfterOneLine;

		hiddenTextWindow.showAtLocation(this.getWindow().getDecorView(),
				Gravity.LEFT | Gravity.TOP, -10000, 0);
		hiddenTextView.requestFocus();
		showKeyboardView();
	}

	protected void onDestroy() {
		FMOD.close();
		nativeUnregisterThis();
		super.onDestroy();
		System.exit(0);
	}

	public void dismissHiddenTextbox() {
		if (hiddenTextWindow == null)
			return;
		hiddenTextWindow.dismiss();
		hideKeyboardView();
	}

	public void showKeyboardView() {
		Log.i("GenericLauncher", "Show keyboard view");
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(getWindow().getDecorView(),
				InputMethodManager.SHOW_FORCED);
	}

	protected void hideKeyboardView() {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(this.getWindow().getDecorView()
				.getWindowToken(), 0);
	}

	public int abortWebRequest(int requestId) {
		Log.i("GenericLauncher", "Abort web request: " + requestId);
		return 0;
	}

	public String getRefreshToken() {
		Log.i("GenericLauncher", "Get Refresh token");
		return "";
	}

	public String getSession() {
		Log.i("GenericLauncher", "Get Session");
		return "";
	}

	public String getWebRequestContent(int requestId) {
		Log.e("GenericLauncher", "Get web request content: " + requestId);
		return "";
	}

	public int getWebRequestStatus(int requestId) {
		Log.e("GenericLauncher", "Get web request status: " + requestId);
		return 0;
	}

	public void openLoginWindow() {
		Log.e("GenericLauncher", "Open login window");
	}

	public void setRefreshToken(String token) {
	}

	public void setSession(String session) {
	}

	public void webRequest(int requestId, long timestamp, String url,
			String method, String cookies) {
		Log.e("GenericLauncher", "webRequest");
	}

	// signature change in 0.7.3
	public void webRequest(int requestId, long timestamp, String url,
			String method, String cookies, String extraParam) {
		Log.e("GenericLauncher", "webRequest");
	}

	public String getAccessToken() {
		Log.i("GenericLauncher", "Get access token");
		return "";
	}

	public String getClientId() {
		Log.i("GenericLauncher", "Get client ID");
		return "";
	}

	public String getProfileId() {
		Log.i("GenericLauncher", "Get profile ID");
		return "";
	}

	public String getProfileName() {
		Log.i("GenericLauncher", "Get profile name");
		return "";
	}

	public void statsTrackEvent(String firstEvent, String secondEvent) {
		Log.i("GenericLauncher", "Stats track: " + firstEvent + ":" + secondEvent);
	}

	public void statsUpdateUserData(String firstEvent, String secondEvent) {
		Log.i("GenericLauncher", "Stats update user data: " + firstEvent + ":"
				+ secondEvent);
	}

	public boolean isDemo() {
		Log.i("GenericLauncher", "Is demo");
		return false;
	}

	public void setLoginInformation(String accessToken, String clientId,
			String profileUuid, String profileName) {
		Log.i("GenericLauncher", "setLoginInformation");
	}

	public void clearLoginInformation() {
		Log.i("GenericLauncher", "Clear login info");
	}

  	public String[] getBroadcastAddresses() {
   		return new String[]{};
  	}

	public String[] getIPAddresses() {
		Log.i("GenericLauncher", "getIPAdresses");
		return new String[]{};
	}

	void setFileDialogCallback(long callback) {
		Log.i("GenericLauncher", "setFileDialogCallback");
	}

	public Intent createAndroidLaunchIntent() {
		Context context = getApplicationContext();
		return context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
	}

	public void updateLocalization(final String language, final String country) {
		runOnUiThread(new Runnable() {
			public void run() {
				Locale locale = new Locale(language,country);
				Locale.setDefault(locale);
				Configuration conf = new Configuration();
				conf.locale = locale;
				MainActivity.this.getResources().updateConfiguration(conf,MainActivity.this.getResources().getDisplayMetrics());
			}
		});
	}
  
  	public long getTotalMemory() {
    		ActivityManager localActivityManager = (ActivityManager)getSystemService("activity");
    		ActivityManager.MemoryInfo localMemoryInfo = new ActivityManager.MemoryInfo();
    		localActivityManager.getMemoryInfo(localMemoryInfo);
    		return localMemoryInfo.availMem;
  	}
  	
  	public void setupTestButton() {
  		TestButton button = new TestButton(this);
  		button.show(getWindow().getDecorView());
  		button.mainButton.setOnClickListener(new View.OnClickListener() {
  			public void onClick(View v) {
  				System.out.println("What does the fox say?");
  			}
  		});
  		
  	}
}
