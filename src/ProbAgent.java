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

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

/**
 * This agent will first collect gold to produce a peasant,
 * then the two peasants will collect gold and wood separately until reach goal.
 * @author Feng
 *
 */
public class ProbAgent extends Agent {
	private static final long serialVersionUID = -4047208702628325380L;
	private static final int GOLD_REQUIRED = 2000;	
	private static final int PEASANT_RANGE = 3;
	private static final int TOWER_RANGE = 4;
	private static final float TOWER_FIRE_RATE = (float)0.75;
	private static final float INITIAL_TOWER_DENSITY = (float)0.01;

	private int step;
	private int startingPeasants = 0;
	private Map<UnitView, Integer> peasantHealth = new HashMap<UnitView, Integer>();
	private Map<UnitView, Pair<Integer, Integer>> peasantLocations = new HashMap<UnitView, Pair<Integer, Integer>>();
	private GameBoard board;
	
	private boolean foundGoldMine = false;
	private Pair<Integer, Integer> estGoldMineLocation;
	
	private StateView currentState;
	
	public ProbAgent(int playernum, String[] arguments) {
		super(playernum);
		
	}

	
	@Override
	public Map<Integer, Action> initialStep(StateView newstate, History.HistoryView statehistory) {
		step = 0;
		
		currentState = newstate;
		
		int width = currentState.getXExtent();
		int height = currentState.getYExtent();
		
		board = new GameBoard(width, height, INITIAL_TOWER_DENSITY);
		
		estGoldMineLocation = new Pair<Integer, Integer>(width - PEASANT_RANGE, PEASANT_RANGE);
		
		for (UnitView unit : currentState.getUnits(playernum)) {
			String unitTypeName = unit.getTemplateView().getName();
			if(unitTypeName.equals("Peasant")) {
				startingPeasants++;
				peasantHealth.put(unit, unit.getHP());
				peasantLocations.put(unit, new Pair<Integer, Integer>(unit.getXPosition(), unit.getYPosition()));
			}
		}
		
		return middleStep(newstate, statehistory);
	}

	@Override
	public Map<Integer,Action> middleStep(StateView newState, History.HistoryView statehistory) {
		step++;
		
		Map<Integer,Action> builder = new HashMap<Integer,Action>();
		currentState = newState;
		
		int currentGold = currentState.getResourceAmount(0, ResourceType.GOLD);

		List<UnitView> peasants = new ArrayList<UnitView>();
		List<UnitView> townhalls = new ArrayList<UnitView>();
		
		for (UnitView unit : currentState.getUnits(playernum)) {
			String unitTypeName = unit.getTemplateView().getName();
			if(unitTypeName.equals("TownHall")) {
				townhalls.add(unit);
			} else if(unitTypeName.equals("Peasant")) {
				peasants.add(unit);
			}
		}
		
		// Build a new peasant if we have lost any
		if (peasants.size() < startingPeasants && currentGold >= peasants.get(0).getTemplateView().getGoldCost()) {
			int townhallID = townhalls.get(0).getID();
			int peasantTemplateID = currentState.getTemplate(playernum, "Peasant").getID();
			builder.put(townhallID, Action.createCompoundProduction(townhallID, peasantTemplateID));
		}
		
		// Find all the peasants that have taken damage
		List<UnitView> hitList = new ArrayList<UnitView>();
		for (UnitView peasant : peasants) {
			int x = peasant.getXPosition();
			int y = peasant.getYPosition();
			
			updatePeasantViewRange(x, y);
			board.incrementVisits(x, y);
			
			if (peasantHealth.get(peasant) < peasant.getHP()) {
				hitList.add(peasant);
				board.incrementHits(x, y);
				updateProbabilities(new Pair<Integer, Integer>(x, y), true);
			} else {
				updateProbabilities(new Pair<Integer, Integer>(x, y), false);
			}
		}
		
		return builder;
	}


	@Override
	public void terminalStep(StateView newstate, History.HistoryView statehistory) {
		step++;
		
	}
	
	private void updatePeasantViewRange(int x, int y) {
		for(int i = -PEASANT_RANGE; i <= PEASANT_RANGE; i++) {
			for(int j = -PEASANT_RANGE; j <= PEASANT_RANGE; j++) {
				updateSeen(x + i, y + j);
			}
		}
	}
	
	private void updateSeen(int x, int y) {
		if (!currentState.inBounds(x, y)) {
			return;
		}
		
		board.setSeen(x, y, true);
		
		if (currentState.isResourceAt(x, y)) {
        	ResourceView resource = currentState.getResourceNode(currentState.resourceAt(x, y));
        	if(resource.getType().equals(ResourceNode.Type.GOLD_MINE)) {
        		foundGoldMine = true;
        		estGoldMineLocation = new Pair<Integer, Integer>(x, y);
        	} else if (resource.getType().equals(ResourceNode.Type.TREE)) {
        		board.setHasTree(x, y, true);
        	}
        	
        	board.setTowerProbability(x, y, 0);
        } else if (currentState.isUnitAt(x, y)) {
        	int unitID = currentState.unitAt(x, y);
        	
            String unitName = currentState.getUnit(unitID).getTemplateView().getName();
            if(unitName.equals("Tower")) {
        		board.setTowerProbability(x, y, 1);
            } else {
            	board.setTowerProbability(x, y, 0);
            }
        }
	}
	
	private float getHitProbability(int x, int y) {
		float probability = 0;
		
		for(int i = -TOWER_RANGE; i <= TOWER_RANGE; i++) {
			for(int j = -TOWER_RANGE; j <= TOWER_RANGE; j++) {
				int curX = x + i;
				int curY = y + j;
				if(currentState.inBounds(curX, curY)) {
					probability = (probability + board.getTowerProbability(curX, curY)) - (probability * board.getTowerProbability(curX, curY));
				}
			}
		}
		
		return probability * TOWER_FIRE_RATE;
	}
	
	private void updateProbabilities(Pair<Integer, Integer> location, boolean hit) {
		
		if (hit) {
			for(int i = -TOWER_RANGE; i <= TOWER_RANGE; i++) {
				for(int j = -TOWER_RANGE; j <= TOWER_RANGE; j++) {
					int x = location.getX() + i;
					int y = location.getY() + j;
					if(currentState.inBounds(x, y) && !board.getSeen(x, y)) {
						// TODO
					}
				}
			}
		} else {
			for(int i = -TOWER_RANGE; i <= TOWER_RANGE; i++) {
				for(int j = -TOWER_RANGE; j <= TOWER_RANGE; j++) {
					int x = location.getX() + i;
					int y = location.getY() + j;
					if(currentState.inBounds(x, y) && !board.getSeen(x, y)) {
						// TODO
					}
				}
			}
		}
	}
	

	private float[][] updateProbabilityMap(Pair<Integer, Integer> location, boolean hit, float[][] old, boolean[][] oofowHasTurret) {
		float[][] map = copyMap(old);
		int x = location.getX();
		int y = location.getY();

		// Assign absolute values to tiles out of FOW.
		updateOutOfFOW(map, x, y, oofowHasTurret);

		// Assign relative values to nearby turrets dependent on whether or not footman was hit.
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
	    float[][] newMap = new float[x][y];
	    for (int i = 0; i < x; i++) {
	        System.arraycopy(map[i], 0, newMap[i], 0, y);
	    }
	    return newMap; 
	}
	
	private List<Pair<Integer, Integer>> getBestPath(Pair<Integer, Integer> curLocation, Pair<Integer, Integer> dest) {
		List<Pair<Integer, Integer>> path =  new ArrayList<Pair<Integer, Integer>>();
		// TODO: do something here
		
		Node current = new Node(curLocation.getX(), curLocation.getY());
		Node target = new Node(dest.getX(), dest.getY());
		List<Node> openSet = new ArrayList<>();
        List<Node> closedSet = new ArrayList<>();
        
        while (true) {
            openSet.remove(current);
            List<Node> adjacent = getAdjacentNodes(current);

            // Find the adjacent node with the lowest heuristic cost.
            for (Node node : adjacent) {
//                System.out.println("\tAdjacent Node: " + current);
                openSet.add(new Node(node, current));
            }

            // Exit search if done.
            if (openSet.isEmpty()) {
                System.out.printf("Target (%d, %d) is unreachable from position (%d, %d).\n",
                                target.getX(), target.getY(), curLocation.getX(), curLocation.getY());
                return null;
            } else if (isAdjacent(current, target)) {
                break;
            }

            // This node has been explored now.
            closedSet.add(current);

            // Find the next open node with the lowest cost.
            Node next = openSet.get(0);
            for (Node node : openSet) {
                if (node.getCost() < next.getCost()) {
                    next = node;
                }
            }
//            System.out.println("Moving to node: " + current);
            current = next;
        }
        
		return path;
	}

	private boolean isAdjacent(Node current, Node target) {
		// TODO Auto-generated method stub
		return false;
	}

	private List<Node> getAdjacentNodes(Node current) {
		List<Node> adjacent = new ArrayList<Node>();
		
		for (int i = -1; i <=1; i++) {
			for (int j = -1; j <=1; j++) {
				if (i == 0 && j == 0) {
					continue;
				}
				int x = current.getX() + i;
				int y = current.getY() + j;
				if (!currentState.inBounds(x, y)
						|| board.getHasTree(x, y)) {
					continue;
				}
				adjacent.add(new Node(x, y, getHitProbability(x, y), current));
			}
		}
		return null;
	}
	
	public static String getUsage() {
		return "Determines the location of enemy towers and avoids them in order to collect 2000 gold.";
	}
	@Override
	public void savePlayerData(OutputStream os) {
		//this agent lacks learning and so has nothing to persist.
		
	}
	@Override
	public void loadPlayerData(InputStream is) {
		//this agent lacks learning and so has nothing to persist.
	}
}
