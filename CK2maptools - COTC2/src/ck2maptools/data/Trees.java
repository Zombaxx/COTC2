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

public enum Trees {
	NO_TREES(0, new Color(0, 0, 0) ),
	UNUSED_TREES(1, new Color(255, 0, 0) ),
	CONIFEROUS1(2, new Color(0x1e, 0x8b, 0x6d) ), //1e8b6d
	CONIFEROUS2(3, new Color(0x12, 0x64, 0x4e) ), //12644e
	CONIFEROUS3(4, new Color(0x08, 0x3a, 0x2c) ), //083a2c
	DECIDUOUS1(5, new Color(0x4c, 0x9c, 0x33) ), //4c9c33
	DECIDUOUS2(6, new Color(0x2f, 0x78, 0x18) ), //2f7818
	DECIDUOUS3(7, new Color(0x14, 0x55, 0) ), //145500
	MEDITERRANEAN1(8, new Color(0x9a, 0x9c, 0x33) ), //9a9c33
	MEDITERRANEAN2(9, new Color(0x76, 0x78, 0x18) ), //767818
	MEDITERRANEAN3(10, new Color(0x53, 0x55, 0) ), //535500
	PALM_TREE1(11, new Color(0xff, 0xff, 0) ), //ffff00
	PALM_TREE2(12, new Color(0xd5, 0xa0, 0) ), //d5a000
	;
	
	private int index;
	private Color color;
	
	private Trees(int index, Color color)
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
		for(Trees t : Trees.values())
		{
			redIndex[t.getIndex()] = (byte)t.getColor().getRed();
			greenIndex[t.getIndex()] = (byte)t.getColor().getGreen();
			blueIndex[t.getIndex()] = (byte)t.getColor().getBlue();
		}
		icm = new IndexColorModel(8, 256, redIndex, greenIndex, blueIndex);
		
		return icm;
	}
}
