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
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ck2maptools.data.Character;
import ck2maptools.data.Climate;
import ck2maptools.data.Coordinates;
import ck2maptools.data.Culture;
import ck2maptools.data.Government;
import ck2maptools.data.InputFile;
import ck2maptools.data.Loader;
import ck2maptools.data.OutputFile;
import ck2maptools.data.Province;
import ck2maptools.data.Terrain;
import ck2maptools.data.Trees;
import ck2maptools.utils.Config;
import ck2maptools.utils.Logger;
import ck2maptools.utils.Utils;

//The big one
public class CK2MakeProvinceSetup implements ICK2MapTool {

	private Loader loader;
	
	private boolean makeLocalisation = true;
	private boolean makeLocalisationTemplate = true;
	private boolean addEmptyBaroniesToLocTemplate = false;
	private boolean makeTechnology = true;
	private boolean makeOldProvinceSetup = false;

	//public void setParamMakeLocalisation(boolean makeLocalisation) {this.makeLocalisation = makeLocalisation;}
	public void setParamMakeTechnology(boolean makeTechnology) {this.makeTechnology = makeTechnology;}	
	public void setParamMakeOldProvinceSetup(boolean makeOldProvinceSetup) {this.makeOldProvinceSetup = makeOldProvinceSetup;}	
	public void setParamMakeLocalisationTemplate(boolean makeLocalisationTemplate) {this.makeLocalisationTemplate = makeLocalisationTemplate;}
	public void setParamAddEmptyBaroniesToLocTemplate(boolean addEmptyBaroniesToLocTemplate) {this.addEmptyBaroniesToLocTemplate = addEmptyBaroniesToLocTemplate;}
	
	public static InputFile[] inputFiles() {
		return new InputFile[]{
				InputFile.Terrain,
				InputFile.Settlements,
				InputFile.Provinces
		};
	}
	
	public static InputFile[] inputFilesOptional() {
		return new InputFile[]{
				InputFile.DeJureD,
				InputFile.DeJureK,
				InputFile.DeJureE,
				InputFile.DeFactoD,
				InputFile.DeFactoK,
				InputFile.DeFactoE,
				InputFile.Government,
				InputFile.Religions,
				InputFile.Cultures,
				InputFile.NumSlots,
				InputFile.Technology, //TODO: move elsewhere
		};
	}

	public static OutputFile[] outputFiles() {
		return new OutputFile[]{
				OutputFile.Definitions,
				OutputFile.Positions,
				OutputFile.Default,
				OutputFile.Climate,
				OutputFile.Continent,
				OutputFile.IslandRegions,
				OutputFile.Statics,
				OutputFile.LandedTitles,
				OutputFile.LandedTitlesPat,
				OutputFile.LandedTitlesMerc,
				OutputFile.HistoryProvincesFolder,
				OutputFile.HistoryTitlesFolder,
				OutputFile.HistoryCharacters,
				OutputFile.Dynasties,
				OutputFile.Localisation,
				OutputFile.HistoryTechnology
		};
	}
	
	public static void main(String[] args) throws Exception {
		
		CK2MakeProvinceSetup t = new CK2MakeProvinceSetup();
		
		//Argument : -nl / -nolocalisation : the program will NOT generate localisation info 
		//Argument : -nt / -notechnology : the program will NOT generate technology info
		for (String arg : args)
		{
			if (arg == null)
				continue;
			
			if (arg.equalsIgnoreCase("-nl") || arg.equalsIgnoreCase("-nolocalisation"))
			{
				t.makeLocalisation = false;
				Logger.log("Will not make localisation");
			}
			
			if (arg.equalsIgnoreCase("-nt") || arg.equalsIgnoreCase("-notechnology"))
			{
				t.makeTechnology = false;
				Logger.log("Will not make technology");
			}
			
			if (arg.equalsIgnoreCase("-ops") || arg.equalsIgnoreCase("-oldProvinceSetup"))
			{
				t.makeOldProvinceSetup = true;
				Logger.log("Will make old Province Setup");
			}
		}	
		
		t.execute();
	}
	
	public int execute() throws Exception {
		int returnCode = ERROR_NONE;
		long ms = System.currentTimeMillis();
		Logger.InitLogger("CK2MakeProvinceSetup");
		
		Utils.checkCriticalResources(inputFiles(), true);
		
		//Parse config.csv
		Config.parseConfig();
		
		//Load the input map
		loader = Loader.getLoader();
		loader.loadTerrain();
		loader.loadSettlements();
		loader.loadProvinces(true, true);
		
		//Optional
		loader.loadClimate();
		loader.loadDeJureMaps();
		loader.loadDeFactoMaps();
		loader.loadGovernment();
		loader.loadReligions(true);
		loader.loadCultures(true);
		loader.loadNumSlots();
		loader.loadTechnology();
	
	
		
		if (makeLocalisation)
		{
			//Optional : Parse localisation.csv
			try {
				List<String[]> locTxtData = Utils.parseCsvFile("./input/localisation.csv");
				
				//Prepare some lists to hold all of the data to assign randomly
				Map<Culture, List<String>> empireNamesMap = new HashMap<Culture, List<String>>();
				Map<Culture, List<String>> kingdomNamesMap = new HashMap<Culture, List<String>>();
				Map<Culture, List<String>> duchyNamesMap = new HashMap<Culture, List<String>>();
				Map<Culture, List<String>> provinceNamesMap = new HashMap<Culture, List<String>>();
				Map<Culture, List<String>> baronyNamesMap = new HashMap<Culture, List<String>>();
				
				for (String[] data : locTxtData)
				{
					if (data[0].isEmpty() || data[0].startsWith("#"))
						continue;
					
					Province p=null; // What we are naming
					
					if (data.length == 4) // Naming something specific : title tier;string;x coord;y coord
					{
						int x=Integer.parseInt(data[2]);
						int y=Integer.parseInt(data[3]);
						
						p=loader.provinceArray[x][y];
			
						if (p != null)
						{
							switch (data[0].toLowerCase())
							{
							case "empire":
								p.getDeJureEmpireCapital().setEmpireName(data[1]);
								break;
							case "kingdom":
								p.getDeJureKingdomCapital().setKingdomName(data[1]);
								break;
							case "duchy":
								p.getDeJureDuchyCapital().setDuchyName(data[1]);
								break;
							case "county":
							case "province":
								p.setProvinceName(data[1]);
								break;
							case "barony":
								for (int b=0; b<8 ; b++)
								{
									if (!p.hasBaronyName(b)) //put it in the first barony that doesn't have a name yet
									{
										p.setBaronyName(b, data[1]);
										break;
									}
								}
								break;
							}
							
							//Logger.log("Assigned "+data[0].toLowerCase()+" name "+data[1]);
						}
						else
						{
							Logger.log("Localisation error : province not found at "+x+" "+y);
							returnCode |= ERROR_LOCALISATION;
						}
					}
					else if (data.length == 3) // Random name : title tier;string;culture
					{
						Culture c = Culture.getCulture(data[2]);
						
						if (c == null)
						{
							//Try to get a random culture from a culture group with that name
							c = Culture.getRandomCultureFromGroup(data[2]);
						}
						
						if (c != null)
						{
							List<String> locList = null; //List that contains all the localisation strings for that tier and culture
							Map<Culture, List<String>> locMap = null; //Map that contains all the lists of strings for that tier, one for each culture
							
							switch (data[0].toLowerCase())
							{
							case "empire": locMap = empireNamesMap; break;
							case "kingdom": locMap = kingdomNamesMap;break;
							case "duchy": locMap = duchyNamesMap;break;
							case "county": case "province": locMap = provinceNamesMap;break;
							case "barony": locMap = baronyNamesMap;break;
							}
							
							if (locMap != null)
							{
								locList = locMap.get(c);
								
								//No list yet, create it and put it in the map
								if (locList == null)
								{
									locList = new ArrayList<String>();
									locMap.put(c, locList);
								}
								
								locList.add(data[1]);
								//Logger.log("Added name "+data[1]+" to "+data[0].toLowerCase()+" list for culture "+data[2]);
							}
						}
						else
						{
							Logger.log("Localisation error : culture or culture group not found : "+data[2]);
							returnCode |= ERROR_LOCALISATION;
						}
					}
	
				}
				
				List<String> globalNamesList = new ArrayList<String>();
				
				//Now that everything has been parsed, assign random names
				for (Province p : loader.provinceList)
				{
					List<String> namesList = null;
					String name = null;
					Culture c = p.getCulture();
					
					if (c != null)
					{
	
						if (p.getDeJureEmpireCapital() == p && !p.hasEmpireName())
						{
							namesList = empireNamesMap.get(c); 
							//Get a random name from the list and remove it so it does not get used again
							if (namesList != null && !namesList.isEmpty())
							{
								name = namesList.remove((int)(Math.random() * namesList.size()));
								globalNamesList.add(name);
								p.setEmpireName(name);
							}
						}
						if (p.getDeJureKingdomCapital() == p && !p.hasKingdomName())
						{
							namesList = kingdomNamesMap.get(c); 
							//Get a random name from the list and remove it so it does not get used again
							if (namesList != null && !namesList.isEmpty())
							{
								name = namesList.remove((int)(Math.random() * namesList.size()));
								globalNamesList.add(name);
								p.setKingdomName(name);
							}
						}
						if (p.getDeJureDuchyCapital() == p && !p.hasDuchyName())
						{
							namesList = duchyNamesMap.get(c); 
							//Get a random name from the list and remove it so it does not get used again
							if (namesList != null && !namesList.isEmpty())
							{
								name = namesList.remove((int)(Math.random() * namesList.size()));
								globalNamesList.add(name);
								p.setDuchyName(name);
							}
						}
						if (!p.hasProvinceName())
						{
							namesList = provinceNamesMap.get(c); 
							//Get a random name from the list and remove it so it does not get used again
							if (namesList != null && !namesList.isEmpty())
							{
								boolean nameFound = false;
								do 
								{
									name = namesList.remove((int)(Math.random() * namesList.size()));
									
									//Make sure the name is not already in use in another barony
									//The game doesn't like it too much when 2 different titles have the same name
									if (!globalNamesList.contains(name))
									{
										globalNamesList.add(name);
										nameFound = true;
									}
								}
								while (!nameFound && !namesList.isEmpty());
								p.setProvinceName(name);
							}
						}
						for (int b=0; b<8 ; b++)
						{
							if (!p.hasBaronyName(b))
							{
								namesList = baronyNamesMap.get(c); 
							
								//Get a random name from the list and remove it so it does not get used again
								if (namesList != null && !namesList.isEmpty())
								{
									boolean nameFound = false;
									do 
									{
										name = namesList.remove((int)(Math.random() * namesList.size()));
										
										//Make sure the name is not already in use in another barony
										//The game doesn't like it much when 2 different titles have the same name
										if (!globalNamesList.contains(name))
										{
											globalNamesList.add(name);
											nameFound = true;
										}
									}
									while (!nameFound && !namesList.isEmpty());
									p.setBaronyName(b, name);
								}
							}
						}
					}
				}
			}
			catch (Exception e)
			{
				Logger.log("localisation.csv file not found or error while parsing : "+e.toString());
				returnCode |= ERROR_LOCALISATION;
			}
		}
		
		//Added by Michael Gruar: Output a localisation-template.csv
		if (makeLocalisationTemplate)
		{
			try
			{
				//Make the file
				File locTemplateCsv = new File("./input/localisation_template.csv");
				Logger.log("Writing "+locTemplateCsv.getPath());
				FileWriter writer = new FileWriter(locTemplateCsv);
				
				//Save wastelands for later
				List<Province> wastelands = new ArrayList<>();
				
				//Start writing
				for (Province p : loader.provinceList)
				{
					if (p.isWasteland())
					{
						wastelands.add(p);
					}
					else if (p.getDeJureEmpireCapital() == p)
					{
						writer.write("empire;"+p.getEmpireName()+";"+p.getX()+";"+p.getY()+"\n");
						for (Province k : p.getDeJureEmpireVassalsPlusSelf())
						{
							if (k.getDeJureKingdomCapital() == k)
							{
								writer.write("kingdom;"+k.getKingdomName()+";"+k.getX()+";"+k.getY()+"\n");
								for (Province d : k.getDeJureKingdomVassalsPlusSelf())
								{
									if (d.getDeJureDuchyCapital() == d)
									{
										writer.write("duchy;"+d.getDuchyName()+";"+d.getX()+";"+d.getY()+"\n");
										for (Province c : d.getDeJureDuchyVassalsPlusSelf())
										{
											writer.write("county;"+c.getProvinceName()+";"+c.getX()+";"+c.getY()+"\n");
											//Skip barony[0], the tool always makes this the same as the province name.
											for (int b=1; b<8; b++)
											{
												//Don't clutter the template with unnamed baronies, unless otherwise specified.
												if (addEmptyBaroniesToLocTemplate || c.hasBaronyName(b))
												{
													writer.write("barony;"+c.getBaronyName(b)+";"+c.getX()+";"+c.getY()+"\n");
												}
											}
										}
									}
								}
							}
						}
					}
				}
				
				//Handle Wastelands
				for (Province waste : wastelands)
				{
					writer.write("province;"+waste.getProvinceName()+";"+waste.getX()+";"+waste.getY()+"\n");
				}
				//Handle Water
				for (Province water : loader.waterProvinceList)
				{
					writer.write("province;"+water.getProvinceName()+";"+water.getX()+";"+water.getY()+"\n");
				}
				writer.close();
			}
			catch (Exception e)
			{
				Logger.log("localisation_template.csv file could not be written : "+e.toString());
				returnCode |= ERROR_LOCALISATION;
			}
		}

		
		
	
		//////////////////////////////////////////////////////////////////////////////////////////////////
		//Moved from CK2MakeProvincesMap.txt
		//Setup output dirs if they don't exist
		File outputDirMap = Utils.mkDir("./output/map");

		//Make the definition.csv file
		File definitionCsv = new File(OutputFile.Definitions.getFileName());
		Logger.log("Writing "+definitionCsv.getPath(),5);
		FileWriter writer = new FileWriter(definitionCsv);
		//1st line is comment
		writer.write("province;red;green;blue;x;x\r\n");
		for (Province p : loader.provinceList)
		{
			int rgb = p.getMapColor();
			if (rgb != Color.BLACK.getRGB() && rgb != Color.WHITE.getRGB())
			{
				int r = Utils.getColorR(rgb);
				int g = Utils.getColorG(rgb);
				int b = Utils.getColorB(rgb);
				writer.write(p.getIndex()+";"+r+";"+g+";"+b+";"+p.getProvinceName()+";x\r\n");
			}
		}
		for (Province p : loader.waterProvinceList)
		{
			int rgb = p.getMapColor();
			int r = Utils.getColorR(rgb);
			int g = Utils.getColorG(rgb);
			int b = Utils.getColorB(rgb);
			writer.write(p.getIndex()+";"+r+";"+g+";"+b+";;x\r\n");
		}
		writer.close();
		
		
		
		//Make the positions.txt file
		File positionsTxt = new File(OutputFile.Positions.getFileName());
		Logger.log("Writing "+positionsTxt.getPath(),10);
		writer = new FileWriter(positionsTxt);
		for (Province p : loader.provinceList)
		{
			int px = p.getX(), py = p.getY();
			
			//Coordinates for the port
			Coordinates port = p.getPortCoordinates();
			double portRotation = 0.0;
			//Coordinates for the councillors
			Coordinates councillor = new Coordinates(px,py);
			
			if (!p.isWasteland())
			{
				//Port
				
				if (port != null)
				{
					Province pnw, pn, pne, pe, pse, ps, psw, pw;
					boolean nw, n, ne, e, se, s, sw, w;

					pnw = loader.provinceArray[port.getX()-1][port.getY()-1];
					pn = loader.provinceArray[port.getX()][port.getY()-1];
					pne = loader.provinceArray[port.getX()+1][port.getY()-1];
					pe = loader.provinceArray[port.getX()+1][port.getY()];
					pse = loader.provinceArray[port.getX()+1][port.getY()+1];
					ps = loader.provinceArray[port.getX()][port.getY()+1];
					psw = loader.provinceArray[port.getX()-1][port.getY()+1];
					pw = loader.provinceArray[port.getX()-1][port.getY()];
										
					nw = pnw != null && pnw.isWater() && !pnw.isRiver();
					n = pn != null && pn.isWater() && !pn.isRiver();
					ne = pne != null && pne.isWater() && !pne.isRiver();
					e = pe != null && pe.isWater() && !pe.isRiver();
					se = pse != null && pse.isWater() && !pse.isRiver();
					s = ps != null && ps.isWater() && !ps.isRiver();
					sw = psw != null && psw.isWater() && !psw.isRiver();
					w = pw != null && pw.isWater() && !pw.isRiver();
					
					//Try to rotate the port towards the sea
					if (nw && w && sw) 
					{
						portRotation = Math.PI * 0.5;//West
						port = new Coordinates(port.getX()-1,port.getY());
					}
					else if (nw && n && ne) 
					{
						portRotation = Math.PI * 1.0;//North
						port = new Coordinates(port.getX(),port.getY()-1);
					}
					else if (ne && e && se) 
					{
						portRotation = Math.PI * 1.5;//East
						port = new Coordinates(port.getX()+1,port.getY());
					}
					else if (se && s && sw) 
					{
						portRotation = Math.PI * 0.0;//South
						port = new Coordinates(port.getX(),port.getY()+1);
					}
					else if (nw && w) 
					{
						portRotation = Math.PI * 0.625;//West-North-West
						port = new Coordinates(port.getX()-1,port.getY());
					}
					else if (nw && n) 
					{
						portRotation = Math.PI * 0.875;//North-North-West
						port = new Coordinates(port.getX(),port.getY()-1);
					}
					else if (n && ne) 
					{
						portRotation = Math.PI * 1.125;//North-North-East
						port = new Coordinates(port.getX(),port.getY()-1);
					}
					else if (ne && e) 
					{
						portRotation = Math.PI * 1.375;//East-North-East
						port = new Coordinates(port.getX()+1,port.getY());
					}
					else if (e && se) 
					{
						portRotation = Math.PI * 1.625;//East-South-East
						port = new Coordinates(port.getX()+1,port.getY());
					}
					else if (se && s) 
					{
						portRotation = Math.PI * 1.875;//South-South-East
						port = new Coordinates(port.getX(),port.getY()+1);
					}
					else if (s && sw) 
					{
						portRotation = Math.PI * 0.125;//South-South-West
						port = new Coordinates(port.getX(),port.getY()+1);
					}
					else if (sw && w) 
					{
						portRotation = Math.PI * 0.375;//West-South-west
						port = new Coordinates(port.getX()-1,port.getY()+1);
					}
					else if (sw) 
					{
						portRotation = Math.PI * 0.25;//South-west
						port = new Coordinates(port.getX()-1,port.getY()+1);
					}
					else if (nw) 
					{
						portRotation = Math.PI * 0.75;//North-west
						port = new Coordinates(port.getX()-1,port.getY()-1);
					}
					else if (ne) 
					{
						portRotation = Math.PI * 1.25;//North-east
						port = new Coordinates(port.getX()+1,port.getY()-1);
					}
					else if (se) 
					{
						portRotation = Math.PI * 1.75;//South-east
						port = new Coordinates(port.getX()+1,port.getY()+1);
					}
				}
			

				//Councillors
				int tries=0;
				int rx, ry;
				boolean valid = false;
				do
				{
					rx = (int) ((Math.random() - Math.random()) * 14);
					ry = (int) ((Math.random() - Math.random()) * 14);
					tries++;
					
					valid = rx >= 0 && rx < loader.sizeX && ry >= 0 && ry < loader.sizeY && loader.provinceArray[rx][ry] == p;
					valid = valid && Utils.getDistanceSquared(px, py, rx, ry) > 100; //Not too close
					valid = valid && Utils.getDistanceSquared(px, py, rx, ry) < 200; //Not too far
				
				}
				while (!valid && tries <= 50);
				
				if (tries > 50)
				{
					rx=px; ry=py; //Default to province position
				}
				
				councillor = new Coordinates(rx,ry);

			}
			
			if (port == null)
				port = new Coordinates(px,py);
			
			
			writer.write("# "+p.getProvinceName()+"\r\n");
			writer.write(p.getIndex()+" =\r\n");
			writer.write("{\r\n");
			writer.write("\tposition={"+(px)+".000 "+(loader.sizeY-py)+".000 " //City
					+(px)+".000 "+(loader.sizeY-py)+".000 " //Unit
					+councillor.getX()+".000 "+(loader.sizeY-councillor.getY())+".000 " //Councillors
					+(px)+".000 "+(loader.sizeY-py)+".000 " //Text
					+port.getX()+".000 "+(loader.sizeY-port.getY())+".000 }\r\n"); //Port
			writer.write("\trotation={0.000 0.000 0.000 0.000 "+String.format(Locale.US, "%.3f", portRotation)+"}\r\n");
			writer.write("\theight={0.000 0.000 0.000 20.000 0.000}\r\n");
			writer.write("}\r\n");
		}
		
		for (Province p : loader.waterProvinceList)
		{
			writer.write("# "+p.getProvinceName()+"\r\n");
			writer.write(p.getIndex()+" =\r\n");
			writer.write("{\r\n");
			writer.write("\tposition={"+p.getX()+".000 "+(loader.sizeY-p.getY())+".000 "+
					p.getX()+".000 "+(loader.sizeY-p.getY())+".000 "+ //Boats
					p.getX()+".000 "+(loader.sizeY-p.getY())+".000 "+
					p.getX()+".000 "+(loader.sizeY-p.getY())+".000 "+ //Text
					p.getX()+".000 "+(loader.sizeY-p.getY())+".000 }\r\n");
			writer.write("\trotation={0.000 0.000 0.000 0.000 0.000}\r\n");
			writer.write("\theight={0.000 0.000 0.000 20.000 0.000}\r\n");
			writer.write("}\r\n");
		}
		writer.close();
		

		//Make the default.map file
		File defaultMap = new File(OutputFile.Default.getFileName());
		Logger.log("Writing "+defaultMap.getPath(),1);
		writer = new FileWriter(defaultMap);
		writer.write("max_provinces = "+(loader.provinceList.size()+loader.waterProvinceList.size()+1)+"\r\n"); //Pretty much the only thing that matters in this file, causes CTD if wrong
		writer.write("definitions = \"definition.csv\"\r\n");
		writer.write("provinces = \"provinces.bmp\"\r\n");
		writer.write("positions = \"positions.txt\"\r\n");
		writer.write("terrain = \"terrain.bmp\"\r\n");
		writer.write("rivers = \"rivers.bmp\"\r\n");
		writer.write("terrain_definition = \"terrain.txt\"\r\n");
		writer.write("heightmap = \"topology.bmp\"\r\n");
		writer.write("tree_definition = \"trees.bmp\"\r\n");
		writer.write("continent = \"continent.txt\"\r\n");
		writer.write("adjacencies = \"adjacencies.csv\"\r\n");
		writer.write("climate = \"climate.txt\"\r\n");
		writer.write("region = \"island_region.txt\"\r\n");	
		writer.write("geographical_region = \"geographical_region.txt\"\r\n");
		writer.write("static = \"statics\"\r\n");
		writer.write("seasons = \"seasons.txt\"\r\n");
		writer.write("\r\n");
		//TODO : find actual sea zones ?
		writer.write("sea_zones = { "+(loader.provinceList.size()+1)+" "+(loader.provinceList.size()+loader.waterProvinceList.size())+" }\r\n");
		writer.write("ocean_region = { sea_zones = { 1 } }\r\n");
		writer.write("# Define which indices in trees.bmp palette which should count as trees for automatic terrain assignment\r\n");
		writer.write("tree = { "+Trees.CONIFEROUS2.getIndex()+" "+Trees.CONIFEROUS3.getIndex()+" "+Trees.DECIDUOUS2.getIndex()+" "+Trees.DECIDUOUS3.getIndex()+" "+Trees.MEDITERRANEAN2.getIndex()+" "+Trees.MEDITERRANEAN3.getIndex()+" }\r\n");
		writer.write("major_rivers = {\r\n\t");
			for (Province p : loader.waterProvinceList)
			{
				if (p.isRiver())
				{
					writer.write(p.getIndex()+" ");
				}
			}
		writer.write("\r\n}\r\n");
		writer.close();
		
		
		
		//Make the climate.txt file
		File climateTxt = new File(OutputFile.Climate.getFileName());
		Logger.log("Writing "+climateTxt.getPath(),5);
		writer = new FileWriter(climateTxt);
		for (Province p : loader.provinceList)
		{
			Climate cli = Climate.getInputClimateAt(p.getX(), p.getY());
			int terrain = loader.bufInTerrain.getRGB(p.getX(), p.getY());
			int winterSeverity = 0;
			switch(cli)
			{
			case HOT:
				winterSeverity = -1; break;			

			case WARM:
				winterSeverity = 0; break;
			
			case TEMPERATE:
				winterSeverity = 1; break;

			case COLD:
				winterSeverity = 2; break;
				
			case POLAR:
				winterSeverity = 3; break;
			}
			
			if (!p.isWasteland()) //Wastelands are typically huge so don't factor in mountains
			{
				if (terrain == Terrain.MOUNTAIN.getRGB())
					winterSeverity++;
				else if (terrain == Terrain.SNOWY_MOUNTAIN.getRGB())
					winterSeverity+=2;
				else if (terrain == Terrain.FROZEN_MOUNTAIN.getRGB())
					winterSeverity+=3;
			}
			
			p.setWinterSeverity(winterSeverity);
		}
		
		writer.write("mild_winter = {\r\n");
		for (Province p : loader.provinceList)
		{
			if (p.getWinterSeverity() == 1)
			{
				writer.write("\t"+p.getIndex()+"\r\n");
			}
		}
		writer.write("}\r\n\r\n");
		writer.write("normal_winter = {\r\n");
		for (Province p : loader.provinceList)
		{
			if (p.getWinterSeverity() == 2)
			{
				writer.write("\t"+p.getIndex()+"\r\n");
			}
		}
		writer.write("}\r\n\r\n");
		writer.write("severe_winter = {\r\n");
		for (Province p : loader.provinceList)
		{
			if (p.getWinterSeverity() >= 3)
			{
				writer.write("\t"+p.getIndex()+"\r\n");
			}
		}
		writer.write("}\r\n\r\n");
		writer.close();
		
		//Make the geographical_region file
		File geographicalRegions = new File(outputDirMap + "/geographical_region.txt");
		writer = new FileWriter(geographicalRegions);
		//Create a region for each dejure empire, and name it the same
		writer.write("# Geographical regions\r\n");
		for (Province p : loader.provinceList)
		{
			if (p.isWasteland())
				continue;
			
			if (p.getDeJureEmpireCapital() == p)
			{
				writer.write("world_"+p.getTitleEmpireName().substring(2)+" = {\r\n");
				writer.write("\tduchies = {\r\n");
				for (Province k : p.getDeJureEmpireVassalsPlusSelf())
				{
					if (k.getDeJureKingdomCapital() == k)
					{
						writer.write("\t\t# "+k.getTitleKingdomName()+"\r\n");
						writer.write("\t\t");
						for (Province d : k.getDeJureKingdomVassalsPlusSelf())
						{
							if (d.getDeJureDuchyCapital() == d)
								writer.write(d.getTitleDuchyName()+" ");
						}
						writer.write("\r\n");
					}
				}
				writer.write("\t}\r\n");
				writer.write("}\r\n");
			}
		}		
		writer.close();
		
		//Make an empty continent.txt file to prevent CTDs
		File continentTxt = new File(OutputFile.Continent.getFileName());
		writer = new FileWriter(continentTxt);
		writer.close();

		//Make the island_region.txt file
		File islandRegion = new File(OutputFile.IslandRegions.getFileName());
		Logger.log("Writing "+islandRegion.getPath(),5);
		writer = new FileWriter(islandRegion);
		List<List<Province>> islandRegions = loader.getIslandRegions();
		islandRegions.remove(0); //Ignore the continent
		for (List<Province> region : islandRegions)
		{
			if (region.size() > 0)
			{
				writer.write("region_"+region.get(0).getDeJureEmpireCapital().getTitleEmpireName()+" = { \r\n");
				writer.write("\tprovinces = { ");
				for (Province p : region)
				{
					writer.write(p.getIndex()+" ");
				}
				writer.write("}\r\n");
				writer.write("}");
			}
		}
		writer.close();	
		
		//Make an empty statics file to prevent CTDs
		//Setup output dirs if they don't exist
		Utils.mkDir("./output/map/statics");
		File statics = new File(OutputFile.Statics.getFileName());
		writer = new FileWriter(statics);
		writer.close();		
		
		
		if (makeOldProvinceSetup)
		{
			//Setup output dirs if they don't exist
			File outputDirProvinceSetup = Utils.mkDir("./output/common/province_setup");
			
			//Make the province_setup file //Useless: Removed since Holy Fury patch
			File provinceSetup = new File(outputDirProvinceSetup + "/province_setup.txt");
			Logger.log("Writing "+provinceSetup.getPath());
			writer = new FileWriter(provinceSetup);
			
			for (Province p : loader.provinceList)
			{
				writer.write(p.getIndex() + " = {\r\n");
				if (!p.isWasteland())
					writer.write("\ttitle="+p.getTitleCountyName()+"\r\n");
				else
					writer.write("\t# "+p.getProvinceName()+"\r\n");
				//writer.write("\tmax_settlements="+p.numSlots+"\r\n"); //Useless, history overwrites
				//override terrain ?
				writer.write("}\r\n");
			}
			for (Province p : loader.waterProvinceList)
			{
				writer.write(p.getIndex() + " = {\r\n");
				writer.write("\t# "+p.getProvinceName()+"\r\n");
				//writer.write("\tmax_settlements=7\r\n");
				writer.write("\tterrain=coastal_desert\r\n");
				writer.write("}\r\n");
			}
			writer.close();
		}
		
		//Setup output dirs if they don't exist
		Utils.mkDir("./output/common/landed_titles");
		
		//Make the landed_titles files
		File landedTitles = new File(OutputFile.LandedTitles.getFileName());
		File landedTitlesPatricians = new File(OutputFile.LandedTitlesPat.getFileName());
		Logger.log("Writing "+landedTitles.getPath(),5);
		writer = new FileWriter(landedTitles);
		FileWriter patricianWriter = new FileWriter(landedTitlesPatricians);
		
		for (Province empire : loader.provinceList)
		{
			if (empire.isWasteland())
				continue;
			
			if (empire.getDeJureEmpireCapital() == empire)
			{
				writer.write(empire.getTitleEmpireName()+" = {\r\n");
				writer.write("\tcolor={ "+empire.getMapColorR()+" "+empire.getMapColorG()+" "+empire.getMapColorB()+" }\r\n");
				writer.write("\tcolor2={ 255 255 255 }\r\n");
				writer.write("\t\r\n");
				writer.write("\tcapital = "+empire.getIndex()+"\r\n");
				writer.write("\t\r\n");
				writer.write("\tculture = "+empire.getCulture()+"\r\n");
				writer.write("\t\r\n");
				
				for (Province kingdom : empire.getDeJureEmpireVassalsPlusSelf())
				{
					if (kingdom.isWasteland())
						continue;
					
					if (kingdom.getDeJureKingdomCapital() == kingdom)
					{
						writer.write("\t"+kingdom.getTitleKingdomName()+" = {\r\n");
						writer.write("\t\tcolor={ "+kingdom.getMapColorR()+" "+kingdom.getMapColorG()+" "+kingdom.getMapColorB()+" }\r\n");
						writer.write("\t\tcolor2={ 255 255 255 }\r\n");
						writer.write("\t\t\r\n");
						writer.write("\t\tcapital = "+kingdom.getIndex()+"\r\n");
						writer.write("\t\t\r\n");
						writer.write("\t\tculture = "+kingdom.getCulture()+"\r\n");
						writer.write("\t\t\r\n");
						
						for (Province duchy : kingdom.getDeJureKingdomVassalsPlusSelf())
						{
							if (duchy.isWasteland())
								continue;
							
							if (duchy.getDeJureDuchyCapital() == duchy)
							{
								writer.write("\t\t"+duchy.getTitleDuchyName()+" = {\r\n");
								writer.write("\t\t\tcolor={ "+duchy.getMapColorR()+" "+duchy.getMapColorG()+" "+duchy.getMapColorB()+" }\r\n");
								writer.write("\t\t\tcolor2={ 255 255 255 }\r\n");
								writer.write("\t\t\t\r\n");
								writer.write("\t\t\tcapital = "+duchy.getIndex()+"\r\n");
								writer.write("\t\t\t\r\n");
								
								if (duchy.getGovernment() == Government.MERCHANT_REPUBLIC)
								{
									writer.write("\t\t\tdynasty_title_names = no\r\n");
									writer.write("\t\t\t\r\n");
									
									patricianWriter.write("# "+duchy.getDuchyName()+" Merchant Republic Palaces\r\n");
									for (int patrician=0; patrician<5; patrician++)
									{
										patricianWriter.write("b_patrician_"+(100000+duchy.getIndex()*10+patrician)+" = {\r\n");
										patricianWriter.write("\tculture = "+duchy.getCulture()+"\r\n");
										patricianWriter.write("\treligion = "+duchy.getReligion()+"\r\n");
										patricianWriter.write("}\r\n");
									}
								}
								
								for (Province county : duchy.getDeJureDuchyVassalsPlusSelf())
								{
									if (county.isWasteland())
										continue;
									
									writer.write("\t\t\t"+county.getTitleCountyName()+" = {\r\n");
									writer.write("\t\t\t\tcolor={ "+county.getMapColorR()+" "+county.getMapColorG()+" "+county.getMapColorB()+" }\r\n");
									writer.write("\t\t\t\tcolor2={ 255 255 255 }\r\n");
									writer.write("\t\t\t\t\r\n");
									if (loader.religionMap != null)
										for (String religion : loader.religionMap.values())
											if (county.isHolysite(religion))
												writer.write("\t\t\t\tholy_site = "+religion+"\r\n");
									writer.write("\t\t\t\t\r\n");
									for (int barony = 0; barony < 8; barony ++)
									{
										//Do not name dynasties after capital baronies of holy sites or merchant republics
										if (barony == 0 && (county.isHolysite(null) || 
												county.getGovernment() == Government.MERCHANT_REPUBLIC))
										{
											writer.write("\t\t\t\t"+county.getTitleBaronyName(barony)+" = {\r\n");
											writer.write("\t\t\t\tdynasty_title_names = no\r\n");
											writer.write("\t\t\t\t}\r\n");
										}
										else
											writer.write("\t\t\t\t"+county.getTitleBaronyName(barony)+" = { }\r\n");
									}
									
									writer.write("\t\t\t}\r\n");
								}
								
								writer.write("\t\t}\r\n");
							}
						}
						
						writer.write("\t}\r\n");
					}
				}
				
				writer.write("}\r\n");
			}
		}
		
		writer.close();
		patricianWriter.close();
		

		//Make a mercenary band for each dejure kingdom
		landedTitles = new File(OutputFile.LandedTitlesMerc.getFileName());
		Logger.log("Writing "+landedTitles.getPath(),1);
		writer = new FileWriter(landedTitles);
		
		for (Province kingdom : loader.provinceList)
		{
			if (kingdom.isWasteland())
				continue;
			
			if (kingdom.getDeJureKingdomCapital() == kingdom)
			{
				writer.write(kingdom.getTitleKingdomName().replace("k_","d_")+"_band = {\r\n");
				writer.write("\tcolor={ "+kingdom.getMapColorR()+" "+kingdom.getMapColorG()+" "+kingdom.getMapColorB()+" }\r\n");
				writer.write("\tcolor2={ 255 255 255 }\r\n");
				writer.write("\t\r\n");
				writer.write("\tcapital = "+kingdom.getIndex()+"\r\n");
				writer.write("\t\r\n");
				writer.write("\tculture = "+kingdom.getCulture()+"\r\n");
				writer.write("\t\r\n");
				writer.write("\tmercenary = yes\r\n");
				writer.write("\ttitle = \"CAPTAIN\"\r\n");
				writer.write("\tfoa = \"CAPTAIN_FOA\"\r\n");
				writer.write("\t\r\n");
				writer.write("\t# Always exists\r\n");
				writer.write("\tlandless = yes\r\n");
				writer.write("\t\r\n");
				writer.write("\t# Cannot be held as a secondary title\r\n");
				writer.write("\tprimary = yes\r\n");
				writer.write("\t\r\n");
				writer.write("\t# Cannot be vassalized\r\n");
				writer.write("\tindependent = yes\r\n");
				writer.write("\t\r\n");
				writer.write("\tstrength_growth_per_century = 1.0\r\n");
				writer.write("\tmercenary_type = "+kingdom.getCulture()+"_band_composition\r\n");
				writer.write("}\r\n");
				//writer.write("\r\n");
			}
		}
		
		writer.close();
		
		//Setup output dirs if they don't exist
		File outputDirHistoryProvinces = Utils.mkDir(OutputFile.HistoryProvincesFolder.getFileName());
		File outputDirHistoryTitles = Utils.mkDir(OutputFile.HistoryTitlesFolder.getFileName());
		Utils.mkDir("./output/history/characters");
		Utils.mkDir("./output/common/dynasties");
		
		Logger.log("Clearing output/history folders for provinces and titles...",1);
		for (File f : outputDirHistoryProvinces.listFiles())
		{
			f.delete();
		}
		for (File f : outputDirHistoryTitles.listFiles())
		{
			f.delete();
		}
		
		File characterHistory = new File(OutputFile.HistoryCharacters.getFileName());
		Logger.log("Writing "+characterHistory.getPath(),5);
		FileWriter characterWriter = new FileWriter(characterHistory);
		File dynasties = new File(OutputFile.Dynasties.getFileName());
		Logger.log("Writing "+dynasties.getPath(),5);
		FileWriter dynastyWriter = new FileWriter(dynasties);
		
		//Make title history (counties only)
		for (Province province : loader.provinceList)
		{
			if (province.isWasteland())
				continue;

			File provinceHistory = new File(outputDirHistoryProvinces + "/" + province.getIndex() + " - "+province.getProvinceName()+".txt");
			Logger.log("Writing "+provinceHistory.getPath(),0);
			writer = new FileWriter(provinceHistory);
			
			writer.write("# "+province.getIndex() + " - "+province.getProvinceName()+"\r\n");
			writer.write("\r\n");
			writer.write("# County Title\r\n");
			writer.write("title = "+province.getTitleCountyName()+"\r\n");
			writer.write("\r\n");
			writer.write("# Settlements\r\n");
			writer.write("max_settlements = "+province.getNumSlots()+"\r\n");
			switch(province.getGovernment())
			{
			default:
			case FEUDAL:
				writer.write(province.getTitleBaronyName(0)+" = castle\r\n");
				if (province.getNumSlots() > 2 || province.getDeJureKingdomCapital() == province || province.isHolysite(null))
					writer.write(province.getTitleBaronyName(1)+" = temple\r\n");
				if (province.getNumSlots() > 3)
					writer.write(province.getTitleBaronyName(2)+" = city\r\n");
				if (province.getNumSlots() > 4)
					writer.write(province.getTitleBaronyName(3)+" = castle\r\n");
				if (province.getNumSlots() > 6)
					writer.write(province.getTitleBaronyName(4)+" = temple\r\n");
				break;
			case REPUBLIC:
			case MERCHANT_REPUBLIC:
				writer.write(province.getTitleBaronyName(0)+" = city\r\n");
				if (province.getNumSlots() > 2 || province.getDeJureKingdomCapital() == province || province.isHolysite(null))
					writer.write(province.getTitleBaronyName(1)+" = temple\r\n");
				if (province.getNumSlots() > 3)
					writer.write(province.getTitleBaronyName(2)+" = castle\r\n");
				if (province.getNumSlots() > 4)
					writer.write(province.getTitleBaronyName(3)+" = city\r\n");
				if (province.getNumSlots() > 6)
					writer.write(province.getTitleBaronyName(4)+" = temple\r\n");
				break;
			case THEOCRACY:
				writer.write(province.getTitleBaronyName(0)+" = temple\r\n");
				if (province.getNumSlots() > 2 || province.getDeJureKingdomCapital() == province)
					writer.write(province.getTitleBaronyName(1)+" = castle\r\n");
				if (province.getNumSlots() > 3)
					writer.write(province.getTitleBaronyName(2)+" = city\r\n");
				if (province.getNumSlots() > 4)
					writer.write(province.getTitleBaronyName(3)+" = temple\r\n");
				if (province.getNumSlots() > 6)
					writer.write(province.getTitleBaronyName(4)+" = castle\r\n");
				break;
			case TRIBAL:
			case NOMAD:
				writer.write(province.getTitleBaronyName(0)+" = tribal\r\n");
				//At least 5 slots or a dejure kingdom / empire capital, add a temple
				if (province.getNumSlots() > 4 ||
						province.getDeJureKingdomCapital() == province || province.isHolysite(null))
					writer.write(province.getTitleBaronyName(1)+" = temple\r\n");
				break;
			case THEOCRACY_TRIBAL: //For theocracies in tribal lands (typically, pagans), temple is the capital but no castles or cities
				writer.write(province.getTitleBaronyName(0)+" = temple\r\n");
				if (province.getNumSlots() > 4 || province.getDeJureKingdomCapital() == province )
					writer.write(province.getTitleBaronyName(1)+" = tribal\r\n");
				break;
			}
			

			writer.write("\r\n");
			writer.write("# Misc\r\n");
			writer.write("culture = "+province.getCulture()+"\r\n");
			writer.write("religion = "+province.getReligion()+"\r\n");
			//writer.write("terrain = "++"\r\n");
			writer.write("\r\n");
			
			//History:
			writer.write("# History\r\n");
				
			//Merchant republics start with a trade post in their capital, it is owned by the ruling patrician
			if (province.getGovernment() == Government.MERCHANT_REPUBLIC)
			{
				writer.write(Config.START_DATE+".1.1 = {\r\n");
				writer.write("\ttrade_post = b_patrician_"+(100000+province.getIndex()*10)+"\r\n");
				writer.write("}\r\n");
			}
			
			writer.close();
			
			File titleHistory = new File(outputDirHistoryTitles + "/" + province.getTitleCountyName() + ".txt");
			Logger.log("Writing "+titleHistory.getPath(),0);
			writer = new FileWriter(titleHistory);
			
			//Nomad province
			if (province.getGovernment() == Government.NOMAD)
			{
				writer.write("20.1.1 = {\r\n");
				writer.write("\thistorical_nomad = yes\r\n");
				writer.write("}\r\n");
			}

			//Create a random character to hold this title at Config.START_DATE
			int index = 100000+province.getIndex()*10;
			Culture culture = province.getCulture();
			String religion = province.getReligion();
			int birth = (int) (Config.START_DATE - 16 - Math.random() * 24); //16-40 yo at game start
			int death = birth + (int) (50 + Math.random() * 25); //dies at the earliest 10 years after start
			
			Character ruler;
			String dynastyName = null;
			Character father = null;
			Character mother = null;
			Character grandfather = null;
			Character greatgrandfather = null;
			Character uncle = null;
			Character cousin1 = null;
			Character cousin2 = null;
			Character sibling1 = null;
			Character sibling2 = null;
			
			//Feudal, tribal or merchant republic or nomadic
			if (province.getGovernment() == Government.FEUDAL || province.getGovernment() == Government.TRIBAL || 
					province.getGovernment() == Government.MERCHANT_REPUBLIC || province.getGovernment() == Government.NOMAD)
			{
				
				//Create a dynasty
				int dynasty = 100000+province.getIndex()*10;

				ruler = new Character(index+5,
						"",
						dynasty,
						province,
						culture,
						religion,
						birth, 
						death,  
						(Math.random() <= ((double)Config.PERCENT_FEMALE * 0.01))
						);
				
				//Can't make a female ruler for a republic/nomad, don't make one that is too old either because they will be childless
				if (province.getGovernment() == Government.MERCHANT_REPUBLIC || province.getGovernment() == Government.NOMAD || birth < Config.START_DATE - 29)
					ruler.setFemale(false);
				
				//Create a small family tree for that character
				Map<String, Character> dynastyMap = makeDynasty(ruler);
				father 				= dynastyMap.get("father");
				mother 				= dynastyMap.get("mother");
				grandfather 		= dynastyMap.get("grandfather");
				greatgrandfather 	= dynastyMap.get("greatgrandfather");
				uncle 				= dynastyMap.get("uncle");
				cousin1 			= dynastyMap.get("cousin1");
				cousin2 			= dynastyMap.get("cousin2");
				sibling1 			= dynastyMap.get("sibling1");
				sibling2 			= dynastyMap.get("sibling2");
				
				if (province.getGovernment() == Government.MERCHANT_REPUBLIC)
				{
					cousin1.setFemale(false);
					cousin2.setFemale(false);
				}
				
				//Now write characters history
				if (greatgrandfather != null)
					writeCharacter(greatgrandfather, characterWriter);
				if (grandfather != null)
					writeCharacter(grandfather, characterWriter);
				if (father != null)
					writeCharacter(father, characterWriter);
				if (uncle != null)
					writeCharacter(uncle, characterWriter);
				if (mother != null)
					writeCharacter(mother, characterWriter);
				
				writeCharacter(ruler, characterWriter);
				
				if (sibling1 != null)
					writeCharacter(sibling1, characterWriter);
				if (sibling2 != null)
					writeCharacter(sibling2, characterWriter);
				if (cousin1 != null)
					writeCharacter(cousin1, characterWriter);
				if (cousin2 != null)
					writeCharacter(cousin2, characterWriter);
				
				
				//Write dynasty info
				if (culture.isFounderNamedDynasties())
				{
					dynastyName = generateDynastyName(greatgrandfather);
				}
				else if (province.getGovernment() == Government.MERCHANT_REPUBLIC)
				{
					dynastyName = culture.getFromDynastyPrefix() + province.getBaronyName(1);
				}
				else
					dynastyName = culture.getFromDynastyPrefix() + province.getProvinceName();
						
				dynastyWriter.write(dynasty+"={\r\n");
				//dynastyWriter.write("\tused_for_random=no"); //useful for ruler designer, allow it
				dynastyWriter.write("\tname=\""+dynastyName+"\"\r\n");
				dynastyWriter.write("\tculture=\""+culture+"\"\r\n");
				dynastyWriter.write("\treligion=\""+religion+"\"\r\n");
				dynastyWriter.write("}\r\n");
				
				//Now write title history
				
				if (greatgrandfather != null && greatgrandfather.getBirthYear() > 0)
				{
					writer.write(greatgrandfather.getBirthYear() + ".1.1 = {\r\n");
					writer.write("\tholder = "+greatgrandfather.getCharId()+"\r\n");
					writer.write("}\r\n");
				}
				if (grandfather != null && grandfather.getBirthYear() > 0)
				{
					writer.write(greatgrandfather.getDeathYear() + ".1.1 = {\r\n");
					writer.write("\tholder = "+grandfather.getCharId()+"\r\n");
					writer.write("}\r\n");
				}
				if (father != null && father.getBirthYear() > 0)
				{
					writer.write(grandfather.getDeathYear() + ".1.1 = {\r\n");
					writer.write("\tholder = "+father.getCharId()+"\r\n");
					writer.write("}\r\n");
				}
				writer.write(Math.max(1, father.getDeathYear()) + ".1.1 = {\r\n");
				writer.write("\tholder = "+ruler.getCharId()+"\r\n");
				writer.write("}\r\n");	
			}
			else //Republic or theocracy. No dynastic succession, don't bother making a list of previous holders
			{
				ruler = new Character(index+5,
						"",
						0, //lowborn
						province,
						culture,
						religion,
						birth, 
						death,  
						false //Always male
						);
				
				writeCharacter(ruler, characterWriter);
				
				writer.write(Math.max(1, Config.START_DATE) + ".1.1 = {\r\n");
				writer.write("\tholder = "+ruler.getCharId()+"\r\n");
				writer.write("}\r\n");
			}
			
			//Write vassal history
			if (province.getDeFactoDuchyCapital() != null)
			{
				writer.write(Config.START_DATE + ".1.1 = {\r\n");
				writer.write("\tliege="+province.getDeFactoDuchyCapital().getTitleDuchyName()+"\r\n");
				writer.write("}\r\n");
			}
			else if (province.getDeFactoKingdomCapital() != null)
			{
				writer.write(Config.START_DATE + ".1.1 = {\r\n");
				writer.write("\tliege="+province.getDeFactoKingdomCapital().getTitleKingdomName()+"\r\n");
				writer.write("}\r\n");
			}
			else if (province.getDeFactoEmpireCapital() != null)
			{
				writer.write(Config.START_DATE + ".1.1 = {\r\n");
				writer.write("\tliege="+province.getDeFactoEmpireCapital().getTitleEmpireName()+"\r\n");
				writer.write("}\r\n");
			}
			writer.close();
			
			//For merchant republics
			if (province.getGovernment() == Government.MERCHANT_REPUBLIC)
			{
				//Create a family palace for the ruling dynasty
				titleHistory = new File(outputDirHistoryTitles + "/b_patrician_" + ruler.getDynasty() + ".txt");
				writer = new FileWriter(titleHistory);
				
				//Now write title history
				if (greatgrandfather != null && greatgrandfather.getBirthYear() > 0)
				{
					writer.write(greatgrandfather.getBirthYear() + ".1.1 = {\r\n");
					writer.write("\tholding_dynasty = "+ruler.getDynasty()+" # "+dynastyName+" - makes this a FAMILY_PALACE type Holding\r\n");
					writer.write("\tliege=\""+province.getDeFactoDuchyCapital().getTitleDuchyName()+"\"\r\n");
					writer.write("\tholder = "+greatgrandfather.getCharId()+"\r\n");
					writer.write("}\r\n");
				}
				if (grandfather != null && grandfather.getBirthYear() > 0)
				{
					writer.write(greatgrandfather.getDeathYear() + ".1.1 = {\r\n");
					writer.write("\tholder = "+grandfather.getCharId()+"\r\n");
					writer.write("}\r\n");
				}
				if (father != null && father.getBirthYear() > 0)
				{
					writer.write(grandfather.getDeathYear() + ".1.1 = {\r\n");
					writer.write("\tholder = "+father.getCharId()+"\r\n");
					writer.write("}\r\n");
				}
				writer.write(Math.max(1, father.getDeathYear()) + ".1.1 = {\r\n");
				writer.write("\tholder = "+ruler.getCharId()+"\r\n");
				writer.write("}\r\n");	
				
				writer.close();
				
				//Create 4 vassal patrician dynasties
				for (int patrician = 0; patrician < 4; patrician++)
				{
					//Create a tiny dynasty : dead father, 2 brothers and a sister
					String patriciandynastyName = null;
					int patriciandynasty = 100000+province.getIndex()*10+patrician+1;
					int patricianindex = 200000+province.getIndex()*20+patrician*5;
					int patricianbirth = (int) (Config.START_DATE - 16 - Math.random() * 24); //16-40 yo at game start
					int patriciandeath = patricianbirth + (int) (50 + Math.random() * 25); //dies at the earliest 10 years after start

					Character patricianruler = new Character(patricianindex,
							"",
							patriciandynasty,
							province,
							culture,
							religion,
							patricianbirth, 
							patriciandeath,  
							false
							);
					
					int patricianfatherBirth = (int) (patricianbirth - 20 - Math.random() * 15); //20-35 y older than ruler
					int patricianfatherDeath = Math.min(Config.START_DATE - 1, 
							(int) (Config.START_DATE - Math.random() * (Config.START_DATE - patricianbirth) * 0.5)) ; //dies at the latest 1 year before start
					Character patricianfather = new Character(patricianindex+1,
							"",
							patriciandynasty,
							province,
							culture,
							religion,
							patricianfatherBirth, 
							patricianfatherDeath,  
							false
							);
					
					patricianruler.setFather(patricianfather);
					
					//Brother and heir
					Character patriciansibling1 = new Character(patricianindex+2,
							"",
							patriciandynasty,
							province,
							culture,
							religion,
							(int) (patricianbirth+1+Math.random()*2), 
							patriciandeath,  
							false
							);
					
					patriciansibling1.setFather(patricianfather);
					
					Character patriciansibling2 = new Character(patricianindex+3,
							"",
							patriciandynasty,
							province,
							culture,
							religion,
							(int) (patricianbirth+4+Math.random()*2), 
							patriciandeath,  
							Math.random() < 0.5
							);
					
					patriciansibling2.setFather(patricianfather);
					
					Character patriciansibling3 = new Character(patricianindex+4,
							"",
							patriciandynasty,
							province,
							culture,
							religion,
							(int) (patricianbirth+7+Math.random()*2), 
							patriciandeath,  
							!patriciansibling2.isFemale()
							);
					
					patriciansibling3.setFather(patricianfather);
					
					writeCharacter(patricianruler, characterWriter);

					if (patricianfather != null)
						writeCharacter(patricianfather, characterWriter);
					if (patriciansibling1 != null)
						writeCharacter(patriciansibling1, characterWriter);
					if (patriciansibling2 != null)
						writeCharacter(patriciansibling2, characterWriter);
					if (patriciansibling3 != null)
						writeCharacter(patriciansibling3, characterWriter);
					
					//Write dynasty info
					if (culture.isFounderNamedDynasties())
					{
						patriciandynastyName = generateDynastyName(patricianfather);
					}
					else
					{
						patriciandynastyName = culture.getFromDynastyPrefix() + province.getBaronyName(1+patrician);
					}
					
					dynastyWriter.write(patriciandynasty+"={\r\n");
					dynastyWriter.write("\tname=\""+patriciandynastyName+"\"\r\n");
					dynastyWriter.write("\tculture=\""+culture+"\"\r\n");
					dynastyWriter.write("\treligion=\""+religion+"\"\r\n");
					dynastyWriter.write("}\r\n");
					
					//Create a family palace for the ruling dynasty
					titleHistory = new File(outputDirHistoryTitles + "/b_patrician_" + patriciandynasty + ".txt");
						
					writer = new FileWriter(titleHistory);
					
					//Now write title history
					writer.write(Math.max(1, patricianfather.getDeathYear()) + ".1.1 = {\r\n");
					writer.write("\tholding_dynasty = "+patriciandynasty+" # "+patriciandynastyName+" - makes this a FAMILY_PALACE type Holding\r\n");
					writer.write("\tliege=\""+province.getDeFactoDuchyCapital().getTitleDuchyName()+"\"\r\n");
					writer.write("\tholder = "+patricianruler.getCharId()+"\r\n");
					writer.write("}\r\n");	
					
					writer.close();
				}
			}
			
			//Now write duchy titles history
			if (province.getDeFactoDuchyCapital() == province)
			{
				titleHistory = new File(outputDirHistoryTitles + "/" + province.getTitleDuchyName() + ".txt");
				Logger.log("Writing "+titleHistory.getPath(),0);
				writer = new FileWriter(titleHistory);
				
				//Nomad province
				if (province.getGovernment() == Government.NOMAD)
				{
					writer.write("20.1.1 = {\r\n");
					writer.write("\thistorical_nomad = yes\r\n");
					writer.write("}\r\n");
				}
				
				if (greatgrandfather != null)
				{
					writer.write(greatgrandfather.getBirthYear() + ".1.1 = {\r\n");
					writer.write("\tholder = "+greatgrandfather.getCharId()+"\r\n");
					writer.write("}\r\n");
				}
				if (grandfather != null)
				{
					writer.write((greatgrandfather==null ? grandfather.getBirthYear() : greatgrandfather.getDeathYear()) + ".1.1 = {\r\n");
					writer.write("\tholder = "+grandfather.getCharId()+"\r\n");
					writer.write("}\r\n");
				}
				if (father != null)
				{
					writer.write((grandfather==null ? father.getBirthYear() : grandfather.getDeathYear()) + ".1.1 = {\r\n");
					writer.write("\tholder = "+father.getCharId()+"\r\n");
					writer.write("}\r\n");
				}
				writer.write((father==null ? Config.START_DATE : father.getDeathYear()) + ".1.1 = {\r\n");
				writer.write("\tholder = "+ruler.getCharId()+"\r\n");
				writer.write("}\r\n");
				
				if (province.getDeFactoKingdomCapital() != null)
				{
					writer.write(Config.START_DATE + ".1.1 = {\r\n");
					writer.write("\tliege="+province.getDeFactoKingdomCapital().getTitleKingdomName()+"\r\n");
					writer.write("}\r\n");
				}
				else if (province.getDeFactoEmpireCapital() != null)
				{
					writer.write(Config.START_DATE + ".1.1 = {\r\n");
					writer.write("\tliege="+province.getDeFactoEmpireCapital().getTitleEmpireName()+"\r\n");
					writer.write("}\r\n");
				}
				
				writer.close();
			}
			
			//Now write kingdom titles history
			if (province.getDeFactoKingdomCapital() == province)
			{
				titleHistory = new File(outputDirHistoryTitles + "/" + province.getTitleKingdomName() + ".txt");
				Logger.log("Writing "+titleHistory.getPath(),0);
				writer = new FileWriter(titleHistory);
				
				//Nomad province
				if (province.getGovernment() == Government.NOMAD)
				{
					writer.write("20.1.1 = {\r\n");
					writer.write("\thistorical_nomad = yes\r\n");
					writer.write("}\r\n");
				}
				
				if (greatgrandfather != null)
				{
					writer.write(greatgrandfather.getBirthYear() + ".1.1 = {\r\n");
					writer.write("\tholder = "+greatgrandfather.getCharId()+"\r\n");
					writer.write("}\r\n");
				}
				if (grandfather != null)
				{
					writer.write((greatgrandfather==null ? grandfather.getBirthYear() : greatgrandfather.getDeathYear()) + ".1.1 = {\r\n");
					writer.write("\tholder = "+grandfather.getCharId()+"\r\n");
					writer.write("}\r\n");
				}
				if (father != null)
				{
					writer.write((grandfather==null ? father.getBirthYear() : grandfather.getDeathYear()) + ".1.1 = {\r\n");
					writer.write("\tholder = "+father.getCharId()+"\r\n");
					writer.write("}\r\n");
				}
				writer.write((father==null ? Config.START_DATE : father.getDeathYear()) + ".1.1 = {\r\n");
				writer.write("\tholder = "+ruler.getCharId()+"\r\n");
				writer.write("}\r\n");
				
				if (province.getDeFactoEmpireCapital() != null)
				{
					writer.write(Config.START_DATE + ".1.1 = {\r\n");
					writer.write("\tliege="+province.getDeFactoEmpireCapital().getTitleEmpireName()+"\r\n");
					writer.write("}\r\n");
				}
				
				writer.close();
			}
			

			//Now write empire titles history
			if (province.getDeFactoEmpireCapital() == province)
			{
				titleHistory = new File(outputDirHistoryTitles + "/" + province.getTitleEmpireName() + ".txt");
				Logger.log("Writing "+titleHistory.getPath(),0);
				writer = new FileWriter(titleHistory);
				
				//Nomad province
				if (province.getGovernment() == Government.NOMAD)
				{
					writer.write("20.1.1 = {\r\n");
					writer.write("\thistorical_nomad = yes\r\n");
					writer.write("}\r\n");
				}
				
				if (greatgrandfather != null)
				{
					writer.write(greatgrandfather.getBirthYear() + ".1.1 = {\r\n");
					writer.write("\tholder = "+greatgrandfather.getCharId()+"\r\n");
					writer.write("}\r\n");
				}
				if (grandfather != null)
				{
					writer.write((greatgrandfather==null ? grandfather.getBirthYear() : greatgrandfather.getDeathYear()) + ".1.1 = {\r\n");
					writer.write("\tholder = "+grandfather.getCharId()+"\r\n");
					writer.write("}\r\n");
				}
				if (father != null)
				{
					writer.write((grandfather==null ? father.getBirthYear() : grandfather.getDeathYear()) + ".1.1 = {\r\n");
					writer.write("\tholder = "+father.getCharId()+"\r\n");
					writer.write("}\r\n");
				}
				writer.write((father==null ? Config.START_DATE : father.getDeathYear()) + ".1.1 = {\r\n");
				writer.write("\tholder = "+ruler.getCharId()+"\r\n");
				writer.write("}\r\n");
				
				writer.close();
			}
			
		}
		
		characterWriter.close();
		dynastyWriter.close();

		
		
		if (makeLocalisation)
		{
			//Make Localisation Strings
			//Setup output dirs if they don't exist
			Utils.mkDir("./output/localisation");
			File provinceLocalisation = new File(OutputFile.Localisation.getFileName());
			Logger.log("Writing "+provinceLocalisation.getPath(),5);
			writer = new FileWriter(provinceLocalisation);
			
			for (Province p : loader.provinceList)
			{
				writer.write("PROV"+p.getIndex()+";"+p.getProvinceName()+";"+p.getProvinceName()+";"+p.getProvinceName()+";;"+p.getProvinceName()+";;;;;;;;;x\r\n");
			}
			for (Province p : loader.waterProvinceList)
			{
				writer.write("PROV"+p.getIndex()+";"+p.getProvinceName()+";"+p.getProvinceName()+";"+p.getProvinceName()+";;"+p.getProvinceName()+";;;;;;;;;x\r\n");		
			}
			for (Province p : loader.provinceList)
			{
				if (!p.isWasteland())
				{
					if (p.getDeJureEmpireCapital() == p)
					{
						writer.write(p.getTitleEmpireName()+";"+p.getEmpireName()+";"+p.getEmpireName()+";"+p.getEmpireName()+";;"+p.getEmpireName()+";;;;;;;;;x\r\n");
						writer.write(p.getTitleEmpireName()+"_adj;"+p.getEmpireName()+";"+p.getEmpireName()+";"+p.getEmpireName()+";;"+p.getEmpireName()+";;;;;;;;;x\r\n");
					}
					if (p.getDeJureKingdomCapital() == p)
					{
						writer.write(p.getTitleKingdomName()+";"+p.getKingdomName()+";"+p.getKingdomName()+";"+p.getKingdomName()+";;"+p.getKingdomName()+";;;;;;;;;x\r\n");
						writer.write(p.getTitleKingdomName()+"_adj;"+p.getKingdomName()+";"+p.getKingdomName()+";"+p.getKingdomName()+";;"+p.getKingdomName()+";;;;;;;;;x\r\n");
					}
					if (p.getDeJureDuchyCapital() == p)
					{
						writer.write(p.getTitleDuchyName()+";"+p.getDuchyName()+";"+p.getDuchyName()+";"+p.getDuchyName()+";;"+p.getDuchyName()+";;;;;;;;;x\r\n");
						writer.write(p.getTitleDuchyName()+"_adj;"+p.getDuchyName()+";"+p.getDuchyName()+";"+p.getDuchyName()+";;"+p.getDuchyName()+";;;;;;;;;x\r\n");
					}
					writer.write(p.getTitleCountyName()+"_adj;"+p.getProvinceName()+";"+p.getProvinceName()+";"+p.getProvinceName()+";;"+p.getProvinceName()+";;;;;;;;;x\r\n");
					for (int barony=0; barony<8; barony++)
						writer.write(p.getTitleBaronyName(barony)+";"+p.getBaronyName(barony)+";"+p.getBaronyName(barony)+";"+p.getBaronyName(barony)+";;"+p.getBaronyName(barony)+";;;;;;;;;x\r\n");
				}
			}
			
			writer.close();
		}
	
		if (makeTechnology)
		{
			//Make technology history
			//Setup output dirs if they don't exist
			Utils.mkDir("./output/history/technology");
			File technologyHistory = new File(OutputFile.HistoryTechnology.getFileName());
			Logger.log("Writing "+technologyHistory.getPath(),5);
			writer = new FileWriter(technologyHistory);
			
			for (Province empire : loader.provinceList)
			{
				if (empire.isWasteland())
					continue;
				
				//Group up by dejure empires, then similar tech level
				if (empire.getDeJureEmpireCapital() == empire)
				{				
					writer.write("# "+empire.getEmpireName()+"\r\n");
					
					HashSet<Integer> techSet = new HashSet<Integer>();
					
					//Find all the different tech states that exist in that empire and put them in a set
					for (Province duchy : loader.provinceList)
					{
						if (duchy.getDeJureEmpireCapital() == empire && duchy.getDeJureDuchyCapital() == duchy)
						{
							techSet.add(duchy.getTechnology());
						}
					}
					
					for (Integer techColor : techSet)
					{
						int military = Utils.getColorR(techColor);
						int economy = Utils.getColorG(techColor);
						int culture = Utils.getColorB(techColor);
						
						writer.write("technology = {\r\n");
						writer.write("\ttitles = {\r\n");
						
						for (Province duchy : loader.provinceList)
						{
							if (duchy.getTechnology() == techColor && duchy.getDeJureEmpireCapital() == empire && duchy.getDeJureDuchyCapital() == duchy)
							{
								writer.write("\t\t"+duchy.getTitleDuchyName()+"\r\n");
							}
						}
						
						writer.write("\t}\r\n");
						writer.write("\t"+Config.START_DATE+" = {\r\n");
						writer.write("\t\tmilitary = "+((float)military/30.0)+"\r\n");
						writer.write("\t\teconomy = "+((float)economy/30.0)+"\r\n");
						writer.write("\t\tculture = "+((float)culture/30.0)+"\r\n");
						writer.write("\t}\r\n");
						writer.write("}\r\n");
					}
					
					writer.write("\r\n");
					
				}
			}
	
			writer.close();
		}
		

		
		//Log Summary
		for (Province p : loader.provinceList)
		{
			if (p.isWasteland())
				continue;
			
			Logger.log(p.getIndex()+" E:"+p.getDeJureEmpireCapital().getIndex()+
					" K:"+p.getDeJureKingdomCapital().getIndex()+
					" D:"+p.getDeJureDuchyCapital().getIndex()+
					" Culture:"+p.getCulture()+
					" Religion:"+p.getReligion()+
					" X/Y:"+p.getX()+";"+p.getY()+
					" Slots: "+p.getNumSlots());
		}
		

		

		Logger.log("Done in "+(System.currentTimeMillis()-ms)+"ms",100);
		Logger.close();
		
		return returnCode;
	}

	






	//Writes param 1 Character data into a CK2 character history file using param 2 FileWriter 
	private static void writeCharacter(Character c, Writer characterWriter) throws IOException
	{
		//NOPE NOPE NOPE
		if (c.getBirthYear() < 1)
			return;
		
		characterWriter.write(c.getCharId()+" = {\r\n");
		characterWriter.write("\tname = \""+c.getName()+"\"\r\n");
		if (c.isFemale())
			characterWriter.write("\tfemale = yes\r\n");
		if (c.getDynasty() > 0)
			characterWriter.write("\tdynasty = "+c.getDynasty()+"\r\n");
		characterWriter.write("\tculture = \""+c.getCulture()+"\"\r\n");
		characterWriter.write("\treligion = \""+c.getReligion()+"\"\r\n");
		if (c.getFather() != null)
			characterWriter.write("\tfather = "+c.getFather().getCharId()+"\r\n");
		if (c.getMother() != null)
			characterWriter.write("\tmother = "+c.getMother().getCharId()+"\r\n");
		if (c.isBadass()) //Legendary character, founder of a dynasty
		{
			double rng = Math.random();
			
			if (rng < 0.4) //40% Epic warrior
			{
				characterWriter.write("\ttrait = brilliant_strategist\r\n");
				characterWriter.write("\tmartial = 10\r\n");
				if (Math.random() < 0.5)
					characterWriter.write("\ttrait = duelist\r\n");
				else
					characterWriter.write("\ttrait = hunter\r\n");
				characterWriter.write("\ttrait = scarred\r\n");
				if (Math.random() < 0.5)
					characterWriter.write("\ttrait = falconer\r\n");
				characterWriter.write("\ttrait = brave\r\n");
				characterWriter.write("\ttrait = strong\r\n");
			}
			else if (rng < 0.7) //30% Legendary wise ruler
			{
				characterWriter.write("\ttrait = midas_touched\r\n");
				characterWriter.write("\tstewardship = 10\r\n");
				if (Math.random() < 0.5)
					characterWriter.write("\ttrait = administrator\r\n");
				else
					characterWriter.write("\ttrait = architect\r\n");
				if (Math.random() < 0.5)
					characterWriter.write("\ttrait = genius\r\n");
				else
					characterWriter.write("\ttrait = quick\r\n");
				characterWriter.write("\ttrait = just\r\n");
			}
			else if (rng < 0.8) //10% Terrifying intriguer
			{
				characterWriter.write("\ttrait = elusive_shadow\r\n");
				characterWriter.write("\tintrigue = 10\r\n");
				if (Math.random() < 0.5)
					characterWriter.write("\ttrait = one_eyed\r\n");
				if (Math.random() < 0.5)
					characterWriter.write("\ttrait = genius\r\n");
				else
					characterWriter.write("\ttrait = quick\r\n");
				characterWriter.write("\ttrait = deceitful\r\n");
				if (Math.random() < 0.5)
					characterWriter.write("\ttrait = cruel\r\n");
				if (Math.random() < 0.5)
					characterWriter.write("\ttrait = impaler\r\n");
				else
					characterWriter.write("\ttrait = schemer\r\n");
			}
			else if (rng < 0.9) //10% Famous socialite
			{
				characterWriter.write("\ttrait = grey_eminence\r\n");
				characterWriter.write("\tdiplomacy = 10\r\n");
				if (Math.random() < 0.5)
					characterWriter.write("\ttrait = fair\r\n");
				else
					characterWriter.write("\ttrait = quick\r\n");
				characterWriter.write("\ttrait = gregarious\r\n");
				if (Math.random() < 0.5)
					characterWriter.write("\ttrait = poet\r\n");
				if (Math.random() < 0.5)
					characterWriter.write("\ttrait = socializer\r\n");
				else
					characterWriter.write("\ttrait = hedonist\r\n");
			}
			else //10% Mythic religious figure
			{
				characterWriter.write("\ttrait = mastermind_theologian\r\n");
				characterWriter.write("\tlearning = 10\r\n");
				if (Math.random() < 0.5)
					characterWriter.write("\ttrait = genius\r\n");
				else
					characterWriter.write("\ttrait = quick\r\n");
				if (Math.random() < 0.5)
					characterWriter.write("\ttrait = erudite\r\n");
				characterWriter.write("\ttrait = zealous\r\n");
				if (Math.random() < 0.5)
					characterWriter.write("\ttrait = humble\r\n");
				if (Math.random() < 0.5)
					characterWriter.write("\ttrait = mystic\r\n");
				else
					characterWriter.write("\ttrait = theologian\r\n");
			}
			
		}
		if (c.getLocation().getGovernment() == Government.THEOCRACY || c.getLocation().getGovernment() == Government.THEOCRACY_TRIBAL)
		{
			//Make sure theocracies are led by a priest
			characterWriter.write("\ttrait = scholarly_theologian\r\n");
		}
		characterWriter.write("\t"+c.getBirthYear()+".1.1={\r\n");
		characterWriter.write("\t\tbirth=\""+c.getBirthYear()+".1.1\"\r\n");
		if (c.getDynasty() < 0)
			characterWriter.write("\t\teffect={ dynasty = father_bastard }\r\n"); //Generate a random noble dynasty
		characterWriter.write("\t}\r\n");
		if (c.getSpouse() != null)
		{
			characterWriter.write("\t"+Math.max(c.getBirthYear()+16, c.getSpouse().getBirthYear()+16)+".1.1={\r\n");
			characterWriter.write("\t\tadd_spouse = "+c.getSpouse().getCharId()+"\r\n");
			characterWriter.write("\t}\r\n");
		}
		characterWriter.write("\t"+c.getDeathYear()+".1.1={\r\n");
		characterWriter.write("\t\tdeath=\""+c.getDeathYear()+".1.1\"\r\n");
		characterWriter.write("\t}\r\n");
		characterWriter.write("}\r\n");
	}


	//Creates a small dynasty of Characters related to the Character in parameter, and returns them in a Map
	private static Map<String, Character> makeDynasty(Character ruler)
	{
		int dynasty = ruler.getDynasty();
		Province province = ruler.getLocation();
		Culture culture = ruler.getCulture();
		String religion = ruler.getReligion();
		int birth = ruler.getBirthYear();
		int index = ruler.getCharId()-5;
		
		Map<String, Character> dynastyMap = new HashMap<String, Character>();
		
		Character father = null;
		Character mother = null;
		Character grandfather = null;
		Character greatgrandfather = null;
		Character uncle = null;
		Character cousin1 = null;
		Character cousin2 = null;
		Character sibling1 = null;
		Character sibling2 = null;
		
		//Father
		int fatherBirth = (int) (birth - 20 - Math.random() * 15); //20-35 y older than ruler
		int fatherDeath = Math.min(Config.START_DATE - 1, 
				(int) (Config.START_DATE - Math.random() * (Config.START_DATE - birth) * 0.5)) ; //dies at the latest 1 year before start
		father = new Character(index+2,
				"",
				dynasty,
				province,
				culture,
				religion,
				fatherBirth, 
				fatherDeath,  
				false
				);
		
		ruler.setFather(father);
		
		//Mother
		int motherBirth = (int) (birth - 17 - Math.random() * 13); //17-30 y older than ruler
		int motherDeath = (int) (motherBirth + 35 + Math.random() * 40); //Likely still around
		
		mother = new Character(index+4,
				"",
				-1, //Make up a random dynasty
				province,
				culture,
				religion,
				motherBirth, 
				motherDeath,  
				true
				);
		
		father.setSpouse(mother);
		ruler.setMother(mother);
		
		//Paternal grandfather
		int grandfatherBirth = (int) (fatherBirth - 20 - Math.random() * 20); //20-40 y older than father
		int grandfatherDeath = Math.min(fatherDeath - 1, 
				(int) (fatherBirth + 10 + Math.random() * 40)); //dies at the latest 1 year before his eldest son
		
		grandfather = new Character(index+1,
				"",
				dynasty,
				province,
				culture,
				religion,
				grandfatherBirth, 
				grandfatherDeath,  
				false
				);
		
		father.setFather(grandfather);
		
		//Legendary Great-grandfather
		int greatgrandfatherBirth = (int) (grandfatherBirth - 30 - Math.random() * 20); //30-50 y older than grandfather
		int greatgrandfatherDeath = Math.min(grandfatherDeath - 1, 
				(int) (grandfatherBirth + 20 + Math.random() * 30)); //dies at the latest 1 year before his eldest son
		
		greatgrandfather = new Character(index+0,
				"",
				dynasty,
				province,
				culture,
				religion,
				greatgrandfatherBirth, 
				greatgrandfatherDeath,  
				false
				);
		
		greatgrandfather.setBadass(true);
		
		grandfather.setFather(greatgrandfather);
		
		//Dead Uncle ?
		int uncleBirth = (int) (fatherBirth + 1 + Math.random() * 5);
		int uncleDeath = Math.min(fatherDeath - 1, (int) (uncleBirth + 30 + Math.random() * 20)); //dies at the latest 1 year before his brother
		if (uncleBirth < grandfatherDeath)
		{
			uncle = new Character(index+3,
					"",
					dynasty,
					province,
					culture,
					religion,
					uncleBirth, 
					uncleDeath,  
					false
					);
			
			uncle.setFather(grandfather);
			
			
			//Cousins
			int cousinBirth = (int) (uncleBirth + 20 + Math.random() * 10);
			int cousinDeath = (int) (cousinBirth + 50 + Math.random() * 25);
			
			if (cousinBirth < uncleDeath)
			{
				cousin1 = new Character(index+8,
						"",
						dynasty,
						province,
						culture,
						religion,
						cousinBirth, 
						cousinDeath,  
						ruler.isFemale() ? false : (Math.random() < 0.4) //If ruler is female, only have male cousins so we have someone at court to matri-marry
						);
				cousin1.setFather(uncle);
				
				cousinBirth = (int) (cousinBirth + 1 + Math.random() * 5);
				cousinDeath = (int) (cousinBirth + 50 + Math.random() * 25);
			
				if (cousinBirth < uncleDeath)
				{
					cousin2 = new Character(index+9,
									"",
									dynasty,
									province,
									culture,
									religion,
									cousinBirth, 
									cousinDeath,  
									ruler.isFemale() ? false : (Math.random() < 0.4)
									);
					cousin2.setFather(uncle);
				}
			}
		}
		
		//Brothers and sisters
		int siblingBirth = (int) (birth + 1 + Math.random() * 5);
		int siblingDeath = (int) (siblingBirth + 50 + Math.random() * 25);
		
		if (siblingBirth < fatherDeath && siblingBirth < motherDeath && siblingBirth < motherBirth + 45)
		{
			sibling1 = new Character(index+6,
					"",
					dynasty,
					province,
					culture,
					religion,
					siblingBirth, 
					siblingDeath,  
					ruler.isFemale() ? true : (Math.random() < 0.6) //If ruler is female, only have sisters
					);
			sibling1.setFather(father);
			sibling1.setMother(mother);
			
			siblingBirth = (int) (siblingBirth + 1 + Math.random() * 5);
			siblingDeath = (int) (siblingBirth + 50 + Math.random() * 25);
			
			if (siblingBirth < fatherDeath && siblingBirth < motherDeath && siblingBirth < motherBirth + 45)
			{
				sibling2 = new Character(index+7,
					"",
					dynasty,
					province,
					culture,
					religion,
					siblingBirth, 
					siblingDeath,  
					ruler.isFemale() ? true : (Math.random() < 0.6)
					);
				
	
				sibling2.setFather(father);
				sibling2.setMother(mother);
				
				//No cousins ? Add even more siblings
				if (cousin2 == null)
				{
					siblingBirth = (int) (siblingBirth + 1 + Math.random() * 5);
					siblingDeath = (int) (siblingBirth + 50 + Math.random() * 25);
					
					if (siblingBirth < fatherDeath && siblingBirth < motherDeath && siblingBirth < motherBirth + 45)
					{
						cousin2 = new Character(index+9,
								"",
								dynasty,
								province,
								culture,
								religion,
								siblingBirth, 
								siblingDeath,  
								ruler.isFemale() ? true : (Math.random() < 0.6)
								);
						
						cousin2.setFather(father);
						cousin2.setMother(mother);
						
						if (cousin1 == null)
						{
							siblingBirth = (int) (siblingBirth + 1 + Math.random() * 5);
							siblingDeath = (int) (siblingBirth + 50 + Math.random() * 25);
							
							if (siblingBirth < fatherDeath && siblingBirth < motherDeath && siblingBirth < motherBirth + 45)
							{
								cousin1 = new Character(index+8,
										"",
										dynasty,
										province,
										culture,
										religion,
										siblingBirth, 
										siblingDeath,  
										ruler.isFemale() ? true : (Math.random() < 0.6)
										);
								
								cousin1.setFather(father);
								cousin1.setMother(mother);
							}
							
						}
					}
				}
			}
		}
		
		dynastyMap.put("father", father);
		dynastyMap.put("mother", mother);
		dynastyMap.put("grandfather", grandfather);
		dynastyMap.put("greatgrandfather", greatgrandfather);
		dynastyMap.put("uncle", uncle);
		dynastyMap.put("cousin1", cousin1);
		dynastyMap.put("cousin2", cousin2);
		dynastyMap.put("sibling1", sibling1);
		dynastyMap.put("sibling2", sibling2);
		
		return dynastyMap;
	}
	
	//Generates a dynastic (eg: abbasid, seleucid ...) name based on the parameter Character's name 
	private static String generateDynastyName(Character founder) {
		String dynastyName = founder.getName();

		//If it ends with one of these and is at least 7 letters long, remove it.
		String final2letters = dynastyName.substring(dynastyName.length()-2);
		switch (final2letters)
		{
			case "os":
			case "us":
				if (dynastyName.length() > 6)
					dynastyName = dynastyName.substring(0, dynastyName.length()-2);
				break;
		}
		
		//Remove any trailing vowels
		boolean shorten = true;
		while (shorten && (dynastyName.length() > 2))
		{
			String finalLetter = dynastyName.substring(dynastyName.length()-1);
			
			
			switch (finalLetter)
			{
			case "a":
			case "e":
			case "i":
			case "o":
			case "u":
			case "y":
				if (dynastyName.length() > 1)
					dynastyName = dynastyName.substring(0, dynastyName.length()-1);
				break;
			default:
				shorten = false;
				break;
			}

		}
		
		//Add id at the end
		return dynastyName+"id";
	}


}
