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

package ck2maptools.main;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ck2maptools.data.Coordinates;
import ck2maptools.data.InputFile;
import ck2maptools.data.Loader;
import ck2maptools.data.OutputFile;
import ck2maptools.data.Province;
import ck2maptools.data.Terrain;
import ck2maptools.utils.Logger;
import ck2maptools.utils.Utils;


public class CK2MakeProvincesMap implements ICK2MapTool {
	
	private Loader loader;
	
	private boolean recolorMode = false;
	private boolean doWater = true;
	
	public void setParamRecolorMode(boolean recolorMode) {this.recolorMode = recolorMode;}
	public void setParamDoWater(boolean doWater) {this.doWater = doWater;}

	//Data Array for output
	private int[][] provincesArray;
	
	public static InputFile[] inputFiles() {
		return new InputFile[]{
				InputFile.Terrain,
				InputFile.Settlements
		};
	}
	
	public static InputFile[] inputFilesOptional() {
		return new InputFile[]{
				InputFile.Provinces,
				InputFile.Cultures
		};
	}

	public static OutputFile[] outputFiles() {
		return new OutputFile[]{
				OutputFile.Provinces
		};
	}
	
	public static InputFile[] outputFilesInput() {
		return new InputFile[]{
				InputFile.Provinces //Doubles as an input file
		};
	}

	public static void main(String[] args) throws Exception {
		
		CK2MakeProvincesMap t = new CK2MakeProvincesMap();
		
		//Argument : -r / -recolor : when used, the program will remake the colors of the existing provinces.bmp based on cultures.bmp but will make no changes to the borders
		//Argument : -nw / -nowater : when used, the program will not generate water provinces
		for (String arg : args)
		{
			if (arg == null)
				continue;
			
			if (arg.equalsIgnoreCase("-r") || arg.equalsIgnoreCase("-recolor"))
			{
				t.recolorMode = true;
				Logger.log("Running in recolor mode");
			}
			if (arg.equalsIgnoreCase("-nw") || arg.equalsIgnoreCase("-nowater"))
			{
				t.doWater = false;
				Logger.log("No Water");
			}
		}
		
		t.execute();
	}
	
	public int execute() throws Exception {
		int returnCode = ERROR_NONE;
		Logger.InitLogger("CK2MakeProvinceMap");
		long ms = System.currentTimeMillis();
		
		Utils.checkCriticalResources(inputFiles(), true);
		
		//Load the input map
		loader = Loader.getLoader();
		loader.loadSettlements();
		loader.loadTerrain();
		loader.loadHeights(); //Needed to know which is water and which is not
		loader.loadProvinces(false, false);
		loader.loadCultures(false);
		
		//Initalize the output images
		BufferedImage bufOutProvinces = new BufferedImage(loader.sizeX, loader.sizeY, BufferedImage.TYPE_INT_RGB);
		
		provincesArray = new int[loader.sizeX][loader.sizeY];
		
		loader.provinceList = new ArrayList<Province>();
		loader.waterProvinceList = new  ArrayList<Province>();

		loader.provinceColorMap = new HashMap<Integer, Province>();
		loader.provinceColorMap.put(Color.BLACK.getRGB(), null);
		loader.provinceColorMap.put(Color.WHITE.getRGB(), null);
		
		//Parse
		Logger.log("Initializing...",0);
		
		for (int x=0; x<loader.sizeX; x++)
			for(int y=0; y<loader.sizeY; y++)
			{

				int rgb = loader.bufInSettlements.getRGB(x, y);
				
				int r,g,b;
				r = Utils.getColorR(rgb);
				g = Utils.getColorG(rgb);
				b = Utils.getColorB(rgb);
				
				//Black, impassable terrain / wasteland. Copy as is in destination array
				if (r==0 && g==0 && b==0)
				{
					provincesArray[x][y] = Color.BLACK.getRGB();
				}

			}

		int index = 0;

		Logger.log("Choosing Colors...",5);
		for (Coordinates c :  loader.settlementsList)
		{
			int color = getUniqueProvinceColor(c.getX(), c.getY());
			
			//Logger.log("Creating new color "+r+" "+g+" "+b+ " at "+c.getX()+" "+c.getY());
			Province p = new Province(c, color, ++index);
			loader.provinceList.add(p);
			loader.provinceColorMap.put(color, p);
			provincesArray[c.getX()][c.getY()] = color;
		}
		
		for (Coordinates c :  loader.seasList)
		{
			int color = getUniqueWaterProvinceColor(c.getX(), c.getY());
			
			//Logger.log("Creating new color "+r+" "+g+" "+b+ " at "+c.getX()+" "+c.getY());
			Province p = new Province(c, color, ++index);
			p.setWater(true);
			loader.waterProvinceList.add(p);
			loader.provinceColorMap.put(color, p);
			provincesArray[c.getX()][c.getY()] = color;
		}
		
		for (Coordinates c :  loader.riversList)
		{
			int color = getUniqueWaterProvinceColor(c.getX(), c.getY());
			
			//Logger.log("Creating new color "+r+" "+g+" "+b+ " at "+c.getX()+" "+c.getY());
			Province p = new Province(c, color, ++index);
			p.setWater(true);
			p.setRiver(true);
			loader.waterProvinceList.add(p);
			loader.provinceColorMap.put(color, p);
			provincesArray[c.getX()][c.getY()] = color;
		}
		
		for (Coordinates c :  loader.wastelandsList)
		{
			int color = getUniqueProvinceColor(c.getX(), c.getY());
			
			//Logger.log("Creating new color "+r+" "+g+" "+b+ " at "+c.getX()+" "+c.getY());
			Province p = new Province(c, color, ++index);
			p.setWasteland(true);
			loader.provinceList.add(p);
			loader.provinceColorMap.put(color, p);
			provincesArray[c.getX()][c.getY()] = color;
		}
		
		if (!recolorMode)
		{			
			//Fill up the rest
			Logger.log("Filling Land...",10);
			boolean progress = true;
			int iteration = 0;
			while(progress)
			{	
				iteration++;
				progress = false;
				Logger.log("Filling Land..."+iteration,(iteration < 100 && (iteration % 5) == 0) ? 1 : 0);
				
				//Create a working copy
				int[][] tempArray = new int[loader.sizeX][loader.sizeY];
				for (int x=0; x<loader.sizeX; x++)
					for(int y=0; y<loader.sizeY; y++)
						tempArray[x][y] = provincesArray[x][y];
		
				for (int x=0; x<loader.sizeX; x++)
					for(int y=0; y<loader.sizeY; y++)
				{
					
					if ( provincesArray[x][y] == 0 && !loader.isWater[x][y] ) //Land Not already claimed ?
					{
						int rx, ry;
						int rand = (int)(Math.random() * 4);
						for (int i=0; i<4; i++)
						{
							switch ((rand+i)%4)
							{
							default:
							case 0:
								rx = x+1;
								ry = y; 
								break;
							
							case 1:
								rx = x;
								ry = y+1; 
								break;
								
							case 2:
								rx = x-1;
								ry = y; 
								break;
								
							case 3:
								rx = x;
								ry = y-1; 
								break;
							}
							
							if (rx<0 || rx>=loader.sizeX || ry<0 || ry>= loader.sizeY)
								continue;
							
							//LAND expanding into LAND
							if (provincesArray[rx][ry] != 0 &&
									provincesArray[rx][ry] != Color.BLACK.getRGB() &&
									//!loader.isWater[x][y] &&
									!loader.isWater[rx][ry])
							{
								//If we make it that far, mark it as progress
								progress = true;
								boolean proceed = false;
								//Some terrain types are filled faster than others because people move faster on these land types...
								//Practically, this means mountains are more likely to become natural borders between provinces, even
								//if the settlements aren't exactly the same distance from the border
								switch (loader.terrainArray[x][y])
								{
								default:
									if (Math.random() < 0.7 || iteration > 256 || iteration < 8) //Randomize it a bit to make less 'clean' borders
										proceed = true;
									break;
		
								case FROZEN_MOUNTAIN:
									if (iteration % 16 == 0 || iteration > 256 || iteration < 8) //Almost impassable
										//if (Math.random() < 0.8) //Randomize it a bit to make less 'clean' borders
											proceed = true;
									break;
									
								case SNOWY_MOUNTAIN:
									if (iteration % 8 == 0 || iteration > 256 || iteration < 8) //Extremely hard to progress into
										//if (Math.random() < 0.8) //Randomize it a bit to make less 'clean' borders
											proceed = true;
									break;
									
								case MOUNTAIN:
								case DESERT_MOUNTAIN:
									if (iteration % 4 == 0 || iteration > 256 || iteration < 8) //Very hard to progress into
										//if (Math.random() < 0.8) //Randomize it a bit to make less 'clean' borders
											proceed = true;
									break;
								case JUNGLE:
								case FOREST_HILLS:
								case DESERT:
								case SANDY_MOUNTAIN:
								case ARCTIC:
									if (iteration % 2 == 0 || iteration > 256 || iteration < 8) //hard to progress into
										//if (Math.random() < 0.8) //Randomize it a bit to make less 'clean' borders
											proceed = true;
								break;
								}
								
								/*
								if (loader.bufInHeights != null)
								{
									if (loader.heightArray[x][y] > loader.heightArray[rx][ry])
									{
										if (iteration % 4 == 0)
											proceed = true;
									}
									else
									{
										proceed = true;
									}
										
								}
								*/
								
								if (proceed)
								{
									tempArray[x][y] = provincesArray[rx][ry];
									continue;
								}
		
								
								
								
							}
						}
					} //end if
				}//next Coordinates
				
				//Overwrite original with working copy
				for (int x=0; x<loader.sizeX; x++)
					for(int y=0; y<loader.sizeY; y++)
						provincesArray[x][y] = tempArray[x][y];
				
				if (!progress)
				{
					int undone = 0;
					
					//Fill anything left with BLACK
					for (int x=0; x<loader.sizeX; x++)
						for(int y=0; y<loader.sizeY; y++)
							if (provincesArray[x][y] == 0 && !loader.isWater[x][y])
							{
								provincesArray[x][y] = Color.BLACK.getRGB();
								undone++;
								Logger.log("No province at coordinates : x"+x+" y"+y);
								returnCode |= ERROR_FILLING_PROVINCE;
							}
					
					Logger.log(undone+" pixels could not be assigned");
				}
				
			}//wend
			
			Logger.log((loader.provinceList.size()) + " provinces created");
			
			if (doWater)
			{
				//Slightly different for water provinces : we want very clean borders here, so each pixel on the map just gets assigned
				//to the nearest sea zone
				Logger.log("Filling Waters...",10);
				int count=0;
				for (int x=0; x<loader.sizeX; x++)
					for(int y=0; y<loader.sizeY; y++)
					{
						int maxCount = (loader.sizeX*loader.sizeY/10);
						if (++count%maxCount==0)
							Logger.log("Filling Waters..."+(10*count/maxCount)+"%...",2);
							
						if (loader.isWater[x][y])
						{
							Province nearest = null;
							int nearestDist = Integer.MAX_VALUE;
							
							for (Province w : loader.waterProvinceList)
							{
								int dist = Utils.getDistanceSquared(x, y, w.getX(), w.getY());
								
								if (w.isRiver())
									dist *= 2;
								
								if (dist < nearestDist)
								{
									nearestDist = dist;
									nearest = w;
								}
							}
							
							if (nearest != null)
								if (existsPath(x,y,nearest.getX(), nearest.getY()))
										provincesArray[x][y] = nearest.getMapColor();								
						}
					}
				
				//Plug the leaks with the same method as for land province generation
				progress = true;
				iteration = 0;
				while(progress)
				{	
					iteration++;
					progress = false;
					Logger.log("Filling Water..."+iteration,(iteration < 100 && (iteration % 5) == 0) ? 1 : 0);
					
					//Create a working copy
					int[][] tempArray = new int[loader.sizeX][loader.sizeY];
					for (int x=0; x<loader.sizeX; x++)
						for(int y=0; y<loader.sizeY; y++)
							tempArray[x][y] = provincesArray[x][y];
			
					for (int x=0; x<loader.sizeX; x++)
						for(int y=0; y<loader.sizeY; y++)
					{
						
						if ( provincesArray[x][y] == 0 && loader.isWater[x][y]) //Water Not already claimed ?
						{
							int rx, ry;
							int rand = (int)(Math.random() * 4);
							for (int i=0; i<4; i++)
							{
								switch ((rand+i)%4)
								{
								default:
								case 0:
									rx = x+1;
									ry = y; 
									break;
								
								case 1:
									rx = x;
									ry = y+1; 
									break;
									
								case 2:
									rx = x-1;
									ry = y; 
									break;
									
								case 3:
									rx = x;
									ry = y-1; 
									break;
								}
								
								if (rx<0 || rx>=loader.sizeX || ry<0 || ry>= loader.sizeY)
									continue;
								
								//WATER expanding into WATER
								if (provincesArray[rx][ry] != 0 &&
										provincesArray[rx][ry] != Color.BLACK.getRGB() &&
										//loader.isWater[x][y] &&
										loader.isWater[rx][ry])
								{
									//If we make it that far, mark it as progress
									progress = true;
									tempArray[x][y] = provincesArray[rx][ry];
									continue;
								}
							}
						} //end if
					}//next Coordinates
					
					//Overwrite original with working copy
					for (int x=0; x<loader.sizeX; x++)
						for(int y=0; y<loader.sizeY; y++)
							provincesArray[x][y] = tempArray[x][y];
					
					if (!progress)
					{
						int undone = 0;
						
						//Fill anything left with WHITE
						for (int x=0; x<loader.sizeX; x++)
							for(int y=0; y<loader.sizeY; y++)
								if (provincesArray[x][y] == 0 && loader.isWater[x][y])
								{
									provincesArray[x][y] = Color.WHITE.getRGB();
									undone++;
									Logger.log("No sea zone at coordinates : x"+x+" y"+y);
									returnCode |= ERROR_FILLING_PROVINCE;
								}
						
						Logger.log(undone+" pixels could not be assigned");
					}
					
				}//wend
				
				Logger.log((loader.waterProvinceList.size()) + " sea zones created");
			}
			
			
			
		}
		else //Recolor mode
		{			
			Logger.log("Replacing colors...",10);
			Map<Integer, Province> oldProvinceColorsMap = new HashMap<Integer, Province>();
			
			for (Province p : loader.provinceList)
			{
				int originalcolor = loader.bufInProvinces.getRGB(p.getX(), p.getY());
				oldProvinceColorsMap.put(originalcolor, p);
			}
			
			//for each province, replace its color in the old bitmap by the new one in the array
			for (int x=0; x<loader.sizeX; x++)
				for(int y=0; y<loader.sizeY; y++)
				{
					Province originalProvince = oldProvinceColorsMap.get(loader.bufInProvinces.getRGB(x, y));
					
					if (originalProvince != null)
						provincesArray[x][y] = originalProvince.getMapColor();
					else //Copy old bitmap into array
						provincesArray[x][y] = loader.bufInProvinces.getRGB(x, y);
				}
		}
		
		
		//Build output
		Logger.log("Building output...",10);
		for (int x=0; x<loader.sizeX; x++)
			for(int y=0; y<loader.sizeY; y++)
			{
				bufOutProvinces.setRGB(x, y, provincesArray[x][y]);
			}
		
		//Setup output dirs if they don't exist
		Utils.mkDir("./output/map");
		
		//Write output images
		Utils.writeOutputImage(OutputFile.Provinces.getFileName(), bufOutProvinces);
		
		//Backup the existing image, if it exists
		File inputProvincesMap = new File(InputFile.Provinces.getFileName());
		File inputProvincesBackupMap = new File(InputFile.Provinces.getFileName().replace(".bmp", "."+Utils.getDateString()+".bmp"));
		if (inputProvincesMap.exists())
		{
			Files.copy(inputProvincesMap.toPath(), inputProvincesBackupMap.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		
		Utils.writeOutputImage(InputFile.Provinces.getFileName(), bufOutProvinces); //Also make a copy in the input folder
		
		//At the end of this, other programs would need to re-parse the provinces.bmp to get territory / border info, so make sure they do
		loader.bufInProvinces = null;
		

		Logger.log("Done in "+(System.currentTimeMillis()-ms)+"ms",100);
		Logger.close();
		return returnCode;
	}
	
	//Draws a straight line from x1,y1 to x2,y2 and returns true if there is only water or only land along the path
	private boolean existsPath(int x1, int y1, int x2, int y2) {
		
		int distance = Utils.getDistanceSquared(x1, y1, x2, y2);
		
		if (distance <= 36) //For small distances always true
			return true;
		
		int step = Math.abs(x2-x1) >= Math.abs(y2-y1) ? Math.abs(x2-x1) : Math.abs(y2-y1);
		
		double dx = (x2-x1)/(double)step;
		double dy = (y2-y1)/(double)step;
		
		double x = x1;
		double y = y1;
		
		boolean startWater = loader.isWater[x1][y1];
		
		for (int i=1; i<=step; i++)
		{
			x=x+dx;
			y=y+dy;
			
			if (loader.isWater[(int)x][(int)y] != startWater)
				return false;
		}
		
		return true;
		
	}

	private int getUniqueProvinceColor(int x, int y)
	{
		//Get a unique color for the province
		int rgb = 0;
		int r,g,b;
		
		//If there is an existing provinces map in the output folder, try to pick the same color
		if (loader.bufInProvinces != null && !recolorMode)
		{
			rgb = loader.bufInProvinces.getRGB(x, y);
		}
		
		//Ensure each color is unique, randomize until a suitable color is found
		while (rgb == 0 || loader.provinceColorMap.get(rgb) != null)
		{
			//If there is a cultures map, use it as a base
			if (loader.bufInCultures != null)
			{
				rgb = loader.bufInCultures.getRGB(x, y);
				r = Utils.getColorR(rgb);
				g = Utils.getColorG(rgb);
				b = Utils.getColorB(rgb);
				
				r = r + (int)((Math.random() - (r / 256.0)) * 64);
				g = g + (int)((Math.random() - (g / 256.0)) * 64);
				b = b + (int)((Math.random() - (b / 256.0)) * 64);
			}
			else
			{
				//Almost completely random
				r = 63 + (int)(Math.random()*192);
				g = (int)(Math.random()*255);
				b = (int)(Math.random()*127);
			}
			
			rgb = (r << 16) + (g << 8) + b;
		}
		
		return rgb;
	}
	
	private int getUniqueWaterProvinceColor(int x, int y)
	{
		//Get a unique color for the province
		int rgb = 0;
		int r,g,b;
		
		//If there is an existing provinces map in the output folder, try to pick the same color
		if (loader.bufInProvinces != null && !recolorMode)
		{
			rgb = loader.bufInProvinces.getRGB(x, y);
		}
		
		//Ensure each color is unique, randomize until a suitable color is found
		while (rgb == 0 || loader.provinceColorMap.get(rgb) != null)
		{
			r = (int)(Math.random()*64);
			g = (int)(Math.random()*128);
			b = 128+(int)(Math.random()*127);
			rgb = (r << 16) + (g << 8) + b;
		}
		
		return rgb;
	}
	
}
