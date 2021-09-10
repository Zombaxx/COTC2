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

public enum Climate {
	POLAR(		new Color(0, 	0, 		255)),
	COLD(		new Color(0, 	255, 	255)),
	TEMPERATE(	new Color(0, 	255,	0)),
	WARM(		new Color(255, 	255, 	0)),
	HOT(		new Color(255, 	0, 		0));

	private Color color;
	private Climate(Color color)
	{
		this.color = color;
	}
	
	public Color getColor() {
		return color;
	}
	
	public int getRGB() {
		return color.getRGB();
	}
	
	
	//Returns climate from climate.bmp at the param coordinates
	//If color is not an exact match for any defined climates, returns the closest match
	// /!\ coordinates are in the provinces.bmp, input map scale applies
	public static Climate getInputClimateAt(int x, int y) {
		
		Loader loader = Loader.getLoader();
		
		if (loader.bufInClimate == null)
			return Climate.TEMPERATE;
		
		//Optimization : cache results in an array
		if (loader.climateArray == null)
			loader.climateArray = new Climate[loader.sizeX][loader.sizeY];
		else if (loader.climateArray[x][y] != null)
			return loader.climateArray[x][y];
		
		Climate bestClimate = null;
		int lowestDiff = Integer.MAX_VALUE;
		
		int rgb = loader.bufInClimate.getRGB(x/Config.INPUT_MAP_SCALE, y/Config.INPUT_MAP_SCALE);

		for (Climate c : Climate.values())
		{
			int crgb = c.getRGB();

			int rdiff = Math.abs(Utils.getColorR(crgb) - Utils.getColorR(rgb)) ;
			int gdiff = Math.abs(Utils.getColorG(crgb) - Utils.getColorG(rgb));
			int bdiff = Math.abs(Utils.getColorB(crgb) - Utils.getColorB(rgb));
			
			int diff = rdiff + gdiff + bdiff;
			
			if (diff < lowestDiff)
			{
				bestClimate = c;
				lowestDiff = diff;
			}
		}
		
		loader.climateArray[x][y] = bestClimate;
		
		return bestClimate;
	}
}
