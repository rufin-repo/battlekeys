// Name: Rufin Hsu
// Date: Jan 19, 2025
// Description: AttackWave class and resources.
// The AttackWave stores information about AttackWaves and sends groups of EnemyShips when applicable.

import java.util.*;
import java.awt.*;
import java.util.List;
import java.awt.geom.*;

class AttackWave {
  private String[] words;         // Words of the phrase, possibly shuffled.
  private int currWordIdx = 0;    // Current index of next word to output
  private String phrase;          // Destruction phrase for this wave.
  public String getPhrase() {
    return phrase;
  }

  private String phraseWords[];   // Words of the phrase, in order.
  private int phraseMatchCt = 0;  // # of words in the phrase matching
  private Game game;              // Reference to the game object.
  private long lastShipAdded; // Time of last EnemyShip addition
  private long startTime;     // Time of creation of this attack wave.
  private static long wavePauseTime = 3000; // Pause time before ships are added to this wave.
  private static long animTime = 500; // Animation time for fade-in/out of the wave number
  int shipsLeft; // Number of ships left in this wave.
  private static long addTime = 5000; // Time between addition of new EnemyShips to the wave.

  boolean canAdd = true;

  private int waveN; // Wave number.

  /*
   * Description: Creates a new AttackWave.
   * Parameters: g: Reference to game object.
   *             p: Destruction phrase for the wave.
   *             w: Wave number.
   * Return: (none)
   */
  AttackWave(Game g, String p, int w) {
    synchronized (g.hShip.getPulses()) {
      // Remove all existing pulses.
      for (Pulse pulse : g.hShip.getPulses()) {
        pulse.pendingRemove = true;
      }
    }
    waveN = w;
    startTime = System.currentTimeMillis();
    shipsLeft = 20;
    phrase = p;
    words = p.split(" ");

    // Shuffle the words.
    for (int i=0; i<words.length; i++) {
      int swapIdx = (int)(Math.random()*(words.length-i));
      String tmp = words[i];
      words[i] = words[swapIdx];
      words[swapIdx] = tmp;
    }

    phraseWords = p.split(" ");
    lastShipAdded = System.currentTimeMillis();
    game = g;
    game.stat.onNextWave();
  }

  /*
   * Description: Submit a word to update the phrase words
   * Parameters: str: String to update.
   * Return: (none)
   */
  void submitWord(String str) {

    // If the phrase matches the next phrase, increase the phrase match word count
    if (phraseMatchCt < phraseWords.length &&
    str.equals(phraseWords[phraseMatchCt]))
      phraseMatchCt++;
    else
      phraseMatchCt = 0;
  }

  /*
   * Description: Gets the next word in the phrase.
   * If the next word in the phrase does not exist, add the next word in the phrase.
   * Parameters: (none)
   * Return: (none)
   */
  String getWord() {
    if (canContinuePhrase()) {
      String out = words[currWordIdx];
      currWordIdx = (currWordIdx+1)%words.length;
      return out;
    }
    return phraseWords[phraseMatchCt];
  }

  /*
   * Description: Checks whether the next word in the phrase exists => the phrase can be continued.
   * Parameters: (none)
   * Return: Whether the phrase can be continued.
   */
  boolean canContinuePhrase() {
    // Phrase has been guessed already
    if (phraseMatchCt >= phraseWords.length) return true;
    String nextWord = phraseWords[phraseMatchCt];
    return game.tGroups.keySet().contains(nextWord);
  }

  private double[][] tracks = { // A set of predefined flight-path tracks.
    // Latest track designs with better visibility.
    // Data is stored in groups of 3.
    // 1st, 2nd values are coordinates. 3rd value is action (7=fill/load, 9=normal)
    {
      1.6,0,7,   0.1,-.4,9,   -.15,-.6,9,  0,-1,9,      .3,-1,9,
      .4,-.7,9,  -.5,-.5,9,   -1,-.2,9,    -0.9,.4,9,  -.7,.5,9,
      -.6,.25,9, -.75,0,9,    -1,.25,9,   -1.2,1.2,7,   -.2,0.8,9,
      .5,1,9,    .7,.8,9,     .4,.6,9,     0,.7,9,      0,1.2,9,
      .6,1.4,9,  1.7,.7,9,    1.6,.1,9
    },
    {
      1.6,0,7,    0.8,-.6,9,   .4,-.9,9,    -.7,-1,9,    -.6,-.7,9,
      -.2,-.6,9,  .3,-.65,9,   .4,-.9,9,    -.2,-1.1,9,  -.75,-.65,9,
      -.75,-.1,9, -.9,.2,9,   -1.3,.2,7,   -1.2,-.25,9,  -.8,-.4,9,
      -.5,-.15,9, -.7,.6,9,   -.4,.8,9,     .25,.75,9,    .2,.5,9,
      -.25,.5,9,  -.2,.8,9,    .6,.9,9,       1,.6,9
    },
    {
      1.6,0,7,     .7,.65,9,    -.1,.5,9,    -.75,.6,9,   -.75,.9,9,
      .2,.9,9,    .2,.75,9,    -.2,.4,9,    -.6,.25,9,   -.8,.7,9,
      -.4,.75,9,   -.25,.25,9,  -.7,.2,9,    -.75,-.75,9,  -.25,-1.3,7,
      .4,-.75,9,   .3,-.4,9,    -.25,-.7,9,  .1,-.9,9,     .9,-.8,9
    }
  };

  private int _trackIdx=2; // Index of the next flight path to use.
  // Every EnemyGroup uses the same flight path, just offsetted and rotated.

  /*
   * Description: Creates a new flight path.
   * Parameters: shipIdx: Index of the ship in the EnemyShip group, for offset.
   *             angle: Base angle rotation of the FlightPath (further rotated according to shipIdx).
   * Return: A transformed FlightPath.
   */
  private FlightPath mkFlightPath(int shipIdx, double angle) {
    if (shipIdx==0) _trackIdx++; // A new base flightpath for every group of enemyships
    double[] trk = tracks[_trackIdx%tracks.length];
    List<FPt> path = Collections.synchronizedList(new ArrayList<>());
    // Loop through flight track data in groups of 3
    for (int i=0; i<trk.length; i+=3) {
      int ptType = FPt.NONE;
      if (i==0 || trk[i+2]==7) // Point is for loading/filling torpedoes.
        ptType=FPt.FILL;
      else {
        int prv = i-3;
        int nxt = i+3;
        if (nxt>=trk.length) nxt=0;
        double vix=-trk[i], viy=-trk[i+1];
        double v1x=trk[i]-trk[prv], v1y=trk[i+1]-trk[prv+1];
        double v2x=trk[nxt]-trk[i], v2y=trk[nxt+1]-trk[i+1];
        if (vix*v1x+viy*v1y>0 && vix*v2x+viy*v2y>0 &&
            G.crossProductZ(v1x, v1y, vix, viy) * G.crossProductZ(v2x,v2y, vix,viy)<0)
        { // Direction before and after will sweep past HomeShip position => can launch
          ptType=FPt.LAUNCH;
        }
      }
      path.add(new FPt(trk[i], trk[i+1], ptType));
    } // for (i)

    FlightPath f = new FlightPath(path);
    return new FlightPath(f, G.normalizeAngle(angle + shipIdx*0.2), game.hShip.getPos());
  }

  /*
   * Description: Adds the specified number of enemy ships to the attack wave.
   * Parameters: ct: Number of ships to add.
   * Return: Whether the current ship supply is exhausted. When the supply is exhausted, the attackWave is complete.
   */
  boolean addEnemyGroup(int ct) {
    if (shipsLeft<=0) return true;    // Squandron exhausted.
    if (!canAdd) return false; // Cannot add new ships, but the number of ships left is not 0.
    lastShipAdded = System.currentTimeMillis();
    double ang = Math.random()*2*Math.PI;
    for (int shipidx=0; shipidx<ct && shipsLeft>0; shipidx++) {
      shipsLeft--;
      String t3xt = getWord();

      new EnemyShip(t3xt, game, mkFlightPath(shipidx, ang), shipidx*1000);
      // Staggered launch with 1000 ms delay per ship.
    }
    return shipsLeft<=0;
  }

  /*
   * Description: Updates the AttackWave by adding enemy groups when necessary.
   * Parameters: bv: Unused.
   * Return: Whether the AttackWave is complete.
   */
  boolean update(BattleView bv) {
    // Do not do anything before the wave grace period has ended.
    if (System.currentTimeMillis() - startTime < wavePauseTime) return false;

    // Add one ship every addTime
    if (System.currentTimeMillis() - lastShipAdded > addTime) {
      return addEnemyGroup(1);
    }
    // Refill ships after they are deleted
    if (game.tGroups.size() == 0) {
      return addEnemyGroup(5);
    }
    // Add words to allow completion of the destruction phrase
    if (!canContinuePhrase()) {
      return addEnemyGroup(1);
    }
    // No updates performed
    return false;
  }

  /*
   * Description: Calls the resize handlers for all EnemyShips.
   * Parameters: bv: BattleView to be passed to the resize handlers
   * Return: (none)
   */
  void onResize(BattleView bv) {
    synchronized (game.tGroups) {
      // Loop through all torpedo groups
      for (List<TorpedoGroup> tgl : game.tGroups.values()) {
        for (TorpedoGroup tg : tgl)
          if (tg.parentShip != null)
            tg.parentShip.onResize(bv);
      }
    }
  }

  private Font waveInfoFont = G.titleFont !=null ? G.titleFont.deriveFont(80f) : new Font("Arial", Font.BOLD, 80);

  /*
   * Description: Draws the wave number display if applicable
   * Parameters: bv: BattleView info
   *             g: Graphics object to draw onto.
   * Return: (none)
   */
  void draw(BattleView bv, Graphics2D g) {
    long timeSinceWave = System.currentTimeMillis() - startTime;
    if (timeSinceWave < wavePauseTime + animTime) { // Need to animate the wave number

      FontMetrics fm = g.getFontMetrics(waveInfoFont);
      String waveStr = "WaVe "+waveN;
      Rectangle2D bounds = fm.getStringBounds(waveStr, g);
      int opacity;
      if (timeSinceWave > animTime) // fade out
        opacity = 255 - (int)(255*(timeSinceWave - wavePauseTime)/animTime);
      else // fade in
        opacity = (int)(255*timeSinceWave/animTime);
      if (opacity < 0) opacity = 0;
      if (opacity > 255) opacity = 255;
      g.setColor(new Color(0xff, 0xd7, 0, opacity));
      g.setFont(waveInfoFont);
      Pt2_i centerPos = bv.toScrPt(new Pt2(0, 0)); // center vertically and horizontally
      g.drawString(waveStr, (int)(centerPos.x - bounds.getWidth()/2), 100);
    }
  }
}