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

public interface ICK2MapTool {
	public static final int ERROR_NONE = 0;
	public static final int ERROR_LOCALISATION = 1;
	public static final int ERROR_FILLING_PROVINCE = 1 << 1;
	public static final int ERROR_RIVERS = 1 << 2;
	public static final int ERROR_TRADE_ROUTES = 1 << 3;
	
	public int execute() throws Exception;
}
