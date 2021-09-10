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

import ck2maptools.data.Coordinates;
import ck2maptools.data.Culture;
import ck2maptools.data.InputFile;
import ck2maptools.data.Loader;
import ck2maptools.data.OutputFile;
import ck2maptools.data.Province;
import ck2maptools.data.Terrain;
import ck2maptools.utils.Logger;
import ck2maptools.utils.Utils;

public class CK2MakeDeJureMaps implements ICK2MapTool {

	private Loader loader;
	
	private boolean makeDuchies = false;
	private boolean makeKingdoms = false;
	private boolean makeEmpires = false;
	
	public void setParamMakeDuchies(boolean makeDuchies) {this.makeDuchies = makeDuchies;}
	public void setParamMakeKingdoms(boolean makeKingdoms) {this.makeKingdoms = makeKingdoms;}
	public void setParamMakeEmpires(boolean makeEmpires) {this.makeEmpires = makeEmpires;}	
	
	public static InputFile[] inputFiles() {
		return new InputFile[]{
				InputFile.Terrain,
				InputFile.Settlements,
				InputFile.Provinces
		};
	}

	public static OutputFile[] outputFiles() {
		return new OutputFile[]{
		};
	}
	
	public static InputFile[] outputFilesInput() {
		return new InputFile[]{
				InputFile.DeJureD,
				InputFile.DeJureK,
				InputFile.DeJureE
		};
	}
	
	public static void main(String[] args) throws Exception {
		
		CK2MakeDeJureMaps t = new CK2MakeDeJureMaps();
		
		//Argument : -d / -duchies : the program will generate a dejure_duchies map
		//Argument : -k / -kingdoms : the program will generate a dejure_kingdoms map
		for (String arg : args)
		{
			if (arg == null)
				continue;
			
			if (arg.equalsIgnoreCase("-d") || arg.equalsIgnoreCase("-duchies"))
			{
				t.makeDuchies = true;
				Logger.log("Will make De Jure Duchies");
			}
			
			if (arg.equalsIgnoreCase("-k") || arg.equalsIgnoreCase("-kingdoms"))
			{
				t.makeKingdoms = true;
				Logger.log("Will make De Jure Kingdoms");
			}
			

			if (arg.equalsIgnoreCase("-e") || arg.equalsIgnoreCase("-empires"))
			{
				t.makeEmpires = true;
				Logger.log("Will make De Jure Empires");
			}
		}
		
		t.execute();
	}
	
	public int execute() throws Exception {
		int returnCode = ERROR_NONE;
		long ms = System.currentTimeMillis();
		Logger.InitLogger("CK2MakeDeJureMaps");
		
		Utils.checkCriticalResources(inputFiles(), true);
		
		//Load the input map
		loader = Loader.getLoader();
		loader.loadSettlements();
		loader.loadProvinces(true, true);
		loader.loadTerrain();
		loader.loadCultures(false);

		
		//Parse Cultures map (simplified)
		for (Province p :  loader.provinceList)
		{
			
			if (loader.bufInCultures != null)
			{
				int rgb = loader.bufInCultures.getRGB(p.getX(), p.getY());
				//Logger.log("Culture for province "+p.getIndex()+" is "+rgb);
				String pseudoCulture = Integer.toString(rgb);
				Culture culture = Culture.getCulture(pseudoCulture);
				if (culture == null)
					culture = new Culture(pseudoCulture, pseudoCulture);
				
				p.setCulture(culture);
			}
			else
			{
				String pseudoCulture = "noculture";
				Culture culture = Culture.getCulture(pseudoCulture);
				if (culture == null)
					culture = new Culture(pseudoCulture, pseudoCulture);
				
				p.setCulture(culture);
			}
		}

		//Make duchies
		if (makeDuchies)
		{
			Logger.log("Making duchies...",0);
	
			//1st pass, "important" provinces vassalize every province they are the neighbour of
			//But closer provinces take priority
			Logger.log("Making duchies...Step 1...",10);		
			for (Province p : loader.provinceList)
			{
				if (p.isWasteland())
					continue;
				
				if (p.isImportant())
					continue;
				
				if (p.getDeJureDuchyVassals().size() == 0 && p.getDeJureDuchyCapital() == p)
				{
					for (Province n : p.getAdjacentProvinces())
					{
						if (n.getCulture().equals(p.getCulture()))
						{
						
							if (n.getDeJureDuchyCapital() == n && n.getDeJureDuchyVassalsPlusSelf().size() < 3) //Don't form duchies larger than 3 provinces
								if (n.isImportant())
								{
									if (p.getDeJureDuchyCapital() == p)
										p.setDeJureDuchyCapital(n);
									else if (Utils.getDistanceSquared(p, p.getDeJureDuchyCapital()) > 
									Utils.getDistanceSquared(p, n))
										p.setDeJureDuchyCapital(n);
								}
						}
					}
				}
			}
			
			//2nd pass, anyone still alone goes to nearest neighbouring duchy
			Logger.log("Making duchies...Step 2...",10);
			for (Province p : loader.provinceList)
			{
				if (p.isWasteland())
					continue;
				
				if (p.isImportant())
					continue;
				
				if (p.getDeJureDuchyVassals().size() == 0 && p.getDeJureDuchyCapital() == p)
				{
					for (Province a : p.getAllAdjacentProvinces())
					{
						
						Province c = a.getDeJureDuchyCapital();
						
						if (c.getCulture().equals(p.getCulture()))
						{
						
							if (c.getDeJureDuchyVassalsPlusSelf().size() < 6) //Don't form duchies larger than 6 provinces
							{
								if (p.getDeJureDuchyCapital() == p)
									p.setDeJureDuchyCapital(c);
								else if (Utils.getDistanceSquared(p, p.getDeJureDuchyCapital()) > 
								Utils.getDistanceSquared(p, c))
									p.setDeJureDuchyCapital(c);
							}
							
						}
					}
				}
			}
	
			//3rd pass : Merge small duchies where appropriate
			Logger.log("Making duchies...Step 3...",10);
			for (Province p : loader.provinceList)
			{
				if (p.isWasteland())
					continue;
				
				if (p.getDeJureDuchyCapital() == p)
				{
					for (Province n : p.getAdjacentProvinces())
					{
						
						boolean canMerge = true; 
						
						Province c = n.getDeJureDuchyCapital();
						
						if (c != p && //Not myself
								!c.isImportant() &&
								(c.isAdjacentProvince(p) || c.isIndirectAdjacentProvince(p)) && c.getCulture().equals(p.getCulture()) &&
								p.getDeJureDuchyVassalsPlusSelf().size() + c.getDeJureDuchyVassalsPlusSelf().size() <= 5) //Don't form duchies larger than 5 provinces
						{
							for (Province v : c.getDeJureDuchyVassals())
							{
								if (!v.isAdjacentProvince(p) || !v.getCulture().equals(p.getCulture()))
								{
									canMerge = false;
									break;
								}
							}
	
							if (canMerge) //All provinces of the c duchy are adjacent to p
							{
								for (Province v : c.getDeJureDuchyVassalsPlusSelf())
								{
									v.setDeJureDuchyCapital(p);
								}
							}
						
						}
					}
				}
			}
		}
		else
		{
			//Parse the existing dejure_duchies.bmp
			File inputDejureDuchiesMap = new File(InputFile.DeJureD.getFileName());
			if (inputDejureDuchiesMap.exists())
			{
				BufferedImage bufInDuchies = (BufferedImage) Utils.readInputImage(InputFile.DeJureD.getFileName());
				
				for (Province p : loader.provinceList)
				{
					int duchyColor = bufInDuchies.getRGB(p.getX(), p.getY());
					p.setDeJureDuchyCapital(loader.provinceColorMap.get(duchyColor));
				}
			}
			
		}
		
		if (makeKingdoms || makeEmpires)
		{
			//Determine duchy adjacencies
			for (Province p : loader.provinceList)
			{
				if (p.isWasteland())
					continue;
				
				if (p.getDeJureDuchyCapital() != p)
					continue;
				
				for (Province n : loader.provinceList)
				{
					if (n.getDeJureDuchyCapital() != n)
						continue;
					
					for (Province v1 : p.getDeJureDuchyVassalsPlusSelf())
						for (Province v2 : n.getDeJureDuchyVassalsPlusSelf())
						{
							if (v1.isAdjacentProvince(v2) || v1.isIndirectAdjacentProvince(v2))
							{
								p.addAdjacentDuchy(n);
								n.addAdjacentDuchy(p);
								break;
							}
						}				
				}
				
			}
		}

		//Make kingdoms
		if (makeKingdoms)
		{
			Logger.log("Making kingdoms...",0);

			//1st pass, "important" duchies vassalize every duchy they are the neighbour of
			//But closer duchies take priority
			Logger.log("Making kingdoms...Step 1...",10);
			for (Province p : loader.provinceList)
			{
				if (p.isWasteland())
					continue;
				
				if (p.getDeJureKingdomCapital() != p)
					continue;
				
				if (p.isImportant())
					continue;
				
				for (Province n : p.getAdjacentDuchies())
				{
					
					if (n.getCulture().equals(p.getCulture()))
					{
					
						if (n.getDeJureKingdomCapital() == n)
							if (n.isImportant())
							{
								if (p.getDeJureKingdomCapital() == p)
									p.setDeJureKingdomCapital(n);
								else if (Utils.getDistanceSquared(p, p.getDeJureKingdomCapital()) > 
									Utils.getDistanceSquared(p, n))
									p.setDeJureKingdomCapital(n);
							}
					
					}
				
				}
			}
	
			//2nd pass, anyone still too small goes to nearest neighbouring kingdom
			Logger.log("Making kingdoms...Step 2...",10);
			for (Province p : loader.provinceList)
			{
				if (p.isWasteland())
					continue;
				
				if (p.getDeJureKingdomCapital() != p)
					continue;
				
				if (p.isImportant())
					continue;
					
				//Still too small ?
				if (p.getDeJureKingdomVassals().size() <= 2)
				{
					//System.out.println(p+" is too small : " + p.getDeJureKingdomVassals().size());
					
					Province nearestNeighbourCapital = null;
					int nearestNeighbourCapitalDistance = Integer.MAX_VALUE;
					
					for (Province a : p.getAdjacentDuchies())
					{
						
						if (a.getCulture().equals(p.getCulture()))
						{
							Province n = a.getDeJureKingdomCapital();
							
							if (p == n)
								continue;
	
							int distance = Utils.getDistanceSquared(p, n);
							
							if (distance < nearestNeighbourCapitalDistance && 
									(n.getDeJureKingdomVassals().size() >= p.getDeJureKingdomVassals().size() || n.isImportant()))
							{
								nearestNeighbourCapitalDistance = distance;
								nearestNeighbourCapital = n;
							}
						}
					}
					
					if (nearestNeighbourCapital != null)
					{
						for (Province v : p.getDeJureKingdomVassals())
							v.setDeJureKingdomCapital(nearestNeighbourCapital);
						
						p.setDeJureKingdomCapital(nearestNeighbourCapital);
						
						//System.out.println(p+" is vassalized by "+nearestNeighbourCapital);
					}
				}
			}
		}
		else
		{
			File inputDejureKingdomsMap = new File(InputFile.DeJureK.getFileName());
			if (inputDejureKingdomsMap.exists())
			{
				BufferedImage bufInKingdoms = (BufferedImage) Utils.readInputImage(InputFile.DeJureK.getFileName());
				
				for (Province p : loader.provinceList)
				{
					int kingdomColor = bufInKingdoms.getRGB(p.getX(), p.getY());
					p.setDeJureKingdomCapital(loader.provinceColorMap.get(kingdomColor));
				}
			}	
		}
		
		//Make Empires
		if (makeEmpires)
		{
			Logger.log("Making empires...",0);
			
			//Determine kingdom adjacencies
			for (Province p : loader.provinceList)
			{
				if (p.isWasteland())
					continue;
				
				if (p.getDeJureKingdomCapital() != p)
					continue;
				
				for (Province n : loader.provinceList)
				{
					if (n.getDeJureKingdomCapital() != n)
						continue;
					
					for (Province v1 : p.getDeJureKingdomVassalsPlusSelf())
						for (Province v2 : n.getDeJureKingdomVassalsPlusSelf())
						{
							if (v1.isAdjacentDuchy(v2))
							{
								p.addAdjacentKingdom(n);
								n.addAdjacentKingdom(p);
								break;
							}
						}				
				}
				
			}
			
			//1st pass, larger kingdoms vassalize every kingdom they are the neighbour of
			//But closer kingdoms take priority
			Logger.log("Making empires...Step 1...",10);
			for (Province p : loader.provinceList)
			{
				if (p.isWasteland())
					continue;
				
				if (p.getDeJureEmpireCapital() != p)
					continue;
				
				for (Province n : p.getAdjacentKingdoms())
				{

					if (n.getDeJureEmpireCapital() == n)

						if (p.getDeJureEmpireCapital() == p)
							p.setDeJureEmpireCapital(n);
						else if (Utils.getDistanceSquared(p, p.getDeJureEmpireCapital()) > 
							Utils.getDistanceSquared(p, n))
							p.setDeJureEmpireCapital(n);
				}
			}
	
			//2nd pass, anyone still too small goes to nearest neighbouring empire
			Logger.log("Making empires...Step 2...",10);
			for (Province p : loader.provinceList)
			{
				if (p.isWasteland())
					continue;
				
				if (p.getDeJureEmpireCapital() != p)
					continue;
				
				if (p.isImportant())
					continue;
					
				//Still too small ?
				if (p.getDeJureEmpireVassals().size() <= 2)
				{
					//System.out.println(p+" is too small : " + p.getDeJureKingdomVassals().size());
					
					Province nearestNeighbourCapital = null;
					int nearestNeighbourCapitalDistance = Integer.MAX_VALUE;
					
					for (Province a : p.getAdjacentKingdoms())
					{
						Province n = a.getDeJureEmpireCapital();
						
						if (p == n)
							continue;

						int distance = Utils.getDistanceSquared(p, n);
						
						if (distance < nearestNeighbourCapitalDistance && 
								(n.getDeJureEmpireVassals().size() >= p.getDeJureEmpireVassals().size()))
						{
							nearestNeighbourCapitalDistance = distance;
							nearestNeighbourCapital = n;
						}
					}
					
					if (nearestNeighbourCapital != null)
					{
						for (Province v : p.getDeJureEmpireVassals())
							v.setDeJureEmpireCapital(nearestNeighbourCapital);
						
						p.setDeJureEmpireCapital(nearestNeighbourCapital);
						
						//System.out.println(p+" is vassalized by "+nearestNeighbourCapital);
					}
				}
			}
		}
		else
		{
			File inputDejureEmpiresMap = new File(InputFile.DeJureE.getFileName());
			if (inputDejureEmpiresMap.exists())
			{
				BufferedImage bufInEmpires = (BufferedImage) Utils.readInputImage(InputFile.DeJureE.getFileName());
				
				for (Province p : loader.provinceList)
				{
					int empireColor = bufInEmpires.getRGB(p.getX(), p.getY());
					p.setDeJureEmpireCapital(loader.provinceColorMap.get(empireColor));
				}
			}	
		}
		
		Logger.log("Finalizing...",0);
		
		//Initalize the output image
		BufferedImage bufOutDuchies = new BufferedImage(loader.sizeX, loader.sizeY, BufferedImage.TYPE_INT_RGB);
		BufferedImage bufOutKingdoms = new BufferedImage(loader.sizeX, loader.sizeY, BufferedImage.TYPE_INT_RGB);
		BufferedImage bufOutEmpires = new BufferedImage(loader.sizeX, loader.sizeY, BufferedImage.TYPE_INT_RGB);
		BufferedImage bufOutTemplate = new BufferedImage(loader.sizeX, loader.sizeY, BufferedImage.TYPE_INT_RGB);

		for (int x=0; x < loader.sizeX ; x++)
		{
			for (int y=0; y < loader.sizeY ; y++)
			{
				Province p = loader.provinceArray[x][y];
				
				if (p != null && !p.isWater() && !p.isWasteland())
				{
					if (loader.provinceBorders[x][y])
					{
						//Black borders		
						int borderColor = Color.BLACK.getRGB();

						//Gold borders
						if (p.isImportant())
							borderColor = Color.YELLOW.getRGB();
						
						bufOutDuchies.setRGB(x, y, borderColor);
						bufOutKingdoms.setRGB(x, y, borderColor);
						bufOutEmpires.setRGB(x, y, borderColor);
						
						bufOutTemplate.setRGB(x, y, borderColor);
					}
					else
					{
						bufOutDuchies.setRGB(x, y, p.getDeJureDuchyCapital().getMapColor());
						bufOutKingdoms.setRGB(x, y, p.getDeJureKingdomCapital().getMapColor());
						bufOutEmpires.setRGB(x, y, p.getDeJureEmpireCapital().getMapColor());
						
						//always
						bufOutTemplate.setRGB(x, y, Color.GRAY.getRGB());
					}

				}
				else
				{
					bufOutDuchies.setRGB(x, y, Color.BLACK.getRGB());
					bufOutKingdoms.setRGB(x, y, Color.BLACK.getRGB());
					bufOutEmpires.setRGB(x, y, Color.BLACK.getRGB());
					
					bufOutTemplate.setRGB(x, y, Color.BLACK.getRGB());
				}
			}
		}
		
		
		//Backup the existing image, if it exists
		File inputDejureDuchiesMap = new File(InputFile.DeJureD.getFileName());
		File inputDejureKingdomsMap = new File(InputFile.DeJureK.getFileName());
		File inputDejureEmpiresMap = new File(InputFile.DeJureE.getFileName());
		
		//Only backup if it has changed
		if (makeDuchies)
		{
			File inputDejureDuchiesBackupMap = new File(InputFile.DeJureD.getFileName().replace(".bmp", "."+Utils.getDateString()+".bmp") );
			if (inputDejureDuchiesMap.exists())
			{
				Files.copy(inputDejureDuchiesMap.toPath(), inputDejureDuchiesBackupMap.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
			
		}
		if (makeKingdoms)
		{
			File inputDejureKingdomsBackupMap = new File(InputFile.DeJureK.getFileName().replace(".bmp", "."+Utils.getDateString()+".bmp") );
			if (inputDejureKingdomsMap.exists())
			{
				Files.copy(inputDejureKingdomsMap.toPath(), inputDejureKingdomsBackupMap.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
		}
		if (makeEmpires)
		{
			File inputDejureEmpiresBackupMap = new File(InputFile.DeJureE.getFileName().replace(".bmp", "."+Utils.getDateString()+".bmp") );
			if (inputDejureEmpiresMap.exists())
			{
				Files.copy(inputDejureEmpiresMap.toPath(), inputDejureEmpiresBackupMap.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
		}		
		
		//Then Write output images
		Utils.writeOutputImage(inputDejureDuchiesMap.getPath(), bufOutDuchies);
		Utils.writeOutputImage(inputDejureKingdomsMap.getPath(), bufOutKingdoms);
		Utils.writeOutputImage(inputDejureEmpiresMap.getPath(), bufOutEmpires);
		
		Utils.writeOutputImage("./input/dejure_template.bmp", bufOutTemplate);
		
		//Make a template for easier creation of trade_routes, since we've already parsed all the relevant data...
		Logger.log("Making trade_route template...",0);
		BufferedImage bufOutTradeRouteTemplate = new BufferedImage(loader.sizeX, loader.sizeY, BufferedImage.TYPE_INT_RGB);
		for (int x=0; x<loader.sizeX; x++)
		{
			for (int y=0; y<loader.sizeY; y++)
			{	
				if (loader.provinceArray[x][y] == null)
				{
					bufOutTradeRouteTemplate.setRGB(x, y, Color.black.getRGB());
				}
				else if (loader.provinceArray[x][y].isWater())
				{
					bufOutTradeRouteTemplate.setRGB(x, y, new Color(0, 0, 64).getRGB());
				}
				else
				{
					bufOutTradeRouteTemplate.setRGB(x, y, loader.bufInSettlements.getRGB(x, y));
				}
				
				//Make Black borders
				if (loader.provinceBorders[x][y])
				{
					Province p = loader.provinceArray[x][y];
						
					if (p != null && p.isImportant()) //Gold border around important settlements
						bufOutTradeRouteTemplate.setRGB(x, y, new Color(160, 160, 32).getRGB());
					else
						bufOutTradeRouteTemplate.setRGB(x, y, new Color(32, 32, 32).getRGB());
				}
			}
			
		}
		
		//Mark settlements
		for (Coordinates c : loader.settlementsList)
		{
			int x = c.getX();
			int y = c.getY();
			bufOutTradeRouteTemplate.setRGB(x, y, new Color(128, 0, 0).getRGB());
		}
		for (Coordinates c : loader.wastelandsList)
		{
			int x = c.getX();
			int y = c.getY();
			bufOutTradeRouteTemplate.setRGB(x, y, new Color(0, 128, 0).getRGB());
		}
		for (Coordinates c : loader.watersList)
		{
			int x = c.getX();
			int y = c.getY();
			bufOutTradeRouteTemplate.setRGB(x, y, new Color(0, 64, 128).getRGB());
		}
		
		Utils.writeOutputImage("./input/trade_route_template.bmp", bufOutTradeRouteTemplate);
		
		
		Logger.log("Done in "+(System.currentTimeMillis()-ms)+"ms",100);
		Logger.close();
		return returnCode;
	}
	


}
