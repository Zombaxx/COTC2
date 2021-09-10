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

public enum Rivers {
	RIVER_SOURCE(0, new Color(0, 255, 0)),
	RIVER_SOURCE_FROM_MAJOR_RIVER(14, new Color(0, 158, 0)),
	MERGING_RIVER(1, new Color(255,0,0)),
	SPLITTING_RIVER(2, new Color(255, 252, 0)),
	WATER(254, new Color(255, 0, 128)),
	LAND(255, new Color(255, 255, 255)),
	RIVER1(3, new Color(0, 225, 255)),
	RIVER2(4, new Color(0, 200, 255)),
	RIVER3(5, new Color(0, 150, 255)),
	RIVER4(6, new Color(0, 100, 255)),
	RIVER5(7, new Color(0, 0, 255)),
	RIVER6(8, new Color(0, 0, 225)),
	RIVER7(9, new Color(0, 0, 200)),
	RIVER8(10, new Color(0, 0, 150)),
	RIVER9(11, new Color(0, 0, 100)),
	UNUSED1(12, new Color(0, 85, 0)),
	UNUSED2(13, new Color(0, 125, 0)),
	UNUSED3(15, new Color(24, 206, 0)),
	;
	
	private int index;
	private Color color;
	
	private Rivers(int index, Color color)
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
	

	public boolean isRiver() {
		return (this == Rivers.RIVER9
				|| this == Rivers.RIVER8 
				|| this == Rivers.RIVER7 
				|| this == Rivers.RIVER6 
				|| this == Rivers.RIVER5
				|| this == Rivers.RIVER4
				|| this == Rivers.RIVER3
				|| this == Rivers.RIVER2
				|| this == Rivers.RIVER1);
	}

	public boolean isRiverOrSpecial() {
		return (this.isRiver() 
				|| this == Rivers.MERGING_RIVER
				|| this == Rivers.SPLITTING_RIVER
				|| this == Rivers.RIVER_SOURCE 
				|| this == Rivers.RIVER_SOURCE_FROM_MAJOR_RIVER
				);
	}
	private static IndexColorModel icm = null;
	
	public static IndexColorModel getIndexColorModel()
	{
		if (icm != null)
			return icm;
		
		byte[] redIndex=new byte[256], greenIndex=new byte[256], blueIndex=new byte[256];
		for(Rivers t : Rivers.values())
		{
			redIndex[t.getIndex()] = (byte)t.getColor().getRed();
			greenIndex[t.getIndex()] = (byte)t.getColor().getGreen();
			blueIndex[t.getIndex()] = (byte)t.getColor().getBlue();
		}
		icm = new IndexColorModel(8, 256, redIndex, greenIndex, blueIndex);
		
		return icm;
	}	
}
