package ck2maptools.data;

//A CK2 character as defined in character history
public class Character {
	private int charId;
	private String name;
	private int dynasty;
	private Culture culture;
	private String religion;
	private Character father;
	private Character mother;
	private Character spouse;
	private Province location;
	private int birthYear;
	private int deathYear;
	private boolean female;
	private boolean badass; //Gives the character really good stats and traits, used for long-dead dynasty founders

	
	public Character(int charId, String name, int dynasty, Province location, Culture culture, String religion, int birthYear,
			int deathYear, boolean female) {
		super();
		this.charId = charId;
		this.name = name;
		this.dynasty = dynasty;
		this.location = location;
		this.culture = culture;
		this.religion = religion;
		this.birthYear = birthYear;
		this.deathYear = deathYear;
		this.female = female;
		
		if (name.isEmpty())
			this.name = culture.getRandomName(female);
	}
	
	public boolean isFemale() {return female;}
	public void setFemale(boolean female) {this.female = female;}
	public int getCharId() {return charId;}
	public void setCharId(int charId) {this.charId = charId;}
	public String getName() {return name;}
	public void setName(String name) {this.name = name;}
	public int getDynasty() {return dynasty;}
	public void setDynasty(int dynasty) {this.dynasty = dynasty;}
	public Culture getCulture() {return culture;}
	public void setCulture(Culture culture) {this.culture = culture;}
	public String getReligion() {return religion;}
	public void setReligion(String religion) {this.religion = religion;}
	public Character getFather() {return father;}
	public void setFather(Character father) {this.father = father;}
	public Character getMother() {return mother;}
	public void setMother(Character mother) {this.mother = mother;}
	public Character getSpouse() {return spouse;}
	public void setSpouse(Character spouse) {
		this.spouse = spouse;	
		spouse.spouse = this;
	}
	public Province getLocation() {return location;}
	public void setLocation(Province location) {this.location = location;}
	public int getBirthYear() {return birthYear;}
	public void setBirthYear(int birthYear) {this.birthYear = birthYear;}
	public int getDeathYear() {return deathYear;}
	public void setDeathYear(int deathYear) {this.deathYear = deathYear;}
	public boolean isBadass() {return badass;}
	public void setBadass(boolean badass) {this.badass = badass;}
}
