package tw.jwzhuang.ipcamviewer.devices;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.graphics.Bitmap;
import android.graphics.Matrix;


public class Utils {
	
	/**
	 * 取得IP
	 * 
	 * @return
	 */
	public static String getIPfromString(String input) {
		String patternStr = "((?:\\d{1,3}\\.){3}\\d{1,3}\\b)";
		Pattern pattern = Pattern.compile(patternStr);
		Matcher matcher = pattern.matcher(input);
		while (matcher.find()) {
			for (int i = 0; i <= matcher.groupCount() - 1;) {
				return matcher.group(i);
			}
		}
		return "";
	}
	
	/**
	 * 取得IP
	 * 
	 * @return
	 */
	public static String getCamerafromString(String input) {
		String patternStr = "(\\w+) ";
		Pattern pattern = Pattern.compile(patternStr);
		Matcher matcher = pattern.matcher(input);
		while (matcher.find()) {
			for (int i = 0; i <= matcher.groupCount() - 1;) {
				return matcher.group(i);
			}
		}
		return "";
	}
	
	/**
	 * 圖片旋轉
	 * @param source Bitmap
	 * @param angle float
	 * @return
	 */
	public static Bitmap RotateBitmap(Bitmap source, float angle)
	{
	      Matrix matrix = new Matrix();
	      matrix.postRotate(angle);
	      return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
	}
}
