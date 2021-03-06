package de.ipbhalle.metfraglib.interfaces;

import de.ipbhalle.metfraglib.settings.Settings;

public interface IScoreInitialiser {

	/**
	 * initialise global object shared by multiple score class instances 
	 */
	public void initScoreParameters(Settings settings) throws Exception;
	
}
