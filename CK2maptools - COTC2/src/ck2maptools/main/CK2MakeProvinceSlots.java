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

import ck2maptools.data.Climate;
import ck2maptools.data.InputFile;
import ck2maptools.data.InputTerrain;
import ck2maptools.data.Loader;
import ck2maptools.data.OutputFile;
import ck2maptools.data.Province;
import ck2maptools.utils.Config;
import ck2maptools.utils.Logger;
import ck2maptools.utils.Utils;

public class CK2MakeProvinceSlots implements ICK2MapTool {
	
	private Loader loader;
	
	public static InputFile[] inputFiles() {
		return new InputFile[]{
				InputFile.Terrain,
				InputFile.Settlements,
				InputFile.Provinces
		};
	}	
	
	public static InputFile[] inputFilesOptional() {
		return new InputFile[]{
				InputFile.Climate,
				InputFile.Input,
				InputFile.Rivers,
				InputFile.DeJureD,
				InputFile.DeJureK,
				InputFile.DeJureE,
		};
	}

	public static OutputFile[] outputFiles() {
		return new OutputFile[]{
		};
	}
	
	public static InputFile[] outputFilesInput() {
		return new InputFile[]{
				InputFile.NumSlots, //Generates / Updates
		};
	}
	
	public static void main(String[] args) throws Exception
	{
		new CK2MakeProvinceSlots().execute();
	}
	
	public int execute() throws Exception {
		int returnCode = ERROR_NONE;
		long ms = System.currentTimeMillis();
		Logger.InitLogger("CK2MakeProvinceSlots");
		
		Utils.checkCriticalResources(inputFiles(), true);
		
		//Parse config.csv
		Config.parseConfig();
		
		//Load the input map
		loader = Loader.getLoader();
		loader.loadSettlements();
		loader.loadTerrain();
		loader.loadProvinces(true, false);
		
		//optional
		loader.loadDeJureMaps();
		loader.loadClimate();
		loader.loadInput();
		loader.loadRivers();

		//Determine land wealth for holding slots
		calcMaxNumSettlements();		

		//Save an image of the number of slots for easy visualization and for manual editing.
		Logger.log("Generating holding slots map...",0);
		
		//Generate a map
		BufferedImage bufOutNumSlots = new BufferedImage(loader.sizeX, loader.sizeY, BufferedImage.TYPE_INT_RGB);
		for (int x=0; x<loader.sizeX; x++)
			for (int y=0; y<loader.sizeY; y++)
			{
				Province p = loader.provinceArray[x][y];
				if (p != null && !p.isWasteland() && !p.isWater())
				{
					bufOutNumSlots.setRGB(x, y, new Color(70 - p.getNumSlots() * 10, p.getNumSlots() * 30, 0).getRGB());
				}
				
				//Make Black borders
				if (loader.provinceBorders[x][y])
				{			
					bufOutNumSlots.setRGB(x, y, Color.BLACK.getRGB());
				}
			}

		//Write output images
		Utils.writeOutputImage(InputFile.NumSlots.getFileName(), bufOutNumSlots);
		
		Logger.log("Done in "+(System.currentTimeMillis()-ms)+"ms",100);
		Logger.close();
		return returnCode;
	}
	
	//Estimates the wealth of each province base on the terrain it occupies and the local climate 
	//then assigns a number of holding slots between 1 and 7 based on this wealth, relative to other provinces
	private void calcMaxNumSettlements() {
		Logger.log("Calculating max holding slots...");
		int maxLandWealth = 0;
		
		for (int x=0; x<loader.sizeX; x++)
			for(int y=0; y<loader.sizeY; y++)
			{
				Province p = loader.provinceArray[x][y];
					
				if (p == null)
					continue;
				
				if (p.isWater())
					continue;
				
				if (p.isWasteland())
				{
					p.setNumSlots(0);
					continue;
				}
				
				int landWealth = 0;
				
				switch (loader.terrainArray[x][y])
				{
				case FARMLAND:
					landWealth=8;
					break;
			
				default:
					if (InputTerrain.getInputTerrainAt(x, y) == InputTerrain.FOREST)
						landWealth=2; //Same as jungle
					else
						landWealth=4;
					break;
	
				case STEPPE:
				case COASTAL_DESERT:
				case JUNGLE:
				case FOREST_HILLS:
				case MOUNTAIN:
					landWealth=2;
					break;
					
				case ARCTIC:
				case DESERT_MOUNTAIN:
				case SNOWY_MOUNTAIN:
				case WATER:
					landWealth=1;
					break;
					
				
				case DESERT:
				case SANDY_MOUNTAIN:
				case FROZEN_MOUNTAIN:
					landWealth=0;
					break;
					
				case UNUSED1:
					landWealth = Config.CUSTOM_TERRAIN_1_WEALTH;
					break;
					
				case UNUSED2:
					landWealth = Config.CUSTOM_TERRAIN_2_WEALTH;
					break;
				}
					
				if (loader.provinceCoastlineArray[x][y])
					landWealth+=12;
				
				if (loader.provinceRiverbankArray[x][y])
				{
					landWealth+=12;
				}
					
				//Crossed by a lesser river
				if (loader.bufInRivers != null)
					if (loader.riverArray[x][y].isRiverOrSpecial())
					{
						landWealth += 8;
					}
				
				//Use the numslots variable to store landWealth
				p.setNumSlots(p.getNumSlots()+Math.max(landWealth, 0));
	
			}//next Coordinates
			
		
		
		
		for (Province p : loader.provinceList)
		{
			int landWealth = p.getNumSlots();
			
			//Small bonus based on local climate
			switch (Climate.getInputClimateAt(p.getX(), p.getY()))
			{
				default:
					break;
					
				case POLAR:
					landWealth *= 0.8;
					break;
				case COLD:
					landWealth *= 0.9;
					break;					
				case WARM:
					landWealth *= 1.1;
				case HOT:
					landWealth *= 1.2;
					break;
			}
			
			//Major Bonus for important settlements
			if (p.isImportant())
				landWealth *= 1.5;
			
			// De jure empire capitals get a big bonus
			if (p.getDeJureEmpireCapital() == p)
				landWealth *= 1.5;
			// De jure kingdom capitals get a bonus
			else if (p.getDeJureKingdomCapital() == p)
				landWealth *= 1.33;
			// De jure duchy capitals get a small bonus
			else if (p.getDeJureDuchyCapital() == p)
				landWealth *= 1.167;
			
			

			
			//Very large provinces get a penalty
			landWealth /= Math.sqrt(p.getTerritorySize());
		
			p.setNumSlots(landWealth);
		}
		
		


		//Get the highest level of land wealth in any province, and scale the rest compared to that
		for (Province p : loader.provinceList)
			if (p.getNumSlots() > maxLandWealth)
				maxLandWealth = p.getNumSlots();
	
		
		//Rank by land wealth
		Logger.log("Sorting provinces by wealth...");
		Province[] provincesByWealth = new Province[loader.provinceList.size()];
		int index = 0;
		
		//Put all provinces in an array
		for (Province p : loader.provinceList) 
		{
			provincesByWealth[index++]=p;
		}
		
		//Sort array by province wealth
		for(int j=1; j < provincesByWealth.length; j++)
		{
			for(int i=0; i < j; i++)
			{
				if (provincesByWealth[i].getNumSlots() > provincesByWealth[j].getNumSlots())
				{
					Province temp = provincesByWealth[i];
					provincesByWealth[i] = provincesByWealth[j];
					provincesByWealth[j] = temp;
				}
			}
		}
		
		//Assign slots based on percentile in the array
		Logger.log("Assigning holding slots...");
		
		//0-5% => 1 slot
		for (index=0; index < provincesByWealth.length * 5 / 100; index++)
		{
			provincesByWealth[index].setNumSlots(1);
		}
		
		//5-20% => 2 slots
		for (index=provincesByWealth.length * 5 / 100; index < provincesByWealth.length * 20 / 100; index++)
		{
			provincesByWealth[index].setNumSlots(2);
		}
		
		//20-50% => 3 slots
		for (index=provincesByWealth.length * 20 / 100; index < provincesByWealth.length * 50 / 100; index++)
		{
			provincesByWealth[index].setNumSlots(3);
		}
		
		//50-80% => 4 slots
		for (index=provincesByWealth.length * 50 / 100; index < provincesByWealth.length * 80 / 100; index++)
		{
			provincesByWealth[index].setNumSlots(4);
		}
		
		//80-95% => 5 slots
		for (index=provincesByWealth.length * 80 / 100; index < provincesByWealth.length * 95 / 100; index++)
		{
			provincesByWealth[index].setNumSlots(5);
		}
		
		//95-99% => 6 slots
		for (index=provincesByWealth.length * 95 / 100; index < provincesByWealth.length; index++)
		{
			provincesByWealth[index].setNumSlots(6);
		}
		
		//99-100% => 7 slots
		for (index=provincesByWealth.length * 99 / 100; index < provincesByWealth.length; index++)
		{
			provincesByWealth[index].setNumSlots(7);
		}

	}

}
