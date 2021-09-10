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
import java.util.ArrayList;
import java.util.List;

import ck2maptools.data.Coordinates;
import ck2maptools.data.InputFile;
import ck2maptools.data.Loader;
import ck2maptools.data.OutputFile;
import ck2maptools.data.Rivers;
import ck2maptools.data.Terrain;
import ck2maptools.utils.Logger;
import ck2maptools.utils.Utils;

public class CK2MakeRiversMap implements ICK2MapTool {
	
	private Loader loader;
	
	private List<List<Coordinates>> riversList; //List of rivers with a primary source
	private List<Coordinates> reconstructedList; //Coordinates of every river pixel that has been iterated upon (so we don't run a pixel twice if it could belong to two different rivers)

	private int returnCode;
	
	public static InputFile[] inputFiles() {
		return new InputFile[]{
				InputFile.Terrain,
				InputFile.Rivers,
				InputFile.Settlements
		};
	}
	
	public static InputFile[] inputFilesOptional() {
		return new InputFile[]{
		};
	}

	public static OutputFile[] outputFiles() {
		return new OutputFile[]{
				OutputFile.Rivers
		};
	}
	
	public static InputFile[] outputFilesInput() {
		return new InputFile[]{
				InputFile.Settlements //Generates / Updates
		};
	}
	
	public static void main(String[] args) throws Exception {
		new CK2MakeRiversMap().execute();
	}
	
	public int execute() throws Exception {
		returnCode = ERROR_NONE;
		long ms = System.currentTimeMillis();
		Logger.InitLogger("CK2MakeRiversMap");
		
		Utils.checkCriticalResources(inputFiles(), true);

		loader = Loader.getLoader();
		loader.loadTerrain();
		loader.loadHeights();
		loader.loadRivers();
		loader.loadSettlements();
		
		riversList = new ArrayList<List<Coordinates>>();
		reconstructedList = new ArrayList<Coordinates>();
	
		
		BufferedImage bufOutRivers = new BufferedImage(loader.sizeX, loader.sizeY, BufferedImage.TYPE_BYTE_INDEXED, Rivers.getIndexColorModel());
		
		//Parse
		Logger.log("Initializing...",0);
		for (int x=0; x<loader.sizeX; x++)
			for(int y=0; y<loader.sizeY; y++)
			{
				if (loader.riverArray[x][y] == Rivers.RIVER_SOURCE)
				{
					if (loader.isWater[x][y])
						loader.riverArray[x][y] = Rivers.RIVER_SOURCE_FROM_MAJOR_RIVER;
					else
						loader.riverArray[x][y] = Rivers.RIVER_SOURCE;
					
					//Make a new river using this point as a start, and add it to the collection of rivers
					List<Coordinates> newRiver = new ArrayList<Coordinates>();
					newRiver.add(new Coordinates(x,y));
					riversList.add(newRiver);
				}
				else if (loader.riverArray[x][y] == Rivers.MERGING_RIVER)
				{
					//Make a new river using this point as a start, and add it to the collection of rivers
					List<Coordinates> newRiver = new ArrayList<Coordinates>();
					newRiver.add(new Coordinates(x,y));
				}
				else if (loader.riverArray[x][y] == Rivers.SPLITTING_RIVER)
				{
					//Make a new river using this point as a start, and add it to the collection of rivers
					List<Coordinates> newRiver = new ArrayList<Coordinates>();
					newRiver.add(new Coordinates(x,y));
				}
			}
		

		Logger.log("Smoothing edges...",5);
		for (int x=1; x<loader.sizeX-1; x++)
			for(int y=1; y<loader.sizeY-1; y++)
			{
				if (!isRiverOrSpecial(x,y))
				{
					boolean left, right, up, down, lu, ld, ru, rd;
					
					left = isRiverOrSpecial(x-1,y);
					right = isRiverOrSpecial(x+1,y);
					up = isRiverOrSpecial(x,y-1);
					down = isRiverOrSpecial(x,y+1);
					
					lu = isRiverOrSpecial(x-1,y-1);
					ld = isRiverOrSpecial(x-1,y+1);
					ru = isRiverOrSpecial(x+1,y-1);
					rd = isRiverOrSpecial(x+1,y+1);
					
					if ((left && up && !lu && !right && !down) ||
							(left && down && !ld && !right && !up) ||
							(right && up && !ru && !left && !down) ||
							(right && down && !rd && !left && !up))
						loader.riverArray[x][y] = Rivers.RIVER9;
				}
			}
		
		//Reconstruct rivers
		Logger.log("Reconstructing rivers...",5);
		int step = 0;
		while(!riversList.isEmpty())
		{
			step++;
			Logger.log("Reconstructing rivers...Step "+step+"...",25);
			riversList = reconstructRivers(riversList);
		}

		

		
		//Build output
		for (int x=0; x<loader.sizeX; x++)
			for(int y=0; y<loader.sizeY; y++)			
				bufOutRivers.setRGB(x, y, loader.riverArray[x][y].getRGB());
		
		//Setup output dirs if they don't exist
		File outputDirMap = Utils.mkDir("./output/map");
		
		//Write output images
		Utils.writeOutputImage(outputDirMap.getPath() + "/rivers.bmp", bufOutRivers);
		
		//Update settlements.bmp with a rivers.bmp overlay
		if (loader.bufInSettlements != null)
		{
			for (int x=0; x<loader.sizeX; x++)
				for(int y=0; y<loader.sizeY; y++)
					if (isRiverOrSpecial(x,y) && !loader.isWater[x][y] &&
							loader.bufInSettlements.getRGB(x, y) != Color.red.getRGB() &&
							loader.bufInSettlements.getRGB(x, y) != Color.yellow.getRGB() &&
							loader.bufInSettlements.getRGB(x, y) != Color.black.getRGB() &&
							loader.bufInSettlements.getRGB(x, y) != Color.green.getRGB() &&
							loader.bufInSettlements.getRGB(x, y) != Color.blue.getRGB() &&
							loader.bufInSettlements.getRGB(x, y) != Color.cyan.getRGB())
						loader.bufInSettlements.setRGB(x, y, new Color(32,96,144).getRGB());
			
			Utils.writeOutputImage(InputFile.Settlements.getFileName(), loader.bufInSettlements);
		}
		
		Logger.log("Done in "+(System.currentTimeMillis()-ms)+"ms",100);
		Logger.close();
		return returnCode;
	}
	
	private boolean isRiver(Coordinates c) {return isRiver(c.getX(),c.getY());}
	private boolean isRiver(int x, int y) {return loader.riverArray[x][y].isRiver();}
	
	//private static boolean isRiverOrSpecial(Coordinates c) {return isRiverOrSpecial(c.getX(),c.getY());}
	private boolean isRiverOrSpecial(int x, int y) {return loader.riverArray[x][y].isRiverOrSpecial();}
	
	private List<List<Coordinates>> reconstructRivers(List<List<Coordinates>> entryList)
	{
		List<List<Coordinates>> retList = new ArrayList<List<Coordinates>>();
		
		for (List<Coordinates> river : entryList) //Only consider rivers with a source at first
		{
			Coordinates start = river.get(0);
			reconstructedList.add(start);
	
			int x = start.getX(), y=start.getY();

			boolean found = false;	
			int width = 1;
			
			boolean isMerging = (loader.riverArray[x][y] == Rivers.MERGING_RIVER);
			boolean isSplitting = (loader.riverArray[x][y] == Rivers.SPLITTING_RIVER);
			
			if (isMerging || isSplitting)
				width = 3;
			
			do {
				found = false;
				
				Coordinates left=null, right=null, up=null, down=null, next=null;
				List<Coordinates> neighbours = new ArrayList<Coordinates>();

				if (x>0)
					left = new Coordinates(x-1,y);
				if (x<loader.sizeX)
					right = new Coordinates(x+1,y);
				if (y>0)
					up = new Coordinates(x,y-1);
				if (y<loader.sizeY)
					down = new Coordinates(x,y+1);
				
				if (!river.contains(left) && !reconstructedList.contains(left))
					neighbours.add(left);
				if (!river.contains(right) && !reconstructedList.contains(right))
					neighbours.add(right);
				if (!river.contains(up) && !reconstructedList.contains(up))
					neighbours.add(up);
				if (!river.contains(down) && !reconstructedList.contains(down))
					neighbours.add(down);
				
				for (Coordinates c : neighbours)
				{
					if (c != null && isRiver(c))
					{
						if (next == null)
						{
							next = c;
							found = true;
							river.add(next);
							reconstructedList.add(next);
							x = next.getX(); y=next.getY();
							
							if (!isMerging && river.size() % 30 == 0)
								width++;
							else if (isMerging && river.size() % 30 == 0 && width > 1)
								width--;
							
							switch (width)
							{
							case 1:
								loader.riverArray[x][y] = Rivers.RIVER1; break;
							case 2:
								loader.riverArray[x][y] = Rivers.RIVER2; break;
							case 3:
								loader.riverArray[x][y] = Rivers.RIVER3; break;
							case 4:
								loader.riverArray[x][y] = Rivers.RIVER4; break;
							case 5:
								loader.riverArray[x][y] = Rivers.RIVER5; break;
							case 6:
								loader.riverArray[x][y] = Rivers.RIVER6; break;
							case 7:
								loader.riverArray[x][y] = Rivers.RIVER7; break;
							case 8:
								loader.riverArray[x][y] = Rivers.RIVER8; break;
							default:
								loader.riverArray[x][y] = Rivers.RIVER9; break;
							}
						}
						else
						{
							Logger.log("River error at coordinates "+x+";"+y+" : found multiple possible paths");
							returnCode |= ERROR_RIVERS;
						}
					}
					else if (c != null && loader.riverArray[c.getX()][c.getY()] == Rivers.SPLITTING_RIVER)
					{
						width = width - 3;
						if (width < 1)
							width = 1;
						
						ArrayList<Coordinates> futureRiver = new ArrayList<Coordinates>();
						futureRiver.add(c);
						retList.add(futureRiver); //To be done next round
					}
					else if (c != null && loader.riverArray[c.getX()][c.getY()] == Rivers.MERGING_RIVER)
					{
						width = width + 3;
						ArrayList<Coordinates> futureRiver = new ArrayList<Coordinates>();
						futureRiver.add(c);
						retList.add(futureRiver); //To be done next round
					}
					else if (c != null && loader.riverArray[c.getX()][c.getY()] == Rivers.RIVER_SOURCE)
					{
						Logger.log("River error at coordinates "+x+";"+y+" : found a river source where not expected");
						returnCode |= ERROR_RIVERS;
					}
				}
			
			}
			while (found);
		} //next river
		
		return retList;
	}
}
