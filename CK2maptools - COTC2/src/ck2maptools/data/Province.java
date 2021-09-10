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

import java.util.ArrayList;
import java.util.List;

import ck2maptools.utils.Utils;

public class Province {
	
	private int x,y;
	private int totalX, totalY, territorySize;
	private int mapColor;
	private int index;
	private int winterSeverity;
	private int numSlots;
	private Coordinates portCoordinates;

	private boolean isWater;	
	private boolean isWasteland;
	private boolean isRiver;
	private Province deJureDuchyCapital;
	private Province deJureKingdomCapital;
	private Province deJureEmpireCapital;
	private List<Province> deJureDuchyVassals;
	private List<Province> deJureKingdomVassals;
	private List<Province> deJureEmpireVassals;
	private Province deFactoDuchyCapital;
	private Province deFactoKingdomCapital;
	private Province deFactoEmpireCapital;
	private List<Province> deFactoDuchyVassals;
	private List<Province> deFactoKingdomVassals;
	private List<Province> deFactoEmpireVassals;
	private List<Province> adjacentProvinces;
	private List<Province> indirectAdjacentProvinces;
	private List<Province> adjacentWaterProvinces;
	private List<Province> adjacentDuchies;
	private List<Province> adjacentKingdoms;
	private boolean port;
	private boolean bordersMajorRiver;
	private Government government;
	private String religion;
	private Culture culture;
	private int technology;
	private List<String> holysite; //List of religions I am a holy site of
	private boolean important;
	private List<Province> islandRegion;
	
	
	//Localisation
	private String provinceName;	//Name of province and county
	private String[] baronyName;	//Name of all 8 potential baronies
	private String duchyName;		//Name of dejure duchy, only valid for duchy capitals
	private String kingdomName;		//Name of dejure kingdom, only valid for kingdom capitals
	private String empireName;		//Name of dejure empire, only valid for empire capitals

	
	public Province(Coordinates coord, int mapColor, int index) {
		super();
		this.x = coord.getX();
		this.y = coord.getY();
		this.mapColor = mapColor;
		this.index = index;
		deJureDuchyVassals = new ArrayList<Province>();
		deJureKingdomVassals = new ArrayList<Province>();
		deJureEmpireVassals = new ArrayList<Province>();
		deFactoDuchyVassals = new ArrayList<Province>();
		deFactoKingdomVassals = new ArrayList<Province>();
		deFactoEmpireVassals = new ArrayList<Province>();
		adjacentProvinces = new ArrayList<Province>();
		indirectAdjacentProvinces = new ArrayList<Province>();
		adjacentDuchies = new ArrayList<Province>();
		adjacentKingdoms = new ArrayList<Province>();
		adjacentWaterProvinces = new ArrayList<Province>();
		holysite = new ArrayList<String>();		
		
		//Initialize all localisation Strings to null
		baronyName = new String[] { null, null, null, null, null, null, null, null };
		provinceName = null;
		duchyName = null;
		kingdomName = null;
		empireName = null;
	}
	
	public int getIndex() {return index;}
	public String getReligion() {return (religion == null ? "pagan" : religion);}
	public void setReligion(String religion) {this.religion = religion;}
	public Culture getCulture() {return (culture == null ? Culture.getCulture("uncultured_swine") : culture);}
	public void setCulture(Culture culture) {this.culture = culture;}
	public Government getGovernment() {return (government == null ? Government.TRIBAL : government);}
	public void setGovernment(Government government) {this.government = government;}
	public boolean isPort() {return port;}
	public void setPort(boolean port) {this.port = port;}
	public boolean bordersMajorRiver() {return bordersMajorRiver;}
	public void setBordersMajorRiver(boolean bordersMajorRiver) {this.bordersMajorRiver = bordersMajorRiver;}
	public void addTerritory(int x, int y) { territorySize++; totalX+=x; totalY+=y;}
	public int getTerritorySize() { return territorySize; }
	public Coordinates getCenter()
	{
		return new Coordinates(totalX / territorySize, totalY / territorySize);
	}
	public Coordinates getPortCoordinates()
	{
		return portCoordinates;
	}
	public void setPortCoordinates(int x, int y) { portCoordinates = new Coordinates(x,y);}
	
	public void addAdjacentWaterProvince(Province p)
	{
		if (!adjacentWaterProvinces.contains(p))
			adjacentWaterProvinces.add(p);
	}
	public boolean isAdjacentWaterProvince(Province p)
	{
		return adjacentWaterProvinces.contains(p);
	}
	public List<Province> getAdjacentWaterProvinces() {
		List<Province> ret = new ArrayList<Province>();
		for (Province p : adjacentWaterProvinces)
			ret.add(p);
		return ret;
	}	
	public void addAdjacentProvince(Province p)
	{
		if (!adjacentProvinces.contains(p))
			adjacentProvinces.add(p);
	}	
	public void addIndirectAdjacentProvince(Province p)
	{
		if (!indirectAdjacentProvinces.contains(p))
			indirectAdjacentProvinces.add(p);
	}	
	public void addAdjacentDuchy(Province p)
	{
		if (!adjacentDuchies.contains(p))
			adjacentDuchies.add(p);
	}	
	public void addAdjacentKingdom(Province p)
	{
		if (!adjacentKingdoms.contains(p))
			adjacentKingdoms.add(p);
	}
	public boolean isAdjacentProvince(Province p)
	{
		return adjacentProvinces.contains(p);
	}
	public boolean isIndirectAdjacentProvince(Province p)
	{
		return indirectAdjacentProvinces.contains(p);
	}
	public boolean isAdjacentDuchy(Province p)
	{
		return adjacentDuchies.contains(p);
	}
	public boolean isAdjacentKingdom(Province p)
	{
		return adjacentKingdoms.contains(p);
	}
	public List<Province> getAdjacentProvinces() {
		List<Province> ret = new ArrayList<Province>();
		for (Province p : adjacentProvinces)
			ret.add(p);
		return ret;
	}
	public List<Province> getIndirectAdjacentProvinces() {
		List<Province> ret = new ArrayList<Province>();
		for (Province p : indirectAdjacentProvinces)
			ret.add(p);
		return ret;
	}
	public List<Province> getAllAdjacentProvinces() {
		List<Province> ret = new ArrayList<Province>();
		for (Province p : adjacentProvinces)
			ret.add(p);
		for (Province p : indirectAdjacentProvinces)
			ret.add(p);
		return ret;
	}
	public List<Province> getAdjacentDuchies() {
		List<Province> ret = new ArrayList<Province>();
		for (Province p : adjacentDuchies)
			ret.add(p);
		return ret;
	}
	public List<Province> getAdjacentKingdoms() {
		List<Province> ret = new ArrayList<Province>();
		for (Province p : adjacentKingdoms)
			ret.add(p);
		return ret;
	}
	public List<Province> getDeJureDuchyVassals() {
		List<Province> ret = new ArrayList<Province>();
		for (Province p : deJureDuchyVassals)
			ret.add(p);
		return ret;
	}
	public List<Province> getDeJureDuchyVassalsPlusSelf() {
		List<Province> ret = getDeJureDuchyVassals();
		ret.add(this);
		return ret;
	}
	public List<Province> getDeJureKingdomVassals() {
		List<Province> ret = new ArrayList<Province>();
		for (Province p : deJureKingdomVassals)
			ret.add(p);
		return ret;
	}
	public List<Province> getDeJureKingdomVassalsPlusSelf() {
		List<Province> ret = getDeJureKingdomVassals();
		ret.add(this);
		return ret;
	}
	public List<Province> getDeJureEmpireVassals() {
		List<Province> ret = new ArrayList<Province>();
		for (Province p : deJureEmpireVassals)
			ret.add(p);
		return ret;
	}
	public List<Province> getDeJureEmpireVassalsPlusSelf() {
		List<Province> ret = getDeJureEmpireVassals();
		ret.add(this);
		return ret;
	}

	
	public Province getDeJureDuchyCapital() {
		return (deJureDuchyCapital == null ? this : deJureDuchyCapital);
	}
	public void setDeJureDuchyCapital(Province deJureDuchyCapital) {
		
		if (deJureDuchyCapital == this)
			deJureDuchyCapital = null;
		
		if (getDeJureDuchyCapital() != this)
			getDeJureDuchyCapital().removeDeJureDuchyVassal(this);
		else if (deJureDuchyCapital != null)
			for (Province p : getDeJureDuchyVassals())
				p.setDeJureDuchyCapital(null);
			
		this.deJureDuchyCapital = deJureDuchyCapital;
		
		if (deJureDuchyCapital != null)
			deJureDuchyCapital.addDeJureDuchyVassal(this);
	}
	public Province getDeJureKingdomCapital() {
		if (getDeJureDuchyCapital() == this)
			if (deJureKingdomCapital == null ) 
				return this;
			else 
				return deJureKingdomCapital;
		else 
			return getDeJureDuchyCapital().getDeJureKingdomCapital();
	}
	public void setDeJureKingdomCapital(Province deJureKingdomCapital) {
		if (deJureKingdomCapital == this)
			deJureKingdomCapital = null;
		
		if (getDeJureKingdomCapital() != this)
			getDeJureKingdomCapital().removeDeJureKingdomVassal(this);
		else if (deJureKingdomCapital != null)
			for (Province p : getDeJureKingdomVassals())
				p.setDeJureKingdomCapital(null);
			
		this.deJureKingdomCapital = deJureKingdomCapital;
		
		if (deJureKingdomCapital != null)
			deJureKingdomCapital.addDeJureKingdomVassal(this);
	}
	public Province getDeJureEmpireCapital() {
		if (getDeJureKingdomCapital() == this)
			if (deJureEmpireCapital == null ) 
				return this;
			else 
				return deJureEmpireCapital;
		else 
			return getDeJureKingdomCapital().getDeJureEmpireCapital();
	}
	public void setDeJureEmpireCapital(Province deJureEmpireCapital) {
		if (deJureEmpireCapital == this)
			deJureEmpireCapital = null;
		
		if (getDeJureEmpireCapital() != this)
			getDeJureEmpireCapital().removeDeJureEmpireVassal(this);
		else if (deJureEmpireCapital != null)
			for (Province p : getDeJureEmpireVassals())
				p.setDeJureEmpireCapital(null);
			
		this.deJureEmpireCapital = deJureEmpireCapital;
		
		if (deJureEmpireCapital != null)
			deJureEmpireCapital.addDeJureEmpireVassal(this);
	}
	private void removeDeJureDuchyVassal(Province province) {
		deJureDuchyVassals.remove(province);
	}
	private void addDeJureDuchyVassal(Province province) {
		if (!deJureDuchyVassals.contains(province) && province != this)
			deJureDuchyVassals.add(province);
	}
	private void removeDeJureKingdomVassal(Province province) {
		deJureKingdomVassals.remove(province);
	}
	private void addDeJureKingdomVassal(Province province) {
		if (!deJureKingdomVassals.contains(province) && province != this)
			deJureKingdomVassals.add(province);
	}
	private void removeDeJureEmpireVassal(Province province) {
		deJureEmpireVassals.remove(province);
	}
	private void addDeJureEmpireVassal(Province province) {
		if (!deJureEmpireVassals.contains(province) && province != this)
			deJureEmpireVassals.add(province);
	}
	
	
	public List<Province> getDeFactoDuchyVassals() {
		List<Province> ret = new ArrayList<Province>();
		for (Province p : deFactoDuchyVassals)
			ret.add(p);
		return ret;
	}
	public List<Province> getDeFactoDuchyVassalsPlusSelf() {
		List<Province> ret = getDeFactoDuchyVassals();
		ret.add(this);
		return ret;
	}
	public List<Province> getDeFactoKingdomVassals() {
		List<Province> ret = new ArrayList<Province>();
		for (Province p : deFactoKingdomVassals)
			ret.add(p);
		return ret;
	}
	public List<Province> getDeFactoKingdomVassalsPlusSelf() {
		List<Province> ret = getDeFactoKingdomVassals();
		ret.add(this);
		return ret;
	}
	public List<Province> getDeFactoEmpireVassals() {
		List<Province> ret = new ArrayList<Province>();
		for (Province p : deFactoEmpireVassals)
			ret.add(p);
		return ret;
	}
	public List<Province> getDeFactoEmpireVassalsPlusSelf() {
		List<Province> ret = getDeFactoEmpireVassals();
		ret.add(this);
		return ret;
	}
	

	public Province getDeFactoDuchyCapital() {
		return deFactoDuchyCapital;
	}
	public void setDeFactoDuchyCapital(Province deFactoDuchyCapital) {
		
		if (getDeFactoDuchyCapital() != null)
			getDeFactoDuchyCapital().removeDeFactoDuchyVassal(this);
		else if (deFactoDuchyCapital != this)
			for (Province p : getDeFactoDuchyVassals())
				p.setDeFactoDuchyCapital(null);
			
		this.deFactoDuchyCapital = deFactoDuchyCapital;
		
		if (deFactoDuchyCapital != null)
			deFactoDuchyCapital.addDeFactoDuchyVassal(this);
	}
	public Province getDeFactoKingdomCapital() {
		if (getDeFactoDuchyCapital() == this || getDeFactoDuchyCapital() == null)
			return deFactoKingdomCapital;
		else 
			return getDeFactoDuchyCapital().getDeFactoKingdomCapital();
	}
	public void setDeFactoKingdomCapital(Province deFactoKingdomCapital) {
		if (getDeFactoKingdomCapital() != null)
			getDeFactoKingdomCapital().removeDeFactoKingdomVassal(this);
		else if (deFactoKingdomCapital != this)
			for (Province p : getDeFactoKingdomVassals())
				p.setDeFactoKingdomCapital(null);
			
		this.deFactoKingdomCapital = deFactoKingdomCapital;
		
		if (deFactoKingdomCapital != null)
			deFactoKingdomCapital.addDeFactoKingdomVassal(this);
	}
	public Province getDeFactoEmpireCapital() {
		if (getDeFactoKingdomCapital() == this || getDeFactoKingdomCapital() == null)
			return deFactoEmpireCapital;
		else 
			return getDeFactoKingdomCapital().getDeFactoEmpireCapital();
	}
	public void setDeFactoEmpireCapital(Province deFactoEmpireCapital) {
		if (getDeFactoEmpireCapital() != null)
			getDeFactoEmpireCapital().removeDeFactoEmpireVassal(this);
		else if (deFactoEmpireCapital != this)
			for (Province p : getDeFactoEmpireVassals())
				p.setDeFactoEmpireCapital(null);
			
		this.deFactoEmpireCapital = deFactoEmpireCapital;
		
		if (deFactoEmpireCapital != null)
			deFactoEmpireCapital.addDeFactoEmpireVassal(this);
	}
	
	private void removeDeFactoDuchyVassal(Province province) {
		deFactoDuchyVassals.remove(province);
	}
	private void addDeFactoDuchyVassal(Province province) {
		if (!deFactoDuchyVassals.contains(province) && province != this)
			deFactoDuchyVassals.add(province);
	}
	private void removeDeFactoKingdomVassal(Province province) {
		deFactoKingdomVassals.remove(province);
	}
	private void addDeFactoKingdomVassal(Province province) {
		if (!deFactoKingdomVassals.contains(province) && province != this)
			deFactoKingdomVassals.add(province);
	}
	private void removeDeFactoEmpireVassal(Province province) {
		deFactoKingdomVassals.remove(province);
	}
	private void addDeFactoEmpireVassal(Province province) {
		if (!deFactoEmpireVassals.contains(province) && province != this)
			deFactoEmpireVassals.add(province);
	}
	
	public int getWinterSeverity() {return winterSeverity;}
	public void setWinterSeverity(int winterSeverity) {this.winterSeverity = winterSeverity;}
	public int getX() {return x;}
	public int getY() {return y;}
	public void setCoord(Coordinates c) {x = c.getX(); y = c.getY();}
	public int getMapColor() {return mapColor;}
	public int getMapColorR() {return Utils.getColorR(mapColor);}
	public int getMapColorG() {return Utils.getColorG(mapColor);}
	public int getMapColorB() {return Utils.getColorB(mapColor);}
	public int getTechnology() {return technology;}
	public void setTechnology(int technology) {this.technology = technology;}

	public boolean isImportant() {return important;}
	public void setImportant(boolean important) {this.important = important;}
	
	//Returns true if province is a holy site of param religion. If param is null, returns true if province is a holy site of ANY religion
	public boolean isHolysite(String religion) {return religion == null ? !holysite.isEmpty() : holysite.contains(religion);}
	public void setHolysite(String religion) {this.holysite.add(religion);}
	
	@Override
	public String toString() {
		return "Province [x=" + x + ", y=" + y + ", provinceName=" + getProvinceName() + "]";
	}
	

	
	public void setProvinceName(String provinceName) {
		this.provinceName = provinceName;
		//Logger.log(this+" received province name "+provinceName);
		//if (baronyName[0] == null) //even if it already had a name
			baronyName[0] = provinceName; //Also name the top level barony like that
		if (duchyName == null)
			duchyName = provinceName; //Also name the duchy like that (will only have any effect if dejure capital)
		if (kingdomName == null)
			kingdomName = provinceName; //Also name the kingdom like that (will only have any effect if dejure capital)
		}
	public String getProvinceName() {return provinceName == null ? "Province"+String.format("%04d", this.index) : provinceName;}
	public boolean hasProvinceName() {return provinceName != null;}
	public String getTitleCountyName() {return "c_"+getProvinceName().toLowerCase().replace(" ", "_");}
	public void setBaronyNames(String[] baronyName) {this.baronyName = baronyName;}
	public void setBaronyName(int index, String baronyName) {
		this.baronyName[index] = baronyName;
		//Logger.log(this+" received barony("+index+") name "+baronyName);
		if (provinceName == null && index == 0)
			setProvinceName(baronyName); //Also name everything like that
		}
	public String getBaronyName(int index) {return baronyName[index] == null ? "Barony"+String.format("%04d", this.index)+(char)(96+index) : baronyName[index];}
	public boolean hasBaronyName(int index) {return baronyName[index] != null;}
	public String getTitleBaronyName(int index) {return "b_"+getBaronyName(index).toLowerCase().replace(" ", "_");}
	public void setDuchyName(String duchyName) {
		this.duchyName = duchyName;
		//Logger.log(this+" received duchy name "+duchyName);
		if (provinceName == null)
			setProvinceName(duchyName); //Also name everything like that
		}
	public String getDuchyName() {return duchyName == null ? "Duchy"+String.format("%04d", this.index) : duchyName;}
	public boolean hasDuchyName() {return duchyName != null;}
	public String getTitleDuchyName() {return "d_"+getDuchyName().toLowerCase().replace(" ", "_");}
	public void setKingdomName(String kingdomName) 
	{
		this.kingdomName = kingdomName;
		//Logger.log(this+" received kingdom name "+kingdomName);
	}
	public String getKingdomName() {return kingdomName == null ? "Kingdom"+String.format("%04d", this.index) : kingdomName;}
	public boolean hasKingdomName() {return kingdomName != null;}
	public String getTitleKingdomName() {return "k_"+getKingdomName().toLowerCase().replace(" ", "_");}
	public void setEmpireName(String empireName) 
	{
		this.empireName = empireName;
		//Logger.log(this+" received empire name "+empireName);
	}
	public String getEmpireName() {return empireName == null ? "Empire"+String.format("%04d", this.index) : empireName;}
	public boolean hasEmpireName() {return empireName != null;}
	public String getTitleEmpireName() {return "e_"+getEmpireName().toLowerCase().replace(" ", "_");}

	public List<Province> getIslandRegion() {return islandRegion;}
	public void setIslandRegion(List<Province> islandRegion) {
		this.islandRegion = islandRegion;
		islandRegion.add(this);
	}

	
	public int getNumSlots() {return numSlots;}
	public void setNumSlots(int numSlots) {this.numSlots = numSlots;}
	public boolean isWater() {return isWater;}
	public void setWater(boolean isWater) {this.isWater = isWater;}
	public boolean isRiver() {return isRiver;}
	public void setRiver(boolean isRiver) {this.isRiver = isRiver;}
	public boolean isWasteland() {return isWasteland;}
	public void setWasteland(boolean isWasteland) {this.isWasteland = isWasteland;}

}
