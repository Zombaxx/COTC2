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

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ck2maptools.data.Coordinates;
import ck2maptools.data.InputFile;
import ck2maptools.data.Loader;
import ck2maptools.data.OutputFile;
import ck2maptools.data.Province;
import ck2maptools.utils.Config;
import ck2maptools.utils.Logger;
import ck2maptools.utils.Utils;

public class CK2MakeAdjacencies implements ICK2MapTool {
	
	private Loader loader;

	public static InputFile[] inputFiles() {
		return new InputFile[]{
				InputFile.Terrain,
				InputFile.Settlements,
				InputFile.Provinces
		};
	}

	public static OutputFile[] outputFiles() {
		return new OutputFile[]{
				OutputFile.Adjacencies,
				OutputFile.AlternateStart
		};
	}

	
	public static void main(String[] args) throws Exception {
		new CK2MakeAdjacencies().execute();
	}
	
	public int execute() throws Exception {
		int returnCode = ERROR_NONE;
		long ms = System.currentTimeMillis();
		Logger.InitLogger("CK2MakeAdjacencies");
		
		Utils.checkCriticalResources(inputFiles(), true);	
		

		
		//Parse config.csv
		Config.parseConfig();
		
		//Load the input map
		loader = Loader.getLoader();
		loader.loadTerrain();
		loader.loadSettlements();
		loader.loadProvinces(true, false);
		
		FileWriter writer;
		Utils.mkDir("./output/map");

		//Make the adjacencies.csv file
		loader.getIslandRegions(); //Calculate island regions without straits
		
		//Refactor: riverbanks and coastlines are no longer permanently stored as Lists of Coordinates, but for this specific case they're much more efficient, so reconstruct them on the spot right here
		Logger.log("Finding coastlines and riverbanks...", 0);
		Map<Province, List<Coordinates>> coastlines = new HashMap<Province, List<Coordinates>>();
		Map<Province, List<Coordinates>> riverbanks = new HashMap<Province, List<Coordinates>>();

		for (int x=0; x<loader.sizeX; x++)
			for (int y=0; y<loader.sizeY; y++)
			{
				Province p = loader.provinceArray[x][y];
				
				if (p == null)
					continue;
				
				if (p.isWasteland() || p.isWater())
					continue;
				
				if (loader.provinceCoastlineArray[x][y])
				{
					List<Coordinates>coastline = coastlines.get(p);
					
					if (coastline==null)
					{
						coastline=new ArrayList<Coordinates>();
						coastlines.put(p, coastline);
					}
					coastline.add(new Coordinates(x,y));
				}
				if (loader.provinceRiverbankArray[x][y])
				{
					List<Coordinates>riverbank = riverbanks.get(p);
					
					if (riverbank==null)
					{
						riverbank=new ArrayList<Coordinates>();
						riverbanks.put(p, riverbank);
					}
					riverbank.add(new Coordinates(x,y));
				}
			}
		
		File adjacenciesCsv = new File(OutputFile.Adjacencies.getFileName());
		Logger.log("Writing "+adjacenciesCsv.getPath(), 20);
		writer = new FileWriter(adjacenciesCsv);
		writer.write("From;To;Type;Through;-1;-1;-1;-1;Comment");
		
		//First make all river crossings
		Logger.log("Finding river crossings...", 10);
		for (Province r : loader.waterProvinceList)
		{
			//River crossings
			if (r.isRiver())
			{
				for (Province p1 : r.getAdjacentProvinces())
				{
					//Shouldn't be possible, but...
					if (riverbanks.get(p1) == null)
					{
						continue;
					}
					
					for (Province p2 : r.getAdjacentProvinces())
					{
						//Shouldn't be possible, but...
						if (riverbanks.get(p2) == null)
						{
							continue;
						}
						
						//two land provinces not connected to each other, both connected to this river
						if (!p1.isAdjacentProvince(p2) && p1.getIndex() < p2.getIndex())
						{
							//Is there any point of province 1 that is close enough to any point from province 2 ?
							boolean connected = false;
							
							for (Coordinates c1 : riverbanks.get(p1))
							{			
								for (Coordinates c2 : riverbanks.get(p2))
								{
									if (Utils.getDistanceSquared(c1, c2) <= 64 )
									{
										connected = true;
										p1.addIndirectAdjacentProvince(p2);
										p2.addIndirectAdjacentProvince(p1);
										writer.write("\r\n"+p1.getIndex()+";"+p2.getIndex()+";major_river;"+r.getIndex()+";-1;-1;-1;-1;"+p1.getProvinceName()+"-"+p2.getProvinceName());
										break;
									}
								}
								
								if (connected)
									break;
							}
						}
					}
				}
			}
		}
		
		//Recalculate island regions
		List<List<Province>> islandRegions = loader.getIslandRegions();
		
		//Now add straits between islandRegions
		Logger.log("Finding straits...", 40);
		for (List<Province> islandRegion1 : islandRegions)
		{
			for (List<Province> islandRegion2 : islandRegions)
			{
				if (islandRegion1 != islandRegion2 //Regions must be different. 
					&& islandRegion1.get(0).getIndex() < islandRegion2.get(0).getIndex()) //Also ensure no duplicates by comparing index of first province
				{
					//Find the shortest possible strait between the 2 island regions
					int bestDistance = Integer.MAX_VALUE;
					Province bestP1 = null, bestP2 = null, bestWater = null;
					
					for (Province p1 : islandRegion1)
					{
						if (coastlines.get(p1) == null)
						{
							continue;
						}
						
						for (Province p2 : islandRegion2)
						{
							if (coastlines.get(p2) == null)
							{
								continue;
							}
							
							for (Province w : p1.getAdjacentWaterProvinces())
							{
								if (!w.isRiver() && w.isAdjacentProvince(p2)) //Try to find a sea zone that connects the 2. Must not be a river.
								{
									
									for (Coordinates c1 : coastlines.get(p1))
									{
										for (Coordinates c2 : coastlines.get(p2))
										{
											//Is there any point of province 1 that is close enough to any point from province 2 to make a land bridge ?
											int dist = Utils.getDistanceSquared(c1, c2); 
											
											if (dist <= (Config.MAX_STRAIT_DISTANCE*Config.MAX_STRAIT_DISTANCE) && dist < bestDistance)
											{
												bestDistance = dist;
												bestP1 = p1;
												bestP2 = p2;
												bestWater = w;
											}
										}
									}
								}
							}
							
						}
					}
					
					if (bestP1 != null && bestP2 != null)
					{
						bestP1.addIndirectAdjacentProvince(bestP2);
						bestP2.addIndirectAdjacentProvince(bestP1);
						writer.write("\r\n"+bestP1.getIndex()+";"+bestP2.getIndex()+";sea;"+bestWater.getIndex()+";-1;-1;-1;-1;"+bestP1.getProvinceName()+"-"+bestP2.getProvinceName());
					}
					
					
				}
			}
		}
		
		if (loader.waterProvinceList.size() > 0)
		{
			//BUG: add a placeholder portage so that it will work...
			Province firstWater = loader.waterProvinceList.get(0);
			Province firstLand = loader.provinceList.get(0);
			writer.write("\r\n"+firstWater.getIndex()+";"+firstWater.getIndex()+";portage;"+firstLand.getIndex()+";-1;-1;-1;-1;It will stop working if you remove this");
		}
		writer.close();

		//NEW with Holy Fury : Make alternate_start data to generate random world
		//This causes crashes if not handled properly...
		File outputDirAlternateStart = Utils.mkDir("./output/common/alternate_start");
		File alternateStartSpread = new File(outputDirAlternateStart + "/01_spread.txt");
		Logger.log("Writing "+alternateStartSpread.getPath(), 40);
		writer = new FileWriter(alternateStartSpread);

		writer.write("adjacencies = {\r\n");
		
		islandRegions = loader.getIslandRegions();
		islandRegions.remove(0); //Ignore the continent
		for (List<Province> region : islandRegions)
		{
			if (region.size() > 0)
			{
				int bestDistance = Integer.MAX_VALUE;
				Province bestP1 = null;
				Province bestP2 = null;
				
				for (Province p1 : region)
				{
					for (Province p2 : loader.provinceList)
					{
						if (p1.getIslandRegion() != p2.getIslandRegion() && p1.getIslandRegion().size() <= p2.getIslandRegion().size())
						{
							int distance = Utils.getDistanceSquared(p1, p2);
							
							if (distance < bestDistance)
							{
								bestP1 = p1;
								bestP2 = p2;
								bestDistance = distance;
							}
						}
					}
				}
				
				//To avoid island regions being completely cutoff from each other, create an adjacency between the two nearest provinces of any island region and any other island region, that will allow cultures to spread there
				if (bestP1 != null && bestP2 != null)
				{
					writer.write("\t"+bestP1.getIndex()+" = "+bestP2.getIndex()+" #"+bestP1.getProvinceName()+" - "+bestP2.getProvinceName()+"\r\n");
				}
			}
		}
		writer.write("}\r\n");
		writer.write("\r\n");
		writer.write("\r\n");

		//Vanilla has 1751 land provinces as of today, how does this compare to our current map ?
		int numProvinceRatio = 10 * loader.provinceList.size() / 1751;
		
		writer.write("culture_group_spread = {\r\n");
		writer.write("\tfactor = 100\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 10\r\n");
		writer.write("\t\tnum_culture_provinces < "+(numProvinceRatio*2)+"\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.8\r\n");
		writer.write("\t\tnum_culture_provinces > "+(numProvinceRatio*3)+"\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.8\r\n");
		writer.write("\t\tnum_culture_provinces > "+(numProvinceRatio*4)+"\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.8\r\n");
		writer.write("\t\tnum_culture_provinces > "+(numProvinceRatio*5)+"\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.8\r\n");
		writer.write("\t\tnum_culture_provinces > "+(numProvinceRatio*6)+"\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.8\r\n");
		writer.write("\t\tnum_culture_provinces > "+(numProvinceRatio*7)+"\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.8\r\n");
		writer.write("\t\tnum_culture_provinces > "+(numProvinceRatio*8)+"\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.8\r\n");
		writer.write("\t\tnum_culture_provinces > "+(numProvinceRatio*9)+"\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.8\r\n");
		writer.write("\t\tnum_culture_provinces > "+(numProvinceRatio*10)+"\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.8\r\n");
		writer.write("\t\tnum_culture_provinces > "+(numProvinceRatio*11)+"\r\n");
		writer.write("\t}\r\n");
		writer.write("\t#TODO: Add culture_group specific modifiers here\r\n");
		writer.write("}\r\n");
		writer.write("\r\n");


		writer.write("culture_spread = {\r\n");
		writer.write("\tfactor = 100\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 10\r\n");
		writer.write("\t\tnum_culture_provinces < 3\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.8\r\n");
		writer.write("\t\tnum_culture_provinces > 5\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.8\r\n");
		writer.write("\t\tnum_culture_provinces > 10\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.8\r\n");
		writer.write("\t\tnum_culture_provinces > 15\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.8\r\n");
		writer.write("\t\tnum_culture_provinces > 20\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.8\r\n");
		writer.write("\t\tnum_culture_provinces > 25\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.8\r\n");
		writer.write("\t\tnum_culture_provinces > 30\r\n");
		writer.write("\t}\r\n");
		writer.write("\t#TODO: Add culture specific modifiers here\r\n");
		writer.write("}\r\n");
		writer.write("\r\n");
		writer.write("\r\n");
		
		//Vanilla is 3072 pixels wide, how does this compare to our current map ?
		int mapSizeRatio = 10 * loader.sizeX / 3072;
		
		writer.write("religion_group_spawn = {\r\n");
		writer.write("\tfactor = 100\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.001\r\n");
		writer.write("\t\tany_religion_distance < "+mapSizeRatio*20+"\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.1\r\n");
		writer.write("\t\tany_religion_distance < "+mapSizeRatio*35+"\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = { # Avoid starting on islands\r\n");
		writer.write("\t\tfactor = 0.1\r\n");
		writer.write("\t\tis_island = yes\r\n");
		writer.write("\t}\r\n");
		writer.write("}\r\n");
		writer.write("\r\n");
		
		writer.write("religion_group_spread = {\r\n");
		writer.write("\tfactor = 100\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 10\r\n");
		writer.write("\t\tnum_religion_provinces < "+(numProvinceRatio*2)+"\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.8\r\n");
		writer.write("\t\tnum_religion_provinces > "+(numProvinceRatio*3)+"\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.8\r\n");
		writer.write("\t\tnum_religion_provinces > "+(numProvinceRatio*4)+"\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.8\r\n");
		writer.write("\t\tnum_religion_provinces > "+(numProvinceRatio*5)+"\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.8\r\n");
		writer.write("\t\tnum_religion_provinces > "+(numProvinceRatio*6)+"\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.8\r\n");
		writer.write("\t\tnum_religion_provinces > "+(numProvinceRatio*7)+"\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.8\r\n");
		writer.write("\t\tnum_religion_provinces > "+(numProvinceRatio*8)+"\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.8\r\n");
		writer.write("\t\tnum_religion_provinces > "+(numProvinceRatio*9)+"\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.8\r\n");
		writer.write("\t\tnum_religion_provinces > "+(numProvinceRatio*10)+"\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.8\r\n");
		writer.write("\t\tnum_religion_provinces > "+(numProvinceRatio*11)+"\r\n");
		writer.write("\t}\r\n");
		writer.write("\t#TODO: Add religion_group specific modifiers here\r\n");
		writer.write("}\r\n");
		writer.write("\r\n");


		writer.write("religion_spread = {\r\n");
		writer.write("\tfactor = 100\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 10\r\n");
		writer.write("\t\tnum_religion_provinces < 3\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.8\r\n");
		writer.write("\t\tnum_religion_provinces > 5\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.8\r\n");
		writer.write("\t\tnum_religion_provinces > 10\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.8\r\n");
		writer.write("\t\tnum_religion_provinces > 15\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.8\r\n");
		writer.write("\t\tnum_religion_provinces > 20\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.8\r\n");
		writer.write("\t\tnum_religion_provinces > 25\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.8\r\n");
		writer.write("\t\tnum_religion_provinces > 30\r\n");
		writer.write("\t}\r\n");
		writer.write("\t#TODO: Add religion specific modifiers here\r\n");
		writer.write("}\r\n");		
		writer.write("\r\n");
		writer.write("\r\n");
		
		
		writer.write("dejure_kingdom_spread = {\r\n");
		writer.write("\tfactor = 100\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 10\r\n");
		writer.write("\t\tdistance = { where = FROM value < 100 }\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.6\r\n");
		writer.write("\t\tdistance = { where = FROM value > 200 }\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.6\r\n");
		writer.write("\t\tdistance = { where = FROM value > 300 }\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.6\r\n");
		writer.write("\t\tdistance = { where = FROM value > 400 }\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.6\r\n");
		writer.write("\t\tdistance = { where = FROM value > 500 }\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.6\r\n");
		writer.write("\t\tdistance = { where = FROM value > 600 }\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.6\r\n");
		writer.write("\t\tdistance = { where = FROM value > 700 }\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.6\r\n");
		writer.write("\t\tdistance = { where = FROM value > 800 }\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.6\r\n");
		writer.write("\t\tdistance = { where = FROM value > 900 }\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.6\r\n");
		writer.write("\t\tdistance = { where = FROM value > 1000 }\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = { #Island kingdoms should stick to islands\r\n");
		writer.write("\t\tfactor = 0.01\r\n");
		writer.write("\t\tOR = {\r\n");
		writer.write("\t\t\tAND = {\r\n");
		writer.write("\t\t\t\tis_island = yes\r\n");
		writer.write("\t\t\t\tFROMFROM = { is_island = no }\r\n");
		writer.write("\t\t\tAND = {\r\n");
		writer.write("\t\t\t}\r\n");
		writer.write("\t\t\t\tis_island = no\r\n");
		writer.write("\t\t\t\tFROMFROM = { is_island = yes }\r\n");
		writer.write("\t\t\t}\r\n");
		writer.write("\t\t}\r\n");
		writer.write("\t}\r\n");
		writer.write("}\r\n");
		writer.write("\r\n");
		
		
		writer.write("dejure_empire_spread = {\r\n");
		writer.write("\tfactor = 100\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 5\r\n");
		writer.write("\t\tdistance = { where = FROM value < 100 }\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.6\r\n");
		writer.write("\t\tdistance = { where = FROM value > 200 }\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.6\r\n");
		writer.write("\t\tdistance = { where = FROM value > 300 }\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.6\r\n");
		writer.write("\t\tdistance = { where = FROM value > 400 }\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.6\r\n");
		writer.write("\t\tdistance = { where = FROM value > 500 }\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.6\r\n");
		writer.write("\t\tdistance = { where = FROM value > 600 }\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.6\r\n");
		writer.write("\t\tdistance = { where = FROM value > 700 }\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.6\r\n");
		writer.write("\t\tdistance = { where = FROM value > 800 }\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.6\r\n");
		writer.write("\t\tdistance = { where = FROM value > 900 }\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.6\r\n");
		writer.write("\t\tdistance = { where = FROM value > 1000 }\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = { #Island empires should stick to islands\r\n");
		writer.write("\t\tfactor = 0.01\r\n");
		writer.write("\t\tOR = {\r\n");
		writer.write("\t\t\tAND = {\r\n");
		writer.write("\t\t\t\tis_island = yes\r\n");
		writer.write("\t\t\t\tFROMFROM = { is_island = no }\r\n");
		writer.write("\t\t\tAND = {\r\n");
		writer.write("\t\t\t}\r\n");
		writer.write("\t\t\t\tis_island = no\r\n");
		writer.write("\t\t\t\tFROMFROM = { is_island = yes }\r\n");
		writer.write("\t\t\t}\r\n");
		writer.write("\t\t}\r\n");
		writer.write("\t}\r\n");
		writer.write("}\r\n");
		writer.write("\r\n");
		writer.write("\r\n");

		
		writer.write("holy_site_spread = {\r\n");
		writer.write("\tfactor = 100\r\n");
		writer.write("\t# We want three holy sites within the religion's borders, and two outside\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0\r\n");
		writer.write("\t\tNOT = { religion = FROM }\r\n");
		writer.write("\t\tFROM = { num_holy_sites_generated < 1 }\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.01\r\n");
		writer.write("\t\tNOT = { religion = FROM }\r\n");
		writer.write("\t\tNOT = { parent_religion = { religion = FROM } }\r\n");
		writer.write("\t\tFROM = { num_holy_sites_generated < 3 }\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.001\r\n");
		writer.write("\t\tNOT = { religion_group = FROM }\r\n");
		writer.write("\t\tFROM = { num_holy_sites_generated < 3 }\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.001\r\n");
		writer.write("\t\treligion = FROM\r\n");
		writer.write("\t\tFROM = { num_holy_sites_generated >= 3 }\r\n");
		writer.write("\t}\r\n");
		writer.write("\t# Stay near our borders\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 2\r\n");
		writer.write("\t\treligion_group = FROM\r\n");
		writer.write("\t}\r\n");
		writer.write("\t# Avoid clustering\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0 # We simply never want to be adjacent to a holy site\r\n");
		writer.write("\t\tany_neighbor_province = {\r\n");
		writer.write("\t\t\tis_holy_site = FROM\r\n");
		writer.write("\t\t}\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0\r\n");
		writer.write("\t\tholy_site_distance = { target = FROM value < "+(10*mapSizeRatio)+" }\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.01\r\n");
		writer.write("\t\tholy_site_distance = { target = FROM value < "+(20*mapSizeRatio)+" }\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0.1\r\n");
		writer.write("\t\tholy_site_distance = { target = FROM value < "+(30*mapSizeRatio)+" }\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 20\r\n");
		writer.write("\t\tholy_site_distance = { target = FROM value > "+(50*mapSizeRatio)+" }\r\n");
		writer.write("\t}\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 0 # We use this as a proxy for religion distance, because religion distance is expensive to calculate\r\n");
		writer.write("\t\tNOT = { religion = FROM }\r\n");
		writer.write("\t\tNAND = {\r\n");
		writer.write("\t\t\tparent_religion = { religion = FROM }\r\n");
		writer.write("\t\t\tFROM = { num_holy_sites_generated >= 3 } # Heresies can put their 3rd holy site in their parent religion\r\n");
		writer.write("\t\t}\r\n");
		writer.write("\t\tFROM = { num_holy_sites_generated > 0 }\r\n");
		writer.write("\t\tholy_site_distance = { target = FROM value > "+(40*mapSizeRatio)+" }\r\n");
		writer.write("\t}\r\n");
		writer.write("\t# Sharing some holy sites with parent religion is neat\r\n");
		writer.write("\tmodifier = {\r\n");
		writer.write("\t\tfactor = 100\r\n");
		writer.write("\t\tFROM = {\r\n");
		writer.write("\t\t\tparent_religion = {\r\n");
		writer.write("\t\t\t\tROOT = {\r\n");
		writer.write("\t\t\t\t\tis_holy_site = PREV\r\n");
		writer.write("\t\t\t\t}\r\n");
		writer.write("\t\t\t}\r\n");
		writer.write("\t\t}\r\n");
		writer.write("\t}\r\n");
		writer.write("}\r\n");
		writer.write("\r\n");
		writer.write("\r\n");
		
		writer.write("holding_spread = {\r\n");
		writer.write("\tfactor = 100\r\n");
		writer.write("}\r\n");
		
		writer.close();
		
		Logger.log("Done in "+(System.currentTimeMillis()-ms)+"ms", 100);
		Logger.close();
		return returnCode;
	}
	

}
