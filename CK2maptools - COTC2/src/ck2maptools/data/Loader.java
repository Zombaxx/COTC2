package ck2maptools.data;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ck2maptools.ui.CK2MapToolsException;
import ck2maptools.utils.Config;
import ck2maptools.utils.Logger;
import ck2maptools.utils.Utils;

//A class to centralize all image loading and parsing, often redundant between 2 programs in previous versions
public class Loader {
	
	public int sizeX=0, sizeY=0;
	
	//Input images
	public BufferedImage bufInInput; 			// input/input.bmp
	public BufferedImage bufInTerrain;			// output/terrain.bmp
	public BufferedImage bufInHeights;			// output/heights.bmp
	public BufferedImage bufInClimate; 			// input/climate.bmp 
	public BufferedImage bufInSettlements; 		// input/settlements.bmp
	public BufferedImage bufInRivers; 			// input/rivers.bmp	
	public BufferedImage bufInProvinces; 		// input/provinces.bmp (must be exported to output whenever it is updated)	

	public BufferedImage bufInDeJureDuchies;	// input/dejure_<tier>.bmp
	public BufferedImage bufInDeJureKingdoms;	
	public BufferedImage bufInDeJureEmpires;
	public BufferedImage bufInDeFactoCounties;	//input/defacto_<tier>.bmp
	public BufferedImage bufInDeFactoDuchies;	//input/defacto_<tier>.bmp
	public BufferedImage bufInDeFactoKingdoms;
	public BufferedImage bufInDeFactoEmpires;

	public BufferedImage bufInGovernment;		// input/government.bmp
	public BufferedImage bufInReligions;			// input/religions.bmp
	public BufferedImage bufInCultures;			// input/cultures.bmp
	
	public BufferedImage bufInNumSlots;			// input/numslots.bmp
	public BufferedImage bufInTechnology;		// input/technology.bmp
	
	//Input.bmp
	public InputTerrain[][] inputArray;
	
	//Climate.bmp
	public Climate[][] climateArray;
	
	//Terrain.bmp
	public Terrain[][] terrainArray;
	
	//Topology.bmp
	public int[][] heightArray;
	public boolean[][] isWater;
	
	//Rivers.bmp
	public Rivers[][] riverArray;
	
	//Settlements.bmp
	public List<Coordinates> settlementsList; //Lists the x,y coordinates of every major settlement (one defines each province)
	public List<Coordinates> importantSettlementsList; //Subset of settlementsList marked as important. Usually kingdom capitals.
	public List<Coordinates> watersList; //Lists the x,y coordinates of a water node (one defines each water province)
	public List<Coordinates> seasList; //Subset of watersList. For navigable sea zones only. (port = yes)
	public List<Coordinates> riversList; //Subset of watersList. For major rivers and lakes.
	public List<Coordinates> wastelandsList; //Lists the x,y coordinates of a wasteland landmark (one defines each wasteland province)

	//Provinces.bmp
	public List<Province> provinceList;
	public List<Province> waterProvinceList;
	public Map<Integer, Province> provinceColorMap; //Maps an integer representing an rgb color to the province associated with it in provinces.bmp. For easy retrieval and optimization purposes.
	public Province[][]provinceArray; //Maps x,y coordinates to the Province at these coordinates
	public boolean[][]provinceCoastlineArray; //Array of coordinates that are on land, but adjacent to a sea province (does not include major river banks)
	public boolean[][]provinceRiverbankArray; //Array of coordinates that are on land, but adjacent to a major river province (does not include coastlines)
	public boolean[][]provinceBorders; //Array of map coordinates that are adjacent to another province. Used to draw cosmetic borders on some generated images not used by the game.

	//Religions.bmp
	public Map<Integer, String> religionMap;
	
	//Cultures.bmp
	public Map<Integer, String> cultureMap;

	
	
	private static Loader singleton;
	
	public static Loader getLoader() {
		if (singleton == null)
		{
			singleton = new Loader();
		}
		
		return singleton;
	}
	
	public static void unload()
	{
		System.out.println("UNLOAD");
		singleton = null;
		
		System.gc();
	}
	
	private Loader() {
		
	}
	
	private void initSize(BufferedImage img)
	{
		if (img == null)
			return;
		
		if (sizeX == 0)
			sizeX = img.getWidth();
		if (sizeY == 0)
			sizeY = img.getHeight();
	}
	
	private void initSizeScaled(BufferedImage img)
	{
		if (img == null)
			return;
		
		if (sizeX == 0)
			sizeX = img.getWidth() * Config.INPUT_MAP_SCALE;
		if (sizeY == 0)
			sizeY = img.getHeight() * Config.INPUT_MAP_SCALE;
	}
	
	public void loadInput()
	{
		if (bufInInput != null)
			return;
		
		try {
			bufInInput = (BufferedImage) Utils.readInputImage(InputFile.Input.getFileName());
		}
		catch(IOException e)
		{
			Logger.log("input map not found");
			bufInInput = null;
			return;
		}

		initSizeScaled(bufInInput);
	}
	public void loadClimate()
	{
		if (bufInClimate != null)
			return;
		
		try {
			bufInClimate = (BufferedImage) Utils.readInputImage(InputFile.Climate.getFileName());
		}
		catch(IOException e)
		{
			Logger.log("climate map not found");
			bufInClimate = null;
			return;
		}
		
		initSizeScaled(bufInClimate);
	}
	public void loadTerrain()
	{
		if (bufInTerrain != null)
			return;
		
		try {
			bufInTerrain = (BufferedImage) Utils.readInputImage(InputFile.Terrain.getFileName());
		}
		catch(IOException e)
		{
			Logger.log("terrain map not found");
			bufInTerrain = null;
			return;
		}

		initSize(bufInTerrain);
		
		terrainArray = new Terrain[sizeX][sizeY];
		
		if (bufInHeights == null)
			isWater = new boolean[sizeX][sizeY];

		for (int x=0; x<sizeX; x++)
			for(int y=0; y<sizeY; y++)
				for (Terrain t : Terrain.values())
					if (t.getRGB() == bufInTerrain.getRGB(x, y) )
					{
						terrainArray[x][y] = t;
						
						if (bufInHeights == null)
							isWater[x][y] = (t == Terrain.WATER); //Fallback if heights cannot be loaded
					}
		
	}	
	public void loadHeights()
	{
		if (bufInHeights != null)
			return;
		
		try {
			bufInHeights = (BufferedImage) Utils.readInputImage(InputFile.Heights.getFileName());
		}
		catch(IOException e)
		{
			Logger.log("topology map not found");
			bufInTerrain = null;
			return;
		}

		initSize(bufInHeights);
		
		heightArray = new int[sizeX][sizeY];		
		isWater = new boolean[sizeX][sizeY];

		int[] heightArrayForRaster = new int[sizeX * sizeY];
		bufInHeights.getRaster().getPixels(0, 0, sizeX, sizeY, heightArrayForRaster);
		
		for (int x=0; x<sizeX; x++)
			for(int y=0; y<sizeY; y++)
			{
				
				heightArray[x][y] = heightArrayForRaster[x+y*sizeX];
				isWater[x][y] = heightArray[x][y] < 95;
				//System.out.println(heightArray[x][y]);
			}
		
	}
	public void loadSettlements()
	{
		if (bufInSettlements != null)
			return;
		
		settlementsList = new ArrayList<Coordinates>();
		importantSettlementsList = new ArrayList<Coordinates>();
		watersList = new ArrayList<Coordinates>();
		seasList = new ArrayList<Coordinates>();
		riversList = new ArrayList<Coordinates>();
		wastelandsList = new ArrayList<Coordinates>();
		
		try {
			bufInSettlements = (BufferedImage) Utils.readInputImage(InputFile.Settlements.getFileName());
		}
		catch(IOException e)
		{
			Logger.log("settlements map not found");
			bufInSettlements = null;
			return;
		}
		
		initSize(bufInSettlements);
		
		for (int x=0; x<sizeX; x++)
			for(int y=0; y<sizeY; y++)
				if (bufInSettlements.getRGB(x, y) == Color.RED.getRGB())
				{
					settlementsList.add(new Coordinates(x,y));
				}
				else if (bufInSettlements.getRGB(x, y) == Color.YELLOW.getRGB())
				{
					settlementsList.add(new Coordinates(x,y));
					importantSettlementsList.add(new Coordinates(x,y));
				}
				else if (bufInSettlements.getRGB(x, y) == Color.BLUE.getRGB())
				{
					watersList.add(new Coordinates(x,y));
					seasList.add(new Coordinates(x,y));
				}
				else if (bufInSettlements.getRGB(x, y) == Color.CYAN.getRGB())
				{
					watersList.add(new Coordinates(x,y));
					riversList.add(new Coordinates(x,y));
				}
				else if (bufInSettlements.getRGB(x, y) == Color.GREEN.getRGB())
				{
					wastelandsList.add(new Coordinates(x,y));
				}
	}
	public void loadRivers()
	{
		if (bufInRivers != null)
			return;
		
		try {
			bufInRivers = (BufferedImage) Utils.readInputImage(InputFile.Rivers.getFileName());
		}
		catch(IOException e)
		{
			Logger.log("rivers map not found");
			bufInRivers = null;
			return;
		}
		
		initSize(bufInRivers);
		
		riverArray = new Rivers[sizeX][sizeY];

		for (int x=0; x<sizeX; x++)
			for(int y=0; y<sizeY; y++)
				if (bufInRivers.getRGB(x, y) == Color.CYAN.getRGB())
					riverArray[x][y] = Rivers.RIVER9;
				else if (bufInRivers.getRGB(x, y) == Color.GREEN.getRGB())
					riverArray[x][y] = Rivers.RIVER_SOURCE;
				else if (bufInRivers.getRGB(x, y) == Color.YELLOW.getRGB())
					riverArray[x][y] = Rivers.SPLITTING_RIVER;
				else if (bufInRivers.getRGB(x, y) == Color.RED.getRGB())
					riverArray[x][y] = Rivers.MERGING_RIVER;
				else if (bufInRivers.getRGB(x, y) == Rivers.WATER.getRGB())
					riverArray[x][y] = Rivers.WATER;
				else
					riverArray[x][y] = Rivers.LAND;
		
	}
	public void loadProvinces(boolean initObjects, boolean parseAdjacencies) throws Exception	{
		
		if (bufInProvinces != null)
			return;
		
		if (initObjects)
		{
			provinceList = new ArrayList<Province>();
			waterProvinceList = new  ArrayList<Province>();
			provinceColorMap = new HashMap<Integer, Province>();
			provinceColorMap.put(Color.BLACK.getRGB(), null);
			provinceColorMap.put(Color.WHITE.getRGB(), null);
			provinceArray = new Province[sizeX][sizeY];
			provinceCoastlineArray = new boolean[sizeX][sizeY];
			provinceRiverbankArray = new boolean[sizeX][sizeY];
			provinceBorders = new boolean[sizeX][sizeY];
		}
		
		try {
			bufInProvinces = (BufferedImage) Utils.readInputImage(InputFile.Provinces.getFileName());
			
			//Export to output folder, overwriting any existing
			Files.copy(new File(InputFile.Provinces.getFileName()).toPath(), new File("./output/map/provinces.bmp").toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		catch(IOException e)
		{
			Logger.log("provinces map not found");
			bufInProvinces = null;
			return;
		}
		
		if (initObjects)
		{
			if (settlementsList == null)
			{
				loadSettlements();
			}
			
			if (settlementsList == null)
			{
				String errMess = "ERROR : a valid settlements.bmp is required to work with provinces.bmp";
				Logger.log(errMess);
				throw new CK2MapToolsException(errMess);
			}
			
			int index = 0;
			
			for (Coordinates c :  settlementsList)
			{
				int color = bufInProvinces.getRGB(c.getX(),c.getY());
				Province p = new Province(c, color, ++index);
				p.setWater(false);
				provinceList.add(p);
				if (provinceColorMap.containsKey(color))
				{
					String errMess = "ERROR : more than 1 settlement in province at coordinates "+c.getX()+";"+c.getY();

					p = provinceColorMap.get(color);
					if (p!= null)
						errMess = errMess + "\r\nExisting node at "+p.getX()+";"+p.getY();
					
					Logger.log(errMess);
					throw new CK2MapToolsException(errMess);
				}
				provinceColorMap.put(color, p);
			}
			
			for (Coordinates c :  importantSettlementsList)
			{
				int color = bufInProvinces.getRGB(c.getX(),c.getY());
				Province p = provinceColorMap.get(color);
				p.setWater(false);
				p.setImportant(true);
			}
			
			for (Coordinates c :  wastelandsList)
			{
				int color = bufInProvinces.getRGB(c.getX(),c.getY());
				Province p = new Province(c, color, ++index);
				p.setWater(false);
				p.setWasteland(true);
				provinceList.add(p);
				if (provinceColorMap.containsKey(color))
				{
					String errMess = "ERROR : wasteland node found in province with settlement or other node at coordinates "+c.getX()+";"+c.getY();
					p = provinceColorMap.get(color);
					if (p!= null)
						errMess = errMess + "\r\nExisting node at "+p.getX()+";"+p.getY();
					Logger.log(errMess);
					throw new CK2MapToolsException(errMess);
				}
				provinceColorMap.put(color, p);
			}

			for (Coordinates c :  watersList)
			{
				int color = bufInProvinces.getRGB(c.getX(),c.getY());
				Province p = new Province(c, color, ++index);
				p.setWater(true);
				p.setRiver(false);
				waterProvinceList.add(p);
				if (provinceColorMap.containsKey(color))
				{
					String errMess = "ERROR : more than 1 water node in province at coordinates "+c.getX()+";"+c.getY();
					p = provinceColorMap.get(color);
					if (p!= null)
						errMess = errMess + "\r\nExisting node at "+p.getX()+";"+p.getY();
					Logger.log(errMess);
					throw new CK2MapToolsException(errMess);
				}
				provinceColorMap.put(color, p);
			}
			
			for (Coordinates c :  riversList)
			{
				int color = bufInProvinces.getRGB(c.getX(),c.getY());
				Province p = provinceColorMap.get(color);
				p.setRiver(true);
			}
			
			//Determine province territory
			Logger.log("Determining province territory...");
			for (int x=0; x<sizeX; x++)
				for(int y=0; y<sizeY; y++)
				{
					//Not a province
					if (bufInProvinces.getRGB(x,y) == Color.BLACK.getRGB())
						continue;
					
					Province p = provinceColorMap.get(bufInProvinces.getRGB(x,y));
					if (p != null)
					{
						provinceArray[x][y] = p;
						p.addTerritory(x, y);
					}
				}
			
			//Little check
			for (Province p : provinceList)
			{
				if (p.isWasteland())
					continue;
				
				if (p.getTerritorySize() == 0)
				{
					String errMess = "ERROR: Province "+p+" has no territory";
					Logger.log(errMess);
					throw new CK2MapToolsException(errMess);
				}
			}
			
			//Determine adjacencies
			Logger.log("Determining adjacencies...");
			for (int x=0; x<sizeX; x++)
				for (int y=0; y<sizeY; y++)
				{
					Province p = provinceArray[x][y];
					
					if (p == null)
						continue;
					if (p.isWasteland() || p.isWater())
						continue;
					
					//Find adjacent coordinates
					int rx, ry;
					for (int i=0; i<4; i++)
					{
						switch (i)
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
						
						//Out of map
						if (rx<0 || rx>=sizeX || ry<0 || ry>= sizeY)
							continue;

						Province n = provinceArray[rx][ry];

						if (n != null && n != p) //there is a province here and it's not me
						{
							if (n.isWater())
							{
								p.addAdjacentWaterProvince(n);
								n.addAdjacentProvince(p);
								
								if (n.isRiver()) //Is this a major river province ?
								{
									provinceRiverbankArray[x][y] = true;
									p.setBordersMajorRiver(true);
								}
								else
								{
									provinceCoastlineArray[x][y] = true;
									p.setPort(true);
									if (p.getPortCoordinates() == null)
										p.setPortCoordinates(x, y);
									else
									{
										int distance1 = Utils.getDistanceSquared(p.getX(), p.getY(), p.getPortCoordinates().getX(), p.getPortCoordinates().getY());
										int distance2 = Utils.getDistanceSquared(p.getX(), p.getY(), x, y);
										if (distance2 < distance1)
											p.setPortCoordinates(x, y);
										
									}
								}
							}
							else if (!n.isWasteland()) //Don't allow wastelands to act as neighbours, they are considered impassable
							{
								p.addAdjacentProvince(n);
								n.addAdjacentProvince(p);
								provinceBorders[x][y] = true;
							}
						} //end if
					}//newt rx,ry
				}//next x,y

			
			if (parseAdjacencies)
			{
				//Parse adjacencies.csv
				File adjacenciesCsv = new File("./output/map/adjacencies.csv");
				if (adjacenciesCsv.exists())
				{
					List<String[]> adjacenciesData;
					try {
						adjacenciesData = Utils.parseCsvFile("./output/map/adjacencies.csv");
					} catch (IOException e) {
						Logger.log("ERROR : "+e.getMessage());
						throw e;
					}
					
					boolean header = true;
					for (String[] data : adjacenciesData)
					{
						if (header) //ignore first line
						{
							header = false;
							continue;
						}
						
						int from = Integer.parseInt(data[0]);
						int to = Integer.parseInt(data[1]);
						
						if (data[2].equals("sea") || data[2].equals("major_river"))
						{
							//Oops
							if (provinceList.size() < from || provinceList.size() < to)
							{
								String errMess = "ERROR : unknown province in adjacencies. Regenerate adjacencies.";
								Logger.log(errMess);
								throw new CK2MapToolsException(errMess);
							}
							
							Province pFrom = provinceList.get(from-1);
							Province pTo = provinceList.get(to-1);
						
							pFrom.addIndirectAdjacentProvince(pTo);
							pTo.addIndirectAdjacentProvince(pFrom);
						}
					}
				}
				
			}
		}
		
		initSize(bufInProvinces);
	}

	public void loadDeJureMaps() throws Exception
	{
		if (bufInDeJureDuchies != null || bufInDeJureKingdoms != null || bufInDeJureEmpires != null )
			return;
		
		try {
			bufInDeJureDuchies = (BufferedImage) Utils.readInputImage(InputFile.DeJureD.getFileName());
		}
		catch(IOException e)
		{
			Logger.log("dejure_duchies map not found");
			bufInDeJureDuchies = null;
		}
		try {
			bufInDeJureKingdoms = (BufferedImage) Utils.readInputImage(InputFile.DeJureK.getFileName());
		}
		catch(IOException e)
		{
			Logger.log("dejure_kingdoms map not found");
			bufInDeJureKingdoms = null;
		}
		try {
			bufInDeJureEmpires = (BufferedImage) Utils.readInputImage(InputFile.DeJureE.getFileName());
		}
		catch(IOException e)
		{
			Logger.log("dejure_empires map not found");
			bufInDeJureEmpires = null;
		}
		
		if (provinceList == null)
		{
			loadProvinces(true, true);

			if (provinceList == null)
			{
				String errMess = "ERROR : a valid provinces.bmp is required to work with dejure maps";
				Logger.log(errMess);
				throw new CK2MapToolsException(errMess);
			}
		}
		
		//Assign duchies, kingdoms and empires
		Logger.log("Assigning de jure territory...");
		for (Province p : provinceList)
		{
			if (p.isWasteland())
				continue;
			
			int x = p.getX();
			int y = p.getY();
			
			if (bufInDeJureDuchies != null)
			{
				Province duchy = provinceColorMap.get(bufInDeJureDuchies.getRGB(x, y));
				p.setDeJureDuchyCapital(duchy);
			}

			
			if (bufInDeJureKingdoms != null)
			{
				Province kingdom = provinceColorMap.get(bufInDeJureKingdoms.getRGB(x, y));
				p.setDeJureKingdomCapital(kingdom);
			}
			
			if (bufInDeJureEmpires != null)
			{
				Province empire = provinceColorMap.get(bufInDeJureEmpires.getRGB(x, y));
				p.setDeJureEmpireCapital(empire);
			}				
		}
		
		//Annoying CTD errors if this is incorrect, worth checking
		Logger.log("Validating de jure territory...");
		validateDeJureTerritory();
	}
	public void loadDeFactoMaps()
	{
		if (bufInDeFactoCounties != null  || bufInDeFactoDuchies != null || bufInDeFactoKingdoms != null || bufInDeFactoEmpires != null )
			return;

		try {
			bufInDeFactoCounties = (BufferedImage) Utils.readInputImage(InputFile.DeFactoC.getFileName());
		}
		catch(IOException e)
		{
			Logger.log("defacto_duchies map not found");
			bufInDeFactoDuchies = null;
		}
		try {
			bufInDeFactoDuchies = (BufferedImage) Utils.readInputImage(InputFile.DeFactoD.getFileName());
		}
		catch(IOException e)
		{
			Logger.log("defacto_duchies map not found");
			bufInDeFactoDuchies = null;
		}
		try {
			bufInDeFactoKingdoms = (BufferedImage) Utils.readInputImage(InputFile.DeFactoK.getFileName());
		}
		catch(IOException e)
		{
			Logger.log("defacto_kingdoms map not found");
			bufInDeFactoKingdoms = null;
		}
		try {
			bufInDeFactoEmpires = (BufferedImage) Utils.readInputImage(InputFile.DeFactoE.getFileName());
		}
		catch(IOException e)
		{
			Logger.log("defacto_empires map not found");
			bufInDeFactoEmpires = null;
		}
		
		//Assign duchies, kingdoms and empires
		Logger.log("Assigning de facto territory...");
		for (Province p : provinceList)
		{
			if (p.isWasteland())
				continue;
			
			int x = p.getX();
			int y = p.getY();
			
			if (bufInDeFactoDuchies != null)
			{
				Province duchy = provinceColorMap.get(bufInDeFactoDuchies.getRGB(x, y));
				p.setDeFactoDuchyCapital(duchy);
			}
			
			if (bufInDeFactoKingdoms != null)
			{
				Province kingdom = provinceColorMap.get(bufInDeFactoKingdoms.getRGB(x, y));
				p.setDeFactoKingdomCapital(kingdom);
			}
			
			if (bufInDeFactoEmpires != null)
			{
				Province empire = provinceColorMap.get(bufInDeFactoEmpires.getRGB(x, y));
				p.setDeFactoEmpireCapital(empire);
			}
				
		}
	}
	
	public void loadGovernment()
	{
		if (bufInGovernment != null)
			return;
		
		try {
			bufInGovernment = (BufferedImage) Utils.readInputImage(InputFile.Government.getFileName());
		}
		catch(IOException e)
		{
			Logger.log("government map not found");
			bufInGovernment = null;
			return;
		}
		
		//Assign government
		Logger.log("Assigning government type...");
		for (Province p : provinceList)
		{
			if (p.isWasteland())
				continue;
			
			int x = p.getX();
			int y = p.getY();

			if (bufInGovernment.getRGB(x, y) == Color.BLUE.getRGB())
			{
				p.setGovernment(Government.FEUDAL);
			}
			else if (bufInGovernment.getRGB(x, y) == Color.RED.getRGB())
			{
				p.setGovernment(Government.REPUBLIC);
				
				//Republic + duchy + coastal province = Merchant republic
				if (p.getDeFactoDuchyCapital() == p && p.isPort())
				{
					p.setGovernment(Government.MERCHANT_REPUBLIC);
				}
			}
			else if (bufInGovernment.getRGB(x, y) == Color.WHITE.getRGB())
			{
				p.setGovernment(Government.THEOCRACY);
			}
			else if (bufInGovernment.getRGB(x, y) == Color.GREEN.getRGB())
			{
				p.setGovernment(Government.THEOCRACY_TRIBAL);
			}
			else if (bufInGovernment.getRGB(x, y) == Color.YELLOW.getRGB())
			{
				p.setGovernment(Government.NOMAD);
			}
			else //Default to tribal
			{
				p.setGovernment(Government.TRIBAL);
			}

	
		}	
		
	}
	public void loadReligions(boolean initObjects)
	{
		if (bufInReligions != null)
			return;
		
		try {
			bufInReligions = (BufferedImage) Utils.readInputImage(InputFile.Religions.getFileName());
		}
		catch(IOException e)
		{
			Logger.log("religions map not found");
			bufInReligions = null;
			return;
		}
		
		if (initObjects)
		{
			religionMap = new HashMap<Integer, String>();
			
			//Parse religions
			try {
				List<String[]> infoTxtData = Utils.parseCsvFile("./input/religions.csv");
				for (String[] data : infoTxtData)
				{
					if (data[0].isEmpty() || data[0].startsWith("#"))
						continue;
					
					religionMap.put(new Color(Integer.parseInt(data[1]), Integer.parseInt(data[2]), Integer.parseInt(data[3]), 255).getRGB(), data[0]);
				}
			}
			catch (Exception e)
			{
				Logger.log("religions.csv file not found or error parsing it : "+e.toString());
			}
			
			//Optional : Parse holysite.csv
			try {
				List<String[]> holyTxtData = Utils.parseCsvFile("./input/holysite.csv");
				for (String[] data : holyTxtData)
				{
					if (data[0].isEmpty() || data[0].startsWith("#"))
						continue;
					
					String religion = data[0];
					int x,y;
					x = Integer.parseInt(data[1]);
					y = Integer.parseInt(data[2]);
					Province p = provinceColorMap.get(bufInProvinces.getRGB(x, y));
					p.setHolysite(religion);
				}
			}
			catch (Exception e)
			{
				Logger.log("holysite.csv file not found or error parsing it : "+e.toString());
			}
			
			//Assign religion
			Logger.log("Assigning religions...");
			for (Province p : provinceList)
			{
				if (p.isWasteland())
					continue;
				
				int x = p.getX();
				int y = p.getY();
						
				int rgb = bufInReligions.getRGB(x, y);
				//Logger.log("Religion for province "+p.getIndex()+" is "+rgb);
				p.setReligion(religionMap.get(rgb));

			}
		}
	}
	public boolean loadCultures(boolean initObjects)
	{
		try {
			bufInCultures = (BufferedImage) Utils.readInputImage(InputFile.Cultures.getFileName());
		}
		catch(IOException e)
		{
			Logger.log("cultures map not found");
			bufInCultures = null;
			return false;
		}
		
		if (initObjects)
		{
			cultureMap = new HashMap<Integer, String>();
			
			//Parse cultures
			try {
				List<String[]> infoTxtData = Utils.parseCsvFile("./input/cultures.csv");
				for (String[] data : infoTxtData)
				{
					if (data[0].isEmpty() || data[0].startsWith("#"))
						continue;

					cultureMap.put(new Color(Integer.parseInt(data[1]), Integer.parseInt(data[2]), Integer.parseInt(data[3]), 255).getRGB(), data[0]);
				}
			}
			catch (Exception e)
			{
				Logger.log("cultures.csv file not found or error parsing it : "+e.toString());
			}
			
			Culture.parseCultures();
			
			//Assign culture
			Logger.log("Assigning cultures...");
			for (Province p : provinceList)
			{
				if (p.isWasteland())
					continue;
				
				int x = p.getX();
				int y = p.getY();
						

				int rgb = bufInCultures.getRGB(x, y);
				//Logger.log("Culture for province "+p.getIndex()+" is "+rgb);
				p.setCulture(Culture.getCulture(cultureMap.get(rgb)));
				if (p.getCulture() == null)
				{
					Logger.log("ERROR: Culture not found for province "+p.toString()+" : "+Utils.getColorR(rgb)+","+Utils.getColorG(rgb)+","+Utils.getColorB(rgb));
					return false;
				}

			}
		}
		
		return true;
	}
	public void loadNumSlots()
	{
		if (bufInNumSlots != null)
			return;
		
		try {
			bufInNumSlots = (BufferedImage) Utils.readInputImage(InputFile.NumSlots.getFileName());
		}
		catch(IOException e)
		{
			Logger.log("numslots map not found");
			bufInNumSlots = null;
			return;
		}
		
		//Assign number of holdings slots
		Logger.log("Assigning holding slots...");
		for (Province p : provinceList)
		{
			//Only the green channel matters
			p.setNumSlots(Utils.getColorG(bufInNumSlots.getRGB(p.getX(), p.getY())) / 30);
			p.setNumSlots(Math.max(1, Math.min(7, p.getNumSlots())));
		}
	}	
	public void loadTechnology()
	{
		if (bufInTechnology != null)
			return;
		
		try {
			bufInTechnology = (BufferedImage) Utils.readInputImage(InputFile.Technology.getFileName());
		}
		catch(IOException e)
		{
			Logger.log("technology map not found");
			bufInTechnology = null;
			return;
		}
		
		//Determine technology level
		for (Province p : provinceList)
		{
			p.setTechnology(bufInTechnology.getRGB(p.getX(), p.getY()));
		}
	}	

	//Checks that there isn't a dejure duchy / kingdom / empire with a capital province that is not a part of it
	//This makes the landed_titles generation sort of fail silently and causes a CTD when launching the game
	public void validateDeJureTerritory() throws Exception {
		for (Province p : provinceList)
		{
			if (p.isWasteland())
				continue;

			Province empire = p.getDeJureEmpireCapital();
			boolean bFoundEmpire = false;
			boolean bFoundKingdom = false;
			boolean bFoundDuchy = false;
			
			for (Province p2 : provinceList)
			{
				if (p2.getDeJureEmpireCapital() == empire)
					if (empire == p2)
					{
						bFoundEmpire = true;
						break;
					}
			}

			
			Province kingdom = p.getDeJureKingdomCapital();
			
			for (Province p2 : provinceList)
			{
				if (p2.getDeJureKingdomCapital() == kingdom)
					if (kingdom == p2)
					{
						bFoundKingdom = true;
						break;
					}
			}

			
			Province duchy = p.getDeJureDuchyCapital();
			bFoundDuchy = false;
			
			for (Province p2 : provinceList)
			{
				if (p2.getDeJureDuchyCapital() == duchy)
					if (duchy == p2)
					{
						bFoundDuchy = true;
						break;
					}
			}
			
			if (!bFoundDuchy || !bFoundKingdom || !bFoundEmpire)
			{
				Province capital = (!bFoundDuchy ? duchy : (!bFoundKingdom ? kingdom : empire));
				String tier = (!bFoundDuchy ? "duchy" : (!bFoundKingdom ? "kingdom" : "empire"));
				
				String errMess = "ERROR : province " + p.getIndex() + "["+p.getX()+";"+p.getY()+"] has color (" + capital.getMapColorR() + "|" + capital.getMapColorG() + "|" + capital.getMapColorB() + ")";
				if (capital != null)
				{
					errMess = errMess + " of province "+capital.getIndex()+ "["+capital.getX()+";"+capital.getY()+"] that does not belong to that "+tier;
				}
				
				Logger.log(errMess);
				throw new CK2MapToolsException(errMess);
			}
				
		}

	}
	
	//Determines the set of all island regions.
	//An island region is a set of provinces that are connected to each other by land or adjacencies, but not to other island regions
	//The largest island region is called the continent and is placed at the start of the returned List, so it can be ignored if needed.
	public List<List<Province>> getIslandRegions() {
		List<List<Province>> islandRegions = new ArrayList<List<Province>>();
		Logger.log("Calculating island regions");
		
		//STEP1: For each province, find all the provinces it is adjacent to
		for (Province currentProvince : provinceList)
		{
			//Get my islandregion
			List<Province> currentIslandRegion = currentProvince.getIslandRegion();
			
			if(currentIslandRegion == null) //Actually I don't have one yet
			{
				currentIslandRegion = new ArrayList<Province>();
				currentProvince.setIslandRegion(currentIslandRegion); //So assign me a brand new islandregion
			}
			
			//For each one of my neighbours...
			for (Province adjacent : currentProvince.getAllAdjacentProvinces())
			{
				//...What islandregion does it belong to ?
				List<Province> adjacentIslandRegion = adjacent.getIslandRegion();
				
				//None
				if (adjacentIslandRegion == null)
				{
					//Add it to my islandregion
					adjacent.setIslandRegion(currentIslandRegion);
				}
				//A different, smaller or equal-sized islandregion 
				else if (adjacentIslandRegion != currentIslandRegion && adjacentIslandRegion.size() <= currentIslandRegion.size())
				{
					
					//Any provinces belonging to its islandregion are moved into my islandregion
					for (Province i : adjacentIslandRegion)
					{
						i.setIslandRegion(currentIslandRegion);
					}
					
					//Empty its previous islandregion and let it be destroyed
					adjacentIslandRegion.clear();
					adjacentIslandRegion = currentIslandRegion;
				}
				//A different, larger islandregion
				else if (adjacentIslandRegion != currentIslandRegion && adjacentIslandRegion.size() > currentIslandRegion.size())
				{

					//Any provinces belonging to my islandregion are moved into its islandregion
					for (Province i : currentIslandRegion)
					{
						i.setIslandRegion(adjacentIslandRegion);
					}
					
					//Empty my previous islandregion and let it be destroyed
					currentIslandRegion.clear();
					currentIslandRegion = adjacentIslandRegion;
				}
			}
		}
		
		//STEP2: Find the biggest island region, that's "the continent" and is not counted as an island
		List<Province> continent = null;
		for (Province currentProvince : provinceList)
		{
			List<Province> islandRegion = currentProvince.getIslandRegion();
			
			if (continent == null || islandRegion.size() > continent.size())
			{
				continent = islandRegion;
			}
		}
		
		//STEP3: add all island regions to the return list, put the continent at position 0
		islandRegions.add(continent);
		for (Province p : provinceList)
		{
			List<Province> islandRegion = p.getIslandRegion();
			
			if (islandRegion != continent && !islandRegions.contains(islandRegion))
				islandRegions.add(islandRegion);
		}
		
		
		return islandRegions;
	}
}
