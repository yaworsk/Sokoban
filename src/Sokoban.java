import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.ArrayList;
import java.io.*;

/**
 * @author Peter
 *
 * CCPS 109 Sokoban Project
 *
 */
public class Sokoban extends JPanel{
    //whether we should print the board
    private boolean isReady = false;
    
    //level variables
    private int level;
    private int moves = 0;
    private int time = 0;
    private static final int DELAY = 1000;
    private Timer timer;
    private JLabel levelLabel = new JLabel ("Level: " + (level + 1));
    private JLabel levelMovesLabel = new JLabel ("Moves: " + (moves));
    private JLabel levelTimerLabel = new JLabel("Time: " + time);
    
    //Board variables
    private LevelReader lr;
    private Contents[][] board;
    
    //Board's Rectangles
    private static final int PREF_W = 32;
    private static final int PREF_H = PREF_W;
    private List<Rectangle> recs;
    
    //images
    private Image imgCrate;
    private Image imgBoxOnGoal;
    private Image imgEmpty;
    private Image imgGoal;
    private Image imgPlayer;
    private Image imgPlayerOnGoal;
    private Image imgWall;
    
    /**
     * Method to add rectangles to the List of Rectangles
     *  
     * @param x top left corner of rectangle
     * @param y top left corner of rectangle
     * @param width width of rectangle
     * @param height height of rectangle
     */
    public void addSquare(int x, int y, int width, int height)  {
        Rectangle rect = new Rectangle(x, y, width, height);
        recs.add(rect);
    }
    
    /**
     * ActionListern for timer counter
     */
    ActionListener timerCounter = new ActionListener() {
        public void actionPerformed(ActionEvent e) {            
            setTimerTime();
            setTimerLabel();
        }
    };
    
    /**
     * Key Listener for Previous and Next Levels
     */
    private class LevelControlKeyListener extends KeyAdapter {
        public void keyPressed(KeyEvent e) {
            //N (78) Pressed, Next Level
            if (e.getKeyCode() == 78) {
                nextLevel();
                initLevel(level);
            }
            //P (80) Pressed, Previous Level
            if (e.getKeyCode() == 80) {
                prevLevel();
                initLevel(level);
            }
            //R (82) Pressed, Restart Level
            if (e.getKeyCode() == 82) initLevel(level);
        }
    }
    
    /**
     * Key Listener for Directional Controls
     */
    private class ControlsKeyListener extends KeyAdapter {
        public void keyPressed(KeyEvent e) {
            boolean exit = false;
            int dx = 0;
            int dy = 0;
        
            //left
            if (e.getKeyCode() == 37) dx = -1;
            //top
            if (e.getKeyCode() == 38) dy = -1;
            //right
            if (e.getKeyCode() == 39) dx = 1;
            //bottom
            if (e.getKeyCode() == 40) dy = 1;
            
            //loop through the board to find the player and move them
            for (int x = 0; x <= board.length - 1; x++) {
                for (int y = 0; y <= board[x].length - 1; y++) {
                    if (board[x][y].toString() == "PLAYER" || board[x][y].toString() == "PLAYERONGOAL") {
                        if (isMoveable(board[x + dx][y + dy])) {
                            //move the player
                            movePlayer(x, y, dx, dy);
                            //increase the number of moves
                            incMoves();
                            //bc the board is 2d, we need a boolean to check so we can get out
                            exit = true;
                            break;
                        } //end can move
                    } //end if player
                } //end loop columns
                if (exit) break;
            } //end loop rows
            repaint();
            if (checkWin()) {
                nextLevel();
                initLevel(level);
            }
        }
    }
    
    /**
     * Check to see if all the blocks are on the goal tiles
     * @return true if all the blocks are on the goal tiles
     */
    public boolean checkWin() {
        boolean ret = true;
        for (int x = 0; x <= board.length - 1; x++) {
            for (int y = 0; y <= board[x].length - 1; y++) {
                if (board[x][y].toString() == "BOX") {
                    ret = false;
                    break;
                }
            }
        }
        //TODO: Add a pause if the level is complete
        return ret;
    }
    
    /**
     * Move the player around the board
     * @param nextTile the next tile the player is moving to
     * @param x the x value for the board tile where the player currently is
     * @param y the y value for the board tile where the player currently is
     * @param dx how many tiles along the x axis to move the player
     * @param dy how many tiles along the y axis to move the player
     */
    public void movePlayer (int x, int y, int dx, int dy) {
        Contents nextTile = board[x + dx][y + dy];
        Contents prevTile = (board[x][y].toString() == "PLAYER") ? Contents.EMPTY : Contents.GOAL;
        boolean moved = false;
        
        //check if the next tile is empty or a goal tile
        if (isE(nextTile) && !moved) {
            board[x + dx][y + dy] = Contents.PLAYER;
            moved = true;
        }
        
        if (isG(nextTile) && !moved) {
            board[x + dx][y + dy] = Contents.PLAYERONGOAL;
            moved = true;
        }

        if (isBoxOnGoal(nextTile, board[x + dx *2][y + dy *2]) && !moved) {
            board[x + dx][y + dy] = Contents.PLAYERONGOAL;
            if (isGoal(board[x + dx * 2][y + dy * 2])) {
                board[x + dx * 2][y + dy * 2] = Contents.BOXONGOAL;
            } else {
                board[x + dx * 2][y + dy * 2] = Contents.BOX;
            }
            moved = true;
        }
        
        //check if the next tile is a box
        if (isBox(nextTile, board[x + dx *2][y + dy *2]) && !moved) {
            board[x + dx][y + dy] = Contents.PLAYER;
            if (isGoal(board[x + dx * 2][y + dy * 2])) {
                board[x + dx * 2][y + dy * 2] = Contents.BOXONGOAL;
            } else {
                board[x + dx * 2][y + dy * 2] = Contents.BOX;
            }
            moved = true;
        }
        
        //if the player moved, update the tile it moved from
        if (moved) {
            board[x][y] = prevTile;
        }
    }
    
    /**
     * Check to see if the player can move to the next tile
     * 
     * @param nextTile The next tile the player is to move on to
     * @return
     */
    public boolean isMoveable (Contents nextTile) {
        return (nextTile.toString() == "EMPTY" || nextTile.toString() == "GOAL" || nextTile.toString() == "BOX" || nextTile.toString() == "BOXONGOAL") ? true : false;
    }
    
    /**
     * Check to see if the next tile is an empty or goal tile
     * 
     * @param nextTile The next tile the player is to move on to
     * @return True if the next tile is an empty or goal tile
     */
    public boolean isE (Contents nextTile) {
        return (nextTile.toString() == "EMPTY") ? true : false;
    }
    
     public boolean isG (Contents nextTile) {
         return (nextTile.toString() == "GOAL") ? true : false;
     }
    
    /**
     * Check if the next tile is a wall
     * @param nextTile The next tile the player is to move on to
     * @return
     */
    public boolean isWall (Contents nextTile) {
        return (nextTile.toString() == "WALL") ? true : false;
    }
    
    /**
     * Check to see if the next tile is a box and can be moved
     * 
     * @param nextTile The next tile the player is to move on to
     * @param doubleNextTile The tile the next tile is to be moved to
     * @return True if the next tile can be moved to the subsequent tile
     */
    public boolean isBox (Contents nextTile, Contents doubleNextTile) {
        return (nextTile.toString() == "BOX"  && (doubleNextTile.toString() == "EMPTY" || doubleNextTile.toString() == "GOAL")) ? true : false;
    }
    
    /**
     * Check to see if the next tile is a goal tile
     * 
     * @param nextTile The next tile
     * @return True if the next tile tile is a goal tile
     */
    public boolean isGoal (Contents nextTile) {
        return (nextTile.toString() == "GOAL") ? true : false;
    }
    
    /**
     * Check to see if the next tile is a box on a goal tile and can be moved
     * 
     * @param nextTile The next tile the player is to move on to
     * @param doubleNextTile The tile the next tile is to be moved to
     * @return True if the box   tile can be moved to the subsequent tile
     */
    public boolean isBoxOnGoal (Contents nextTile, Contents doubleNextTile) {
        return (nextTile.toString() == "BOXONGOAL"  && (doubleNextTile.toString() == "EMPTY" || doubleNextTile.toString() == "GOAL")) ? true : false;
    }

    
    /**
     * Increase the number of moves a player has taken by 1 and redraw the JLabel
     */
    private void incMoves() {
        moves += 1;
        setMovesLabel();
    }
    
    /**
     * Reset the moves to zero
     */
    private void resetMoves() {
        moves = 0;
        setMovesLabel();
    }
    
    /**
     * Set the Moves Label
     */
    private void setMovesLabel() {
        levelMovesLabel.setText("Moves: " + (moves));
    }
    
    
    /**
     * Go to the next level
     */
    private void nextLevel () {
        //TODO: Check if the user just won the game by seeing how many moves they made
        level = (level == 154 ? 0 : level + 1);
    }
    
    /**
     * Go to the previous level
     */
    private void prevLevel() {
        level = (level == 0 ? 154 : level - 1);
    }
    
    /**
     * Set the Level Label
     */
    private void setLevelLabel() {
        levelLabel.setText("Level: " + (level + 1));
    }
    
    /**
     * Reset the timer time
     */
    private void resetTimerTime() {
        time = 0;
        timer.restart();
    }
    
    /**
     * Set the level time
     */
    private void setTimerTime() {
        time += 1;
    }
    
    /**
     * Set the level Timer Label
     */
    private void setTimerLabel() {
        levelTimerLabel.setText("Time: " + time);
    }
    
    /**
     * Update the file with the last level played
     */
    public void writeLastLevel() {
        try {
            PrintWriter w = new PrintWriter("lastLevel.txt");
            w.print(level);
            w.close();
        } catch (IOException e) {
            System.out.println("Error writing file!");
        }
    }
    
    /**
     * Get the last level played
     */
    private void getLastLevel() {
        
        try {
            BufferedReader br = new BufferedReader(new FileReader("lastLevel.txt"));
            String txt = br.readLine();
            try {
                level = Integer.valueOf(txt);
            } catch (NumberFormatException e) {
                System.out.println("Error reading level number");
                level = 0;
            }
            br.close();
        }catch(IOException e) {
            System.out.println("Error reading level file!");
            level = 0;
        }
    }
    
    /**
     * Sokoban Constructor. Get's the levels and initializes the current level
     * 
     * @param fileName Sokoban Level File
     */
    public Sokoban (String fileName) {
        //initialize all the images
        imgCrate = Toolkit.getDefaultToolkit().getImage("img/crate.png");
        imgBoxOnGoal = Toolkit.getDefaultToolkit().getImage("img/boxongoal.png");
        imgEmpty = Toolkit.getDefaultToolkit().getImage("img/empty.png");
        imgGoal = Toolkit.getDefaultToolkit().getImage("img/goal.png");
        imgPlayer = Toolkit.getDefaultToolkit().getImage("img/player.png");
        imgPlayerOnGoal = Toolkit.getDefaultToolkit().getImage("img/playerongoal.png");
        imgWall = Toolkit.getDefaultToolkit().getImage("img/wall.png");
        
        //add the images to the mediatracker
        MediaTracker m = new MediaTracker(this);
        m.addImage(imgCrate, 0);
        m.addImage(imgBoxOnGoal, 0);
        m.addImage(imgEmpty, 0);
        m.addImage(imgGoal, 0);
        m.addImage(imgPlayer, 0);
        m.addImage(imgPlayerOnGoal, 0);
        m.addImage(imgWall, 0);
        try { m.waitForAll(); } catch(InterruptedException e) { }
        
        //set the screen
        setFocusable(true);
        this.setPreferredSize(new Dimension(1000, 575));
        
        //add key listeners
        this.addKeyListener(new ControlsKeyListener());
        this.addKeyListener(new LevelControlKeyListener());
        
        //add the level and move label
        this.add(levelLabel);
        this.add(levelMovesLabel);
        this.add(levelTimerLabel);
        
        //set the timer
        timer = new Timer (DELAY, timerCounter);
        
        //get the last level
        getLastLevel();
        
        //start the game
        lr = new LevelReader();
        lr.readLevels(fileName);
        initLevel(this.level);
    }
    
    /**
     * Build the Sokoban level board and array of rectangles for tiles
     * 
     * @param level The Sokoban Level
     */
    public void initLevel(int level) {
        int width = lr.getWidth(level);
        int height = lr.getHeight(level);
        this.board = new Contents [width][height];
        recs = new ArrayList<Rectangle>();
        for (int x = 0; x <= width - 1; x++) {
            for (int y = 0; y <= height - 1; y++) {
                this.board[x][y] = lr.getTile(level, x, y);
                addSquare(x * 32, y * 32, PREF_W, PREF_H);
            }
        }
        this.isReady = true;
        setLevelLabel();
        resetMoves();
        resetTimerTime();
        writeLastLevel();
        repaint();
    }

    public void paintComponent(Graphics g) {
        if(!isReady) return;
        super.paintComponent(g);  
        Graphics2D g2 = (Graphics2D) g;
        int xVal;
        int yVal;
        for (int x = 0; x <= board.length - 1; x++) {
            for (int y = 0; y <= board[x].length - 1; y++) {
                xVal = (int)recs.get((board[x].length)*x+y).getX() + 20;
                yVal = (int)recs.get((board[x].length)*x+y).getY() + 20;
                if (board[x][y].toString() == "BOX") g2.drawImage(imgCrate, xVal, yVal, this);
                if (board[x][y].toString() == "WALL") g2.drawImage(imgWall, xVal, yVal, this);
                if (board[x][y].toString() == "EMPTY") g2.drawImage(imgEmpty, xVal, yVal, this);
                if (board[x][y].toString() == "GOAL") g2.drawImage(imgGoal, xVal, yVal, this);
                if (board[x][y].toString() == "BOXONGOAL") g2.drawImage(imgBoxOnGoal, xVal, yVal, this);
                if (board[x][y].toString() == "PLAYER") g2.drawImage(imgPlayer, xVal, yVal, this);
                if (board[x][y].toString() == "PLAYERONGOAL") g2.drawImage(imgPlayerOnGoal, xVal, yVal, this);
            }
        }
    }
    
    /**
     * Main file execution.
     * @param args
     */
    public static void main(String args[]) {
        JFrame f = new JFrame("Sokoban");
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.setLayout(new FlowLayout());
        f.add(new Sokoban("m1.txt"));
        f.pack();
        f.setVisible(true); 
    }
}
