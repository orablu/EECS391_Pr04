/**
 *  Strategy Engine for Programming Intelligent Agents (SEPIA)
    Copyright (C) 2012 Case Western Reserve University

    This file is part of SEPIA.

    SEPIA is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    SEPIA is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SEPIA.  If not, see <http://www.gnu.org/licenses/>.
 */
//package edu.cwru.sepia.agent;


import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.persistence.UnitTemplateAdapter;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.ResourceNode.Type;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Template.TemplateView;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.environment.model.state.UnitTemplate;
import edu.cwru.sepia.environment.model.state.UnitTemplate.UnitTemplateView;
import edu.cwru.sepia.experiment.Configuration;
import edu.cwru.sepia.experiment.ConfigurationValues;
import edu.cwru.sepia.agent.Agent;

/**
 * This agent will first collect gold to produce a peasant,
 * then the two peasants will collect gold and wood separately until reach goal.
 * @author Feng
 *
 */
public class ProbAgent extends Agent {
	private static final long serialVersionUID = -4047208702628325380L;
	private static final Logger logger = Logger.getLogger(ProbAgent.class.getCanonicalName());

	private int goldRequired;
	private int woodRequired;
	
	private int step;
	
	public ProbAgent(int playernum, String[] arguments) {
		super(playernum);
		
		goldRequired = Integer.parseInt(arguments[0]);
		woodRequired = Integer.parseInt(arguments[1]);
	}

	StateView currentState;
	
	@Override
	public Map<Integer, Action> initialStep(StateView newstate, History.HistoryView statehistory) {
		step = 0;
		return middleStep(newstate, statehistory);
	}

	@Override
	public Map<Integer,Action> middleStep(StateView newState, History.HistoryView statehistory) {
		step++;
		if(logger.isLoggable(Level.FINE))
		{
			logger.fine("=> Step: " + step);
		}
		
		Map<Integer,Action> builder = new HashMap<Integer,Action>();
		currentState = newState;
		
		int currentGold = currentState.getResourceAmount(0, ResourceType.GOLD);
		int currentWood = currentState.getResourceAmount(0, ResourceType.WOOD);
		if(logger.isLoggable(Level.FINE))
		{
			logger.fine("Current Gold: " + currentGold);
		}
		if(logger.isLoggable(Level.FINE))
		{
			logger.fine("Current Wood: " + currentWood);
		}
		List<Integer> myUnitIds = currentState.getUnitIds(playernum);
		List<Integer> peasantIds = new ArrayList<Integer>();
		List<Integer> townhallIds = new ArrayList<Integer>();
		List<Integer> farmIds = new ArrayList<Integer>();
		List<Integer> barracksIds = new ArrayList<Integer>();
		List<Integer> footmanIds = new ArrayList<Integer>();
		for(int i=0; i<myUnitIds.size(); i++) {
			int id = myUnitIds.get(i);
			UnitView unit = currentState.getUnit(id);
			String unitTypeName = unit.getTemplateView().getName();
			System.out.println("Unit Type Name: " + unitTypeName);
			if(unitTypeName.equals("TownHall"))
				townhallIds.add(id);
			if(unitTypeName.equals("Peasant"))
				peasantIds.add(id);
			if(unitTypeName.equals("Farm"))
				farmIds.add(id);
			if(unitTypeName.equals("Barracks"))
				barracksIds.add(id);
			if(unitTypeName.equals("Footman"))
				footmanIds.add(id);
		}
		
		List<Integer> enemyUnitIds = currentState.getAllUnitIds();
		enemyUnitIds.removeAll(myUnitIds);
		
		if(peasantIds.size()>=3) {  // collect resources
			if (farmIds.size() < 1 && currentGold >= 500 && currentWood >= 250) {
				System.out.println("Building a Farm");
				int peasantId = peasantIds.get(0);
				Action b = Action.createPrimitiveBuild(peasantId, currentState.getTemplate(playernum, "Farm").getID());
				builder.put(peasantId, b);
			} else if (barracksIds.size() < 1 && currentGold >= 700 && currentWood >= 400) {
				System.out.println("Building a Barracks");
				int peasantId = peasantIds.get(0);
				Action b = Action.createPrimitiveBuild(peasantId, currentState.getTemplate(playernum, "Barracks").getID());
				builder.put(peasantId, b);
			} else if(barracksIds.size() > 0 && footmanIds.size() < 2 && currentGold >= 600) {
				System.out.println("Building a Footman");
				int barracksId = barracksIds.get(0);
				Action b = Action.createCompoundProduction(barracksId, currentState.getTemplate(playernum, "Footman").getID());
				builder.put(barracksId, b);
			} else {
				if (footmanIds.size() >= 2) { //attack enemies
					System.out.println("Attacking enemies");
					for (int i : footmanIds) {
						Action b = Action.createCompoundAttack(i, enemyUnitIds.get(0));
						builder.put(i, b);
					}
				}
				
				int peasantId = peasantIds.get(1);
				int townhallId = townhallIds.get(0);
				Action b = null;
				if(currentState.getUnit(peasantId).getCargoAmount()>0)
					b = new TargetedAction(peasantId, ActionType.COMPOUNDDEPOSIT, townhallId);
				else {
					List<Integer> resourceIds = currentState.getResourceNodeIds(Type.TREE);
					b = new TargetedAction(peasantId, ActionType.COMPOUNDGATHER, resourceIds.get(0));
				}
				builder.put(peasantId, b);
				
				peasantId = peasantIds.get(0);
				if(currentState.getUnit(peasantId).getCargoType() == ResourceType.GOLD && currentState.getUnit(peasantId).getCargoAmount()>0)
					b = new TargetedAction(peasantId, ActionType.COMPOUNDDEPOSIT, townhallId);
				else {
					List<Integer> resourceIds = currentState.getResourceNodeIds(Type.GOLD_MINE);
					b = new TargetedAction(peasantId, ActionType.COMPOUNDGATHER, resourceIds.get(0));
				}
				builder.put(peasantId, b);
				
				peasantId = peasantIds.get(2);
				if(currentState.getUnit(peasantId).getCargoType() == ResourceType.GOLD && currentState.getUnit(peasantId).getCargoAmount()>0)
					b = new TargetedAction(peasantId, ActionType.COMPOUNDDEPOSIT, townhallId);
				else {
					List<Integer> resourceIds = currentState.getResourceNodeIds(Type.GOLD_MINE);
					b = new TargetedAction(peasantId, ActionType.COMPOUNDGATHER, resourceIds.get(0));
				}
				
				builder.put(peasantId, b);
			}
		}
		else {  // build peasant
			if(currentGold>=400) {
				System.out.println("Building peasant");
				if(logger.isLoggable(Level.FINE))
				{
					logger.fine("already have enough gold to produce a new peasant.");
				}
				TemplateView peasanttemplate = currentState.getTemplate(playernum, "Peasant");
				int peasanttemplateID = peasanttemplate.getID();
				if(logger.isLoggable(Level.FINE))
				{
					logger.fine(String.valueOf(peasanttemplate.getID()));
				}
				int townhallID = townhallIds.get(0);
					builder.put(townhallID, Action.createCompoundProduction(townhallID, peasanttemplateID));
			} else {
				System.out.println("Collecting gold");
				int peasantId = peasantIds.get(0);
				int townhallId = townhallIds.get(0);
				Action b = null;
				if(currentState.getUnit(peasantId).getCargoType() == ResourceType.GOLD && currentState.getUnit(peasantId).getCargoAmount()>0)
					b = new TargetedAction(peasantId, ActionType.COMPOUNDDEPOSIT, townhallId);
				else {
					List<Integer> resourceIds = currentState.getResourceNodeIds(Type.GOLD_MINE);
					b = new TargetedAction(peasantId, ActionType.COMPOUNDGATHER, resourceIds.get(0));
				}
				builder.put(peasantId, b);
			}
		}
		return builder;
	}

	@Override
	public void terminalStep(StateView newstate, History.HistoryView statehistory) {
		step++;
		if(logger.isLoggable(Level.FINE))
		{
			logger.fine("=> Step: " + step);
		}
		
		int currentGold = newstate.getResourceAmount(0, ResourceType.GOLD);
		int currentWood = newstate.getResourceAmount(0, ResourceType.WOOD);
		
		if(logger.isLoggable(Level.FINE))
		{
			logger.fine("Current Gold: " + currentGold);
		}
		if(logger.isLoggable(Level.FINE))
		{
			logger.fine("Current Wood: " + currentWood);
		}
		if(logger.isLoggable(Level.FINE))
		{
			logger.fine("Congratulations! You have finished the task!");
		}
	}
	
	public static String getUsage() {
		return "Two arguments, amount of gold to gather and amount of wood to gather";
	}
	@Override
	public void savePlayerData(OutputStream os) {
		//this agent lacks learning and so has nothing to persist.
		
	}
	@Override
	public void loadPlayerData(InputStream is) {
		//this agent lacks learning and so has nothing to persist.
	}

	private float[][] updateProbabilityMap(int x, int y, boolean hit, float[][] old, boolean[][] oofowHasTurret) {
		float[][] map = copyMap(old);

		// Assign absolute values to tiles out of FOW.
		updateOutOfFOW(map, x, y, oofowHasTurret);

		// Assign relative values to nearby turrets dependent on wether or not footnam was hit.
		updateFromHit(map, x, y, hit);

		return map;
	}

	private void updateOutOfFOW(float[][] map, int x, int y, boolean[][] turret) {
		for (int r = 0; r < turret.length; r++) {
			for (int c = 0; c < turret[r].length; c++) {
				map[x+r][y+c] = turret[r][c] ? 1 : 0;
			}
		}
	}

	private void updateFromHit(float[][] map, int x, int y, boolean hit) {
		// TODO: Implement
	}

	private float[][] copyMap(float[][] map) {
		int x = map.length;
		int y = map[0].length;
	    int[][] newMap = new int[x][y];
	    for (int i = 0; i < x; i++) {
	        System.arraycopy(map[i], 0, newMap[i], 0, y);
	    }
	    return newMap; 
	}
}
