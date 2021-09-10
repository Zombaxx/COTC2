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
import java.io.IOException;
import java.security.GeneralSecurityException;

import ck2maptools.data.Climate;
import ck2maptools.data.Coordinates;
import ck2maptools.data.InputFile;
import ck2maptools.data.InputTerrain;
import ck2maptools.data.Loader;
import ck2maptools.data.OutputFile;
import ck2maptools.data.Rivers;
import ck2maptools.data.Terrain;
import ck2maptools.data.Trees;
import ck2maptools.utils.Config;
import ck2maptools.utils.Logger;
import ck2maptools.utils.Utils;

public class CK2MakeTerrainMap implements ICK2MapTool {

	private Loader loader;
	
	//CK2 hardcoded
	private static final int MIN_HEIGHT_LAND 	= 96;
	private static final int MAX_HEIGHT_WATER 	= 94;

	private static final int DEEP_WATER_HEIGHT = 		MAX_HEIGHT_WATER - 45;
	private static final int NORMAL_WATER_HEIGHT = 	MAX_HEIGHT_WATER - 15;
	private static final int SHALLOW_WATER_HEIGHT = 	MAX_HEIGHT_WATER - 5;

	private boolean fastMode = false;
	
	public void setParamFastMode(boolean fastMode) {this.fastMode = fastMode;}
	private boolean hasVolcano = false;
	
	//Data arrays for output images
	public Terrain[][] terrainArray;
	public int[][] heightArray;
	public Trees[][] treeArray;
	public int[][] normalArray;	
	public int[][] provincesArray; //Contains the argb color of every pixel on the provinces.bmp map in the making
	private boolean[][] isWater;

	private int returnCode;


	public static InputFile[] inputFiles() {
		return new InputFile[]{
				InputFile.Input
		};
	}
	
	public static InputFile[] inputFilesOptional() {
		return new InputFile[]{
				InputFile.Climate,
				InputFile.Rivers,
				InputFile.Settlements
		};
	}

	public static OutputFile[] outputFiles() {
		return new OutputFile[]{
				OutputFile.Terrain,
				OutputFile.Topology,
				OutputFile.Trees,
				OutputFile.Normals
		};
	}
	
	public static InputFile[] outputFilesInput() {
		return new InputFile[]{
				InputFile.Rivers, //Generates / Updates
				InputFile.Terrain, //Doubles as an input file
				InputFile.Settlements //Generates / Updates
		};
	}

	public static void main(String[] args) throws Exception {
		
		CK2MakeTerrainMap t = new CK2MakeTerrainMap();
		
		//Argument : -f / -fast : the program will run lower-quality smoothing
		for (String arg : args)
		{
			if (arg == null)
				continue;
			
			if (arg.equalsIgnoreCase("-f") || arg.equalsIgnoreCase("-fast"))
			{
				t.fastMode = true;
			}
		}
		
		t.execute();
	}
	
	public int execute() throws Exception {
		returnCode = ERROR_NONE;
		long ms = System.currentTimeMillis();
		Logger.InitLogger("CK2MakeTerrainMap");
		
		Utils.checkCriticalResources(inputFiles(), true);

		
		//Parse config.csv
		Config.parseConfig();
		
		//Load the input maps
		loader = Loader.getLoader();
		loader.loadInput();
		loader.loadClimate();
		loader.loadSettlements();
		loader.loadRivers();


		//Initialize the output images
		BufferedImage bufOutTerrain = new BufferedImage(loader.sizeX, loader.sizeY, BufferedImage.TYPE_BYTE_INDEXED, Terrain.getIndexColorModel());
		BufferedImage bufOutTopology = new BufferedImage(loader.sizeX, loader.sizeY, BufferedImage.TYPE_BYTE_GRAY);
		BufferedImage bufOutTrees = new BufferedImage(loader.sizeX/Config.TREE_MAP_SCALE, loader.sizeY/Config.TREE_MAP_SCALE, BufferedImage.TYPE_BYTE_INDEXED, Trees.getIndexColorModel());
		BufferedImage bufOutNormal = new BufferedImage(loader.sizeX, loader.sizeY, BufferedImage.TYPE_INT_RGB);
		
		//Initialize Data arrays
		terrainArray = new Terrain[loader.sizeX][loader.sizeY];
		heightArray = new int[loader.sizeX][loader.sizeY];
		treeArray = new Trees[(loader.sizeX/Config.TREE_MAP_SCALE) + (loader.sizeX % Config.TREE_MAP_SCALE > 0 ? 1 : 0)][(loader.sizeY/Config.TREE_MAP_SCALE) + (loader.sizeY % Config.TREE_MAP_SCALE > 0 ? 1 : 0)];
		normalArray = new int[loader.sizeX][loader.sizeY];
		isWater = new boolean[loader.sizeX][loader.sizeY];
		
	
		//Initialize
		smoothMajorRiverEdges();
		initTerrainAndHeights();


		//Smooth Coastlines
		smoothCoastlines(true); //First expand land into water
		smoothCoastlines(false); //Then do the reverse
		
		//Smooth Heights
		int i=1;

		if (!fastMode)
		{		
			smoothHeights(Config.SMOOTH_RADIUS, 255, i++);
			smoothHeights(Config.SMOOTH_RADIUS, Config.MOUNTAIN_HEIGHT, i++);
			smoothHeights(Config.SMOOTH_RADIUS, Config.HILLS_HEIGHT, i++);
		}

		makeNoise();

		if (!fastMode)
			smoothHeights(3, 255, i++);
		
		if (hasVolcano)
			makeVolcanoes();

		smoothHeights(3, 255, i++);
		
		//Additional height smoothing around settlements
		smoothHeightsForSettlements();
		
		//Make coastal desert, etc
		smoothTerrain();

		makeTrees();
		
		makeNormalMap();

		//Update or generate settlements.bmp with a darkened input.bmp
		if (loader.bufInSettlements == null)
		{
			loader.bufInSettlements = new BufferedImage(loader.sizeX, loader.sizeY, BufferedImage.TYPE_INT_RGB);
			for (int x=0; x<loader.sizeX; x++)
				for(int y=0; y<loader.sizeY; y++)
					loader.bufInSettlements.setRGB(x, y, Color.gray.getRGB());
		}
		
		//Update or generate rivers.bmp with a darkened input.bmp
		
		if (loader.bufInRivers == null)
		{
			loader.bufInRivers = new BufferedImage(loader.sizeX, loader.sizeY, BufferedImage.TYPE_INT_RGB);
			for (int x=0; x<loader.sizeX; x++)
				for(int y=0; y<loader.sizeY; y++)
					loader.bufInRivers.setRGB(x, y, Color.gray.getRGB());
		}
		
		//Build output
		for (int x=0; x<loader.sizeX; x++)
			for(int y=0; y<loader.sizeY; y++)
			{
				bufOutTerrain.setRGB(x, y, terrainArray[x][y].getRGB());			
				bufOutTopology.getRaster().setPixel(x, y, new int[]{ heightArray[x][y] });				
				bufOutNormal.setRGB(x, y, normalArray[x][y] );
				
				//Update or generate settlements.bmp with a darkened input.bmp
				if (loader.bufInSettlements.getRGB(x, y) != Color.red.getRGB() &&
						loader.bufInSettlements.getRGB(x, y) != Color.yellow.getRGB() &&
						loader.bufInSettlements.getRGB(x, y) != Color.black.getRGB() &&
						loader.bufInSettlements.getRGB(x, y) != Color.green.getRGB() &&
						loader.bufInSettlements.getRGB(x, y) != Color.blue.getRGB() &&
						loader.bufInSettlements.getRGB(x, y) != Color.cyan.getRGB())
				{
					int newRGB = loader.bufInInput.getRGB(x/Config.INPUT_MAP_SCALE, y/Config.INPUT_MAP_SCALE);
					int newR = Utils.getColorR(newRGB);
					int newG = Utils.getColorG(newRGB);
					int newB = Utils.getColorB(newRGB);
					newRGB = ((newR / 2) << 16) + ((newG / 2) << 8) + (newB / 2);  
					
					loader.bufInSettlements.setRGB(x, y, newRGB);
				}
				
				//Update or generate rivers.bmp with a darkened input.bmp
				if (loader.bufInRivers.getRGB(x, y) != Color.red.getRGB() &&
					loader.bufInRivers.getRGB(x, y) != Color.green.getRGB() &&
					loader.bufInRivers.getRGB(x, y) != Color.cyan.getRGB()&&
					loader.bufInRivers.getRGB(x, y) != Color.yellow.getRGB())
				{
					int newRGB = loader.bufInInput.getRGB(x/Config.INPUT_MAP_SCALE, y/Config.INPUT_MAP_SCALE);
					
					if (isWater[x][y])
					{
						newRGB = Rivers.WATER.getRGB();
					}
					else
					{

						int newR = Utils.getColorR(newRGB);
						int newG = Utils.getColorG(newRGB);
						int newB = Utils.getColorB(newRGB);
						newRGB = ((newR / 2) << 16) + ((newG / 2) << 8) + (newB / 2);  
					}
					
					loader.bufInRivers.setRGB(x, y, newRGB);
				}
			}
		

		for (int x=0; x<loader.sizeX/Config.TREE_MAP_SCALE; x++)
			for(int y=0; y<loader.sizeY/Config.TREE_MAP_SCALE; y++)
			{
				bufOutTrees.setRGB(x, y, treeArray[x][y].getRGB());
			}

		
		//Setup output dirs if they don't exist
		Utils.mkDir("./output/map");
		
		//Write output images
		Utils.writeOutputImage(OutputFile.Terrain.getFileName(), bufOutTerrain);
		Utils.writeOutputImage(OutputFile.Topology.getFileName(), bufOutTopology);
		Utils.writeOutputImage(OutputFile.Trees.getFileName(), bufOutTrees);
		Utils.writeOutputImage(OutputFile.Normals.getFileName(), bufOutNormal);
		

		//Update or generate settlements.bmp
		Utils.writeOutputImage(InputFile.Settlements.getFileName(), loader.bufInSettlements);
		//Update or generate rivers.bmp
		Utils.writeOutputImage(InputFile.Rivers.getFileName(), loader.bufInRivers);
		
		Logger.log("Done in "+(System.currentTimeMillis()-ms)+"ms",100);
		Logger.close();
		return returnCode;
	}


	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



	private void initTerrainAndHeights() {
		Logger.log("Initializing terrain and heights...",5);
		
		for (int x=0; x<loader.sizeX; x++)
			for(int y=0; y<loader.sizeY; y++)
			{				
				InputTerrain t = InputTerrain.getInputTerrainAt(x, y);
				Climate c = Climate.getInputClimateAt(x, y);

				//Init Terrain
				switch (t)
				{
				default:
				case DEEP_WATER:
				case WATER:
				case MAJOR_RIVER:
					terrainArray[x][y] = Terrain.WATER;
					isWater[x][y] = true;
					break;
					
				case DESERT:
					terrainArray[x][y] = Terrain.DESERT;
					break;
					
				case SEMI_DESERT:
					terrainArray[x][y] = Terrain.COASTAL_DESERT;
					break;	
					
				case PLAINS:
					if (c == Climate.POLAR)
						terrainArray[x][y] = Terrain.ARCTIC;
					else
						terrainArray[x][y] = Terrain.PLAINS;
					break;

				case VOLCANO:
					hasVolcano = true;
				case MOUNTAIN:
				case PLATEAU:
				case MOUNTAIN_PEAK:
					if (c == Climate.POLAR)
						terrainArray[x][y] = Terrain.ARCTIC;
					else if (c == Climate.HOT)
						terrainArray[x][y] = Terrain.DESERT;
					else
						terrainArray[x][y] = Terrain.PLAINS;
					break;
					

				case FOREST_HILL:
				case FOREST:
					if (c == Climate.HOT)
						terrainArray[x][y] = Terrain.JUNGLE;
					else if (c == Climate.POLAR)
						terrainArray[x][y] = Terrain.ARCTIC;
					else
						terrainArray[x][y] = Terrain.PLAINS;
					break;
					
				case FARMLANDS:
					terrainArray[x][y] = Terrain.FARMLAND;
					break;
					
				case STEPPE:
					terrainArray[x][y] = Terrain.STEPPE;
					break;
					
				case UNUSED1:
					terrainArray[x][y] = Terrain.UNUSED1;
					break;
					
				case UNUSED2:
					terrainArray[x][y] = Terrain.UNUSED2;
					break;
				}
				
				//Init Heights
				switch (t)
				{
					case DEEP_WATER:
						heightArray[x][y] = DEEP_WATER_HEIGHT;
						break;
					case WATER:
						heightArray[x][y] = NORMAL_WATER_HEIGHT;
						break;
					case MAJOR_RIVER:
						heightArray[x][y] = SHALLOW_WATER_HEIGHT;
						break;
						
					default:
						heightArray[x][y] = (MIN_HEIGHT_LAND * 3 + Config.HILLS_HEIGHT) / 4;
						break;
						
					case UNUSED1:
						heightArray[x][y] = Config.CUSTOM_TERRAIN_1_HEIGHT;
						break;

					case UNUSED2:
						heightArray[x][y] = Config.CUSTOM_TERRAIN_2_HEIGHT;
						break;

					case FOREST_HILL:
					case PLATEAU:
					case VOLCANO:
						heightArray[x][y] = (Config.HILLS_HEIGHT + Config.MOUNTAIN_HEIGHT) / 2;
						break;
						
					case MOUNTAIN:
						heightArray[x][y] = (Config.MOUNTAIN_HEIGHT + Config.PEAK_HEIGHT) / 2;
						break;
						
					case MOUNTAIN_PEAK:
						heightArray[x][y] = Config.PEAK_HEIGHT;
						break;
				}
				
				if (terrainArray[x][y] == null)
					terrainArray[x][y] = Terrain.WATER;
				
				//Minor rivers initialize at lowest land level
				if (loader.bufInRivers != null && loader.riverArray[x][y].isRiverOrSpecial())
				{
					heightArray[x][y] = MIN_HEIGHT_LAND;
				}
			}		
	}
	
	private void smoothMajorRiverEdges() {
		Logger.log("smoothing major river edges...",0);
		for (int x=1; x<loader.sizeX-1; x++)
			for(int y=1; y<loader.sizeY-1; y++)
			{
				if (InputTerrain.getInputTerrainAt(x, y) != InputTerrain.MAJOR_RIVER)
				{
					boolean lc, rc, mu, md, lu, ld, ru, rd;
					
					lc = InputTerrain.getInputTerrainAt(x-1, y) == InputTerrain.MAJOR_RIVER;
					rc = InputTerrain.getInputTerrainAt(x+1, y) == InputTerrain.MAJOR_RIVER;
					mu = InputTerrain.getInputTerrainAt(x, y-1) == InputTerrain.MAJOR_RIVER;
					md = InputTerrain.getInputTerrainAt(x, y+1) == InputTerrain.MAJOR_RIVER;
					
					lu = InputTerrain.getInputTerrainAt(x-1, y-1) == InputTerrain.MAJOR_RIVER;
					ld = InputTerrain.getInputTerrainAt(x-1, y+1) == InputTerrain.MAJOR_RIVER;
					ru = InputTerrain.getInputTerrainAt(x+1, y-1) == InputTerrain.MAJOR_RIVER;
					rd = InputTerrain.getInputTerrainAt(x+1, y+1) == InputTerrain.MAJOR_RIVER;
					
					if ((lc && mu && !lu && !rc && !md) ||
							(lc && md && !ld && !rc && !mu) ||
							(rc && mu && !ru && !lc && !md) ||
							(rc && md && !rd && !lc && !mu))
					{
						//Logger.log("Adding major river @"+x+";"+y);
						loader.inputArray[x][y] = InputTerrain.MAJOR_RIVER;
					}
				}
			}
	}
	
	//Since the input map is half the size of the final map, coastlines don't have a very good "resolution".
	//This function somewhat solves that problem by rounding up square "land edges"
	private void smoothCoastlines(boolean water) throws IOException {
		Logger.log("smoothing coastlines...",5);
		
		boolean[][] smoothIsWater = new boolean[loader.sizeX][loader.sizeY];
		
		for (int x=0; x<loader.sizeX; x++)
			for(int y=0; y<loader.sizeY; y++)
			{
				smoothIsWater[x][y] = isWater[x][y];
				
				if (x==0 || x==loader.sizeX-1 || y==0 || y==loader.sizeY-1)
					continue;
				
				boolean ul, uc, ur;
				boolean ml, mc, mr;
				boolean dl, dc, dr;
				
				ul = isWater[x-1][y-1];
				uc = isWater[x][y-1];
				ur = isWater[x+1][y-1];
				ml = isWater[x-1][y];
				mc = isWater[x][y];
				mr = isWater[x+1][y];
				dl = isWater[x-1][y+1];
				dc = isWater[x][y+1];
				dr = isWater[x+1][y+1];
				
				if (mc != water)
					continue;
				
				if (!mc) //Land 
				{
					//Edged by water on 4 "sides" out of 8
					//A little more generous with water to help with major rivers
					if ((ul && uc /*&& ur*/ && mr && dr) ||
						(ur && mr /*&& dr*/ && dc && dl ) ||
						(dr && dc /*&& dl*/ && ml && ul) ||
						(dl && ml /*&& ul*/ && uc && ur))
					{
						terrainArray[x][y] = Terrain.WATER;
						smoothIsWater[x][y] = true;
					}
				}
				else //Water
				{
					//Edged by land on 5 "sides" out of 8
					if ((!ul && !uc && !ur && !mr && !dr) ||
						(!ur && !mr && !dr && !dc && !dl ) ||
						(!dr && !dc && !dl && !ml && !ul) ||
						(!dl && !ml && !ul && !uc && !ur))
					{
						terrainArray[x][y] = Terrain.WATER;
						smoothIsWater[x][y] = false;
					}
				}
			}
		
		isWater = smoothIsWater;
	}

	//Smoothes the height levels all over the map by setting each point at the average height of surrounding points
	//Degree determines the radius of the smoothing, higher means overall flatter
	//Cap limits this function to points that are below a certain height. This means that high lands like mountains can be less smoothed and have more "edge" than low lands.
	//Iter is the number of iterations, for logging purpose only
	private void smoothHeights(int radius, int cap, int iter) throws IOException
	{
		Logger.log("smoothing heights ("+iter+")...",0);
		
		int[][] smoothedHeightArray = new int[loader.sizeX][loader.sizeY];
		
		int count = 0;
		int countMax = loader.sizeX*loader.sizeY;
		
		for (int x=0; x<loader.sizeX; x++)
			for(int y=0; y<loader.sizeY; y++)
			{
				
				if (++count % (countMax/10) == 0)
					Logger.log("smoothing heights ("+iter+")..."+(10 * count / (countMax / 10)) + "%",1);
				
				if (heightArray[x][y] <= cap)
				{
					smoothedHeightArray[x][y] = getSmoothedHeightAtPoint(x,y,radius);
				}
				else
				{
					smoothedHeightArray[x][y] = heightArray[x][y];
				}
			}
		
		heightArray = smoothedHeightArray;
	}

	//Calculates the average height of all points around coordinates x,y within a radius of degree
	private int getSmoothedHeightAtPoint(int x, int y, int radius) {
		int sum = 0;
		int num = 0;
		int ret = heightArray[x][y];
		
		for (int rx=x-radius; rx<=x+radius; rx++)
			for (int ry=y-radius; ry<=y+radius; ry++)
			{ 
				if (Coordinates.isValidCoordinates(rx, ry))
					if (Utils.getDistanceSquared(rx, ry, x, y) <= radius*radius)
					{
						num++; 
						sum += heightArray[rx][ry];
					}
			}
		
		if (num > 0)
			ret = sum / num;
		
		if (isWater[x][y] && ret > MAX_HEIGHT_WATER)
			ret = MAX_HEIGHT_WATER;
		if (!isWater[x][y] && ret < MIN_HEIGHT_LAND)
			ret = MIN_HEIGHT_LAND;
		
		return ret;
	}

	//Adds a bit of randomness to hills and mountains to make them look more natural and interesting
	private void makeNoise() throws IOException {
		
		Logger.log("Adding noise...",10);
		
		int[][] noiseArray = new int[loader.sizeX][loader.sizeY];
		
		for (int x=0; x<loader.sizeX; x+=Config.NOISE_PATCH_SIZE)
			for (int y=0; y< loader.sizeY; y+=Config.NOISE_PATCH_SIZE)
			{
				double rand = Math.random() * Config.NOISE_FACTOR_MAX;
				
				for (int i=0; i<Config.NOISE_PATCH_SIZE && x+i<loader.sizeX ;i++)
					for (int j=0; j<Config.NOISE_PATCH_SIZE && y+j<loader.sizeY ;j++)
						//No height noise around water
						if (terrainArray[x+i][y+j] != Terrain.WATER && !(loader.bufInRivers != null && loader.riverArray[x+i][y+j].isRiverOrSpecial()))
							noiseArray[x+i][y+j] = 
							Math.max(MIN_HEIGHT_LAND, 
									Math.min(255, (int) (
											heightArray[x+i][y+j] + (Config.NOISE_BASELINE + heightArray[x+i][y+j] - MIN_HEIGHT_LAND) * rand)
											)
									);
						else
							noiseArray[x+i][y+j] = heightArray[x+i][y+j];
			}
		
		//No noise right around volcanoes
		if (hasVolcano)
		{
			for (int x=0; x<loader.sizeX; x+=2)
				for (int y=0; y<loader.sizeY; y+=2)
				{
					if (InputTerrain.getInputTerrainAt(x, y) == InputTerrain.VOLCANO)
					{
						noiseArray[x][y] = heightArray[x][y];
						noiseArray[x+1][y] = heightArray[x+1][y];
						noiseArray[x][y+1] = heightArray[x][y+1];
						noiseArray[x+1][y+1] = heightArray[x+1][y+1];
					}
				}
		}
		
		
		heightArray = noiseArray;
	}

	//If using the volcano special input terrain type, creates a crater that partially ignores height smoothing
	private void makeVolcanoes() throws IOException {
		Logger.log("Making volcanoes...",0);

		//Punch a hole in the ground
		for (int x=0; x<loader.sizeX; x++)
			for (int y=0; y< loader.sizeY; y++)
			{
				if (InputTerrain.getInputTerrainAt(x, y) == InputTerrain.VOLCANO)
				{
					heightArray[x][y] = Math.max(heightArray[x][y]-30, 0);
				}

			}

	}
	
	//Makes terrain more flat around cities because it doesn't look good otherwise
	private void smoothHeightsForSettlements() throws IOException {
		if (loader.settlementsList.isEmpty())
			return;
		
		Logger.log("Smoothing heights for settlements...",0);
		
		int smoothedHeightArray[][] = new int[loader.sizeX][loader.sizeY];
		
		for (int x=0; x<loader.sizeX; x++)
			for (int y=0; y<loader.sizeY; y++)
				smoothedHeightArray[x][y] = heightArray[x][y];
		
		for (int iteration=0; iteration<3; iteration++)
			for (Coordinates s : loader.settlementsList)
			{
				int radius = Config.SMOOTH_RADIUS, x=s.getX(), y=s.getY();
				for (int rx=x-radius; rx<=x+radius; rx++)
					for (int ry=y-radius; ry <=y+radius; ry++)
					{
						if (Coordinates.isValidCoordinates(rx,ry))
							if (Utils.getDistanceSquared(rx, ry, x, y) <= radius*radius)
								smoothedHeightArray[rx][ry] = getSmoothedHeightAtPoint(rx, ry, 3);
					}
			}
		
		heightArray = smoothedHeightArray;
	}	

	//Paints hill/mountain terrain
	//Shuffles terrain a bit to avoid hard, unnatural-looking transitions from one type to another
	private void smoothTerrain() throws IOException
	{
		Logger.log("smoothing terrain...",10);
		
		//Change terrain based on heights
		for (int x=0; x<loader.sizeX; x++)
			for(int y=0; y<loader.sizeY; y++)
			{				
				int mountainType = 0;
				
				if (InputTerrain.getInputTerrainAt(x, y) == InputTerrain.FOREST || InputTerrain.getInputTerrainAt(x, y) == InputTerrain.FOREST_HILL)
					continue;
				
				if (heightArray[x][y] > Config.PEAK_HEIGHT)
					if (Math.random() < 0.67 || heightArray[x][y] > (Config.PEAK_HEIGHT * 3 - Config.SNOW_HEIGHT) / 2)
						mountainType = 4;
					else
						mountainType = 3;
				else if (heightArray[x][y] > Config.SNOW_HEIGHT)
					if (Math.random() < 0.67 || heightArray[x][y] > (Config.SNOW_HEIGHT + Config.PEAK_HEIGHT) / 2)
						mountainType = 3;
					else
						mountainType = 2;
				else if (heightArray[x][y] > Config.MOUNTAIN_HEIGHT)
					if (Math.random() < 0.67 || heightArray[x][y] > (Config.MOUNTAIN_HEIGHT + Config.SNOW_HEIGHT) / 2)
						mountainType = 2;
					else
						mountainType = 1;
				else if (heightArray[x][y] > Config.HILLS_HEIGHT)
					if (Math.random() < 0.67 || heightArray[x][y] > (Config.HILLS_HEIGHT + Config.MOUNTAIN_HEIGHT) / 2)
						mountainType = 1;
					
				if (mountainType > 0)
				{
					switch (Climate.getInputClimateAt(x, y))
					{
					case HOT:
						switch (mountainType)
						{
						case 1:
							terrainArray[x][y] = Terrain.SANDY_MOUNTAIN; break;
						case 2:
							terrainArray[x][y] = Terrain.DESERT_MOUNTAIN; break;
						case 3:
							terrainArray[x][y] = Terrain.MOUNTAIN; break;
						default:
							if (Math.random() < 0.5)
								terrainArray[x][y] = Terrain.FROZEN_MOUNTAIN; 
							else
								terrainArray[x][y] = Terrain.SNOWY_MOUNTAIN;
						}
						break;
					case WARM:
						switch (mountainType)
						{
						case 1:
							terrainArray[x][y] = Terrain.FOREST_HILLS; break;
						case 2:
							terrainArray[x][y] = Terrain.MOUNTAIN; break;
						case 3:
							if (Math.random() < 0.5)
								terrainArray[x][y] = Terrain.SNOWY_MOUNTAIN; 
							else
								terrainArray[x][y] = Terrain.MOUNTAIN;
							break;
						default:
							if (Math.random() < 0.75)
								terrainArray[x][y] = Terrain.FROZEN_MOUNTAIN; 
							else
								terrainArray[x][y] = Terrain.SNOWY_MOUNTAIN;
							break;
						}
						break;
					default:
						switch (mountainType)
						{
						case 1:
							terrainArray[x][y] = Terrain.FOREST_HILLS; break;
						case 2:
							terrainArray[x][y] = Terrain.MOUNTAIN; break;
						case 3:
							terrainArray[x][y] = Terrain.SNOWY_MOUNTAIN; break;
						default:
							terrainArray[x][y] = Terrain.FROZEN_MOUNTAIN; break;
						}
						break;
					case COLD:
						switch (mountainType)
						{
						case 1:
							terrainArray[x][y] = Terrain.FOREST_HILLS; break;
						case 2:
							if (Math.random() < 0.25)
								terrainArray[x][y] = Terrain.SNOWY_MOUNTAIN;
							else
								terrainArray[x][y] = Terrain.MOUNTAIN; 
							break;
						case 3:
							if (Math.random() < 0.25)
								terrainArray[x][y] = Terrain.FROZEN_MOUNTAIN;
							else
								terrainArray[x][y] = Terrain.SNOWY_MOUNTAIN;
							break;
						default:
							terrainArray[x][y] = Terrain.FROZEN_MOUNTAIN; break;
						}
						break;
					case POLAR:
						switch (mountainType)
						{
						case 1:
							terrainArray[x][y] = Terrain.FOREST_HILLS; break;
						case 2:
							if (Math.random() < 0.5)
								terrainArray[x][y] = Terrain.SNOWY_MOUNTAIN;
							else
								terrainArray[x][y] = Terrain.MOUNTAIN; 
							break;
						case 3:
							if (Math.random() < 0.5)
								terrainArray[x][y] = Terrain.FROZEN_MOUNTAIN;
							else
								terrainArray[x][y] = Terrain.SNOWY_MOUNTAIN;
							break;
						default:
							terrainArray[x][y] = Terrain.FROZEN_MOUNTAIN; break;
						}
						break;
						
					}

				}


			}
		

		
		//Make smooth borders
		Terrain[][] smoothedTerrainArray = new Terrain[loader.sizeX][loader.sizeY];
		for (int x=0; x<loader.sizeX; x++)
			for(int y=0; y<loader.sizeY; y++)
			{
				smoothedTerrainArray[x][y] = terrainArray[x][y];
				
				int radius = 2;
				
				for (int rx=x-radius; rx<=x+radius; rx++)
					for (int ry=y-radius; ry<=y+radius; ry++)
					{
						if (Coordinates.isValidCoordinates(rx, ry))
						{

							if (Utils.getDistanceSquared(x,y,rx,ry) <= radius*radius)
							{
					
								//All water gets WATER (sandy beach) as a border
								if (isWater[rx][ry]
										&& !isWater[x][y] 
										&& (InputTerrain.getInputTerrainAt(rx, ry) == InputTerrain.DEEP_WATER || InputTerrain.getInputTerrainAt(rx, ry) == InputTerrain.WATER))
								{
									smoothedTerrainArray[x][y] = Terrain.WATER;
								}
		
								
								//Desert and non-desert terrain border is coastal desert
								if ((terrainArray[rx][ry] == Terrain.DESERT
										|| terrainArray[rx][ry] == Terrain.DESERT_MOUNTAIN
										|| terrainArray[rx][ry] == Terrain.SANDY_MOUNTAIN)
										&& (terrainArray[x][y] == Terrain.PLAINS
										|| terrainArray[x][y] == Terrain.STEPPE
										|| terrainArray[x][y] == Terrain.JUNGLE
										|| terrainArray[x][y] == Terrain.ARCTIC
										))
								{
									smoothedTerrainArray[x][y] = Terrain.COASTAL_DESERT;
									
									if (Math.random() < 0.5)
										smoothedTerrainArray[rx][ry] = Terrain.COASTAL_DESERT;
								}
								
								//Diffuse steppe border
								if (terrainArray[rx][ry] == Terrain.STEPPE 
										&& terrainArray[x][y] != Terrain.WATER
										&& terrainArray[x][y] != Terrain.COASTAL_DESERT
										&& terrainArray[x][y] != Terrain.STEPPE)
								{
									if (Math.random() < 0.5)
										smoothedTerrainArray[x][y] = Terrain.STEPPE;
								}
								
								//Diffuse arctic border
								if (terrainArray[rx][ry] == Terrain.ARCTIC 
										&& terrainArray[x][y] != Terrain.WATER
										&& terrainArray[x][y] != Terrain.COASTAL_DESERT
										&& terrainArray[x][y] != Terrain.ARCTIC)
								{
									if (Math.random() < 0.5)
										smoothedTerrainArray[x][y] = Terrain.ARCTIC;
								}
								
								//Diffuse coastal desert border
								if (terrainArray[rx][ry] == Terrain.COASTAL_DESERT 
										&& terrainArray[x][y] != Terrain.WATER
										&& terrainArray[x][y] != Terrain.COASTAL_DESERT)
								{
									if (Math.random() < 0.5)
										smoothedTerrainArray[x][y] = Terrain.COASTAL_DESERT;	
								}
								
								//Smooth farmlands border
								if (terrainArray[x][y] == Terrain.FARMLAND 
										&& terrainArray[rx][ry] != Terrain.WATER
										&& terrainArray[rx][ry] != Terrain.FARMLAND)
								{
									if (Math.random() < 0.5)
										smoothedTerrainArray[x][y] = Terrain.PLAINS;
								}
							}
						}
					}

			}
		
		//Rivers cause some vegetation to grow in the deserts
		for (int x=0; x<loader.sizeX; x++)
			for(int y=0; y<loader.sizeY; y++)
			{
				if (loader.bufInRivers != null && loader.riverArray[x][y].isRiverOrSpecial())
				{
					
					int radius=4;
					for (int rx=x-radius; rx<=x+radius; rx++)
						for (int ry=y-radius; ry<=y+radius; ry++)
						{
							if (Coordinates.isValidCoordinates(rx, ry))
							{
								int distance = Utils.getDistanceSquared(x,y,rx,ry);
								
								if (distance <= radius*radius)
								{
									if ((terrainArray[rx][ry] == Terrain.DESERT || terrainArray[rx][ry] == Terrain.COASTAL_DESERT) && Math.random() < 0.1 * distance)
									{
										if (Math.random() < 0.5)
											smoothedTerrainArray[rx][ry] = Terrain.PLAINS;
										else
											smoothedTerrainArray[rx][ry] = Terrain.COASTAL_DESERT;
										
										if (Math.random() < 0.5)
											treeArray[rx/Config.TREE_MAP_SCALE][ry/Config.TREE_MAP_SCALE] = Trees.PALM_TREE1;
									}
								}
							}
					}
				}
			}
			
		
		//Add some trees and coastal desert terrain right around desert settlements to make it look like an oasis of sorts (because how else would anyone live there ?)
		for (Coordinates s : loader.settlementsList)
		{
			
			int radius=3, x=s.getX(), y=s.getY();
			
			for (int rx=x-radius; rx<=x+radius; rx++)
				for (int ry=y-radius; ry<=y+radius; ry++)
				{
					if (Coordinates.isValidCoordinates(rx, ry))
					{

						if (Utils.getDistanceSquared(x,y,rx,ry) <= radius*radius)
						{
							if (terrainArray[rx][ry] == Terrain.DESERT ||
									terrainArray[rx][ry] == Terrain.COASTAL_DESERT ||
									terrainArray[rx][ry] == Terrain.SANDY_MOUNTAIN||
									terrainArray[rx][ry] == Terrain.DESERT_MOUNTAIN )
							{
								treeArray[rx/Config.TREE_MAP_SCALE][ry/Config.TREE_MAP_SCALE] = (Math.random() < 0.33 ? Trees.PALM_TREE2 : Math.random() < 0.5 ? Trees.PALM_TREE1 : treeArray[rx/Config.TREE_MAP_SCALE][ry/Config.TREE_MAP_SCALE]);
							}
							
							if (terrainArray[rx][ry] == Terrain.DESERT && Math.random() < 0.4)
								smoothedTerrainArray[rx][ry] = Terrain.COASTAL_DESERT;
						}
					}
				}
		}
		
		terrainArray = smoothedTerrainArray;
		
	}
	
	//Adds trees and stuff
	private void makeTrees() {
		
		Logger.log("Planting trees...",5);
		
		//Init Trees
		for (int x=0; x<loader.sizeX; x++)
			for(int y=0; y<loader.sizeY; y++)
			{
				InputTerrain i = InputTerrain.getInputTerrainAt(x, y);
				Terrain t = terrainArray[x][y];
				Climate c = Climate.getInputClimateAt(x, y);
				
				//Might be some trees already from smoothTerrain, so default to what is already present in the array
				Trees tree = treeArray[x/Config.TREE_MAP_SCALE][y/Config.TREE_MAP_SCALE];
				if (tree == null)
					tree = Trees.NO_TREES;
				
				
				if (x%Config.TREE_MAP_SCALE == Config.TREE_MAP_SCALE/2 && y%Config.TREE_MAP_SCALE == Config.TREE_MAP_SCALE/2)
				{
					int treeDensity = 0;
					
					switch (i)
					{
						default:
							switch (t)
							{
							default:
							case WATER:
								treeDensity = 0;
								break;
								
							case PLAINS:
								if (Math.random() < 0.2)
									treeDensity = 1;
								break;
								
							case ARCTIC:
							case COASTAL_DESERT:
							case FARMLAND:
							case FOREST_HILLS:
								if (Math.random() < 0.1)
									treeDensity = 1;
								break;
								

							case STEPPE:
							case DESERT:
								if (Math.random() < 0.02)
									treeDensity = 1;
								
							}
							break;
						case FOREST:
						case FOREST_HILL:
							if (Math.random() < 0.25)
								treeDensity = 3;
							else if (Math.random() < 0.25)
								treeDensity = 1;
							else
								treeDensity = 2;
							break;
					}
							
					
					if (treeDensity > 0)
					{
						switch (c)
						{
						case COLD:
						case POLAR:
							switch (treeDensity) 
							{
								case 1:
									tree = Trees.CONIFEROUS1; break;
								case 2:
									tree = Trees.CONIFEROUS2; break;
								default:
									tree = Trees.CONIFEROUS3; break;
							}
							break;
						case TEMPERATE:
							switch (treeDensity) 
							{
								case 1:
									tree = Trees.DECIDUOUS1; break;
								case 2:
									tree = Trees.DECIDUOUS2; break;
								default:
									tree = Trees.DECIDUOUS3; break;
							}
							break;
						case WARM:
							switch (treeDensity) 
							{
								case 1:
									tree = Trees.MEDITERRANEAN1; break;
								case 2:
									tree = Trees.MEDITERRANEAN2; break;
								default:
									tree = Trees.MEDITERRANEAN3; break;
							}
							break;
						case HOT:
							switch (treeDensity) 
							{
								case 1:
									tree = Trees.PALM_TREE1; break;
								default:
									tree = Trees.PALM_TREE2; break;
							}
							break;
						}
					}
					
					treeArray[x/Config.TREE_MAP_SCALE][y/Config.TREE_MAP_SCALE] = tree;
				}
			}

		
		//Clear out any trees that are too close to water or mountains
		for (int x=0; x<loader.sizeX; x++)
			for(int y=0; y<loader.sizeY; y++)
			{
				boolean clearTrees = false;
			
				switch (terrainArray[x][y])
				{
				//case ARCTIC:
				case WATER:
				case FROZEN_MOUNTAIN:
				case SNOWY_MOUNTAIN:
				case DESERT_MOUNTAIN:
					clearTrees = true;
					break;
				default:
					break;
				}
				
				/*
				 if (heightArray[x][y] >= Config.SNOW_HEIGHT)
					clearTrees = true;
					*/
				
				//Also clear trees near the edges of the map
				if (x <= 0 || x >= loader.sizeX - 1 || y <= 0 || y >= loader.sizeY - 1)
					clearTrees = true;
				
				if (clearTrees)
				{
					treeArray[x/Config.TREE_MAP_SCALE][y/Config.TREE_MAP_SCALE] = Trees.NO_TREES;

					if (x/Config.TREE_MAP_SCALE+1 < loader.sizeX/Config.TREE_MAP_SCALE)
						treeArray[x/Config.TREE_MAP_SCALE+1][y/Config.TREE_MAP_SCALE] = Trees.NO_TREES;
					if (y/Config.TREE_MAP_SCALE-1 >= 0)
						treeArray[x/Config.TREE_MAP_SCALE][y/Config.TREE_MAP_SCALE-1] = Trees.NO_TREES;
					if ((x/Config.TREE_MAP_SCALE+1 < loader.sizeX/Config.TREE_MAP_SCALE) && (y/Config.TREE_MAP_SCALE-1 >= 0))
						treeArray[x/Config.TREE_MAP_SCALE+1][y/Config.TREE_MAP_SCALE-1] = Trees.NO_TREES;
				}
				
			}
					
		//... or settlements
		for (Coordinates c : loader.settlementsList)
		{
			treeArray[c.getX()/Config.TREE_MAP_SCALE][c.getY()/Config.TREE_MAP_SCALE] = Trees.NO_TREES;
			if (c.getX()/Config.TREE_MAP_SCALE+1 < loader.sizeX/Config.TREE_MAP_SCALE)
				treeArray[c.getX()/Config.TREE_MAP_SCALE+1][c.getY()/Config.TREE_MAP_SCALE] = Trees.NO_TREES;
			if (c.getY()/Config.TREE_MAP_SCALE-1 >= 0)
				treeArray[c.getX()/Config.TREE_MAP_SCALE][c.getY()/Config.TREE_MAP_SCALE-1] = Trees.NO_TREES;
			if ((c.getX()/Config.TREE_MAP_SCALE+1 < loader.sizeX/Config.TREE_MAP_SCALE) && (c.getY()/Config.TREE_MAP_SCALE-1 >= 0))
				treeArray[c.getX()/Config.TREE_MAP_SCALE+1][c.getY()/Config.TREE_MAP_SCALE-1] = Trees.NO_TREES;
		}
	}

	
	//Makes the normal map, or a workable approximation of it.
	//A normal map is a bitmap where every pixel's color(r,g,b) represents a 3D vector(x,y,z)
	//color component -> vector coordinate
	//0 -> -1
	//128 -> 0
	//255 -> 1
	//If the vector is pointing straight up(0,0,1), such as on an horizontal surface, the rgb value is then (128, 128, 255)
	private void makeNormalMap() throws IOException {
		Logger.log("Making normal map...",5);
		
		int xComp, yComp, zComp;
		
		for (int x=0 ; x<loader.sizeX ; x++)
			for (int y=0 ; y<loader.sizeY ; y++)
			{
				//Compare height at these coordinates with height at neighbouring coordinates
				//The math isn't correct at all, but at least the vector is pointing roughly in the right direction and it looks good enough in-game
				//Magic number : 11. Because 11² is just less than 127
				xComp = Math.min(11, Math.max(-11, (heightArray[Math.max(0, x-1)][y] - heightArray[Math.min(loader.sizeX-1, x+1)][y]) ));
				yComp = Math.min(11, Math.max(-11, (heightArray[x][Math.max(0, y-1)] - heightArray[x][Math.min(loader.sizeY-1, y+1)]) ));
				
				//Clear out negligible height differences to limit shadow artifacts in almost-flat plains
				if (heightArray[x][y] < Config.HILLS_HEIGHT)
				{
					if (Math.abs(xComp) <= 1)
						xComp = 0;
					if (Math.abs(yComp) <= 1)
						yComp = 0;
				}
				
				//Exaggerate for effect
				xComp = xComp * Math.abs(xComp);
				yComp = yComp * Math.abs(yComp);

				//This is supposed to have a total vector length of 127
				//x²+y²+z²=127² => z²=127²-x²-y²
				zComp = (int)Math.sqrt(Math.max(0,127*127 - xComp*xComp - yComp*yComp));
				
				normalArray[x][y] = new Color(xComp + 128, yComp + 128, zComp + 128).getRGB();
			}
	}
	
}
