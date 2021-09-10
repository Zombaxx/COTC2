package ck2maptools.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;

import ck2maptools.utils.Config;
import ck2maptools.utils.Logger;

public class Culture {
	private String name;
	private String cultureGroup;

	private List<String> male_names;
	private List<String> female_names;

	private boolean founderNamedDynasties;
	private String fromDynastyPrefix;

	public String getName() {
		return name;
	}
	public String getCultureGroup() {
		return cultureGroup;
	}

	public List<String> getMaleNames() {
		return male_names;
	}

	public List<String> getFemaleNames() {
		return female_names;
	}

	public String getRandomName(boolean female) {
		if (female)
			return female_names.get((int) (Math.random() * female_names.size()));
		else
			return male_names.get((int) (Math.random() * male_names.size()));
	}

	public boolean isFounderNamedDynasties() {
		return founderNamedDynasties;
	}

	public String getFromDynastyPrefix() {
		return fromDynastyPrefix;
	}

	@Override
	public String toString() {
		return name;
	}

	private static HashMap<String, Culture> cultureMap;
	private static HashMap<String, List<Culture>> cultureGroupMap;

	public static Collection<Culture> getCultures() {
		if (Culture.cultureMap == null) {
			parseCultures();
		}

		return cultureMap.values();
	}

	public static Culture getCulture(String s) {
		if (Culture.cultureMap == null) {
			parseCultures();
		}

		return cultureMap.get(s);
	}
	
	public static Culture getRandomCultureFromGroup(String s) {
		if (Culture.cultureMap == null) {
			parseCultures();
		}
		
		List<Culture> list = Culture.cultureGroupMap.get(s);
		Culture c = null;
		
		if (list != null)
			c = list.get((int)(Math.random()*list.size()));
		
		return c;
	}
	
	public Culture(String name, String group) {
		if (Culture.cultureMap == null) {
			cultureMap = new HashMap<String, Culture>();
		}

		male_names = new ArrayList<String>();
		female_names = new ArrayList<String>();
		this.cultureGroup = group;

		this.name = name;
		this.fromDynastyPrefix = ""; //Default to just the name of the barony, without prefix, still needs to be initialized or it shows as "null"

		cultureMap.put(name, this);
	}

	public static void main(String[] args) {
		Config.parseConfig();
		parseCultures();
	}

	public static boolean parseCultures() {
		
		Logger.log("Creating cultures...");
		
		//BUGBUG: Create a dummy culture with an improbable name so that the province setup can at least work even if nothing else is right
		Culture defaultCulture = new Culture("uncultured_swine", "uncultured_group");
		defaultCulture.male_names.add("Adam");
		defaultCulture.female_names.add("Eve");
		
		File cultureFolder = new File(Config.MOD_FOLDER + "/common/cultures");

		if (!cultureFolder.exists()) {
			Logger.log(cultureFolder.getAbsolutePath() + " folder not found, ignored");
			return false;
		}

		if (cultureFolder.listFiles() == null) {
			Logger.log(cultureFolder.getAbsolutePath() + " folder empty, ignored");
			return false;
		}

		for (File cultureFile : cultureFolder.listFiles()) {
			Logger.log("Parsing " + cultureFile.getName() + "...");

			Scanner reader;
			try {
				reader = new Scanner(cultureFile);
			} catch (FileNotFoundException e) {
				//Should be impossible
				return false;
			}

			int lineNumber = 0;

			String currentCultureGroup = null;
			Culture currentCulture = null;
			int numOpenBracket = 0;
			boolean hasEquals = false;
			Stack<String> tokenStack = new Stack<String>();
			tokenStack.push("TOPLEVEL");

			while (reader.hasNext()) {
				String line = reader.nextLine();
				lineNumber++;
				
				//Insert some whitespaces around = { and } otherwise the regex won't work, even though it's technically correct for CK2...
				line = line.replaceAll("\\{", " { ");
				line = line.replaceAll("\\}", " } ");
				line = line.replaceAll("\\=", " = ");
				
				
				String[] tokens = line.split("\\s+(?=([^\"]*\"[^\"]*\")*[^\"]*$)"); // Split by whitespaces, disregarding those that are between quotes
				
				String previousToken = null;

				for (String token : tokens) {
					if (!token.isEmpty()) {
						// System.out.println(token);

						if (token.substring(0, 1).equals("#")) {
							// Comment, ignore remainder of the line
							break;
						}
						else if (token.equals("=")) {
							//System.out.println("Found = ");
							switch (tokenStack.peek()) {
							case "TOPLEVEL":
								// New culture group
								if (currentCultureGroup == null) {
									currentCultureGroup = previousToken;
									tokenStack.push("CULTURE_GROUP");
								}
								break;

							case "CULTURE_GROUP":
								//Treat those as normal (ignore them) 
								if (previousToken.equals("graphical_cultures") || previousToken.equals("alternate_start")) {
									tokenStack.push(previousToken);
								}
								else if (currentCulture == null) {
									// New culture in the group
									currentCulture = new Culture(previousToken, currentCultureGroup);
									Logger.log("Found culture group "+currentCultureGroup+"; culture " + currentCulture.getName());
									tokenStack.push("CULTURE");
								}
								break;
								
							default:
								tokenStack.push(previousToken.toLowerCase());
							}
							hasEquals = true;
						}
						else if (token.equals("{")) {

							//System.out.println("Starting " + tokenStack.peek());
							
							if (hasEquals)
							{
								numOpenBracket++;
								hasEquals = false;
							}
							else
							{
								Logger.log("ERROR Parsing cultures: found opening bracket without preceding '=' at line " + lineNumber);
								Logger.close();
								reader.close();
								return false;
							}
						} 
						else if (token.equals("}")) {
							
							//System.out.println("Ending " + tokenStack.peek());
							
							switch (tokenStack.peek())
							{
							default:
								if (numOpenBracket > 0)
								{
									numOpenBracket--;
									tokenStack.pop();
								}
								else
								{
									Logger.log("ERROR Parsing cultures: found closing bracket without matching opening bracket at line " + lineNumber);
									Logger.close();
									reader.close();
									return false;
								}
								break;

							case "TOPLEVEL":
								Logger.log("ERROR Parsing cultures: found closing bracket without matching opening bracket at line " + lineNumber);
								Logger.close();
								reader.close();
								return false;

							case "CULTURE_GROUP":
								currentCultureGroup = null;
								tokenStack.pop();
								break;

							case "CULTURE":
								currentCulture = null;
								tokenStack.pop();
								break;
							}
						}
						else {
							switch (tokenStack.peek()) {

							case "male_names":
								// Remove the part after the underscore if there is one
								token = token.split("_")[0];
								//System.out.println("Adding male name " + token);
								currentCulture.male_names.add(token);
								break;

							case "female_names":
								// Remove the part after the underscore if there is one
								token = token.split("_")[0];
								//System.out.println("Adding female name " + token);
								currentCulture.female_names.add(token);
								break;

							case "from_dynasty_prefix":
								currentCulture.fromDynastyPrefix = token.replace("\"", "");
								//System.out.println("Setting dynasty prefix " + currentCulture.fromDynastyPrefix);
								break;

							case "founder_named_dynasties":
								currentCulture.founderNamedDynasties = token.toLowerCase().equals("yes");
								//System.out.println("Setting founder -named dynasties : " + currentCulture.founderNamedDynasties);
								break;
							}
							
							previousToken = token;
							
							if (hasEquals) //We just met the right-hand side of an equals
							{
								hasEquals = false;
								tokenStack.pop(); //Remove the left-hand side of the equals from the stack
								previousToken = null;
							}
						} // end if
					} // end if
					
					//System.out.println("Token : " + tokenStack.peek());
				} // next token
			} // wend

			reader.close();
		} // next file
		


		Logger.log("Setting culture groups...");
		cultureGroupMap = new HashMap<String, List<Culture>>();

		for (Culture c : cultureMap.values())
		{
			List<Culture> list = cultureGroupMap.get(c.getCultureGroup());
			
			if (list == null)
			{
				list = new ArrayList<Culture>();
				cultureGroupMap.put(c.cultureGroup, list);
			}
			
			list.add(c);
		}
		
		return true;
	}

}
