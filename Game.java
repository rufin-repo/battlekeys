// Name: Rufin Hsu
// Date: Jan 19, 2025
// Description: Game and BattleView classes.

import java.util.*;
import java.util.List;
import java.io.*;
import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.*;

class Game implements KeyListener, ActionListener {
  JFrame appFrame; // Main app display frame
  JPanel appPanel; // Gameplay panel
  JPanel statPanel; // Statistics bottom panel
  JPanel titlePanel; // Title screen panel
  JPanel summaryPanel; // Post-game summary panel

  String helpTxt; // Help text, HTML-formatted.

  BattleView battleView;
  AttackWave aWave;
  HomeShip hShip;
  Thread thread; // Game animation thread
  GameStat stat;

  // Stores every torpedo group, indexed by its torpedo group sequence.
  // The List<TorpedoGroup> allows for thread-safe storage of duplicate torpedo groups.
  ConcurrentHashMap<String, List<TorpedoGroup>> tGroups;


  static long winWaitTime = 10000; // Time in ms to wait after a win before displaying the summary.
  long winTime = -1; // Time of overall game win.
  int waveCt; // Wave number, starting at 1.

  // Iterator for the list of phrases read in from the file.
  Iterator<String> phraseIt;

  JPanel activePanel = null;
  /*
   * Description: Sets the active (displayed) panel to the given panel.
   * Parameters: p: Panel to display.
   * Return: (none)
   */
  void setActivePanel(JPanel p) {
    if (activePanel != null) activePanel.setVisible(false);
    activePanel = p;
    p.setVisible(true);
  }

  /*
   * Desciption: Initialises the main JFrame, title and game panels
   * Parameters: (none)
   * Return: (none)
   */
  void initGraphics() {

    appFrame = new JFrame("Battle Keys");
    appFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    appFrame.setFocusable(true);
    appFrame.addKeyListener(this);

    JPanel globalCtn = new JPanel(new CardLayout());
    JScrollPane jsp = new JScrollPane(globalCtn);
    appFrame.add(jsp);

    titlePanel = new JPanel(new GridBagLayout());
    titlePanel.setBackground(Color.BLACK);
    GridBagConstraints tgbc = new GridBagConstraints();
    // tgbc.ipady = 50;
    tgbc.insets = new Insets(25, 0, 25, 0);

    globalCtn.add(titlePanel);

    G.init();

    // BufferedImage bi = ImageIO.read(new File("./es.png"));
    // JLabel name = new JLabel(new ImageIcon("./es.png"));
    JLabel name = new JLabel("BaTtLe KeYs");
    name.setFont(G.titleFont.deriveFont(100f) ); //new Font("Courier New", Font.PLAIN, 50));
    // name.setFont(btnFont.deriveFont(100f) ); //new Font("Courier New", Font.PLAIN, 50));
    name.setForeground(new Color(0xffd700)); //Color(60,60,180));
    tgbc.gridy++;
    titlePanel.add(name, tgbc);
    JButton btn = new JButton("Start");
    btn.setFont(G.btnFont.deriveFont(30f)); //new Font("Courier New", Font.PLAIN, 30));
    btn.setPreferredSize(new Dimension(240,40));
    btn.addActionListener(this);
    btn.setActionCommand("start");
    tgbc.gridy++;
    titlePanel.add(btn, tgbc);

    btn = new JButton("How to Play");
    btn.setFont(G.btnFont.deriveFont(30f)); //new Font("Courier New", Font.PLAIN, 30));
    btn.setPreferredSize(new Dimension(240,40));
    btn.addActionListener(this);
    btn.setActionCommand("help");
    tgbc.gridy++;
    titlePanel.add(btn, tgbc);

    btn = new JButton("Exit");
    btn.setFont(G.btnFont.deriveFont(30f)); //new Font("Courier New", Font.PLAIN, 30));
    btn.setPreferredSize(new Dimension(240,40));
    btn.addActionListener(this);
    btn.setActionCommand("exit");
    tgbc.gridy++;
    titlePanel.add(btn, tgbc);

    globalCtn.add(titlePanel);
    titlePanel.setVisible(true);
    activePanel = titlePanel;

    summaryPanel = new JPanel(new GridBagLayout());


    globalCtn.add(summaryPanel);
    summaryPanel.setVisible(false);


    appPanel = new JPanel(new GridBagLayout());
    globalCtn.add(appPanel);
    appPanel.setVisible(false);


    appFrame.setMinimumSize(new Dimension(800, 700));
    appFrame.setPreferredSize(new Dimension(700, 800));
    appFrame.pack();
    appFrame.setVisible(true);
  }

  /*
   * Description: Initialises graphics.
   * Parameters: (none)
   * Return: (none)
   */
  Game() {
    initGraphics();
  }

  /*
   * Description: Starts a new game and rebuilds the main game panel.
   * Parameters: (none)
   * Return: (none)
   */
  void startGame() {
    appFrame.requestFocus();

    waveCt = 0;

    stat = new GameStat(this);
    tGroups = new ConcurrentHashMap<>();
    hShip = new HomeShip(new Pt2(0, 0), this);

    ArrayList<String> phrases = new ArrayList<>();
    // Each line is a phrase.
    try {
      BufferedReader s = new BufferedReader(new FileReader("./MasterPhrases.txt"));

      String read = "";
      // Read from the file until all lines are exhausted
      while ((read = s.readLine()) != null) {
        phrases.add(read);
      }
      s.close();
    } catch (IOException ioe) {}

    phraseIt = phrases.iterator();
    aWave = new AttackWave(this, phraseIt.next(), ++waveCt);
    battleView = new BattleView(new Pt2(2.0, 2.0), this);

    appPanel.removeAll();
    appPanel.setBackground(Color.DARK_GRAY);
    GridBagConstraints gbc = new GridBagConstraints();

    gbc.insets = new Insets(0, 0, 0, 0);
    gbc.ipadx = 0;
    gbc.ipady = 0;
    gbc.weightx = 1;
    gbc.weighty = 1;
    gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.SOUTH;
    appPanel.add(battleView, gbc);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1;
    gbc.gridy = 1;
    gbc.weighty = 1;
    gbc.anchor = GridBagConstraints.SOUTH;
    stat.setPreferredSize(new Dimension(0, GameStat.bottomBarHgt));
    appPanel.add(stat, gbc);
    setActivePanel(appPanel);
    appFrame.revalidate();

    // Set up the animation/update thread.
    thread = new Thread(new Runnable() {
      public void run() {
        try {
          while (true) {
            appFrame.requestFocusInWindow();

            // Update the current attack wave
            if (aWave.update(battleView)) {
              // Move onto the next wave if the current one was cleared
              if (phraseIt.hasNext())
                aWave = new AttackWave(hShip.getGame(), phraseIt.next(), ++waveCt); // "this" is apparently not a Game object
              else if (winTime < 0) {
                winTime = System.currentTimeMillis();
              }
              else if (System.currentTimeMillis() - winTime > winWaitTime) {
                thread.interrupt();
                stat.showSummary();
              }
            };
            TorpedoGroup.moveFwd(tGroups, battleView);

            TorpedoGroup p = TorpedoGroup.getFocused(tGroups, hShip.getPos());

            hShip.moveFwd(battleView, p);

            battleView.repaint();
            stat.repaint();
            Thread.sleep(1000 / 60);
          }
        } catch (InterruptedException ie) {
        }
      }
    });
    thread.start();
  }

  /*
   * Description: Stops the current game and returns to the title screen.
   * Parameters: (none)
   * Return: (none)
   */
  void quitGame() {
    thread.interrupt();
    setActivePanel(titlePanel);
  }

  public static void main(String[] args) {
    new Game();
  }

  /*
   * Description: Handles button events from the title screen.
   * Parameters: ae: ActionEvent containing the button's action command
   * Return: (none)
   */
  public void actionPerformed (ActionEvent ae) {
    String aStr = ae.getActionCommand();
    // Start game
    if (aStr.equals("start")) {
      startGame();
    }
    // Exit program
    else if (aStr.equals("exit")) {
      System.exit(0);
    }
    // How to play
    else if (aStr.equals("help")) {
      // Try to bring up the browser to display the help document.
      try {
        File htmlFile = new File("BattleKeys.html");
        Desktop.getDesktop().browse(htmlFile.toURI());
      }
      catch (IOException e) {
        // Failed. Display some basic help info in a dialog.
        JOptionPane.showMessageDialog(battleView,
"""
BATTLE KEYS: A space typing game...
[Start]      : Start a New Game
[Exit]       : Close the App
[How to Play]:
Destroy the enemy torpedo chains by typing
out the text sequence accurately.
Guess and type out the Master Phrase to
wipe out the entire squadron in one go!

See BattleKeys.html for more details...
"""    ,
          "Quick Help",
          JOptionPane.INFORMATION_MESSAGE);
      }
    }
  }

  /*
   * Description: Handles keyboard events for the game.
   * Parameters: e: Keyboard event
   * Return: (none)
   */
  public void keyPressed(KeyEvent e) {
    if (hShip != null) hShip.pulse(e.getKeyChar());
  }

  // Other unused methods required by KeyListener
  public void keyReleased(KeyEvent e) {
  }

  public void keyTyped(KeyEvent e) {

  }
}

class BattleView extends JPanel {

  Pt2 cov; // Center of view, in BattleSpace units (BSUs)
  Pt2 viewD; // Dimensions of the view area in BSUs
  Pt2_i scrD; // Dimensions of the screen, in pixels
  Game game; // Game pointer

  /*
   * Description: Creates a new BattleView.
   * Parameters: dim: Dimensions of the view, in BattleSpace.
   *             g: Reference to the game object.
   * Return: (none)
   */
  BattleView(Pt2 dim, Game g) {
    viewD = dim;
    scrD = new Pt2_i(0, 0);
    game = g;
    cov = g.hShip.getPos();
  }

  /*
   * Description: Converts coordinates from BattleSpace to screen pixels.
   * Parameters: bsPt: Point in BattleSpace.
   * Return: Point in screen pixels.
   */
  Pt2_i toScrPt(Pt2 bsPt) {
    Pt2 out = new Pt2((bsPt.x - cov.x + viewD.x / 2) / viewD.x * scrD.x,
        (1.0 - (bsPt.y - cov.y + viewD.y / 2) / viewD.y) * scrD.y);
    return new Pt2_i(out);
  }

  /*
   * Description: Scales a dimension in BattleSpace to a dimension in screen pixels.
   * Parameters: bsDim: A dimension in BattleSpace.
   * Return: Dimension in screen pixels.
   */
  Pt2_i scale(Pt2 bsDim) {
    return new Pt2_i(bsDim.x / viewD.x * scrD.x, bsDim.y / viewD.y * scrD.y);
  }

  /*
   * Description: Returns the current scale factor from BattleSpace to screen display.
   * Parameters: (none)
   * Return: Current scale factor in a Pt2.
   */
  Pt2 getScaleFac() {
    return new Pt2(scrD.x/viewD.x, scrD.y/viewD.y);
  }

  /*
   * Description: Checks whether the given BattleSpace coordinate is in the view area.
   * Parameters: bsPt: BattleSpace coordinate to check.
   * Return: Whether the coordinate is in view.
   */
  boolean inView(Pt2 bsPt) {
    return bsPt.x < cov.x + viewD.x / 2
        && bsPt.x > cov.x - viewD.x / 2
        && bsPt.y < cov.y + viewD.y / 2
        && bsPt.y > cov.y - viewD.y / 2;
  }

  // If this does not match the current screen dimensions, the resize event methods are called.
  Pt2_i lastScrD = new Pt2_i(0, 0);

  /*
   * Description: Redraws everything on the game display.
   * Parameters: _g: Graphics element to draw with.
   * Return: (none)
   */
  public void paintComponent(Graphics _g) {
    Graphics2D g = (Graphics2D) _g.create();

    int pWidth = getParent().getWidth();
    int pHeight = getParent().getHeight();
    int scrSz = (int)(Math.min(pWidth, pHeight - GameStat.bottomBarHgt)*0.9);
    // If the screen dimensions have changed, call the resize handlers.
    if (!lastScrD.equals(new Pt2_i(pWidth, pHeight))) {
      scrD.x = scrSz;
      scrD.y = scrSz;
      lastScrD = new Pt2_i(pWidth, pHeight);
      setPreferredSize(new Dimension(scrSz, scrSz));
      revalidate();
      game.aWave.onResize(this);
      game.hShip.onResize(this);
      TxTorpedo.onResize(this);
    }

    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setBackground(Color.BLACK);
    g.clearRect(0, 0, pWidth, pHeight);
    game.hShip.draw(this, g);
    game.aWave.draw(this, g);

    // Draw every torpedo group.
    synchronized (game.tGroups) {
      for (List<TorpedoGroup> tgs : game.tGroups.values()) {
        synchronized (tgs) {
          for (TorpedoGroup tg : tgs)
            tg.draw(this, g);
        }
      }
    }
    game.stat.draw(this, g);
  }

  static final Font dbgFont = new Font("Arial", Font.PLAIN, 10);

  /*
   * Description: Draws a BattleSpace point labelled with its coordinate.
   * Parameters: bsCoord: Coordinate in BattleSpace
   *             g: Graphics object to draw onto.
   *             c: Colour to draw with.
   * Return: (none)
   */
  void dbgPt(Pt2 bsCoord, Graphics g, Color c) {
    dbgStr(bsCoord.toString(), bsCoord, g, c);
  }

  /*
   * Description: Draws a string.
   * Parameters: s: String to draw.
   *             bsCoord: Coordinate, in BattleSpace, to draw the string.
   *             _g: Graphics object to draw onto.
   *             c: Colour to draw with.
   * Return: (none)
   */
  void dbgStr(String s, Pt2 bsCoord, Graphics _g, Color c) {
    Graphics2D g = (Graphics2D) _g;
    Color prevC = g.getColor();
    Font prevF = g.getFont();
    Stroke prevS = g.getStroke();

    g.setStroke(new BasicStroke(1));
    g.setFont(dbgFont);
    g.setColor(c);
    Pt2_i scrCoord = toScrPt(bsCoord);
    int x = scrCoord.x;
    int y = scrCoord.y;
    g.drawLine(x - 3, y - 3, x + 3, y + 3);
    g.drawLine(x + 3, y - 3, x - 3, y + 3);
    g.drawString(s, scrCoord.x, scrCoord.y);

    g.setFont(prevF);
    g.setColor(prevC);
    g.setStroke(prevS);
  }

  /*
   * Description: Draws a point using the default colour (white).
   * Parameters: bsCoord: Coordinate, in BattleSpace, to draw at.
   *             g: Graphics object to draw onto.
   * Return: (none)
   */
  void dbgPt(Pt2 bsCoord, Graphics g) {
    dbgPt(bsCoord, g, Color.WHITE);
  }

}