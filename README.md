# EECS 391 - Programming Assignment 4
### Authors: Josh Braun (jxb532), Wes Rupert (wkr3)

This is fourth programming assignment for Case Western Reserve University's EECS 391 Introduction to Artificial Intelligence course. The project requires CWRU's SEPIA AI engine to run.

The program is designed to find the probability of enemy tower locations and avoid them to gather gold.

At each turn, information is saved for each square that details the number of times it has been visited, whether it has been seen by a peasant, and how many times a peasant has been attacked there.
This information is used to calculate a probability distribution of likely tower locations.
A* search is used to find a path with the least likelihood of being attacked by a tower.

Initially, the peasants walk randomly. This phase ends when a peasant gets attacked.
After this, the peasants try to find a path to the top right of the game board, in search of the goldmine.
Once the peasants reach the top, they will expand their search for the goldmine until it has been seen by a peasant.
Once the goldmine has been located, the peasants will carry gold to the town hall.
If a peasant dies, then a new one will be created once there is enough gold.

Our performance on the small map (19x25) is usually very good.
Every so often the peasants will die, but the majority of the runs reach the objective.
However, in the large board, the peasants are almost never able to find a path through the towers before dying.
To fix this, we created a Persistent mode (see below).

Persistent Mode:
	Persistent mode saves the current game board (tower probabilities, etc.) when all of the peasants die.
	Then, when the same game is replayed in persistent mode, the old probabilities are loaded.
	This allows the peasants multiple tries when trying to locate towers. We found that this was necessary to beat the larger game board.
	To run in persistent mode, see the execution instructions below.
	If you want to run persistent mode on any other maps besides the two provided, just include "Persistent" as an argument in the config file.
	If you want to start a fresh run with a new game board, just delete the "SavedBoards" directory. This is sometimes necessary if the peasants keep running into the same dead-end.
	
To run the assignment parts, execute the following:

```bat
sh buildProbGame.sh
sh buildProbGame2.sh
sh buildProbGameP.sh
sh buildProbGame2P.sh
```

The different scenarios are:

buildProbGame:
	Runs the 19x25 map
	
buildProbGame2:
	Runs the 32x32 map
	
buildProbGameP:
	Runs the game in persistent mode. For more details, see above
	
