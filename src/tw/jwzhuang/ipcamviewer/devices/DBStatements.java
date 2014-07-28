package tw.jwzhuang.ipcamviewer.devices;


public class DBStatements {

	public final static String DB = "cameras.db";
	public final static String Cameras_Fileds[][] = { { "_id", "ip", "pwd" } };
	public final static String Cameras_FieldType[][] = { {"INTEGER PRIMARY KEY NOT NULL", "TEXT NOT NULL", "TEXT NOT NULL" } };
	public final static String Cameras_TABLE[] = { "cameras" };
	

}
