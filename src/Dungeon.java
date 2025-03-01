import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The Dungeon class implement the DungeonInterface, in order to provide all the publicly
 * usable functionalities.
 * It represents the game basically. It has everything in it.
 * player, caves, tunnels etc.
 */
public class Dungeon implements DungeonInterface {
  private final int[] sizeOfDungeon;
  private CaveTunnelInterface[][] grid2D;
  private int[] startPosition;
  private int[] endPosition;
  //  private final int treasurePercentage;
  private static final int minPathLength = 5;
  private final Player player;
  //  private final boolean warpAllowed;
  private List<Graph.Edge> allUsedEdges;


  /**
   * Constructor takes in the data and creates a desired object of the dungeon model.
   *
   * @param sizeRxC           The size of dungeon as rows and columns.
   * @param startPos          The starting position.
   * @param endPos            The ending position.
   * @param treasurePerc      The treasure percentage.
   * @param interConnectivity The Inter-Connectivity.
   * @param player            The Player.
   * @param warpAllowed       Is this dungeon Warping or not.
   */
  Dungeon(String sizeRxC, String startPos, String endPos, int treasurePerc,
          int interConnectivity, Player player, boolean warpAllowed) {
    String[] strArr = sizeRxC.strip().split("x");
    int rowCount = Integer.parseInt(strArr[0]);
    int colCount = Integer.parseInt(strArr[1]);
    if (rowCount < 1) {
      throw new IllegalArgumentException("Row count can not be less than 1");
    }
    if (colCount < 1) {
      throw new IllegalArgumentException("Column count can not be less than 1");
    }

    strArr = startPos.strip().split(",");
    int startRow = Integer.parseInt(strArr[0]);
    int startCol = Integer.parseInt(strArr[1]);
    if (startRow < 0 || startRow > rowCount - 1) {
      throw new IllegalArgumentException("Invalid Start Row .");
    }
    if (startCol < 0 || startCol > colCount - 1) {
      throw new IllegalArgumentException("Invalid Start Column.");
    }


    strArr = endPos.strip().split(",");
    int endRow = Integer.parseInt(strArr[0]);
    int endCol = Integer.parseInt(strArr[1]);
    if (endRow < 0 || endRow > rowCount - 1) {
      throw new IllegalArgumentException("Invalid End Row .");
    }
    if (endCol < 0 || endCol > colCount - 1) {
      throw new IllegalArgumentException("Invalid End Column.");
    }


    if (treasurePerc < 0 || treasurePerc > 100) {
      throw new IllegalArgumentException("Invalid Treasure Percentage.");
    }

    if (rowCount * colCount < 10) {
      throw new IllegalArgumentException("The number of caves/tunnels {rows*columns} must exceed"
              + " 10 in the worst case scenario.");
    }

    if (interConnectivity < 0) {
      throw new IllegalArgumentException("Invalid Inter-Connectivity value.");
    }

    this.sizeOfDungeon = new int[]{rowCount, colCount};

    this.setRandomStartEnd();


    generateGrid(rowCount, colCount, interConnectivity, warpAllowed);  // grid2D =
    boolean satisfiedRequirement = minPathLengthCheck(startPosition, endPosition);
    if (!satisfiedRequirement) {
      throw new IllegalArgumentException("Path Length from START to End cannot be satisfied.");
    }

    this.player = player;
    this.player.moveInDungeon(startPosition,
            this.grid2D[this.startPosition[0]][this.startPosition[1]]);

    this.addTreasures(treasurePerc);
  }

  private void generateGrid(int rowsCount, int columnCount, int interConnectivity,
                            boolean warpAllowed) {
    CaveTunnelInterface[][] g2d = new CaveTunnelInterface[rowsCount][columnCount];
    List<int[]> edges = new ArrayList<>();    //int[totalEdges][3];

    // setting up right nodes
    for (int i = 0; i < rowsCount; i++) {
      for (int j = 0; j < columnCount - 1; j++) {
        int sourceNodeName = i * columnCount + j;
        int endNodeName = i * columnCount + j + 1;
        int randomWeight = ThreadLocalRandom.current().nextInt(100);
        int[] temp = {sourceNodeName, endNodeName, randomWeight};
        edges.add(temp);
      }
    }
    // setting up down nodes
    for (int i = 0; i < rowsCount - 1; i++) {
      for (int j = 0; j < columnCount; j++) {
        int sourceNodeName = i * columnCount + j;
        int endNodeName = i * columnCount + j + columnCount;
        int randomWeight = ThreadLocalRandom.current().nextInt(100);
        int[] temp = {sourceNodeName, endNodeName, randomWeight};
        edges.add(temp);
      }
    }

    int vertexCount = rowsCount * columnCount; // Number of vertices in graph
    int edgesCount = edges.size(); // Number of edges in graph
    Graph dungeonGraph = new Graph(vertexCount, edgesCount);


    // add edges
    for (int i = 0; i < edgesCount; i++) {
      int[] data = edges.get(i);

      int src = data[0];
      int dest = data[1];
      int weight = data[2];

      dungeonGraph.edge[i].src = src;
      dungeonGraph.edge[i].dest = dest;
      dungeonGraph.edge[i].weight = weight;

    }

    Graph.Edge[][] allData = dungeonGraph.kruskalMST();
    Graph.Edge[] mstEdges = allData[0];
    Graph.Edge[] redundantEdges = allData[1];

    List<Graph.Edge> finalEdges = new ArrayList<>(List.of(mstEdges));
    for (int i = 0; i < interConnectivity; i++) {
      try {
        finalEdges.add(redundantEdges[i]);
      } catch (IndexOutOfBoundsException e) {
        throw new IllegalArgumentException("Not Enough Edges To Satisfy Interconnectivity");
      }
    }

    this.allUsedEdges = finalEdges;

    // initialise with base containers
    for (int i = 0; i < rowsCount; i++) {
      for (int j = 0; j < columnCount; j++) {
        //g2d[i][j] = new Cave();
        CaveTunnelInterface caveCumTunnel = new CaveTunnel();
        caveCumTunnel.setMatrixIndex(new int[]{i, j});
        caveCumTunnel.setNodeName(String.valueOf(i * columnCount + columnCount));
        g2d[i][j] = caveCumTunnel;
      }
    }

    // add edges to containers
    for (Graph.Edge edge : finalEdges) {
      int src_row = edge.src / columnCount;
      int src_col = edge.src % columnCount;
      int dest_row = edge.dest / columnCount;
      int dest_col = edge.dest % columnCount;

      if (src_row == dest_row) {
        g2d[src_row][src_col].setRightNode(g2d[dest_row][dest_col]);
        g2d[dest_row][dest_col].setLeftNode(g2d[src_row][src_col]);
      } else if (src_col == dest_col) {
        g2d[src_row][src_col].setDownNode(g2d[dest_row][dest_col]);
        g2d[dest_row][dest_col].setUpNode(g2d[src_row][src_col]);
      } else {
        throw new RuntimeException("_____________________");
      }

      this.grid2D = g2d;
    }

    if (warpAllowed) {
      this.addWarpEdges(rowsCount, columnCount);
    }

  }

  private void addWarpEdges(int rowsCount, int columnCount) {
    for (int i = 0; i < rowsCount; i++) {
      this.grid2D[i][0].setLeftNode(this.grid2D[i][columnCount - 1]);
      this.grid2D[i][columnCount - 1].setRightNode(this.grid2D[i][0]);
    }
    for (int j = 0; j < columnCount; j++) {
      this.grid2D[0][j].setUpNode(this.grid2D[rowsCount - 1][j]);
      this.grid2D[rowsCount - 1][j].setDownNode(this.grid2D[0][j]);
    }
  }


  /**
   * Check Min Path Length from starting node to ending node .
   *
   * @param startPosition the starting position.
   * @param endPosition   the starting position.
   * @return Satisfied or not satisfied as true or false.
   */
  public boolean minPathLengthCheck(int[] startPosition, int[] endPosition) {
    int diff = Math.abs(endPosition[0] - startPosition[0])
            + Math.abs(endPosition[1] - startPosition[1]);
    return diff >= Dungeon.minPathLength;
  }


  private void addTreasures(int percentage) {
    int totalCaves = this.sizeOfDungeon[0] * this.sizeOfDungeon[1];
    int treasureCaves = (int) Math.ceil(totalCaves * percentage / 100.0);

    HashSet<Integer> set = new HashSet<>();
    while (set.size() != treasureCaves) {
      int randCaveNum = ThreadLocalRandom.current().nextInt(0, totalCaves);
      int row = randCaveNum / this.sizeOfDungeon[1];
      int col = randCaveNum % this.sizeOfDungeon[1];

      int randTreasureCount = ThreadLocalRandom.current().nextInt(0, 2);
      for (int x = 0; x <= randTreasureCount; x++) {
        this.grid2D[row][col].addTreasure(new Treasure());
      }
      set.add(randCaveNum);
    }
  }


  private List<int[]> getMovesFrom(int[] location) {
    return this.grid2D[location[0]][location[1]].getPossibleMoves();
  }

  List<CaveTunnelInterface> getTreasureFullContainersList() {
    List<CaveTunnelInterface> list = new ArrayList<>();
    for (int i = 0; i < this.sizeOfDungeon[0]; i++) {
      for (int j = 0; j < this.sizeOfDungeon[1]; j++) {
        if (this.grid2D[i][j].getTreasureList().size() > 0) {
          list.add(this.grid2D[i][j]);
        }
      }
    }
    return list;
  }


  @Override
  public void movePlayer(int[] newLocation) {
    CaveTunnelInterface newCaveCumTunnel = this.grid2D[newLocation[0]][newLocation[1]];
    player.moveInDungeon(newLocation, newCaveCumTunnel);
  }

  @Override
  public List<int[]> getCurrentMoves() {
    return getMovesFrom(this.player.getCurrLoc());
  }


  /**
   * Gives the starting position object (int []).
   *
   * @return The starting position object.
   */
  public int[] getStartPosition() {
    return startPosition;
  }

  /**
   * Gives the ending position object (int []).
   *
   * @return The ending position object.
   */
  public int[] getEndPosition() {
    return endPosition;
  }

  /**
   * Forcefully sets the starting position object.
   *
   * @param pos The starting position object.
   */
  public void forceSetStartPosition(int[] pos) {
    this.startPosition = pos;
  }

  /**
   * Forcefully sets the ending position object.
   *
   * @param pos The ending position object.
   */
  public void forceSetEndPosition(int[] pos) {
    this.endPosition = pos;
  }

  /**
   * Sets random start end positions in the dungeon model at the time of initialisation.
   */
  public void setRandomStartEnd() {
    double totalCaves = this.sizeOfDungeon[0] * this.sizeOfDungeon[1];

    int randCaveNum = ThreadLocalRandom.current().nextInt((int) totalCaves);
    int startRow = randCaveNum / this.sizeOfDungeon[1];
    int startCol = randCaveNum % this.sizeOfDungeon[1];

    randCaveNum = ThreadLocalRandom.current().nextInt((int) totalCaves);
    int endRow = randCaveNum / this.sizeOfDungeon[1];
    int endCol = randCaveNum % this.sizeOfDungeon[1];


    if (minPathLengthCheck(new int[]{startRow, startCol}, new int[]{endRow, endCol})) {
      this.forceSetStartPosition(new int[]{startRow, startCol});
      this.forceSetEndPosition(new int[]{endRow, endCol});
    } else {
      this.setRandomStartEnd();
    }


  }

  @Override
  public String toString() {
    int rows = this.sizeOfDungeon[0];
    int cols = this.sizeOfDungeon[1];
    String[][] strArr = new String[rows * 7][cols];
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        strArr[i * 7][j] = "|‾‾‾|   ";  // i * 7 + 0
        if (i == this.startPosition[0] && j == this.startPosition[1]) {
          strArr[i * 7 + 1][j] = "| S |   ";
        } else if (i == this.endPosition[0] && j == this.endPosition[1]) {
          strArr[i * 7 + 1][j] = "| E |   ";
        } else {
          strArr[i * 7 + 1][j] = "|   |   ";
        }

        boolean thereIsRightwardEdge = false;
        for (int[] x : this.grid2D[i][j].getPossibleMoves()) {
          if (x[1] == 1 + j) {
            thereIsRightwardEdge = true;
            break;
          }
        }
        if (thereIsRightwardEdge) {
          if (i == this.player.getCurrLoc()[0] & j == this.player.getCurrLoc()[1]) {
            strArr[i * 7 + 2][j] = "| P |---";
          } else {
            strArr[i * 7 + 2][j] = "|   |---";
          }
        } else {
          if (i == this.player.getCurrLoc()[0] & j == this.player.getCurrLoc()[1]) {
            strArr[i * 7 + 2][j] = "| P |   ";
          } else {
            strArr[i * 7 + 2][j] = "|   |   ";
          }
        }


        if (this.grid2D[i][j].getTreasureList().size() > 0) {
          strArr[i * 7 + 3][j] = "| T |   ";
        } else {
          strArr[i * 7 + 3][j] = "|   |   ";
        }

        strArr[i * 7 + 4][j] = "|___|   ";


        boolean thereIsDownwardEdge = false;
        for (int[] x : this.grid2D[i][j].getPossibleMoves()) {
          if (x[0] == 1 + i) {
            thereIsDownwardEdge = true;
            break;
          }
        }
        if (thereIsDownwardEdge) {
          strArr[i * 7 + 5][j] = "  |     ";
          strArr[i * 7 + 6][j] = "  |     ";
        } else {
          strArr[i * 7 + 5][j] = "        ";
          strArr[i * 7 + 6][j] = "        ";
        }

      }
    }

    StringBuilder strB = new StringBuilder();
    for (int i = 0; i < rows * 7; i++) {
      for (int j = 0; j < cols; j++) {
        strB.append(strArr[i][j]);
      }
      strB.append("\n");
    }

    return strB.toString();
  }

  /**
   * Gives the list of all the graph edges.
   *
   * @return List of all the Graph.Edges  .
   */
  public List<Graph.Edge> getDungeonEdges() {
    return new ArrayList<>(this.allUsedEdges);
  }

  /**
   * Gives the total number of the caves/ tunnels the dungeon have.
   *
   * @return int count of the nodes.
   */
  public int getTotalNodes() {
    return this.sizeOfDungeon[0] * this.sizeOfDungeon[1];
  }

  @Override
  public int[] getPlayerCurrentLocation() {
    return this.player.getCurrLoc();
  }
}
