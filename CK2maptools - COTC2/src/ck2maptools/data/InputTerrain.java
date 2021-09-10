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

package ck2maptools.data;

import java.awt.Color;

import ck2maptools.utils.Config;
import ck2maptools.utils.Logger;
import ck2maptools.utils.Utils;

public enum InputTerrain {
	PLAINS(			new Color(0, 	128, 	0) ),
	FARMLANDS(		new Color(64, 	192, 	64) ),
	FOREST(			new Color(0,	64,		0) ),
	FOREST_HILL(	new Color(64,	64,		0) ),
	PLATEAU(		new Color(128,	64,		0) ),
	MOUNTAIN(		new Color(64, 	0, 		0) ),
	MOUNTAIN_PEAK(	new Color(255, 	255, 	255) ),
	VOLCANO(		new Color(255, 	0, 		0) ),
	
	DESERT(			new Color(255, 	192, 	0) ),
	SEMI_DESERT(	new Color(192, 	255, 	0) ),
	STEPPE(			new Color(128, 	128,	0) ),
	
	UNUSED1(		new Color(0, 	0,		0) ),
	UNUSED2(		new Color(0, 	0,		0) ),
	
	WATER(			new Color(0, 	128, 	255) ),
	DEEP_WATER(		new Color(0, 	0, 		255) ),
	MAJOR_RIVER(	new Color(0, 	255, 	255) );

	private Color color;
	private InputTerrain(Color color)
	{
		this.color = color;
	}
	
	public Color getColor() {
		return color;
	}
	
	public int getRGB() {
		return color.getRGB();
	}
	
	//Returns InputTerrain from input.bmp at the param coordinates
	//If color is not an exact match for any defined terrains, returns the closest match
	// /!\ coordinates are in the provinces.bmp, input map scale applies
	public static InputTerrain getInputTerrainAt(int x, int y) {
		
		Loader loader = Loader.getLoader();
		
		if (loader.bufInInput == null)
			return InputTerrain.PLAINS;
		
		//Optimization : cache results in an array
		if (loader.inputArray == null)
			loader.inputArray = new InputTerrain[loader.sizeX][loader.sizeY];
		else if (loader.inputArray[x][y] != null)
			return loader.inputArray[x][y];
		
		InputTerrain bestTerrain = null;
		int lowestDiff = Integer.MAX_VALUE;
	
		int rgb = loader.bufInInput.getRGB(x/Config.INPUT_MAP_SCALE, y/Config.INPUT_MAP_SCALE);
		
		for (InputTerrain t : InputTerrain.values())
		{
			int trgb = t.getRGB();
	
			int rdiff = Math.abs(Utils.getColorR(trgb) - Utils.getColorR(rgb)) ;
			int gdiff = Math.abs(Utils.getColorG(trgb) - Utils.getColorG(rgb));
			int bdiff = Math.abs(Utils.getColorB(trgb) - Utils.getColorB(rgb));
			
			int diff = rdiff + gdiff + bdiff;
	
			if (diff < lowestDiff)
			{
				bestTerrain = t;
				lowestDiff = diff;
			}
		}
	
		loader.inputArray[x][y] = bestTerrain;
		
		return bestTerrain;
	}
	
	public static void setCustomTerrainColor(int index, Color c)
	{
		if (index == 2)
			UNUSED2.color = c;
		else
			UNUSED1.color = c;
	}
	
	public static Color getCustomTerrainColor(int index)
	{
		if (index == 2)
			return UNUSED2.color;
		else
			return UNUSED1.color;
	}
}
