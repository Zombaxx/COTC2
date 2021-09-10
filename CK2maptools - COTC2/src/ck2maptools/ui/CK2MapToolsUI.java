package ck2maptools.ui;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;

import ck2maptools.data.Loader;
import ck2maptools.main.CK2MakeAdjacencies;
import ck2maptools.main.CK2MakeColorMaps;
import ck2maptools.main.CK2MakeDeJureMaps;
import ck2maptools.main.CK2MakeProvinceSetup;
import ck2maptools.main.CK2MakeProvinceSlots;
import ck2maptools.main.CK2MakeProvincesMap;
import ck2maptools.main.CK2MakeRiversMap;
import ck2maptools.main.CK2MakeSettlements;
import ck2maptools.main.CK2MakeTerrainMap;
import ck2maptools.main.CK2MakeTradeRoutes;
import ck2maptools.main.CK2MapReverse;
import ck2maptools.main.CK2MapToolsMain;
import ck2maptools.main.ICK2MapTool;
import ck2maptools.utils.Config;
import ck2maptools.utils.Logger;

public class CK2MapToolsUI extends JFrame implements PropertyChangeListener, ActionListener, WindowListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private UIState ui_state;
	private JPanel contentPane;
	private JButton btnMakeTerrain;
	private JButton btnMakeProvinces;
	private JButton btnMakeSetup;
	private JButton btnUtilities;
	private JButton btnGo;
	private JPanel panelOptions;
	private JPanel panelOptionsTerrain;
	private JPanel panelOptionsProvinces;
	private JPanel panelOptionsSetup;
	private JPanel panelOptionsUtilities;
	
	private JFormattedTextField textFieldHillsHeight;
	private JFormattedTextField textFieldMountainHeight;
	private JFormattedTextField textFieldSnowHeight;
	private JFormattedTextField textFieldPeakHeight;
	private JFormattedTextField textFieldInputScale;
	private JFormattedTextField textFieldTreeScale;
	private JFormattedTextField textFieldNoisePatchSize;
	private JFormattedTextField textFieldNoiseFactorMax;
	private JFormattedTextField textFieldNoiseBaseline;
	private JFormattedTextField textFieldSmoothRadius;
	private JTextField textFieldMaxTerrainHeight;
	private JFormattedTextField textFieldSettlementsMinDistance;
	private JFormattedTextField textFieldSeanodeMinDistance;
	private JFormattedTextField textFieldStraitDistance;
	private JFormattedTextField textFieldStartyear;
	private JFormattedTextField textFieldPercentfemale;
	private JFormattedTextField textFieldModfolder;
	
	private JCheckBox chkBxMakeTerrain;
	private JCheckBox chkBxFastMode;
	private JCheckBox chkBxMakeRivers;
	private JCheckBox chkBxMakeTerrainColors;
	private JCheckBox chkBxMakeSettlements;
	private JCheckBox chkBxMakeProvinces;
	private JCheckBox chkBxRecolorOnly;
	private JCheckBox chkBxMakeDeJureD;
	private JCheckBox chkBxMakeDeJureK;
	private JCheckBox chkBxMakeDeJureE;
	private JCheckBox chkBxCalculateHoldingSlots;
	private JCheckBox chkBxLocalisationTemplate;
	private JCheckBox chckbxIncludeBaronies;
	private JCheckBox chkBxMakeAdjacencies;
	private JCheckBox chkBxTechnology;
	private JCheckBox chkBxNoWater;
	private JCheckBox chkBxTradeRoutes;
	private JCheckBox chkbxMakeProvinceSetup;
	private JCheckBox chkbxMakeDeJureTemplate;
	private JCheckBox chkbxGenerateSeaNodes;
	private JRadioButton rdbtnReverseEngineer;
	private JProgressBar progressBar;
	private JButton btnBrowsefiles;
	private JFileChooser fileChooser;

	private long ms;





	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		CK2MapToolsMain.main(args);
	}

	/**
	 * Create the frame.
	 */
	public CK2MapToolsUI() {
		setTitle("CK2MapTools");
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		addWindowListener(this);
		setBounds(100, 100, 640, 495);
		ui_state = UIState.MAKE_TERRAIN;
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		progressBar = new JProgressBar();
		progressBar.setFont(new Font("Tahoma", Font.BOLD, 20));
		progressBar.setStringPainted(true);
		progressBar.setValue(50);
		progressBar.setToolTipText("Progress");
		progressBar.setBounds(10, 375, 614, 80);
		progressBar.setVisible(false);
		contentPane.add(progressBar);
		
		
		
		btnMakeTerrain = new JButton("Make Terrain");
		btnMakeTerrain.setToolTipText("Make the terrain, topology, rivers...");
		btnMakeTerrain.addActionListener(this);
		btnMakeTerrain.setFont(new Font("Dialog", Font.BOLD, 20));
		btnMakeTerrain.setBounds(10, 11, 200, 80);
		contentPane.add(btnMakeTerrain);
		
		btnMakeProvinces = new JButton("Make Provinces");
		btnMakeProvinces.setToolTipText("Make the Provinces, Adjacencies, De Jure borders and Holding slots.");
		btnMakeProvinces.addActionListener(this); 
		btnMakeProvinces.setFont(new Font("Dialog", Font.BOLD, 20));
		btnMakeProvinces.setBounds(10, 102, 200, 80);
		contentPane.add(btnMakeProvinces);
		
		btnMakeSetup = new JButton("Make Setup");
		btnMakeSetup.setToolTipText("Make Province History, generate Characters ...");
		btnMakeSetup.addActionListener(this); 
		btnMakeSetup.setFont(new Font("Dialog", Font.BOLD, 20));
		btnMakeSetup.setBounds(10, 193, 200, 80);
		contentPane.add(btnMakeSetup);
		
		btnUtilities = new JButton("Utilities");
		btnUtilities.addActionListener(this); 
		btnUtilities.setFont(new Font("Dialog", Font.BOLD, 20));
		btnUtilities.setBounds(10, 284, 200, 80);
		contentPane.add(btnUtilities);
		
		btnGo = new JButton("GO!");
		btnGo.addActionListener(this); 
		btnGo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
		btnGo.setBounds(10, 375, 614, 80);
		contentPane.add(btnGo);
		
		panelOptions = new JPanel();
		panelOptions.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		panelOptions.setBounds(220, 11, 404, 353);
		contentPane.add(panelOptions);
		panelOptions.setLayout(new CardLayout(0, 0));
		
		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		
		panelOptionsTerrain = new JPanel();
		panelOptions.add(panelOptionsTerrain, "panelOptionsTerrain");
		panelOptionsTerrain.setLayout(null);
		
		textFieldInputScale = new JFormattedTextField();
		textFieldInputScale.setFont(new Font("Tahoma", Font.PLAIN, 11));
		textFieldInputScale.addPropertyChangeListener("value", this);
		textFieldInputScale.setValue(Config.INPUT_MAP_SCALE);
		textFieldInputScale.setToolTipText("Scale of the provinces map compared to the input map. Defaults to 2, meaning the provinces map will be twice as large as the input map.");
		textFieldInputScale.setColumns(3);
		textFieldInputScale.setBounds(6, 11, 30, 20);
		panelOptionsTerrain.add(textFieldInputScale);
		
		JLabel labelInputScale = new JLabel("Input Map Scale");
		labelInputScale.setFont(new Font("Tahoma", Font.PLAIN, 11));
		labelInputScale.setBounds(46, 14, 87, 14);
		panelOptionsTerrain.add(labelInputScale);
		
		textFieldTreeScale = new JFormattedTextField();
		textFieldTreeScale.setFont(new Font("Tahoma", Font.PLAIN, 11));
		textFieldTreeScale.addPropertyChangeListener("value", this);
		textFieldTreeScale.setToolTipText("Scale of the tree map compared to the provinces map. Defaults to 6, meaning the provinces map is 6 times larger than the tree map. A lower number will create more trees in-game, possibly hampering performance.");
		textFieldTreeScale.setValue(Config.TREE_MAP_SCALE);
		textFieldTreeScale.setColumns(3);
		textFieldTreeScale.setBounds(173, 11, 30, 20);
		panelOptionsTerrain.add(textFieldTreeScale);
		
		JLabel labelTreeScale = new JLabel("Tree Map Scale");
		labelTreeScale.setFont(new Font("Tahoma", Font.PLAIN, 11));
		labelTreeScale.setBounds(213, 14, 87, 14);
		panelOptionsTerrain.add(labelTreeScale);
		
		chkBxMakeTerrain = new JCheckBox("Make Terrain");
		chkBxMakeTerrain.setSelected(true);
		chkBxMakeTerrain.setToolTipText("Creates terrain.bmp, toplogy.bmp, trees.bmp and world_normal_height.bmp.");
		chkBxMakeTerrain.setFont(new Font("Tahoma", Font.BOLD, 11));
		chkBxMakeTerrain.setBounds(6, 38, 101, 23);
		panelOptionsTerrain.add(chkBxMakeTerrain);
		
		chkBxFastMode = new JCheckBox("Fast Mode");
		chkBxFastMode.setFont(new Font("Tahoma", Font.PLAIN, 11));
		chkBxFastMode.setToolTipText("Much faster but very ugly. For testing purposes only.");
		chkBxFastMode.setBounds(112, 38, 75, 23);
		panelOptionsTerrain.add(chkBxFastMode);
		
		JLabel lblHeights = new JLabel("Heights");
		lblHeights.setFont(new Font("Tahoma", Font.PLAIN, 11));
		lblHeights.setBounds(16, 65, 46, 14);
		panelOptionsTerrain.add(lblHeights);
		
		textFieldHillsHeight = new JFormattedTextField();
		textFieldHillsHeight.setValue(Config.HILLS_HEIGHT);
		textFieldHillsHeight.addPropertyChangeListener("value", this);
		textFieldHillsHeight.setFont(new Font("Tahoma", Font.PLAIN, 11));
		textFieldHillsHeight.setBounds(16, 87, 50, 20);
		panelOptionsTerrain.add(textFieldHillsHeight);
		textFieldHillsHeight.setColumns(3);
		
		JLabel lblHillsHeight = new JLabel("Hills");
		lblHillsHeight.setFont(new Font("Tahoma", Font.PLAIN, 11));
		lblHillsHeight.setBounds(76, 90, 87, 14);
		panelOptionsTerrain.add(lblHillsHeight);
		
		textFieldMountainHeight = new JFormattedTextField();
		textFieldMountainHeight.setValue(Config.MOUNTAIN_HEIGHT);
		textFieldMountainHeight.addPropertyChangeListener("value", this);
		textFieldMountainHeight.setFont(new Font("Tahoma", Font.PLAIN, 11));
		textFieldMountainHeight.setColumns(3);
		textFieldMountainHeight.setBounds(16, 118, 50, 20);
		panelOptionsTerrain.add(textFieldMountainHeight);
		
		JLabel lblMountainHeight = new JLabel("Mountains");
		lblMountainHeight.setFont(new Font("Tahoma", Font.PLAIN, 11));
		lblMountainHeight.setBounds(76, 121, 87, 14);
		panelOptionsTerrain.add(lblMountainHeight);
		
		textFieldSnowHeight = new JFormattedTextField();
		textFieldSnowHeight.setValue(Config.SNOW_HEIGHT);
		textFieldSnowHeight.addPropertyChangeListener("value", this);
		textFieldSnowHeight.setFont(new Font("Tahoma", Font.PLAIN, 11));
		textFieldSnowHeight.setColumns(3);
		textFieldSnowHeight.setBounds(16, 149, 50, 20);
		panelOptionsTerrain.add(textFieldSnowHeight);
		
		JLabel lblSnowHeight = new JLabel("Snow");
		lblSnowHeight.setFont(new Font("Tahoma", Font.PLAIN, 11));
		lblSnowHeight.setBounds(76, 152, 87, 14);
		panelOptionsTerrain.add(lblSnowHeight);
		
		textFieldPeakHeight = new JFormattedTextField();
		textFieldPeakHeight.setValue(Config.PEAK_HEIGHT);
		textFieldPeakHeight.addPropertyChangeListener("value", this);
		textFieldPeakHeight.setFont(new Font("Tahoma", Font.PLAIN, 11));
		textFieldPeakHeight.setColumns(3);
		textFieldPeakHeight.setBounds(16, 180, 50, 20);
		panelOptionsTerrain.add(textFieldPeakHeight);
		
		JLabel lblPeakHeight = new JLabel("Peak");
		lblPeakHeight.setFont(new Font("Tahoma", Font.PLAIN, 11));
		lblPeakHeight.setBounds(76, 183, 87, 14);
		panelOptionsTerrain.add(lblPeakHeight);
		
		JLabel lblRandomNoise = new JLabel("Random Noise");
		lblRandomNoise.setFont(new Font("Tahoma", Font.PLAIN, 11));
		lblRandomNoise.setBounds(142, 68, 97, 14);
		panelOptionsTerrain.add(lblRandomNoise);
		
		textFieldNoisePatchSize = new JFormattedTextField();
		textFieldNoisePatchSize.setFont(new Font("Tahoma", Font.PLAIN, 11));
		textFieldNoisePatchSize.addPropertyChangeListener("value", this);
		textFieldNoisePatchSize.setValue(Config.NOISE_PATCH_SIZE);
		textFieldNoisePatchSize.setToolTipText("A higher patch size will make the terrain more uniform. A lower size will increase bumpiness.");
		textFieldNoisePatchSize.setColumns(3);
		textFieldNoisePatchSize.setBounds(142, 87, 50, 20);
		panelOptionsTerrain.add(textFieldNoisePatchSize);
		
		JLabel labelNoisePatchSize = new JLabel("Patch Size");
		labelNoisePatchSize.setFont(new Font("Tahoma", Font.PLAIN, 11));
		labelNoisePatchSize.setBounds(202, 90, 87, 14);
		panelOptionsTerrain.add(labelNoisePatchSize);
		
		textFieldNoiseFactorMax = new JFormattedTextField();
		textFieldNoiseFactorMax.setFont(new Font("Tahoma", Font.PLAIN, 11));
		textFieldNoiseFactorMax.addPropertyChangeListener("value", this);
		textFieldNoiseFactorMax.setValue(Config.NOISE_FACTOR_MAX);
		textFieldNoiseFactorMax.setToolTipText("A higher factor will increase height variations, especially on higher terrain.");
		textFieldNoiseFactorMax.setColumns(4);
		textFieldNoiseFactorMax.setBounds(142, 118, 50, 20);
		panelOptionsTerrain.add(textFieldNoiseFactorMax);
		
		JLabel labelNoiseFactorMax = new JLabel("Factor");
		labelNoiseFactorMax.setFont(new Font("Tahoma", Font.PLAIN, 11));
		labelNoiseFactorMax.setBounds(202, 121, 87, 14);
		panelOptionsTerrain.add(labelNoiseFactorMax);
		
		textFieldNoiseBaseline = new JFormattedTextField();
		textFieldNoiseBaseline.setFont(new Font("Tahoma", Font.PLAIN, 11));
		textFieldNoiseBaseline.addPropertyChangeListener("value", this);
		textFieldNoiseBaseline.setValue(Config.NOISE_BASELINE);
		textFieldNoiseBaseline.setToolTipText("A higher baseline will increase height variations. This effect will be equally pronounced on low or high terrain. Especially useful if you don't want too flat plains.");
		textFieldNoiseBaseline.setColumns(3);
		textFieldNoiseBaseline.setBounds(142, 149, 50, 20);
		panelOptionsTerrain.add(textFieldNoiseBaseline);
		
		JLabel labelNoiseBaseline = new JLabel("Baseline");
		labelNoiseBaseline.setFont(new Font("Tahoma", Font.PLAIN, 11));
		labelNoiseBaseline.setBounds(202, 152, 87, 14);
		panelOptionsTerrain.add(labelNoiseBaseline);
		
		textFieldMaxTerrainHeight = new JTextField();
		textFieldMaxTerrainHeight.setFont(new Font("Tahoma", Font.PLAIN, 11));
		updateTextFieldMaxTerrainHeight();
		textFieldMaxTerrainHeight.setEditable(false);
		textFieldMaxTerrainHeight.setColumns(3);
		textFieldMaxTerrainHeight.setBounds(16, 211, 50, 20);
		panelOptionsTerrain.add(textFieldMaxTerrainHeight);
		
		JLabel lblMaxTerrainHeight = new JLabel("Max Terrain Height (try to make it ~255)");
		lblMaxTerrainHeight.setFont(new Font("Tahoma", Font.PLAIN, 11));
		lblMaxTerrainHeight.setBounds(76, 214, 199, 14);
		panelOptionsTerrain.add(lblMaxTerrainHeight);
		
		textFieldSmoothRadius = new JFormattedTextField();
		textFieldSmoothRadius.setFont(new Font("Tahoma", Font.PLAIN, 11));
		textFieldSmoothRadius.addPropertyChangeListener("value", this);
		textFieldSmoothRadius.setValue(Config.SMOOTH_RADIUS);
		textFieldSmoothRadius.setToolTipText("A higher number means smoother, flatter terrain, but also more time to process. Low numbers will lead to steep transitions between mountain types.");
		textFieldSmoothRadius.setColumns(3);
		textFieldSmoothRadius.setBounds(193, 39, 30, 20);
		panelOptionsTerrain.add(textFieldSmoothRadius);
		
		JLabel labelSmoothRadius = new JLabel("Smooth Radius");
		labelSmoothRadius.setFont(new Font("Tahoma", Font.PLAIN, 11));
		labelSmoothRadius.setBounds(233, 42, 87, 14);
		panelOptionsTerrain.add(labelSmoothRadius);
		
		chkBxMakeRivers = new JCheckBox("Make Rivers");
		chkBxMakeRivers.setSelected(true);
		chkBxMakeRivers.setToolTipText("Creates rivers.bmp");
		chkBxMakeRivers.setFont(new Font("Tahoma", Font.BOLD, 11));
		chkBxMakeRivers.setBounds(6, 238, 97, 23);
		panelOptionsTerrain.add(chkBxMakeRivers);
		
		chkBxMakeTerrainColors = new JCheckBox("Make Terrain Colors");
		chkBxMakeTerrainColors.setSelected(true);
		chkBxMakeTerrainColors.setToolTipText("Creates the images giving terrain and water their colors in-game. Climate and terrain type affect the result, with some randomness added to break the uniformity. Colors are based on samples from the base game.");
		chkBxMakeTerrainColors.setFont(new Font("Tahoma", Font.BOLD, 11));
		chkBxMakeTerrainColors.setBounds(6, 264, 203, 23);
		panelOptionsTerrain.add(chkBxMakeTerrainColors);
		
		chkBxMakeSettlements = new JCheckBox("Fill Settlements Map");
		chkBxMakeSettlements.setToolTipText("Automatically fills the settlements.bmp with randomly placed settlements. Each \"settlement\" will create a province around it.");
		chkBxMakeSettlements.setFont(new Font("Tahoma", Font.BOLD, 11));
		chkBxMakeSettlements.setBounds(6, 290, 141, 23);
		panelOptionsTerrain.add(chkBxMakeSettlements);
		
		textFieldSettlementsMinDistance = new JFormattedTextField();
		textFieldSettlementsMinDistance.setFont(new Font("Tahoma", Font.PLAIN, 11));
		textFieldSettlementsMinDistance.addPropertyChangeListener("value", this);
		textFieldSettlementsMinDistance.setValue(Config.MIN_SETTLEMENT_DISTANCE);
		textFieldSettlementsMinDistance.setToolTipText("The minimum distance between 2 settlements when using the filling tool. A greater number will generate larger provinces.");
		textFieldSettlementsMinDistance.setColumns(3);
		textFieldSettlementsMinDistance.setBounds(153, 291, 50, 20);
		panelOptionsTerrain.add(textFieldSettlementsMinDistance);
		
		JLabel labelSettlementsMindistance = new JLabel("Min Distance between Settlements");
		labelSettlementsMindistance.setFont(new Font("Tahoma", Font.PLAIN, 11));
		labelSettlementsMindistance.setBounds(213, 294, 177, 14);
		panelOptionsTerrain.add(labelSettlementsMindistance);
		
		textFieldSeanodeMinDistance = new JFormattedTextField();
		textFieldSeanodeMinDistance.setFont(new Font("Tahoma", Font.PLAIN, 11));
		textFieldSeanodeMinDistance.addPropertyChangeListener("value", this);
		textFieldSeanodeMinDistance.setValue(Config.MIN_SEA_NODE_DISTANCE);
		textFieldSeanodeMinDistance.setToolTipText("The minimum distance between 2 sea nodes when using the filling tool. A greater number will generate larger sea zones.");
		textFieldSeanodeMinDistance.setColumns(3);
		textFieldSeanodeMinDistance.setBounds(153, 316, 50, 20);
		panelOptionsTerrain.add(textFieldSeanodeMinDistance);
		
		JLabel labelSeanodeMindistance = new JLabel("Min Distance between Sea nodes");
		labelSeanodeMindistance.setFont(new Font("Tahoma", Font.PLAIN, 11));
		labelSeanodeMindistance.setBounds(213, 319, 177, 14);
		panelOptionsTerrain.add(labelSeanodeMindistance);
		
		chkbxGenerateSeaNodes = new JCheckBox("Generate sea nodes");
		chkbxGenerateSeaNodes.setFont(new Font("Tahoma", Font.PLAIN, 11));
		chkbxGenerateSeaNodes.setBounds(16, 315, 123, 23);
		panelOptionsTerrain.add(chkbxGenerateSeaNodes);
		
		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		
		panelOptionsProvinces = new JPanel();
		panelOptions.add(panelOptionsProvinces, "panelOptionsProvinces");
		panelOptionsProvinces.setLayout(null);
		
		chkBxMakeProvinces = new JCheckBox("Make Provinces");
		chkBxMakeProvinces.setSelected(true);
		chkBxMakeProvinces.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
		chkBxMakeProvinces.setBounds(6, 7, 120, 23);
		panelOptionsProvinces.add(chkBxMakeProvinces);
		
		chkBxRecolorOnly = new JCheckBox("Recolor only");
		chkBxRecolorOnly.setFont(new Font("Tahoma", Font.PLAIN, 11));
		chkBxRecolorOnly.setBounds(128, 8, 97, 23);
		panelOptionsProvinces.add(chkBxRecolorOnly);
		
		chkBxMakeDeJureD = new JCheckBox("Make De Jure Duchies");
		chkBxMakeDeJureD.setFont(new Font("Tahoma", Font.PLAIN, 11));
		chkBxMakeDeJureD.setBounds(16, 130, 143, 23);
		panelOptionsProvinces.add(chkBxMakeDeJureD);
		
		chkBxMakeDeJureK = new JCheckBox("Make De Jure Kingdoms");
		chkBxMakeDeJureK.setFont(new Font("Tahoma", Font.PLAIN, 11));
		chkBxMakeDeJureK.setBounds(16, 156, 143, 23);
		panelOptionsProvinces.add(chkBxMakeDeJureK);
		
		chkBxMakeDeJureE = new JCheckBox("Make De Jure Empires");
		chkBxMakeDeJureE.setFont(new Font("Tahoma", Font.PLAIN, 11));
		chkBxMakeDeJureE.setBounds(16, 182, 143, 23);
		panelOptionsProvinces.add(chkBxMakeDeJureE);
		
		chkBxCalculateHoldingSlots = new JCheckBox("Calculate Holding Slots");
		chkBxCalculateHoldingSlots.setSelected(true);
		chkBxCalculateHoldingSlots.setFont(new Font("Tahoma", Font.BOLD, 11));
		chkBxCalculateHoldingSlots.setBounds(6, 233, 179, 23);
		panelOptionsProvinces.add(chkBxCalculateHoldingSlots);
		
		chkBxMakeAdjacencies = new JCheckBox("Make Adjacencies");
		chkBxMakeAdjacencies.setSelected(true);
		chkBxMakeAdjacencies.setFont(new Font("Tahoma", Font.BOLD, 11));
		chkBxMakeAdjacencies.setBounds(6, 59, 127, 23);
		panelOptionsProvinces.add(chkBxMakeAdjacencies);
		
		textFieldStraitDistance = new JFormattedTextField();
		textFieldStraitDistance.setFont(new Font("Tahoma", Font.PLAIN, 11));
		textFieldStraitDistance.addPropertyChangeListener("value", this);
		textFieldStraitDistance.setValue(Config.MAX_STRAIT_DISTANCE);
		textFieldStraitDistance.setColumns(3);
		textFieldStraitDistance.setBounds(136, 60, 30, 20);
		panelOptionsProvinces.add(textFieldStraitDistance);
		
		JLabel lblMaximumStraitDistance = new JLabel("Maximum Strait Distance");
		lblMaximumStraitDistance.setFont(new Font("Tahoma", Font.PLAIN, 11));
		lblMaximumStraitDistance.setBounds(176, 63, 132, 14);
		panelOptionsProvinces.add(lblMaximumStraitDistance);
		
		chkBxNoWater = new JCheckBox("Only Land");
		chkBxNoWater.setFont(new Font("Tahoma", Font.PLAIN, 11));
		chkBxNoWater.setToolTipText("The program should not generate water provinces (will copy from existing map if possible)");
		chkBxNoWater.setBounds(227, 8, 97, 23);
		panelOptionsProvinces.add(chkBxNoWater);
		
		chkbxMakeDeJureTemplate = new JCheckBox("Make/Update De Jure Template");
		chkbxMakeDeJureTemplate.setSelected(true);
		chkbxMakeDeJureTemplate.setFont(new Font("Tahoma", Font.BOLD, 11));
		chkbxMakeDeJureTemplate.setBounds(6, 104, 231, 23);
		panelOptionsProvinces.add(chkbxMakeDeJureTemplate);
		
		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		
		panelOptionsSetup = new JPanel();
		panelOptions.add(panelOptionsSetup, "panelOptionsSetup");
		panelOptionsSetup.setLayout(null);
		
		textFieldStartyear = new JFormattedTextField();
		textFieldStartyear.setFont(new Font("Tahoma", Font.PLAIN, 11));
		textFieldStartyear.addPropertyChangeListener("value", this);
		textFieldStartyear.setColumns(4);
		textFieldStartyear.setValue(Config.START_DATE);
		textFieldStartyear.setBounds(10, 11, 60, 20);
		panelOptionsSetup.add(textFieldStartyear);
		
		JLabel lblStartYear = new JLabel("Start Year");
		lblStartYear.setBounds(80, 14, 80, 14);
		panelOptionsSetup.add(lblStartYear);
		
		textFieldModfolder = new JFormattedTextField();
		textFieldModfolder.setFont(new Font("Tahoma", Font.PLAIN, 11));
		textFieldModfolder.addPropertyChangeListener("value", this);
		textFieldModfolder.setValue(Config.MOD_FOLDER);
		textFieldModfolder.setBounds(10, 67, 347, 20);
		panelOptionsSetup.add(textFieldModfolder);
		
		fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		try {
			File fcTest1 = new File(Config.MOD_FOLDER);
			File fcTest2 = new File("C:/Program Files (x86)/Steam/Steamapps/Common/Crusader Kings II");
			File fcTest3 = new File("D:/Program Files (x86)/Steam/Steamapps/Common/Crusader Kings II");
			
			if (fcTest1.exists())
				fileChooser.setCurrentDirectory(fcTest1);
			else if (fcTest2.exists())
			{
				fileChooser.setCurrentDirectory(fcTest2);
				textFieldModfolder.setValue(fcTest2.getCanonicalPath());
			}
			else if (fcTest3.exists())
			{
				fileChooser.setCurrentDirectory(fcTest3);
				textFieldModfolder.setValue(fcTest3.getCanonicalPath());
			}
		}
		catch (IOException e)
		{
			//Meh...
		}
		
		
		btnBrowsefiles = new JButton("...");
		btnBrowsefiles.setFont(new Font("Tahoma", Font.BOLD, 11));
		btnBrowsefiles.setBounds(363, 66, 27, 23);
		btnBrowsefiles.addActionListener(this); 
		panelOptionsSetup.add(btnBrowsefiles);
		
		JLabel lblModOrGame = new JLabel("Mod or Game Folder:");
		lblModOrGame.setBounds(10, 42, 167, 14);
		panelOptionsSetup.add(lblModOrGame);
		
		chkBxTechnology = new JCheckBox("Technology");
		chkBxTechnology.setSelected(true);
		chkBxTechnology.setFont(new Font("Tahoma", Font.PLAIN, 11));
		chkBxTechnology.setBounds(10, 124, 97, 23);
		panelOptionsSetup.add(chkBxTechnology);
		
		textFieldPercentfemale = new JFormattedTextField();
		textFieldPercentfemale.setColumns(2);
		textFieldPercentfemale.setFont(new Font("Tahoma", Font.PLAIN, 11));
		textFieldPercentfemale.addPropertyChangeListener("value", this);
		textFieldPercentfemale.setValue(Config.PERCENT_FEMALE);
		textFieldPercentfemale.setBounds(10, 184, 27, 20);
		panelOptionsSetup.add(textFieldPercentfemale);
		
		JLabel lblOfFemale = new JLabel("% of female rulers");
		lblOfFemale.setFont(new Font("Tahoma", Font.PLAIN, 11));
		lblOfFemale.setBounds(47, 187, 97, 14);
		panelOptionsSetup.add(lblOfFemale);
		
		chkBxTradeRoutes = new JCheckBox("Trade Routes");
		chkBxTradeRoutes.setSelected(true);
		chkBxTradeRoutes.setFont(new Font("Tahoma", Font.PLAIN, 11));
		chkBxTradeRoutes.setBounds(10, 154, 97, 23);
		panelOptionsSetup.add(chkBxTradeRoutes);
		
		chkbxMakeProvinceSetup = new JCheckBox("Make Province Setup (unused since Holy Fury)");
		chkbxMakeProvinceSetup.setFont(new Font("Tahoma", Font.PLAIN, 11));
		chkbxMakeProvinceSetup.setBounds(10, 211, 247, 23);
		panelOptionsSetup.add(chkbxMakeProvinceSetup);
		

		chkBxLocalisationTemplate = new JCheckBox("Localisation Template");
		chkBxLocalisationTemplate.setSelected(true);
		chkBxLocalisationTemplate.setFont(new Font("Tahoma", Font.PLAIN, 11));
		chkBxLocalisationTemplate.setBounds(10, 94, 150, 23);
		panelOptionsSetup.add(chkBxLocalisationTemplate);
		
		chckbxIncludeBaronies = new JCheckBox("Include Unnamed Baronies");
		chckbxIncludeBaronies.setSelected(true);
		chckbxIncludeBaronies.setFont(new Font("Tahoma", Font.PLAIN, 11));
		chckbxIncludeBaronies.setBounds(162, 94, 195, 23);
		panelOptionsSetup.add(chckbxIncludeBaronies);

		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		
		panelOptionsUtilities = new JPanel();
		panelOptions.add(panelOptionsUtilities, "panelOptionsUtilities");
		panelOptionsUtilities.setLayout(null);
		
		rdbtnReverseEngineer = new JRadioButton("Reverse Engineer Settlements");
		rdbtnReverseEngineer.setSelected(true);
		rdbtnReverseEngineer.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
		rdbtnReverseEngineer.setBounds(6, 7, 298, 23);
		panelOptionsUtilities.add(rdbtnReverseEngineer);
	}
	
	private void updateTextFieldMaxTerrainHeight() {
		if (textFieldMaxTerrainHeight==null)
			return;
		Integer i = new Integer((int)(Config.PEAK_HEIGHT + (Config.PEAK_HEIGHT-96+Config.NOISE_BASELINE) * Config.NOISE_FACTOR_MAX));
		textFieldMaxTerrainHeight.setText(i.toString());
	}

	private void start() {
		setEnabledAll(false);
		progressBar.setString("Starting...");
		progressBar.setValue(0);
		progressBar.setVisible(true);
		btnGo.setVisible(false);
		
		Config.saveConfig();

		ms = System.currentTimeMillis();
		
		List<ICK2MapTool> toolbox = new ArrayList<ICK2MapTool>();
		
		switch(ui_state)
		{
		case MAKE_TERRAIN:
			if (chkBxMakeTerrain.isSelected())
			{
				CK2MakeTerrainMap t = new CK2MakeTerrainMap();
				t.setParamFastMode(chkBxFastMode.isSelected());
				
				toolbox.add(t);
			}
			if (chkBxMakeRivers.isSelected())
			{
				toolbox.add(new CK2MakeRiversMap());
			}
			if (chkBxMakeTerrainColors.isSelected())
			{
				toolbox.add(new CK2MakeColorMaps());
			}
			if (chkBxMakeSettlements.isSelected() || chkbxGenerateSeaNodes.isSelected())
			{
				CK2MakeSettlements t = new CK2MakeSettlements();
				t.setParamMakeLand(chkBxMakeSettlements.isSelected());
				t.setParamMakeWater(chkbxGenerateSeaNodes.isSelected());
				toolbox.add(t);
			}
			
			break;
		case MAKE_PROVINCES:

			if (chkBxMakeProvinces.isSelected())
			{
				CK2MakeProvincesMap t = new CK2MakeProvincesMap();

				t.setParamRecolorMode(chkBxRecolorOnly.isSelected());
				t.setParamDoWater(!chkBxNoWater.isSelected());
				
				toolbox.add(t);
			}
			if (chkBxMakeAdjacencies.isSelected() )
			{
				toolbox.add(new CK2MakeAdjacencies());
			}
			if (chkbxMakeDeJureTemplate.isSelected() || chkBxMakeDeJureD.isSelected() || chkBxMakeDeJureK.isSelected() || chkBxMakeDeJureE.isSelected())
			{
				CK2MakeDeJureMaps t = new CK2MakeDeJureMaps();
				
				t.setParamMakeDuchies(chkBxMakeDeJureD.isSelected());
				t.setParamMakeKingdoms(chkBxMakeDeJureK.isSelected());
				t.setParamMakeEmpires(chkBxMakeDeJureE.isSelected());
				
				toolbox.add(t);
			}
			if (chkBxCalculateHoldingSlots.isSelected())
			{
				toolbox.add(new CK2MakeProvinceSlots());
			}

			break;
		case MAKE_SETUP:
	
			{
				CK2MakeProvinceSetup t = new CK2MakeProvinceSetup();
			
					t.setParamMakeLocalisationTemplate(chkBxLocalisationTemplate.isSelected());
					t.setParamAddEmptyBaroniesToLocTemplate(chckbxIncludeBaronies.isSelected());
					t.setParamMakeTechnology(chkBxTechnology.isSelected());
					t.setParamMakeOldProvinceSetup(chkbxMakeProvinceSetup.isSelected());
					toolbox.add(t);
			}
			if (chkBxTradeRoutes.isSelected())
			{
				toolbox.add(new CK2MakeTradeRoutes());
			}

			break;
		case UTILITIES:

			if (rdbtnReverseEngineer.isSelected())
			{
				toolbox.add(new CK2MapReverse());
			}

			break;
		default:
			//Hmmm ?
			break;
			
		}
		
		Logger.registerUI(this);
		Runnable worker = new CK2MapToolsWorker(this, toolbox);
		Thread thread = new Thread(worker);
		thread.start();

	}

	public void end(int returnCode ) {
		
		Logger.registerUI(null);
		progressBar.setVisible(false);
		btnGo.setVisible(true);
		
		if (returnCode == ICK2MapTool.ERROR_NONE)
			infoMessage("Completed without errors in "+((System.currentTimeMillis()-ms)*0.001)+"s");
		else if (returnCode == -1)
		{
			//A major error has occured, an error message was already displayed to the user
		}
		else
		{
			StringBuilder message = new StringBuilder("Completed with minor errors in "+((System.currentTimeMillis()-ms)*0.001)+"s");
			if ((returnCode & ICK2MapTool.ERROR_FILLING_PROVINCE) != 0)
			{
				message.append("\r\nErrors while filling provinces.");
			}
			if ((returnCode & ICK2MapTool.ERROR_LOCALISATION) != 0)
			{
				message.append("\r\nErrors in localisation.");
			}
			if ((returnCode & ICK2MapTool.ERROR_RIVERS) != 0)
			{
				message.append("\r\nErrors in rivers.");
			}
			if ((returnCode & ICK2MapTool.ERROR_TRADE_ROUTES) != 0)
			{
				message.append("\r\nErrors in trade routes.");
			}
			message.append("\r\nCheck the logs for more details.");
			warningMessage(message.toString());
		}
		
		Loader.unload();
		
		setEnabledAll(true);
	}
	
	private void setEnabledAll(boolean b) {
		for (Component comp : panelOptionsTerrain.getComponents())
		{
			comp.setEnabled(b);
		}
		for (Component comp : panelOptionsProvinces.getComponents())
		{
			comp.setEnabled(b);
		}
		for (Component comp : panelOptionsSetup.getComponents())
		{
			comp.setEnabled(b);
		}
		for (Component comp : panelOptionsUtilities.getComponents())
		{
			comp.setEnabled(b);
		}
		btnGo.setEnabled(b);
		btnBrowsefiles.setEnabled(b);
		btnMakeTerrain.setEnabled(b);
		btnMakeProvinces.setEnabled(b);
		btnMakeSetup.setEnabled(b);
		btnUtilities.setEnabled(b);
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if(evt.getSource() == textFieldInputScale)
		{
			Config.INPUT_MAP_SCALE = (Integer)textFieldInputScale.getValue();
		}
		if(evt.getSource() == textFieldTreeScale)
		{
			Config.TREE_MAP_SCALE = (Integer)textFieldTreeScale.getValue();
		}
		if(evt.getSource() == textFieldHillsHeight)
		{
			Config.HILLS_HEIGHT = (Integer)textFieldHillsHeight.getValue();
		}
		if(evt.getSource() == textFieldMountainHeight)
		{
			Config.MOUNTAIN_HEIGHT = (Integer)textFieldMountainHeight.getValue();
		}
		if(evt.getSource() == textFieldSnowHeight)
		{
			Config.SNOW_HEIGHT = (Integer)textFieldSnowHeight.getValue();
		}
		if(evt.getSource() == textFieldPeakHeight)
		{
			Config.PEAK_HEIGHT = (Integer)textFieldPeakHeight.getValue();
		}
		if(evt.getSource() == textFieldNoiseBaseline)
		{
			Config.NOISE_BASELINE = (Integer)textFieldNoiseBaseline.getValue();
		}
		if(evt.getSource() == textFieldNoiseFactorMax)
		{
			Config.NOISE_FACTOR_MAX = Double.parseDouble(textFieldNoiseFactorMax.getValue().toString());
		}
		if(evt.getSource() == textFieldNoisePatchSize)
		{
			Config.NOISE_PATCH_SIZE = (Integer)textFieldNoisePatchSize.getValue();
		}
		if(evt.getSource() == textFieldSmoothRadius)
		{
			Config.SMOOTH_RADIUS = (Integer)textFieldSmoothRadius.getValue();
		}
		if(evt.getSource() == textFieldStraitDistance)
		{
			Config.MAX_STRAIT_DISTANCE = (Integer)textFieldStraitDistance.getValue();
		}
		if (evt.getSource() == textFieldSettlementsMinDistance)
		{
			Config.MIN_SETTLEMENT_DISTANCE = (Integer)textFieldSettlementsMinDistance.getValue();
		}
		if (evt.getSource() == textFieldSeanodeMinDistance)
		{
			Config.MIN_SEA_NODE_DISTANCE = (Integer)textFieldSeanodeMinDistance.getValue();
		}
		if(evt.getSource() == textFieldModfolder)
		{
			Config.MOD_FOLDER = (String) textFieldModfolder.getValue();
		}
		if(evt.getSource() == textFieldStartyear)
		{
			Config.START_DATE = (Integer)textFieldStartyear.getValue();
		}
		if(evt.getSource() == textFieldPercentfemale)
		{
			Config.PERCENT_FEMALE = (Integer)textFieldPercentfemale.getValue();
		}
		updateTextFieldMaxTerrainHeight();
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		CardLayout cardLayout = (CardLayout)(panelOptions.getLayout());
		
		if (e.getSource() == btnGo)
		{
			start();
		}
		else if (e.getSource() == btnBrowsefiles)
		{
			int returnVal = fileChooser.showOpenDialog(this);

	        if (returnVal == JFileChooser.APPROVE_OPTION) {
	        	try {
		            File file = fileChooser.getSelectedFile();
		            if (file.exists())
		            {
		            	File cultures = new File(file.getCanonicalFile()+"/common/cultures");
		            	if (!cultures.exists())
		            	{
		            		warningMessage("This folder does not contain a common/cultures folder");
		            	}
		            	textFieldModfolder.setValue(file.getCanonicalPath());
		            }
		            else //Dafuq ?
		            {
		            	throw new IOException(); 
		            }
	        	} catch (IOException ex) {
	        		errorMessage(ex.toString());
				}
	        }
		}
		else
		{
			if (e.getSource() == btnMakeTerrain)
			{
				ui_state = UIState.MAKE_TERRAIN;
			}
			if (e.getSource() == btnMakeProvinces)
			{
				ui_state = UIState.MAKE_PROVINCES;
			}
			if (e.getSource() == btnMakeSetup)
			{
				ui_state = UIState.MAKE_SETUP;
			}
			if (e.getSource() == btnUtilities)
			{
				ui_state = UIState.UTILITIES;
			}
		}
		
		switch (ui_state)
		{
		case MAKE_TERRAIN:
			cardLayout.show(panelOptions, "panelOptionsTerrain"); break;
		case MAKE_PROVINCES:
			cardLayout.show(panelOptions, "panelOptionsProvinces"); break;
		case MAKE_SETUP:
			cardLayout.show(panelOptions, "panelOptionsSetup"); break;
		case UTILITIES:
			cardLayout.show(panelOptions, "panelOptionsUtilities"); break;
		}

	}
	
	public void errorMessage(String message)
	{
		JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
	}
	public void warningMessage(String message)
	{
		JOptionPane.showMessageDialog(this, message, "Warning", JOptionPane.WARNING_MESSAGE);
	}
	public void infoMessage(String message)
	{
		JOptionPane.showMessageDialog(this, message, "Information", JOptionPane.INFORMATION_MESSAGE);
	}
	
	public void updateProgress(String text, int progress)
	{
		int currentValue = progressBar.getValue();
		int newValue = currentValue + progress;
		newValue = newValue > 100 ? 100 : newValue;
		
		progressBar.setValue(newValue);
		progressBar.setString(text);
	}
	
	public void resetProgress(String text)
	{
		progressBar.setValue(0);
		progressBar.setString(text);
	}

	@Override
	public void windowActivated(WindowEvent e) {}

	@Override
	public void windowClosed(WindowEvent e) {}

	@Override
	public void windowClosing(WindowEvent e) {
		Logger.close();
	}

	@Override
	public void windowDeactivated(WindowEvent e) {}

	@Override
	public void windowDeiconified(WindowEvent e) {}

	@Override
	public void windowIconified(WindowEvent e) {}

	@Override
	public void windowOpened(WindowEvent e) {}
}
