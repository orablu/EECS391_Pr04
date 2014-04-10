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
import java.util.LinkedList;
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
import edu.cwru.sepia.util.Direction;

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
	private static final float TOWER_FIRE_RATE = 0.75f;
	private static final float INITIAL_TOWER_DENSITY = 0.01f;

	private int step;
	private int startingPeasants = 0;
	private Map<Integer, Integer> peasantHealth = new HashMap<Integer, Integer>();
	private Map<Integer, Pair<Integer, Integer>> peasantLocations = new HashMap<Integer, Pair<Integer, Integer>>();
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
				peasantHealth.put(unit.getID(), unit.getHP());
				peasantLocations.put(unit.getID(), new Pair<Integer, Integer>(unit.getXPosition(), unit.getYPosition()));
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
				peasantLocations.put(unit.getID(), new Pair<Integer, Integer>(unit.getXPosition(), unit.getYPosition()));
			}
		}
		
		// Build a new peasant if we have lost any
		if (peasants.size() < startingPeasants && currentGold >= peasants.get(0).getTemplateView().getGoldCost()) {
			int townhallID = townhalls.get(0).getID();
			int peasantTemplateID = currentState.getTemplate(playernum, "Peasant").getID();
			System.out.println("Making new peasant");
			builder.put(townhallID, Action.createCompoundProduction(townhallID, peasantTemplateID));
		}
		
		// Find all the peasants that have taken damage
		List<UnitView> hitList = new ArrayList<UnitView>();
		for (UnitView peasant : peasants) {
			int x = peasant.getXPosition();
			int y = peasant.getYPosition();
			
			updatePeasantViewRange(x, y);
			board.incrementVisits(x, y);
			System.out.println("Visits incremented");
			if (!peasantHealth.containsKey(peasant.getID())) {
				peasantHealth.put(peasant.getID(), peasant.getHP());
				peasantLocations.put(peasant.getID(), new Pair<Integer, Integer>(peasant.getXPosition(), peasant.getYPosition()));
			}
			
			if (peasantHealth.get(peasant.getID()) < peasant.getHP()) {
				System.out.println("Peasant " + peasant.getID() + " has been hit!");
				hitList.add(peasant);
				board.incrementHits(x, y);
				updateFromHit(x, y, true);
			} else {
				updateFromHit(x, y, false);
			}
			System.out.println("Probabilities updated");
		}
		
		for (UnitView peasant : peasants) {
			if (peasant.getCargoAmount() == 0) {
				if (isAdjacent(peasant.getXPosition(), peasant.getYPosition(), estGoldMineLocation.getX(), estGoldMineLocation.getY())
						&& foundGoldMine) {
					System.out.println("Found gold mine, harvesting!");
					Action a = Action.createCompoundGather(peasant.getID(), currentState.resourceAt(estGoldMineLocation.getX(), estGoldMineLocation.getY()));
					builder.put(peasant.getID(), a);
				} else {
					System.out.println("Moving towards goldmine!");
					List<Pair<Integer, Integer>> bestPath = getBestPath(new Pair<Integer, Integer>(peasant.getXPosition(), peasant.getYPosition()), estGoldMineLocation);
					Pair<Integer, Integer> nextStep = bestPath.get(0);
					
					Direction direction = getDirection(nextStep.getX() - peasant.getXPosition(), nextStep.getY() - peasant.getYPosition());
					System.out.println("Peasant " + peasant.getID() + " moving " + direction);
					Action a = Action.createPrimitiveMove(peasant.getID(), direction);
					builder.put(peasant.getID(), a);
				}
			} else {
				if (isAdjacent(peasant.getXPosition(), peasant.getYPosition(), townhalls.get(0).getXPosition(), townhalls.get(0).getYPosition())) {
					System.out.println("Depositing!");
					Action a = Action.createCompoundDeposit(peasant.getID(), townhalls.get(0).getID());
					builder.put(peasant.getID(), a);
				} else {
					System.out.println("Moving towards townhall!");
					List<Pair<Integer, Integer>> bestPath = getBestPath(new Pair<Integer, Integer>(peasant.getXPosition(), peasant.getYPosition()),
							new Pair<Integer, Integer>(townhalls.get(0).getXPosition(), townhalls.get(0).getYPosition()));
					Pair<Integer, Integer> nextStep = bestPath.get(0);
					Direction direction = getDirection(nextStep.getX() - peasant.getXPosition(), nextStep.getY() - peasant.getYPosition());
					System.out.println("Peasant " + peasant.getID() + " moving " + direction);
					Action a = Action.createPrimitiveMove(peasant.getID(), direction);
					builder.put(peasant.getID(), a);
				}
			}
		}
		
		return builder;
	}


	@Override
	public void terminalStep(StateView newstate, History.HistoryView statehistory) {
		step++;
		
	}
	
    private Direction getDirection(int x, int y) {
        if (x == 1 && y == 0) {
            return Direction.EAST;
        } else if (x == 1 && y == -1) {
            return Direction.NORTHEAST;
        } else if (x == 0 && y == -1) {
            return Direction.NORTH;
        } else if (x == -1 && y == -1) {
            return Direction.NORTHWEST;
        } else if (x == -1 && y == 0) {
            return Direction.WEST;
        } else if (x == -1 && y == 1) {
            return Direction.SOUTHWEST;
        } else if (x == 0 && y == 1) {
            return Direction.SOUTH;
        } else if (x == 1 && y == 1) {
            return Direction.SOUTHEAST;
        } else {
            System.out.println("Something bad happened while calculating direction");
            return null;
        }
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

	private static final float ACCURACY = 0.75f;
	private void updateFromHit(int x, int y, boolean hit) {
		float[][] old = board.getBoardCopy();
		int fromx = Math.max(x - TOWER_RANGE, 0);
		int tox   = Math.min(old.length, x + TOWER_RANGE);
		int fromy = Math.max(y - TOWER_RANGE, 0);
		int toy   = Math.min(old[0].length, y + TOWER_RANGE);
		for (int r = fromx; r < tox; r++) {
			for (int c = fromy; c < toy; c++) {
				if (board.getSeen(r, c)) continue; // Only need to update out-of-view cells.

				float phn, pht;
				if (hit) {
					// P(H|N) = 1 - P(S|N)
					phn = 1;
					for (int rr = fromx; rr < tox; rr++) {
						for (int cc = fromy; cc < toy; cc++) {
							if (rr == r && cc == c) continue;
							// P(S) = P(N)+(P(T)*(1-P(H)))
							phn *= (1f - old[rr][cc]) + (old[rr][cc] * (1f - ACCURACY));
						}
					}
					phn = 1f - phn;

					// P(H|T): same as above, but without skipping r, c
					// Simplifies to 1-((1-P(H|N))*P(M)) since tower existing is given.
					pht = 1f - ((1f - phn) * (1f - ACCURACY));
				} else {
					// P(S|N) = P(S)
					phn = 1;
					for (int rr = fromx; rr < tox; rr++) {
						for (int cc = fromy; cc < toy; cc++) {
							if (rr == r && cc == c) continue;
							// P(S) = P(N)+(P(T)*(1-P(H)))
							phn *= (1f - old[rr][cc]) + (old[rr][cc] * (1f - ACCURACY));
						}
					}

					// P(S|T): same as above, but without skipping r, c
					// Simplifies to P(H|N))*P(M) since tower existing is given.
					pht = phn * (1f - ACCURACY);
				}

				// P(T|H) = P(H|T)*P(T)/(P(H|T)*P(T)+P(H|N)*P(N))
				board.setTowerProbability(r, c, pht * old[r][c] / (pht * old[r][c] + phn * (1 - old[r][c])));
			}
		}
	}

	private List<Pair<Integer, Integer>> getBestPath(Pair<Integer, Integer> curLocation, Pair<Integer, Integer> dest) {
		List<Pair<Integer, Integer>> path =  new LinkedList<Pair<Integer, Integer>>();
		// TODO: do something here
		
		Node current = new Node(curLocation.getX(), curLocation.getY(), getHitProbability(curLocation.getX(), curLocation.getY()));
		Node target = new Node(dest.getX(), dest.getY(), getHitProbability(dest.getX(), dest.getY()));
		List<Node> openSet = new ArrayList<>();
        List<Node> closedSet = new ArrayList<>();
        
        while (true) {
            openSet.remove(current);
            List<Node> adjacent = getAdjacentNodes(current, closedSet);

            // Find the adjacent node with the lowest heuristic cost.
            List<Node> openSetCopy = new ArrayList<Node>(openSet);
            for (Node neighbor : adjacent) {
            	boolean inOpenset = false;
            	for (Node node : openSetCopy) {
            		if (neighbor.equals(node)) {
            			inOpenset = true;
            			if (neighbor.getAccumulatedCost() < node.getAccumulatedCost()) {
            				openSet.remove(node);
            				openSet.add(neighbor);
            			}
            		}
            	}
            	
            	if (!inOpenset) {
            		openSet.add(neighbor);
            	}
            	openSetCopy = new ArrayList<>(openSet);
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
                if (node.getCost(target) < next.getCost(target)) {
                    next = node;
                }
            }
//            System.out.println("Moving to node: " + current);
            current = next;
        }
        
        path.add(new Pair<Integer, Integer>(curLocation.getX(), curLocation.getY()));
        while(current.getParent() != null) {
        	current = current.getParent();
        	path.add(0, new Pair<Integer, Integer>(current.getX(), current.getY()));
        }
        
        if (path.size() > 1) {
        	path.remove(0);
        }
        
		return path;
	}

	private boolean isAdjacent(Node current, Node target) {
        return isAdjacent(current.getX(), current.getY(), target.getX(), target.getY());
	}
	
	private boolean isAdjacent(int x, int y, int targetX, int targetY) {
	        for (int i = x - 1; i <= x + 1; i++) {
	            for (int j = y - 1; j <= y + 1; j++) {
	                if (i == targetX && j == targetY) {
	                    return true;
	                }
	            }
	        }
	        return false;
	}

	private List<Node> getAdjacentNodes(Node current, List<Node> closedSet) {
		List<Node> adjacent = new ArrayList<Node>();
		
		for (int i = -1; i <=1; i++) {
			inner:
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
				Node node = new Node(x, y, getHitProbability(x, y), current);
				for (Node visitedNode : closedSet) {
					if (node.equals(visitedNode)) {
						continue inner;
					}
				}
				adjacent.add(node);
			}
		}
//		System.out.println("AT " + current);
//		System.out.println("NEIGHBORS:");
//		for (Node node : adjacent) {
//			System.out.println("\t" + node);
//		}
		
		return adjacent;
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
