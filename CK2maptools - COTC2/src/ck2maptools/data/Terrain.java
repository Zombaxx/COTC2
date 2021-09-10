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
import java.awt.image.IndexColorModel;

public enum Terrain {
	PLAINS(0, new Color(86, 124, 27) ),
	FARMLAND(1, new Color(138, 11, 26) ),
	COASTAL_DESERT(2, new Color(130, 158, 75) ),
	DESERT(3, new Color(206, 169, 99) ),
	SANDY_MOUNTAIN(4, new Color(112, 74, 31) ),
	STEPPE(5, new Color(255, 186, 0) ),
	ARCTIC(6, new Color(13, 96, 62) ),
	DESERT_MOUNTAIN(7, new Color(86, 46, 0) ),
	FOREST_HILLS(8, new Color(0, 86, 6) ),
	MOUNTAIN(9, new Color(65, 42, 17) ),
	SNOWY_MOUNTAIN(10, new Color(155, 155, 155) ),
	FROZEN_MOUNTAIN(11, new Color(255, 255, 255) ),
	JUNGLE(12, new Color(40, 180, 149) ),
	UNUSED1(13, new Color(213, 144, 199) ),
	UNUSED2(14, new Color(127, 24, 60) ),
	WATER(15, new Color(69, 91, 186) ),
	/*
	0 		Plains 	plains 	86 124 27 	
	1 		Farmland 	farmlands 	138 11 26 	
	2 		Coastal desert 	plains 	130 158 75 	Mix between desert and patches of grass.
	3 		Desert 	desert 	206 169 99 	
	4 		Sandy mountain 	mountain 	112 74 31 	Should be in the valleys or where the desert meets the mountains
	5 		Steppe 	steppe 	255 186 0 	
	6 		Arctic 	arctic 	13 96 62 	Similar color to pine needles
	7 		Desert mountain 	mountain 	86 46 0 	Mix of desert and mountain rock
	8 		Forest 	hills 	0 86 6 	
	9 		Snowless mountain 	mountain 	65 42 17 	
	10 		Snow covered mountain 	155 155 155 	
	11 		More snow covered mountain 	255 255 255 	
	12 		Jungle 	jungle 	40 180 149 	
	13 		N/A 	coastal_desert 	213 144 199 	Not used in the base game
	14 		N/A 	127 24 60
	15 		Water 	69 91 186 	On the default map this covers all sea/ocean areas and navigable rivers. Additionally it extends approximately 1 pixel in around coastlines, giving the appearance of a beach
	*/ 

	;
	
	private int index;
	private Color color;
	
	private Terrain(int index, Color color)
	{
		this.index = index;
		this.color = color;
	}

	public int getIndex() {
		return index;
	}

	public Color getColor() {
		return color;
	}
	
	public int getRGB() {
		return color.getRGB();
	}
	
	private static IndexColorModel icm = null;
	
	public static IndexColorModel getIndexColorModel()
	{
		if (icm != null)
			return icm;
		
		byte[] redIndex=new byte[256], greenIndex=new byte[256], blueIndex=new byte[256];
		for(Terrain t : Terrain.values())
		{
			redIndex[t.getIndex()] = (byte)t.getColor().getRed();
			greenIndex[t.getIndex()] = (byte)t.getColor().getGreen();
			blueIndex[t.getIndex()] = (byte)t.getColor().getBlue();
		}
		icm = new IndexColorModel(8, 256, redIndex, greenIndex, blueIndex);
		
		return icm;
	}
}
