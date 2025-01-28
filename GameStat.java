// Name: Rufin Hsu
// Date: Jan 19, 2025
// Description: Game statistics manager.

import java.util.*;
import java.awt.*;
import javax.swing.*;

import java.awt.event.*;
import java.awt.geom.*;

public class GameStat extends JPanel implements ActionListener {
  int pts; // Number of points.

  private JLabel pointsL; // JLabel for number of points for the bottom bar.

  private StringBuilder comboStr = new StringBuilder(); // Current combo string (with spaces).
  public StringBuilder getComboStr() {
    return comboStr;
  }

  final static int bottomBarHgt = 50; // Height of the bottom statistics bar, in pixels.

  private ArrayList<String> bestCombo; // Best combo per level.
  boolean canStartWord = false; // Whether a new word can be started without breaking the combo as it usually would.

  Game game;

  HashMap<String, Integer> seenWords; // Running frequency count of words destroyed.

  Font labelFont = new Font("Arial", Font.PLAIN, 17);
  Font btnFont = new Font("Arial", Font.PLAIN, 17);
  Font valueFont = new Font("Arial", Font.BOLD, 20);

  /*
   * Description: sets up the graphics for the bottom statistics panel in the game screen.
   * Parameters: g: Reference to the game object.
   * Return: (none).
   */
  GameStat(Game g) {
    bestCombo = new ArrayList<>();

    seenWords = new HashMap<>();
    game = g;

    setBackground(Color.WHITE);

    JPanel ptsPanel = new JPanel();
      ptsPanel.setBackground(Color.WHITE);
      pointsL = new JLabel("0");
      pointsL.setFont(G.LCDFont.deriveFont(Font.BOLD, 24f));
      ptsPanel.add(pointsL);
      JLabel lab_p = new JLabel("Pts");
      lab_p.setFont(G.typoFont.deriveFont(Font.BOLD, 24f));
      ptsPanel.add(lab_p);
    add(ptsPanel);

    JButton stopBtn = new JButton("Quit");
      // stopBtn.setFont(btnFont);
      stopBtn.setFont(G.btnFont.deriveFont(22f));
      stopBtn.setPreferredSize(new Dimension(110,40));
      stopBtn.addActionListener(this);
      stopBtn.setActionCommand("quit");
    add(stopBtn);

    JButton restartBtn = new JButton("Restart");
      restartBtn.setMargin(new Insets(0, 0, 0, 0));
      restartBtn.setFont(G.btnFont.deriveFont(22f));
      restartBtn.setPreferredSize(new Dimension(110,40));
      restartBtn.addActionListener(this);
      restartBtn.setActionCommand("restart");
    add(restartBtn);
  }

  /*
   * Description: Action handler for button events.
   * Parameters: ae: Action event containing action command.
   * Return: (none)
   */
  public void actionPerformed(ActionEvent ae) {
    String aStr = ae.getActionCommand();
    game.quitGame();
    // Restarting the game means quitting and starting.
    if (aStr.equals("restart")) {
      game.quitGame();
      game.startGame();
    }
    else if (aStr.equals("quit")) { // Return to main menu.
      game.quitGame();
    }
    else { // Unknown action command.
      G.sysprtf("Invalid action command.\n");
    }
  }

  /*
   * Description: Adds the specified word to the seen words set.
   * Parameters: word: Word to add.
   * Return: (none)
   */
  void addWord(String word) {
    int i = seenWords.getOrDefault(word, 0);
    seenWords.put(word, i+1);
  }

  /*
   * Description: Event handler for new waves. Sets the best combo for this wave to
   * an empty string.
   * Parameters: (none)
   * Return: (none)
   */
  void onNextWave() {
    bestCombo.add("");
  }

  /*
   * Description: Rebuilds and updates the post-game summary JPanel.
   * Parameters: (none)
   * Return: (none)
   */
  void showSummary() {
    JPanel sPanel = game.summaryPanel;
    sPanel.setBackground(new Color(0xffd700));
    sPanel.removeAll();
    sPanel.setLayout(new BorderLayout());
    JPanel ctnPanel = new JPanel(new GridBagLayout()); // for aligning to top.
    ctnPanel.setBackground(sPanel.getBackground());
    sPanel.add(ctnPanel, BorderLayout.NORTH);

    GridBagConstraints gbc = new GridBagConstraints();
    ctnPanel.setVisible(true);
    gbc.gridwidth = 2;
    gbc.gridy = 0;
    gbc.gridx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.ipady = 30;
    JLabel title = new JLabel("YoU wIn");
    title.setFont(G.titleFont.deriveFont(80f));
    ctnPanel.add(title, gbc);


    gbc.ipady = 30;
    gbc.gridwidth = 1;
    gbc.gridy++;
    JLabel sText = new JLabel("Score:");
    sText.setFont(G.typoFont.deriveFont(Font.BOLD, 24f));
    ctnPanel.add(sText, gbc);

    gbc.gridx = 1;

    JLabel sVal = new JLabel(""+pts);
    sVal.setFont(G.typoFont.deriveFont(Font.BOLD, 24f));
    ctnPanel.add(sVal, gbc);

    gbc.gridx = 0;

    gbc.gridy++;
    gbc.gridx = 0;
    gbc.gridwidth = 2;
    JLabel mostCommonTtl = new JLabel("Most common torpedoes destroyed:");
    mostCommonTtl.setFont(G.typoFont.deriveFont(Font.BOLD, 24f));
    ctnPanel.add(mostCommonTtl, gbc);
    gbc.gridwidth = 1;
    gbc.ipady = 0;
    TreeSet<FreqData> mostCommon = new TreeSet<>();

    // Sorts the word frequency data by frequency.
    for (String s : seenWords.keySet()) {
      mostCommon.add(new FreqData(s, seenWords.get(s)));
    }

    gbc.gridwidth = 1;
    Iterator<FreqData> it = mostCommon.descendingIterator();
    // Display the 20 most common torpedo types destroyed.
    for (int i=0; i<20; i++) {
      if (!it.hasNext()) break;
      gbc.gridx = 0;
      gbc.gridy++;
      FreqData wd = it.next();
      JLabel hdrLabel = new JLabel(String.format("#%d. %dx",i+1, wd.ct));
      hdrLabel.setFont(G.typoFont.deriveFont(Font.BOLD, 20f));

      ctnPanel.add(hdrLabel, gbc);
      gbc.gridx = 1;
      JLabel wLabel = new JLabel(wd.w);
      wLabel.setFont(G.typoFont.deriveFont(Font.BOLD, 20f));
      ctnPanel.add(wLabel, gbc);
    }

    gbc.ipady = 30;
    gbc.gridy++;
    gbc.gridx = 0;
    gbc.gridwidth = 2;
    JLabel comboTitleLab = new JLabel("Longest combo sequences:");
    comboTitleLab.setFont(G.typoFont.deriveFont(Font.BOLD, 24f));
    ctnPanel.add(comboTitleLab, gbc);

    gbc.ipady = 0;
    // Display the best combo string per wave.
    for (int i=0; i<bestCombo.size(); i++) {
      gbc.gridx = 0;
      gbc.gridy++;
      gbc.gridwidth = 1;
      JLabel comboLab = new JLabel(String.format("Wave %d: ", i+1));
      comboLab.setFont(G.typoFont.deriveFont(Font.BOLD, 20f));
      ctnPanel.add(comboLab, gbc);
      gbc.gridx = 1;
      String comboStr = bestCombo.get(i);
      int len = comboStr.length();
      if (comboStr.length() > 20) comboStr = comboStr.substring(0, 21)+"...";
      JLabel comboLab2 = new JLabel(comboStr.length()==0 ? "(None)":String.format("(%d) %s", len-1, comboStr));
      comboLab2.setFont(G.typoFont.deriveFont(Font.BOLD, 20f));
      ctnPanel.add(comboLab2, gbc);
    }

    gbc.gridwidth = 2;
    gbc.gridy++;
    gbc.gridx = 0;
    gbc.insets = new Insets(30, 0, 0, 0);
    JButton rtnBtn = new JButton("Return to main menu");
    rtnBtn.setFont(G.btnFont.deriveFont(22f));
    rtnBtn.setActionCommand("quit");
    rtnBtn.addActionListener(this);
    ctnPanel.add(rtnBtn, gbc);

    game.setActivePanel(sPanel);
  }

  /*
   * Description: Adds a word to the combo string.
   * Parameters: str: Word to be added
   *             waveCt: current wave count (in case of concurrency issues)
   * Return: (none)
   */
  void addCombo(String str, int waveCt) {
    addWord(str);
    game.aWave.submitWord(str);
    comboStr.append(str);
    comboStr.append(' ');
    game.stat.updateBestCombo(waveCt);
  }

  /*
   * Description: Clears the current combo.
   * Parameters: waveCt: current wave count
   * Return: (none)
   */
  void clearCombo(int waveCt) {
    // game.stat.updateBestCombo(waveCt);
    game.aWave.submitWord("");
    pts += comboStr.length();
    comboStr = new StringBuilder();
  }

  /*
   * Description: Gets the current combo length.
   * Parameters: (none)
   * Return: Length of the current combo.
   */
  int getCombo() {
    return comboStr.length();
  }

  /*
   * Description: Updates the best combo if necessary.
   * Parameters: (none)
   * Return: (none)
   */
  void updateBestCombo(int waveCt) {
    if (bestCombo.get(waveCt-1).length() < comboStr.length())
      bestCombo.set(waveCt-1, comboStr.toString());
  }

  /*
   * Description: Updates the number of points in the stats JPanel.
   * Parameters: (none)
   * Return: (none)
   */
  void updateDisplays() {
    pointsL.setText(""+pts);
  }

  Font statFont = new Font("Arial", Font.PLAIN, 20); // Font for statistics.
  /*
   * Description: Draws the current statistics onto the game JPanel.
   * Parameters: bv: BattleView for the game.
   *             g: Graphics object to draw onto.
   * Return: (none)
   */

  void draw(BattleView bv, Graphics2D g) {
    g.setFont(G.pulseFont.deriveFont(30f));
    FontMetrics fm = g.getFontMetrics(g.getFont());
    Rectangle2D bounds = fm.getStringBounds(game.hShip.getActivePulseStr().toString(), g);
    // g.setColor(Color.WHITE);
    g.setColor(new Color(0xaa,0xaa,0xff));

    int padding = 10;
    int left = bounds.getWidth()>bv.scrD.x-2*padding ? -(int)(bounds.getWidth() - bv.scrD.x + padding) : padding;
    g.drawString(game.hShip.getActivePulseStr().toString(), left, 50);

    g.setFont(G.comboFont.deriveFont(30f));
    g.setColor(new Color(0xffd700));
    String dispStr = comboStr.toString();
    // Trim the string if it is too long.
    fm = g.getFontMetrics(g.getFont());
    bounds = fm.getStringBounds(comboStr.toString(), g);
    left = bounds.getWidth()>bv.scrD.x-2*padding ? -(int)(bounds.getWidth() - bv.scrD.x + padding) : padding;
    // if (comboStr.length() > 30) dispStr = dispStr.substring(comboStr.length()-30);
    g.drawString(dispStr, left, bv.scrD.y - padding);
  }

}

class FreqData implements Comparable<FreqData> {
  String w; // Word
  int ct; // Frequency

  /*
   * Description: Creates a new WordData object.
   * Parameters: s: Word string, v: Frequency value.
   * Return: (none)
   */
  FreqData(String s, int v) {
    w = s;
    ct = v;
  }

  /*
   * Description: Compares a WordData object to another by frequency.
   * Parameters: o2: WordData object to compare to.
   * Return: <0 if this is less than o2,
   *         >0 if this is greater than o2,
   *         =0 if this is identical to o2.
   */
  public int compareTo(FreqData o2) {
    int delta = ct - o2.ct;
    if (delta == 0) return w.compareTo(o2.w);
    else return delta;
  }
}