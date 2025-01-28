// Name: Rufin Hsu
// Date: Jan 19, 2025
// Description: Pulse class. Interactions, movement and drawing of HomeShip pulses.
import java.util.*;
import java.util.List;
import java.awt.*;
import java.awt.geom.*;

class Pulse {
  private Game game;
  // private char ch;
  private double rad = 0;       // radius of pulse
  private double speed = 1.5;   // bsu/sec
  private long animTime = 750;  // Animation time of a pulse.
  private long startTime;       // in ms
  private Pt2 center;

  static final long clearTime = 10000; // Time until dissipation of a pulse.

  // whether pulse has made contact (and destroyed) a TorpedoGroup yet.
  // Pulses can only destroy one torpedo group at a time.
  private boolean contactedTG = false;
  boolean pendingRemove = false; // Whether the pulse needs to be removed.

  private List<TorpedoGroup> targetTGs; // Possible target TorpedoGroups.

  private int type = NORMAL;

  static final int NORMAL = 0;
  static final int CLEARALL = 1;

  /*
   * Description: Creates a new pulse.
   * Parameters: p: Center of the pulse
   *             g: Reference to the game object.
   * Return: (none)
   */
  Pulse(Pt2 p, Game g) {
    game = g;
    center = new Pt2(p);
    startTime = System.currentTimeMillis();
    game.hShip.updateAllMatchCts(true, this);
  }


  /*
   * Description: Checks if a point is in range of the pulse.
   * Parameters: pt: Point to check.
   * Return: Whether the point is in range.
   */
  boolean inRange(Pt2 pt) {
    Pt2 toCenter = pt.diff(center);
    return toCenter.magn() < rad;
  }

  /*
   * Description: Checks for a full-word match and updates the possible targets.
   * Parameters: (none)
   * Return: Whether a match has been found.
   */
  boolean checkFullMatch() {
    boolean matches = false;

    targetTGs = new ArrayList<>();
    synchronized (game.tGroups) {
      // Loop through all TorpedoGroups
      StringBuilder pulseStr = game.hShip.getActivePulseStr();
      for (int s = 0; s < pulseStr.length(); s++) {
        String sequence = pulseStr.substring(s);
        // Check if any substring matches the string
        List<TorpedoGroup> newTGs = game.tGroups.get(sequence);
        if (newTGs != null) { // Add torpedo groups to the list of possible targets.
          for (TorpedoGroup tg : newTGs) {
            targetTGs.add(tg);
            tg.matchCt = tg.seq.length(); // update to fully matched
          }
          matches = true;

        }
      }
    }
    return matches;
  }

  /*
   * Description: Updates the position of the pulse.
   * Parameters: bv: BattleView information.
   *             delta: Time elapsed, in ms, since the last update.
   * Return: Whether the Pulse should be removed.
   */
  boolean moveFwd(BattleView bv, long delta) {
    if (pendingRemove) return true;
    // Remove if pending
    // Remove if dissipated
    if (System.currentTimeMillis() - startTime > Pulse.clearTime) {
      return true;
    }
    // Remove if clearall pulse has cleared everything
    if (type == CLEARALL && !bv.inView(new Pt2(rad, rad))
    && game.tGroups.size() == 0) {
      game.aWave.shipsLeft = 0;
      return true;
    }

    // Do not pulse items far off-screen.
    boolean pulseActive = rad < Math.max(bv.viewD.x, bv.viewD.y);

    if (type == CLEARALL) { // clearall pulses pulse everything in range - TGs with/without torpedoes/enemy ships
      synchronized (game.tGroups) {
        for (List<TorpedoGroup> tglist : game.tGroups.values()) // loop through torpedo groups
        for (TorpedoGroup tg : tglist) {
          if (tg.torps.size() == 0) { // no torpedoes - only parent - lock parent
            if (tg.parentShip != null) tg.parentShip.lock(this);
            continue;
          }
          if (inRange(tg.torps.get(0).getPos()) && tg.locked != this) { // has torpedoes - lock torpedoes
            game.stat.addCombo(tg.seq, game.waveCt); // add to combo here
            tg.lock(this, tg.torps.get(0));
          }
        }
      }
    }

    // Check if any targets are in range
    if (targetTGs != null && pulseActive && !contactedTG) {
      for (TorpedoGroup tg : targetTGs) {
        TxTorpedo inRange = null;
        for (int i=0; i<tg.torps.size(); i++) { // loop through all torpedo groups in target
          TxTorpedo torp = tg.torps.get(i);
          if (inRange(torp.getPos())) { // check in range
            inRange = torp;
            break;
          }
        }
        if (inRange != null && tg.locked != this) { // do not lock more than once, do not pulse more than one TorpedoGroup
          game.stat.addCombo(tg.seq, game.waveCt); // add combo here
          contactedTG = true;
          tg.lock(this, inRange);
        }
      }
    }

    // move pulse forward
    rad += speed * delta / 1000;
    return false;
  }

  /*
   * Description: Draws the current pulse.
   * Parameters: bv: BattleView object.
   *             _g: Graphics object to draw onto.
   * Return: (none)
   */
  void draw(BattleView bv, Graphics _g) {
    long deltaT = System.currentTimeMillis() - startTime;
    if (deltaT > animTime) return;
    Graphics2D g = (Graphics2D) _g;
    Pt2_i hsPos = bv.toScrPt(center);
    int r = bv.scale(new Pt2(rad, 0)).x;

    // Create a vibrating string as a cool representation of the T3xt-pulse.
    Path2D wave = new Path2D.Double();

    int npt = (4+(int)3*r/5);               // Larger radius -> more points.
    double[] p = new double[npt*2+2];
    double addturn = Math.random();         // Give the vibrating string an additional random turn.

    for (int i=0; i<npt; i++) {
      double rr=(0.9 + Math.random()*0.2)*r;  // Random radial perturbation
      double a = 2*Math.PI*i/npt + addturn;   // The points are equally spaced angularly.
      p[i*2]  =hsPos.x + rr*Math.cos(a);
      p[i*2+1]=hsPos.y + rr*Math.sin(a);
    } // for (i)
    p[npt*2]=p[0]; p[npt*2+1]=p[1]; // Wrap around back to the first pt for the following loop.

    // The random points in p[] are used as control pts for the quadratic curves in the wave path.
    wave.moveTo((p[0]+p[2])/2,(p[1]+p[3])/2); // Use the mid-point of adjacent control pts as joining pts to ensure continuity.
    // wave.moveTo(p[0],p[1]); //<- for debugging the shape of the control points.
    for (int i=1; i<npt; i++) {
      int i2=i*2;
      wave.quadTo(p[i2],p[i2+1], (p[i2]+p[i2+2])/2, (p[i2+1]+p[i2+3])/2);
      // wave.lineTo(p[i2],p[i2+1]); //<- for debugging only.
    }
    wave.closePath();

    int grey = 105 - (int) (105 * Math.min((double) deltaT / animTime, 1));
    g.setStroke(new BasicStroke(1.0f));
    Color red2 = new Color(255, grey, grey);
    Color c = type==NORMAL ? new Color(grey, grey, grey) : red2;
    g.setColor(c);
    g.draw(wave);
  }

  // Setters
  public void setType(int type) {
    this.type = type;
  }
}
