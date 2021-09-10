package ck2maptools.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import ck2maptools.ui.CK2MapToolsUI;

//A basic handmade logger
public class Logger {

	private static CK2MapToolsUI ui;
	private static String name;
	private static File logFile;
	private static FileWriter writer;
	
	
	public static void InitLogger(String fileName)
	{
		File logPath = Utils.mkDir("./logs");
		name = fileName;
		logFile = new File(logPath+"/"+fileName+".log");
		try {
			writer = new FileWriter(logFile);
		} catch (Exception e) {
			System.out.println("Logger.InitLogger failed "+e.getMessage());
			e.printStackTrace();
		}
		
		if (ui != null)
			ui.resetProgress("Starting "+name+"...");
	}
	
	public static void log(String text)	{ log(text, -1); }
	
	public static void log(String text, int progress)
	{
		System.out.println(text);
		
		try {
			if (writer != null)
				writer.write("["+Utils.getDateString()+"]"+text+"\r\n");
		}
		catch (Exception e)
		{
			System.out.println("Logger.log failed "+e.getMessage());
		}
		
		if (ui != null && progress>=0)
			ui.updateProgress(text, progress);
	}

	
	public static void close()
	{
		try {
			if (writer != null)
				writer.close();
		}
		catch (Exception e)
		{
			System.out.println("Logger.close failed "+e.getMessage());
		}
		
		writer = null;
		
		if (ui != null)
			ui.updateProgress("Finished "+name+"...", 100);
	}
	
	public static void registerUI(CK2MapToolsUI ui)
	{
		Logger.ui = ui;
	}
}
