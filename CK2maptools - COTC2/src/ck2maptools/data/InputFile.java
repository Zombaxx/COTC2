package ck2maptools.data;

import java.io.File;

public enum InputFile {
	
	Input("./input/input.bmp"),
	Climate("./input/climate.bmp"),
	Terrain("./output/map/terrain.bmp"),
	Rivers("./input/rivers.bmp"),
	Settlements("./input/settlements.bmp"),
	Provinces("./input/provinces.bmp"),
	DeJureD("./input/dejure_duchies.bmp"),
	DeJureK("./input/dejure_kingdoms.bmp"),
	DeJureE("./input/dejure_empires.bmp"),
	DeFactoC("./input/defacto_counties.bmp"),
	DeFactoD("./input/defacto_duchies.bmp"),
	DeFactoK("./input/defacto_kingdoms.bmp"),
	DeFactoE("./input/defacto_empires.bmp"),
	NumSlots("./input/numslots.bmp"),
	Technology("./input/technology.bmp"),
	Government("./input/government.bmp"),
	Cultures("./input/cultures.bmp"),
	Religions("./input/religions.bmp"), 
	Heights("./output/map/topology.bmp"),;
	
	private String fileName;
	
	private InputFile(String fileName)
	{
		this.fileName = fileName;
	}

	public String getFileName() {
		return fileName;
	}
	
	public boolean exists() {
		File f = new File (fileName);
		return f.exists();
	}
}
