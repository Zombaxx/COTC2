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

import ck2maptools.data.Coordinates;
import ck2maptools.data.InputFile;
import ck2maptools.data.Loader;
import ck2maptools.data.Province;
import ck2maptools.data.Terrain;
import ck2maptools.utils.Logger;
import ck2maptools.utils.Utils;

public class CK2MapReverse implements ICK2MapTool {

	private Loader loader;
	
	public static void main(String[] args) throws Exception {
		new CK2MapReverse().execute();
	}
	
	public int execute() throws Exception
	{
		int returnCode = ERROR_NONE;
		long ms = System.currentTimeMillis();
		Logger.InitLogger("CK2MapReverse");

		loader = Loader.getLoader();
		loader.loadProvinces(false, false);
		loader.loadTerrain();
		loader.loadHeights();
		
		loader.provinceColorMap=new HashMap<Integer, Province>();
		loader.provinceList=new ArrayList<Province>();
		loader.provinceArray=new Province[loader.sizeX][loader.sizeY];

		Logger.log("Reverse engineering provinces...",0);
		int provinceIndex = 0;
		for (int x=0; x<loader.sizeX; x++)
			for(int y=0; y<loader.sizeY; y++)
			{
				int rgb = loader.bufInProvinces.getRGB(x, y);
				
				//Ignore impassable and mare incognitum
				if (rgb == Color.BLACK.getRGB())
					continue;
				else if (rgb == Color.WHITE.getRGB())
					continue;		
				
				//Is this a new color ?
				Province p = loader.provinceColorMap.get(rgb);
				
				Coordinates c = new Coordinates(x,y);
				
				if (p == null) //Yes it is. Create a Province for it
				{
					p = new Province(c, rgb, ++provinceIndex);
					loader.provinceColorMap.put(rgb, p);
					loader.provinceList.add(p);
				}
				
				p.addTerritory(x,y); //Mark this pixel as belonging to that Province
				loader.provinceArray[x][y]=p;
			}
		
		//For each province, try to find its "center" and place the settlement coordinates there
		for (Province p : loader.provinceList)
		{	
			//Get a pixel in the center of the Province's extreme coordinates
			Coordinates center = p.getCenter();
			
			//Is it not in the Province ?
			if (loader.provinceArray[center.getX()][center.getY()]!=p)
			{
				Coordinates best = null;
				int bestScore = Integer.MAX_VALUE;

				for (int x=0; x<loader.sizeX; x++)
					for (int y=0; y<loader.sizeY; y++)
					{
						if (loader.provinceArray[x][y] == p)
						{
							int distance = Utils.getDistanceSquared(x, y, center.getX(), center.getY());
							if (distance < bestScore)
							{
								best=new Coordinates(x,y);
								bestScore = distance;
							}
						}
					}

				center = best;
			}

			p.setCoord(center);
			
			if (loader.isWater[center.getX()][center.getY()])
				p.setWater(true);
		}
		
		

		//Build output
		BufferedImage bufOutSettlements = new BufferedImage(loader.sizeX, loader.sizeY, BufferedImage.TYPE_INT_RGB);
		

		for (Province p : loader.provinceList)
		{
			Color c = p.isWater() ? Color.BLUE : Color.RED;
			bufOutSettlements.setRGB(p.getX(), p.getY(), c.getRGB());
		}

		//Backup the existing image, if it exists
		File inputSettlementsMap = new File(InputFile.Settlements.getFileName());
		File inputSettlementsBackupMap = new File(InputFile.Settlements.getFileName().replace(".bmp", "."+Utils.getDateString()+".bmp"));
		if (inputSettlementsMap.exists())
		{
			Files.copy(inputSettlementsMap.toPath(), inputSettlementsBackupMap.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		//Write output images
		Utils.writeOutputImage(InputFile.Settlements.getFileName(), bufOutSettlements);
		
		Logger.log("Done in "+(System.currentTimeMillis()-ms)+"ms",100);
		Logger.close();
		return returnCode;
	}
	
}
