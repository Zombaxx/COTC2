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

public class Coordinates {
	
	@Override
	public String toString() {
		return "[x=" + x + ", y=" + y + "]";
	}
	
	@Override
	public boolean equals(Object obj) {

		if (obj instanceof Coordinates)
		{
			Coordinates compared = (Coordinates)obj;
					
			return (compared.x == this.x && compared.y == this.y);
		}
		
		return false;
	}

	private int x;
	private int y;
	public Coordinates(int x, int y) {
		super();
		this.x = x;
		this.y = y;
	}
	public int getX() {
		return x;
	}
	public int getY() {
		return y;
	}
		
	public boolean isValidCoordinates()
	{
		return isValidCoordinates(x,y);
	}
	public static boolean isValidCoordinates(int x, int y)
	{
		return x >= 0 && y >= 0 && x < Loader.getLoader().sizeX && y < Loader.getLoader().sizeY;
	}

}
