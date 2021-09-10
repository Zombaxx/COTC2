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

import java.awt.image.BufferedImage;

import ck2maptools.data.Climate;
import ck2maptools.data.Coordinates;
import ck2maptools.data.InputFile;
import ck2maptools.data.Loader;
import ck2maptools.data.OutputFile;
import ck2maptools.data.Terrain;
import ck2maptools.utils.Config;
import ck2maptools.utils.Logger;
import ck2maptools.utils.Utils;

public class CK2MakeColorMaps implements ICK2MapTool {

	private Loader loader;
	
	//Data Arrays
	private int[][] colorArray;
	private int[][] colorWaterArray;
	private int[][] climateArray;
	
	public static InputFile[] inputFiles() {
		return new InputFile[]{
				InputFile.Terrain,
				InputFile.Climate
		};
	}

	public static OutputFile[] outputFiles() {
		return new OutputFile[]{
				OutputFile.ColorMap,
				OutputFile.ColorMapWater
		};
	}

	public static void main(String[] args) throws Exception {
		new CK2MakeColorMaps().execute();
	}

	public int execute() throws Exception {
		int returnCode = ERROR_NONE;
		long ms = System.currentTimeMillis();
		Logger.InitLogger("CK2MakeColorMaps");
		
		Utils.checkCriticalResources(inputFiles(), true);
		
		//Parse config.csv
		Config.parseConfig();
		
		//Load the input map
		loader = Loader.getLoader();
		loader.loadTerrain();
		loader.loadHeights();
		loader.loadClimate();

		colorArray = new int[loader.sizeX][loader.sizeY];
		colorWaterArray = new int[loader.sizeX][loader.sizeY];
		climateArray = new int[loader.sizeX/4/Config.INPUT_MAP_SCALE][loader.sizeY/4/Config.INPUT_MAP_SCALE];


		BufferedImage bufOutColorMap = new BufferedImage(loader.sizeX, loader.sizeY, BufferedImage.TYPE_INT_RGB);
		BufferedImage bufOutColorMapWater = new BufferedImage(loader.sizeX, loader.sizeY, BufferedImage.TYPE_INT_RGB);
		
		//Blend climate map to get smooth transitions
		if (loader.bufInClimate != null)
		{
			Logger.log("Preparing climate...", 0);
			
			int radius = 16;
			int count = 0;
			int countMod = (loader.sizeX/4/Config.INPUT_MAP_SCALE)*(loader.sizeY/4/Config.INPUT_MAP_SCALE)/10;
			
			for (int x=0 ; x<loader.sizeX/4/Config.INPUT_MAP_SCALE ; x++)
				for (int y=0 ; y<loader.sizeY/4/Config.INPUT_MAP_SCALE ; y++)
				{

					if (++count % countMod == 0)
						Logger.log("Preparing climate..."+(count * 10 / countMod) + "%", 5);

					int r=0,g=0,b=0,num=0;
					
					for (int rx=x-radius; rx<=x+radius;  rx++)
					{
						for (int ry=y-radius; ry<=y+radius;  ry++)
						{
							if (Coordinates.isValidCoordinates(rx*4*Config.INPUT_MAP_SCALE, ry*4*Config.INPUT_MAP_SCALE))
							{

								if (Utils.getDistanceSquared(rx, ry, x, y) <= (radius*radius))
								{
									r +=Utils.getColorR(loader.bufInClimate.getRGB(rx*4, ry*4));
									g +=Utils.getColorG(loader.bufInClimate.getRGB(rx*4, ry*4));
									b +=Utils.getColorB(loader.bufInClimate.getRGB(rx*4, ry*4));
									num++;
								}
							}
						}
					}
					
					climateArray[x][y] = (r/num << 16) + (g/num << 8) + b/num; 
				}
		}
		else
		{
			for (int x=0 ; x<loader.sizeX/4/Config.INPUT_MAP_SCALE ; x++)
				for (int y=0 ; y<loader.sizeY/4/Config.INPUT_MAP_SCALE ; y++)
					climateArray[x][y] = Climate.TEMPERATE.getRGB();
		}
		
		Logger.log("Making color maps...", 0);

		for (int x=0 ; x<loader.sizeX ; x++)
			for (int y=0 ; y<loader.sizeY ; y++)
			{
				
				double polar, temperate, hot;
				hot 		= Utils.getColorR(climateArray[x/4/Config.INPUT_MAP_SCALE][y/4/Config.INPUT_MAP_SCALE]) / 255.0;
				temperate 	= Utils.getColorG(climateArray[x/4/Config.INPUT_MAP_SCALE][y/4/Config.INPUT_MAP_SCALE]) / 255.0;
				polar 		= Utils.getColorB(climateArray[x/4/Config.INPUT_MAP_SCALE][y/4/Config.INPUT_MAP_SCALE]) / 255.0;
				
				double sum = (hot + temperate + polar);
				
				if (sum == 0)
				{
					polar = 1.0f;
					sum = 1.0f;
				}
				
				hot = hot / sum;
				temperate = temperate / sum;
				polar = polar / sum;
				
				int r=0,g=0,b=0;
				
				
				
				switch(loader.terrainArray[x][y])
				{
						
					default:
						r=(int) (60*polar + 120*temperate + 190*hot);
						g=(int) (110*polar + 150*temperate + 170*hot);
						b=(int) (75*polar + 60*temperate + 90*hot);
						break;
						

					case FARMLAND:
						r=(int) (30*polar + 60*temperate + 96*hot);
						g=(int) (67*polar + 92*temperate + 104*hot);
						b=(int) (13*polar + 10*temperate + 16*hot);
						break;
						
					case WATER:
						//Paint it blue if it's below the sea level
						if (loader.isWater[x][y])
						{
							r=27; g=48; b=57;
							break;
						}
						//Otherwise paint it like a sandy beach :
						
					case DESERT:
						r=(int) (170*polar + 195*temperate + 220*hot);
						g=(int) (160*polar + 175*temperate + 190*hot);
						b=(int) (100*polar + 110*temperate + 120*hot);
						break;
						
					case FROZEN_MOUNTAIN:
						r=220; g=220; b=220;
						break;
						
					case DESERT_MOUNTAIN:
					case MOUNTAIN:
					case SNOWY_MOUNTAIN:
					case SANDY_MOUNTAIN:
					case FOREST_HILLS:
						r=(int) (75*polar + 100*temperate + 120*hot);
						g=(int) (65*polar + 85*temperate + 100*hot);
						b=(int) (50*polar + 60*temperate + 45*hot);
						break;
						
					case JUNGLE:
						r=96; g=104; b=16;
						break;						
				}
				
				colorArray[x][y] = (r << 16) + (g << 8) + b;
				
				//Override for custom terrains :
				if (loader.terrainArray[x][y] == Terrain.UNUSED1)
					colorArray[x][y] = Config.CUSTOM_TERRAIN_1_COLOR.getRGB();
				else if (loader.terrainArray[x][y] == Terrain.UNUSED2)
					colorArray[x][y] = Config.CUSTOM_TERRAIN_2_COLOR.getRGB();
				
				switch(loader.terrainArray[x][y])
				{
					case WATER:
						r=(int) (24*polar + 32*temperate + 38*hot);
						g=(int) (32*polar + 64*temperate + 75*hot);
						b=(int) (60*polar + 80*temperate + 82*hot);
						break;
						
					default:
						r=(int) (30*polar + 44*temperate + 47*hot);
						g=(int) (73*polar + 145*temperate + 185*hot);
						b=(int) (173*polar + 255*temperate + 255*hot);
						break;
				}
				
				colorWaterArray[x][y] = (r << 16) + (g << 8) + b;
			}
		
		//Add some noise
		for (int x=0 ; x<loader.sizeX ; x+=4)
			for (int y=0 ; y<loader.sizeY ; y+=4)
			{
				double rr = Math.random() * 0.4 + 0.8;
				double rg = rr;
				double rb = Math.random() * 0.4 + 0.8;
			
				for (int x2=x; x2<x+4; x2++)
					for (int y2=y; y2<y+4; y2++)
						if (!loader.isWater[x2][y2])
						{
							int r = Utils.getColorR(colorArray[x2][y2]);
							int g = Utils.getColorG(colorArray[x2][y2]);
							int b = Utils.getColorB(colorArray[x2][y2]);
							
							r = Math.min(255,  (int) (r * rr));
							g = Math.min(255,  (int) (g * rg));
							b = Math.min(255,  (int) (b * rb));
							
							colorArray[x2][y2] = (r << 16) + (g << 8) + b;
						}
			}
		
		//Now blend it all
		int[][] colorArrayTemp = new int[loader.sizeX][loader.sizeY];
		int[][] colorWaterArrayTemp = new int[loader.sizeX][loader.sizeY];
		
		int maxIterations = 1;
		int radius = 5;
		int count = 0;
		int countMod = loader.sizeX*loader.sizeY*maxIterations/10;
		for (int iter = 0; iter < maxIterations; iter++)
		{
			for (int x=0 ; x<loader.sizeX ; x++)
				for (int y=0 ; y<loader.sizeY ; y++)
				{
					if (++count % countMod == 0)
						Logger.log("Making color maps..."+(count * 10 / countMod) + "%", 5);
					
					int num=0, numW=0, sumR=0, sumG=0, sumB=0, sumWR=0, sumWG=0, sumWB=0;
					
					colorArrayTemp[x][y] = colorArray[x][y];
					colorWaterArrayTemp[x][y] = colorWaterArray[x][y];
					
					for (int rx=x-radius; rx<=x+radius; rx++)
						for (int ry=y-radius; ry<=y+radius; ry++)
						{
							if (Coordinates.isValidCoordinates(rx, ry))
							{
								if (Utils.getDistanceSquared(rx, ry, x, y)<=radius*radius)
								{
									//don't blend water color into land
									if (!loader.isWater[rx][ry] && 
											!loader.isWater[x][y])
									{
										num++;
										sumR+=Utils.getColorR(colorArray[rx][ry]);
										sumG+=Utils.getColorG(colorArray[rx][ry]);
										sumB+=Utils.getColorB(colorArray[rx][ry]);
									}
									
									//do blend land color into water
									numW++;
									sumWR+=Utils.getColorR(colorWaterArray[rx][ry]);
									sumWG+=Utils.getColorG(colorWaterArray[rx][ry]);
									sumWB+=Utils.getColorB(colorWaterArray[rx][ry]);
								}
							}
						}
					
					if (num > 0)
					{
						sumR /= num;
						sumG /= num;
						sumB /= num;
						colorArrayTemp[x][y] = (sumR << 16) + (sumG << 8) + sumB;
					}
					
					if (numW > 0)
					{
						sumWR /= numW;
						sumWG /= numW;
						sumWB /= numW;
						colorWaterArrayTemp[x][y] = (sumWR << 16) + (sumWG << 8) + sumWB;
					}
				}

			colorArray = colorArrayTemp;
			colorWaterArray = colorWaterArrayTemp;
		}
		
		//Build output
		for (int x=0; x<loader.sizeX; x++)
			for(int y=0; y<loader.sizeY; y++)
			{
				bufOutColorMap.setRGB(x, y, colorArray[x][y] );	
				bufOutColorMapWater.setRGB(x, y, colorWaterArray[x][y] );
			}
		
		//Setup output dirs if they don't exist
		Utils.mkDir("./output/map/terrain");
		
		Utils.writeOutputImage(OutputFile.ColorMap.getFileName(), bufOutColorMap);
		Utils.writeOutputImage(OutputFile.ColorMapWater.getFileName(), bufOutColorMapWater);
		
		Logger.log("Done in "+(System.currentTimeMillis()-ms)+"ms", 100);
		//Logger.log(Coordinates.getPerfCounter()+" getCoordinatesInRadius() calls. Total time : "+Coordinates.getPerfTimer()+"ms");
		Logger.close();
		return returnCode;
	}
	
}
