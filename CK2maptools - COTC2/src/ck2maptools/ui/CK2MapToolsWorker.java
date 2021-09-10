package ck2maptools.ui;

import java.util.List;

import ck2maptools.main.ICK2MapTool;
import ck2maptools.utils.Logger;

public class CK2MapToolsWorker implements Runnable {

	private CK2MapToolsUI ui;
	private List<ICK2MapTool> toolsList;
	
	public CK2MapToolsWorker(CK2MapToolsUI ui, List<ICK2MapTool> toolsList) {
		super();
		this.ui = ui;
		this.toolsList = toolsList;
	}
	
	
	@Override
	public void run() {
		try {
			int returnCode = ICK2MapTool.ERROR_NONE;
			
			if (toolsList != null)
			{
				for (ICK2MapTool t : toolsList)
				{
					returnCode |= t.execute();
				}
			}
			
			ui.end(returnCode);
		}
		catch (CK2MapToolsException e)
		{
			ui.errorMessage(e.getMessage());
			Logger.log(e.toString());
			Logger.close();
			ui.end(-1);
		}
		catch (Exception e)
		{
			ui.errorMessage(e.toString());
			Logger.log(e.toString());
			for (StackTraceElement ste : e.getStackTrace())
			{
				Logger.log(ste.toString());
			}
			Logger.close();
			ui.end(-1);
		}
	}
}
