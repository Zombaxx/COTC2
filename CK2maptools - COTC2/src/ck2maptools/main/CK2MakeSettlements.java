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
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import ck2maptools.data.Coordinates;
import ck2maptools.data.InputFile;
import ck2maptools.data.InputTerrain;
import ck2maptools.data.Loader;
import ck2maptools.data.OutputFile;
import ck2maptools.data.Terrain;
import ck2maptools.utils.Config;
import ck2maptools.utils.Logger;
import ck2maptools.utils.Utils;

public class CK2MakeSettlements implements ICK2MapTool {

	private Loader loader;
	private boolean makeLand=false;
	private boolean makeWater=false;
	
	public void setParamMakeLand(boolean b){ makeLand=b; }
	public void setParamMakeWater(boolean b){ makeWater=b; }
	
	public static InputFile[] inputFiles() {
		return new InputFile[]{
				InputFile.Input,
				InputFile.Terrain,
				InputFile.Settlements
		};
	}
	
	public static InputFile[] inputFilesOptional() {
		return new InputFile[]{
				InputFile.Rivers,
		};
	}

	public static OutputFile[] outputFiles() {
		return new OutputFile[]{

		};
	}
	
	public static InputFile[] outputFilesInput() {
		return new InputFile[]{
				InputFile.Settlements //Generates / Updates
		};
	}
	
	public static void main(String[] args) throws Exception {
		CK2MakeSettlements t = new CK2MakeSettlements();
		
		//Argument : -l / -land : the program will generate land provinces
		//Argument : -w / -water : the program will generate water provinces
		for (String arg : args)
		{
			if (arg == null)
				continue;
			
			if (arg.equalsIgnoreCase("-l") || arg.equalsIgnoreCase("-duchies"))
			{
				t.makeLand = true;
				Logger.log("Will make De Jure Duchies");
			}
			
			if (arg.equalsIgnoreCase("-w") || arg.equalsIgnoreCase("-kingdoms"))
			{
				t.makeWater = true;
				Logger.log("Will make De Jure Kingdoms");
			}
		}
		
		t.execute();
	}
	
	public int execute() throws Exception {
		int returnCode = ERROR_NONE;
		Logger.InitLogger("CK2MakeSettlements");
		long ms = System.currentTimeMillis();
		
		Utils.checkCriticalResources(inputFiles(), true);
		
		//Load the input maps
		loader = Loader.getLoader();
		loader.loadSettlements();
		loader.loadTerrain();
		loader.loadHeights();
		loader.loadInput();
		loader.loadRivers();
		
		//Parse config.csv
		Config.parseConfig();

		if (makeLand)
		{
			Logger.log("Populating along rivers and coastlines...", 0);
			int maxTries=loader.sizeX*loader.sizeY/5;
			for (int tries=1; tries<maxTries; tries++)
			{
				if (tries % (maxTries/10) == 0)
					Logger.log("Populating along rivers and coastlines..."+(10 * tries / (maxTries/10)) + "%", 3);
				
				int x=(int) (Math.random() * loader.sizeX);
				int y=(int) (Math.random() * loader.sizeY);
				
				if (loader.riverArray != null && loader.riverArray[x][y].isRiverOrSpecial()) //On a minor river
				{
					addSettlementIfValid(x,y, false);
					continue;
				}
	
				//Try to be close to water
				int radius = 5;
				for (int rx=x-radius; rx<=x+radius; rx++)
					for (int ry=y-radius; ry<=y+radius; ry++)
					{
						if (Coordinates.isValidCoordinates(rx, ry))
						{
							if (Utils.getDistanceSquared(rx, ry, x, y) <= radius*radius)
							{
								if (loader.isWater[rx][ry]) //Some sea, lake or major river is nearby, try to add it
								{
									addSettlementIfValid(x,y,false);
									break;
								}
							}
						}
					}
			}
			
			Logger.log("Populating at Random...", 0);
			maxTries=loader.sizeX*loader.sizeY/10;
			for (int tries=1; tries<maxTries; tries++)
			{
				if (tries % (maxTries/10) == 0)
					Logger.log("Populating at Random..."+(10 * tries / (maxTries/10)) + "%", 3);
				
				int x=(int) (Math.random() * loader.sizeX);
				int y=(int) (Math.random() * loader.sizeY);
				
				addSettlementIfValid(x,y,false);
			}
		}
		
		if (makeWater)
		{
			Logger.log("Marking water at Random...", 0);
			int maxTries=loader.sizeX*loader.sizeY/10;
			for (int tries=1; tries<maxTries; tries++)
			{
				if (tries % (maxTries/10) == 0)
					Logger.log("Marking water at Random..."+(10 * tries / (maxTries/10)) + "%", 3);
				
				int x=(int) (Math.random() * loader.sizeX);
				int y=(int) (Math.random() * loader.sizeY);
				
				addSettlementIfValid(x,y,true);
			}
		}
		
		//Build output
		for (Coordinates settlement : loader.settlementsList)
			loader.bufInSettlements.setRGB(settlement.getX(), settlement.getY(), Color.RED.getRGB());
		
		for (Coordinates settlement : loader.importantSettlementsList)
			loader.bufInSettlements.setRGB(settlement.getX(), settlement.getY(), Color.YELLOW.getRGB());
		
		for (Coordinates settlement : loader.watersList)
			loader.bufInSettlements.setRGB(settlement.getX(), settlement.getY(), Color.BLUE.getRGB());
		
		for (Coordinates settlement : loader.riversList)
			loader.bufInSettlements.setRGB(settlement.getX(), settlement.getY(), Color.CYAN.getRGB());
		
		//Backup the existing image, if it exists
		File inputSettlements = new File(InputFile.Settlements.getFileName());
		File inputSettlementsBackup = new File(InputFile.Settlements.getFileName().replace(".bmp", "."+Utils.getDateString()+".bmp"));
		if (inputSettlements.exists())
		{
			Files.copy(inputSettlements.toPath(), inputSettlementsBackup.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		
		//Update or generate settlements.bmp
		Utils.writeOutputImage(InputFile.Settlements.getFileName(), loader.bufInSettlements);
		
		Logger.log("Total "+loader.settlementsList.size()+" settlements and "+loader.watersList.size()+" sea nodes");
		
		Logger.log("Done in "+(System.currentTimeMillis()-ms)+"ms",100);
		Logger.close();
		return returnCode;
	}

	private void addSettlementIfValid(int x, int y, boolean water) {
		
		//Immediately disqualify map edges
		if ( x < Config.MIN_SETTLEMENT_DISTANCE/3 || x > loader.sizeX - Config.MIN_SETTLEMENT_DISTANCE/3 || y < Config.MIN_SETTLEMENT_DISTANCE/3 || y > loader.sizeY - Config.MIN_SETTLEMENT_DISTANCE/3)
			return;
		
		int minDistance = water ? Config.MIN_SEA_NODE_DISTANCE : Config.MIN_SETTLEMENT_DISTANCE;
		
		if (water)
		{
			//Not water, abort
			if (!loader.isWater[x][y])
				return;
			
			//Shallow water, reduce distance
			if (InputTerrain.getInputTerrainAt(x, y) == InputTerrain.MAJOR_RIVER)
				 minDistance = (int) (Config.MIN_SEA_NODE_DISTANCE * 0.75);
			
			//Deep water, increase distance
			if (InputTerrain.getInputTerrainAt(x, y) == InputTerrain.DEEP_WATER)
				 minDistance = (int) (Config.MIN_SEA_NODE_DISTANCE * 1.5);
		}
		else
		{
			//Uninhabitable terrain, abort
			if (loader.bufInSettlements.getRGB(x, y) == Color.black.getRGB() ||
					InputTerrain.getInputTerrainAt(x, y) == InputTerrain.MOUNTAIN_PEAK ||
					InputTerrain.getInputTerrainAt(x, y) == InputTerrain.MOUNTAIN ||
					loader.isWater[x][y])
				return;
			
			//Barely habitable, increase distance
			if (loader.terrainArray[x][y] == Terrain.DESERT || 
					loader.terrainArray[x][y] == Terrain.ARCTIC || 
					loader.terrainArray[x][y] == Terrain.STEPPE || 
					InputTerrain.getInputTerrainAt(x, y) == InputTerrain.FOREST ||
					loader.terrainArray[x][y] == Terrain.DESERT_MOUNTAIN || 
					loader.terrainArray[x][y] == Terrain.MOUNTAIN)
				 minDistance = (int) (Config.MIN_SETTLEMENT_DISTANCE * 1.25);
			
			//Very habitable, reduce distance
			if (loader.terrainArray[x][y] == Terrain.FARMLAND)
				minDistance = (int) (Config.MIN_SETTLEMENT_DISTANCE * 0.8);
			
			//On a minor river, reduce distance
			if (loader.riverArray != null && loader.riverArray[x][y].isRiverOrSpecial()) 
				minDistance = (int) (Config.MIN_SETTLEMENT_DISTANCE * 0.75);
		}
		
		Coordinates newSettlement = new Coordinates(x,y);
		
		//Try not to be too close to water/land
		int radius = water ? 20 : 3;
		for (int rx=x-radius; rx<=x+radius; rx++)
			for (int ry=y-radius; ry<=y+radius; ry++)
			{
				if (Coordinates.isValidCoordinates(rx, ry))
				{
					if (Utils.getDistanceSquared(rx, ry, x, y) <= radius*radius)
					{
						if (loader.isWater[rx][ry])
						{
							if (!water)
								return; //Too close, abort
						}
						else
						{
							if (water)
								return; //Too close, abort
						}
					}
				}
			}

		
		if (water)
		{
			//Try not to be close to another existing settlement
			for (Coordinates settlement : loader.watersList)
			{
				if (Utils.getDistanceSquared(newSettlement, settlement) < minDistance * minDistance)
					return; //Too close, abort
			}
			
			loader.watersList.add(newSettlement);			
		}
		else
		{
			//Try not to be close to another existing settlement
			for (Coordinates settlement : loader.settlementsList)
			{
				if (Utils.getDistanceSquared(newSettlement, settlement) < minDistance * minDistance)
					return; //Too close, abort
			}
			
			loader.settlementsList.add(newSettlement);
		}
	}
}
