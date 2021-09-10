package ck2maptools.utils;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import ck2maptools.data.InputTerrain;

public class Config {
	
	public static int START_DATE = 1066;
	public static String MOD_FOLDER = "C:/Program Files (x86)/Steam/steamapps/common/Crusader Kings II";
	public static int INPUT_MAP_SCALE = 2;
	public static int TREE_MAP_SCALE = 6;
	public static int PEAK_HEIGHT 		= 185;
	public static int SNOW_HEIGHT 		= 155;
	public static int MOUNTAIN_HEIGHT 	= 135;
	public static int HILLS_HEIGHT 		= 115;	
	public static int NOISE_PATCH_SIZE = 4;
	public static int NOISE_BASELINE = 0;
	public static double NOISE_FACTOR_MAX = 0.8f;
	public static int SMOOTH_RADIUS = 6;
	public static Color CUSTOM_TERRAIN_1_COLOR = new Color(96, 96, 96);
	public static Color CUSTOM_TERRAIN_2_COLOR = new Color(96, 104, 16);
	public static int CUSTOM_TERRAIN_1_WEALTH = 2;
	public static int CUSTOM_TERRAIN_2_WEALTH = 2;	
	public static int CUSTOM_TERRAIN_1_HEIGHT = 96;
	public static int CUSTOM_TERRAIN_2_HEIGHT = 96;
	public static int MIN_SETTLEMENT_DISTANCE = 24;
	public static int MIN_SEA_NODE_DISTANCE = 64;
	public static int MAX_STRAIT_DISTANCE = 32;
	public static int PERCENT_FEMALE = 10;
	
	public static boolean parseConfig()
	{
		boolean ret = true;
		
		//Parse config.csv
		try {
			List<String[]> infoTxtData = Utils.parseCsvFile("./input/config.csv");
			for (String[] data : infoTxtData)
			{
				if (data[0].isEmpty() || data[0].startsWith("#"))
					continue;
				
				String header = data[0].toLowerCase();
				if (header.equals("start_year"))
				{
					try {
						START_DATE = Integer.parseInt(data[1]);
					}
					catch (NumberFormatException e)
					{
						Logger.log("ERROR parsing config.csv file : start_year has incorrect format");
						ret = false;
					}
				}
				else if (header.equals("mod_folder"))
				{
					MOD_FOLDER = data[1];
					File modFolder = new File(MOD_FOLDER);
					if (!modFolder.exists())
					{
						Logger.log("ERROR parsing config.csv file : mod_folder does not exist");
						ret = false;
					}
				}
				else if (header.equals("hills_height"))
				{
					try {
						HILLS_HEIGHT = Integer.parseInt(data[1]);
					}
					catch (NumberFormatException e)
					{
						Logger.log("ERROR parsing config.csv file : hills_height has incorrect format");
						ret = false;
					}
				}
				else if (header.equals("mountains_height"))
				{
					try {
						MOUNTAIN_HEIGHT = Integer.parseInt(data[1]);
					}
					catch (NumberFormatException e)
					{
						Logger.log("ERROR parsing config.csv file : mountains_height has incorrect format");
						ret = false;
					}
				}
				else if (header.equals("snow_height"))
				{
					try {
						SNOW_HEIGHT = Integer.parseInt(data[1]);
					}
					catch (NumberFormatException e)
					{
						Logger.log("ERROR parsing config.csv file : snow_height has incorrect format");
						ret = false;
					}
				}
				else if (header.equals("peak_height"))
				{
					try {
						PEAK_HEIGHT = Integer.parseInt(data[1]);
					}
					catch (NumberFormatException e)
					{
						Logger.log("ERROR parsing config.csv file : peak_height has incorrect format");
						ret = false;
					}
				}
				else if (header.equals("input_map_scale"))
				{
					try {
						INPUT_MAP_SCALE = Integer.parseInt(data[1]);
					}
					catch (NumberFormatException e)
					{
						Logger.log("ERROR parsing config.csv file : input_map_scale has incorrect format");
						ret = false;
					}
				}
				else if (header.equals("tree_map_scale"))
				{
					try {
						TREE_MAP_SCALE = Integer.parseInt(data[1]);
					}
					catch (NumberFormatException e)
					{
						Logger.log("ERROR parsing config.csv file : tree_map_scale has incorrect format");
						ret = false;
					}
				}
				else if (header.equals("noise_patch_size"))
				{
					try {
						NOISE_PATCH_SIZE = Integer.parseInt(data[1]);
					}
					catch (NumberFormatException e)
					{
						Logger.log("ERROR parsing config.csv file : noise_patch_size has incorrect format");
						ret = false;
					}
				}
				else if (header.equals("noise_baseline"))
				{
					try {
						NOISE_BASELINE = Integer.parseInt(data[1]);
					}
					catch (NumberFormatException e)
					{
						Logger.log("ERROR parsing config.csv file : noise_baseline has incorrect format");
						ret = false;
					}
				}
				else if (header.equals("noise_factor_max"))
				{
					try {
						NOISE_FACTOR_MAX = Double.parseDouble(data[1]);
					}
					catch (NumberFormatException e)
					{
						Logger.log("ERROR parsing config.csv file : noise_factor_max has incorrect format");
						ret = false;
					}
				}
				else if (header.equals("smooth_radius"))
				{
					try {
						SMOOTH_RADIUS = Integer.parseInt(data[1]);
					}
					catch (NumberFormatException e)
					{
						Logger.log("ERROR parsing config.csv file : smooth_radius has incorrect format");
						ret = false;
					}
				}
				else if (header.equals("custom_terrain_1_colormap"))
				{
					int r = 96, g = 96, b = 96; 
					try {
						r = Integer.parseInt(data[1]);
						g = Integer.parseInt(data[2]);
						b = Integer.parseInt(data[3]);
					}
					catch (NumberFormatException e)
					{
						Logger.log("ERROR parsing config.csv file : custom_terrain_1_color_map has incorrect format");
					}
					CUSTOM_TERRAIN_1_COLOR = new Color(r,g,b);
				}
				else if (header.equals("custom_terrain_2_colormap"))
				{
					int r = 96, g = 104, b = 16; 
					try {
						r = Integer.parseInt(data[1]);
						g = Integer.parseInt(data[2]);
						b = Integer.parseInt(data[3]);
					}
					catch (NumberFormatException e)
					{
						Logger.log("ERROR parsing config.csv file : custom_terrain_2_color_map has incorrect format");
					}
					CUSTOM_TERRAIN_2_COLOR = new Color(r,g,b);
				}
				else if (header.equals("custom_terrain_1_input_color"))
				{
					int r = 128, g = 128, b = 128; 
					try {
						r = Integer.parseInt(data[1]);
						g = Integer.parseInt(data[2]);
						b = Integer.parseInt(data[3]);
					}
					catch (NumberFormatException e)
					{
						Logger.log("ERROR parsing config.csv file : custom_terrain_1_input_color has incorrect format");
					}
					InputTerrain.setCustomTerrainColor(1, new Color(r,g,b));
				}
				else if (header.equals("custom_terrain_2_input_color"))
				{
					int r = 0, g = 128, b = 128; 
					try {
						r = Integer.parseInt(data[1]);
						g = Integer.parseInt(data[2]);
						b = Integer.parseInt(data[3]);
					}
					catch (NumberFormatException e)
					{
						Logger.log("ERROR parsing config.csv file : custom_terrain_2_input_color has incorrect format");
					}
					InputTerrain.setCustomTerrainColor(2, new Color(r,g,b));
				}
				else if (header.equals("custom_terrain_1_height"))
				{
					try {
						CUSTOM_TERRAIN_1_HEIGHT = Integer.parseInt(data[1]);
					}
					catch (NumberFormatException e)
					{
						Logger.log("ERROR parsing config.csv file : custom_terrain_1_height has incorrect format");
						ret = false;
					}
				}
				else if (header.equals("custom_terrain_2_height"))
				{
					try {
						CUSTOM_TERRAIN_2_HEIGHT = Integer.parseInt(data[1]);
					}
					catch (NumberFormatException e)
					{
						Logger.log("ERROR parsing config.csv file : custom_terrain_2_height has incorrect format");
						ret = false;
					}
				}
				else if (header.equals("custom_terrain_1_wealth"))
				{
					try {
						CUSTOM_TERRAIN_1_WEALTH = Integer.parseInt(data[1]);
					}	
					catch (NumberFormatException e)
					{
						Logger.log("ERROR parsing config.csv file : custom_terrain_1_wealth has incorrect format");
						ret = false;
					}
				}
				else if (header.equals("custom_terrain_2_wealth"))
				{
					try {
						CUSTOM_TERRAIN_2_WEALTH = Integer.parseInt(data[1]);
					}
					catch (NumberFormatException e)
					{
						Logger.log("ERROR parsing config.csv file : custom_terrain_2_wealth has incorrect format");
						ret = false;
					}
				}
				else if (header.equals("settlements_min_distance"))
				{
					try {
						MIN_SETTLEMENT_DISTANCE = Integer.parseInt(data[1]);
					}
					catch (NumberFormatException e)
					{
						Logger.log("ERROR parsing config.csv file : settlements_min_distance has incorrect format");
						ret = false;
					}
				}
				else if (header.equals("sea_nodes_min_distance"))
				{
					try {
						MIN_SEA_NODE_DISTANCE = Integer.parseInt(data[1]);
					}
					catch (NumberFormatException e)
					{
						Logger.log("ERROR parsing config.csv file : sea_nodes_min_distance has incorrect format");
						ret = false;
					}
				}
				else if (header.equals("max_strait_distance"))
				{
					try {
						MAX_STRAIT_DISTANCE = Integer.parseInt(data[1]);
					}
					catch (NumberFormatException e)
					{
						Logger.log("ERROR parsing config.csv file : max_strait_distance has incorrect format");
						ret = false;
					}
				}
				else if (header.equals("percent_female"))
				{
					try {
						PERCENT_FEMALE = Integer.parseInt(data[1]);
					}
					catch (NumberFormatException e)
					{
						Logger.log("ERROR parsing config.csv file : max_strait_distance has incorrect format");
						ret = false;
					}
				}
			}
		}
		catch (IOException e)
		{
			Logger.log("config.csv file not found");
			ret = false;
		}
		catch (Exception e)
		{
			Logger.log("ERROR parsing config.csv file : "+e.getMessage());
			ret = false;
		}
		
		HILLS_HEIGHT = Math.max(96, HILLS_HEIGHT);
		MOUNTAIN_HEIGHT = Math.max(HILLS_HEIGHT, MOUNTAIN_HEIGHT);
		SNOW_HEIGHT = Math.max(MOUNTAIN_HEIGHT, SNOW_HEIGHT);
		PEAK_HEIGHT = Math.max(SNOW_HEIGHT, PEAK_HEIGHT);

		//int maxHeight = (int) (PEAK_HEIGHT+(NOISE_BASELINE+PEAK_HEIGHT-96)*NOISE_FACTOR_MAX); 
		
		return ret;
	}
	
	public static void saveConfig()
	{

		File csvFile = new File("./input/config.csv");
		try {
			FileWriter writer = new FileWriter(csvFile);
			int rgb = 0;
			writer.write("#CK2MapTools Config file;This file is automatically regenerated\r\n");
			writer.write("#SETTING;VALUE\r\n");
			writer.write("#START_YEAR;Target year for which to generate history.\r\n");
			writer.write("start_year;"+START_DATE+"\r\n");
			writer.write("#MOD_FOLDER;Folder of your mod, or of the base game. Used only to read cultures.\r\n");
			writer.write("mod_folder;"+MOD_FOLDER+"\r\n");
			writer.write("#HEIGHTS;Tweak these to alter terrain height. Default values 125,150,175,200. Each one should be higher than the last and all should be higher than 96.\r\n");
			writer.write("hills_height;"+HILLS_HEIGHT+"\r\n");
			writer.write("mountains_height;"+MOUNTAIN_HEIGHT+"\r\n");
			writer.write("snow_height;"+SNOW_HEIGHT+"\r\n");
			writer.write("peak_height;"+PEAK_HEIGHT+"\r\n");
			writer.write("#INPUT_MAP_SCALE;How much smaller the input.bmp and climate.bmp are compared to the provinces.bmp. Higher value means lower resolution. Defaults to 2, meaning the provinces.bmp is twice as large as input.bmp\r\n");
			writer.write("input_map_scale;"+INPUT_MAP_SCALE+"\r\n");
			writer.write("#TREE_MAP_SCALE;How much smaller the tree.bmp is compared to the provinces.bmp. Higher value means lower resolution, fewer trees and better in-game performance. Defaults to 6, meaning the provinces.bmp is 6 times as large as the tree.bmp.\r\n");
			writer.write("tree_map_scale;"+TREE_MAP_SCALE+"\r\n");
			writer.write("#NOISE;Params for randomness in terrain height. For each square of noise_patch_size pixels, a random number rand is chosen between 0 and noise_factor_max.\r\n");
			writer.write("#NOISE;Final terrain height for each pixel is equal to [base_height + (noise_baseline + base_height - water_height) * rand]\r\n");
			writer.write("#NOISE;[peak_height + (noise_baseline + peak_height - 96) * (noise_factor_max)] should not be much higher than 255\r\n");
			writer.write("#NOISE;A higher patch size will make the terrain more uniform. A lower size will increase bumpiness.\r\n");
			writer.write("noise_patch_size;"+NOISE_PATCH_SIZE+"\r\n");
			writer.write("#NOISE;A higher factor will increase height variations, especially on higher terrain.\r\n");
			writer.write("noise_factor_max;"+NOISE_FACTOR_MAX+"\r\n");
			writer.write("#NOISE;A higher baseline will increase height variations. This effect will be equally pronounced on low or high terrain.\r\n");
			writer.write("noise_baseline;"+NOISE_BASELINE+"\r\n");
			writer.write("#SMOOTHING;A higher number means smoother, flatter terrain, but also more time to process. Low numbers will lead to steep, artificial transitions between mountain types.\r\n");
			writer.write("smooth_radius;"+SMOOTH_RADIUS+"\r\n");
			writer.write("#PROVINCES;The minimum distance between 2 settlements when using the filling tool. A greater number will generate larger provinces\r\n");
			writer.write("settlements_min_distance;"+MIN_SETTLEMENT_DISTANCE+"\r\n");
			writer.write("#PROVINCES;The minimum distance between 2 sea nodes when using the filling tool. A greater number will generate larger sea zones\r\n");
			writer.write("sea_nodes_min_distance;"+MIN_SEA_NODE_DISTANCE+"\r\n");
			writer.write("#ADJACENCIES;Used for adjacencies calculations. Maximum squared distance between 2 provinces that can allow a strait.\r\n");
			writer.write("max_strait_distance;"+MAX_STRAIT_DISTANCE+"\r\n");
			writer.write("#RULERS;Percentage of female rulers to generate (feudal/tribal only)\r\n");
			writer.write("percent_female;"+PERCENT_FEMALE+"\r\n");
			writer.write("#CUSTOM_TERRAIN;There are 2 unused terrains in the game's terrain.bmp, you can use them and define their properties (for the tools) here\r\n");
			writer.write("#CUSTOM_TERRAIN;Color by which the terrain is identified in input.bmp. custom_terrain_1 will map to index 13 and custom_terrain_2 will map to index 14\r\n");
			rgb = InputTerrain.getCustomTerrainColor(1).getRGB();
			writer.write("custom_terrain_1_input_color;"+Utils.getColorR(rgb)+";"+Utils.getColorG(rgb)+";"+Utils.getColorB(rgb)+"\r\n");
			rgb = InputTerrain.getCustomTerrainColor(2).getRGB();
			writer.write("custom_terrain_2_input_color;"+Utils.getColorR(rgb)+";"+Utils.getColorG(rgb)+";"+Utils.getColorB(rgb)+"\r\n");
			writer.write("#CUSTOM_TERRAIN;Initial height for each custom terrain type. Note : th tool turns all terrain into hills/mountains or their desert equivalents above certain height levels, so keep this below hills_height as defined above\r\n");
			writer.write("custom_terrain_1_height;"+CUSTOM_TERRAIN_1_HEIGHT+"\r\n");
			writer.write("custom_terrain_2_height;"+CUSTOM_TERRAIN_2_HEIGHT+"\r\n");
			writer.write("#CUSTOM_TERRAIN;Color of the terrain's texture on the colormap.dds\r\n");
			rgb = CUSTOM_TERRAIN_1_COLOR.getRGB();
			writer.write("custom_terrain_1_colormap;"+Utils.getColorR(rgb)+";"+Utils.getColorG(rgb)+";"+Utils.getColorB(rgb)+"\r\n");
			rgb = CUSTOM_TERRAIN_2_COLOR.getRGB();
			writer.write("custom_terrain_2_colormap;"+Utils.getColorR(rgb)+";"+Utils.getColorG(rgb)+";"+Utils.getColorB(rgb)+"\r\n");
			writer.write("#CUSTOM_TERRAIN;Used for province holding slots calculations. For comparison : farmlands=8, plains=4, forest=2, steppe=2, hills=2, arctic=1, desert=0\r\n");
			writer.write("custom_terrain_1_wealth;"+CUSTOM_TERRAIN_1_WEALTH+"\r\n");
			writer.write("custom_terrain_2_wealth;"+CUSTOM_TERRAIN_2_WEALTH+"\r\n");
			writer.close();
		} catch (IOException e) {
			Logger.log("config.csv file not found or error parsing it : "+e.toString());
		}
	}

}
