/* 
 * This file is part of the CK2MapTools distribution.
 * Copyright (c) 2018 Loïc Visse.
 * 
 * This program is free software: you can redistribute it and/or modify  
 * it under the terms of the GNU General Public License as published by  
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License 
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package ck2maptools.utils;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import javax.imageio.ImageIO;

import ck2maptools.data.Coordinates;
import ck2maptools.data.InputFile;
import ck2maptools.data.Province;
import ck2maptools.ui.CK2MapToolsException;

public class Utils {
	
	public static File mkDir(String path)
	{
		//Setup output dirs if they don't exist
		File ret = new File(path);
		if (!ret.exists())
			ret.mkdirs();
		
		return ret;
	}

	public static RenderedImage readInputImage(String path) throws IOException
	{
		Logger.log("Loading "+path);
		File input = new File(path);
		return ImageIO.read(input);
	}
	
	public static void writeOutputImage(String path, RenderedImage buffer) throws IOException
	{
		Logger.log("Writing "+path);
		File output = new File(path);
		ImageIO.write(buffer, "bmp", output);		
	}
	
	//Scans a csv file and returns an List of all rows, where each row is an array of Strings
	public static List<String[]> parseCsvFile(String path) throws IOException
	{
		Logger.log("Loading "+path);
		File csvFile = new File(path);
		
		List<String[]> result = new ArrayList<String[]>();

		Scanner reader = new Scanner(csvFile);
		
		while(reader.hasNextLine())
		{
			result.add(reader.nextLine().split(";"));
		}
		
		reader.close();

		return result;
	}
	
	public static int getDistanceSquared(int x1, int y1, int x2, int y2)
	{
		return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
	}
	public static int getDistanceSquared(Coordinates c1, Coordinates c2)
	{
		return getDistanceSquared(c1.getX(), c1.getY(), c2.getX(), c2.getY());
	}
	public static int getDistanceSquared(Province p1, Province p2) {
		return getDistanceSquared(p1.getX(), p1.getY(), p2.getX(), p2.getY());
	}
	
	public static String getDateString()
	{
		return new SimpleDateFormat("yyyy-MM-dd.HH-mm-ss-SSS").format(new Date());
	}
	
	public static int getColorR(int color)
	{
		return (color & 0xFF0000) >> 16;
	}
	
	public static int getColorG(int color)
	{
		return (color & 0x00FF00) >> 8;
	}
	
	public static int getColorB(int color)
	{
		return (color & 0x0000FF);
	}
	
	public static boolean checkCriticalResources(InputFile[] inputFiles, boolean throwError) throws Exception
	{
		for (InputFile inputFile : inputFiles)
			if (!inputFile.exists())
			{
				String errMessage = "ERROR : missing critical input file "+inputFile.getFileName();
				Logger.log(errMessage);
				if (throwError)
					throw new CK2MapToolsException(errMessage);
				
				return false;
			}
		
		return true;
	}

}
