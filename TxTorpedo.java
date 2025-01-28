// Name: Rufin Hsu
// Date: Jan 19, 2025
// Description: TxTorpedo class and TorpedoGroup class.
// All EnemyShips and TextTorpedoes are part of a TorpedoGroup. Updates to
// EnemyShips and TextTorpedoes are all handled at once by the static methods in TorpedoGroup.

import java.util.*;
import java.util.concurrent.*;
import java.util.List;
import java.awt.*;
import java.awt.geom.*;

class TxTorpedo {
  // getters
  public Pt2 getPos() {
    return pos;
  }

  public Pt2 getVel() {
    return vel;
  }

  private Pt2 pos, vel; // Position and velocity, in BattleSpace.
  private char ch; // Character of the torpedo.
  private Pt2 targetPt; // Target position. In follow mode, the torpedo will keep itself at minDist from the target position.
  public Pt2 getTargetPt() {
    return targetPt;
  }

  public void setTargetPt(Pt2 targetPt) {
    this.targetPt = targetPt;
  }

  double maxSpeed = 0.5; // in BSU / sec
  private double minDist; // Minimum distance from the target point. Keeps letters from jumbling together.
  private double angle; // Angle of the letter, in radians.

  HomeShip homeShip;

  // State constants.
  static final int FOLLOW = 0; // Following the parent EnemyShip.
  static final int RELEASED = 1; // Released (no directionality changes possible)
  static final int PULSED = 2; // Being destroyed by a Pulse.

  private double maxAccel = 1; // Maximum acceleration, in BSUs/sec^2.
  private double minSpeed = 0.1; // Minimum speed. The TxTorpedo will decelerate to this speed when released.
  int state = FOLLOW; // Current state of the torpedo.
  static final double spd_pulsed = 0.5; // Speed of torpedoes when they are being destroyed.

    static Font font = new Font("Courier New", Font.BOLD, 15);

    /*
     * Description: Creates a new TxTorpedo.
     * Parameters: c: Chacter of the torpedo.
     *             initPos: Initial position, in BattleSpace.
     *             target: Target position, in BattleSpace.
     *             first: Whether this is the first torpedo.
     *             hs: Reference to the home ship.
     *             eShip: Parent enemy ship.
     * Return: (none)
     */
    TxTorpedo(char c, Pt2 initPos, Pt2 target, boolean first, HomeShip hs, EnemyShip eShip) {
      pos = new Pt2(initPos); // initpos may have properties modified later
      vel = eShip.getVel();
      targetPt = target;
      ch = c;
      homeShip = hs;
      minDist = first ? 0.03 : 0.04; // follow a bit further away for the 1st letter
    }

    /*
     * Description: Resize event handler: Updates the font size.
     * Parameters: bv: BattleView object to get scaling info.
     * Return: (none)
     */
    static void onResize(BattleView bv) {
      font = new Font("Courier New", Font.BOLD, bv.scale(new Pt2(0.06, 0)).x);
    }

    /*
     * Description: Launches the current text torpedo at the given initial velocity.
     * Parameters: v: Initial velocity of the torpedo.
     * Return: (none)
     */
    void launch(Pt2 v) {
      // targetPt = new Pt2(newTarget);
      vel = new Pt2(v);
      // accelerate to reorient to home ship
      state = RELEASED;
      // subtract direction vectors, convert to direction vector, 0.8 bsu /sec^2 accel
      // maxAccelTime = (int) (700 + Math.random() * 1800);
    }

    /*
     * Description: Updates the position and state of the torpedo.
     * Parameters: bv: BattleView information,
     *             deleta: Time passed since last update, in ms.
     * Return: (null)
     */
    void moveFwd(BattleView bv, long delta) {

      Pt2 deltaD = targetPt.diff(pos);
      // Update position of the torpedo according to its state.
      switch (state) {
        case RELEASED: // Released: Decelerate if required to minSpeed and move forward.
          pos.add(vel.scl(delta / 1000.0));
          double deltaS = maxAccel * delta / 1000.0;
          // Decelerate to minSpeed.
          if (vel.magn() - deltaS > minSpeed)
            vel.sub(vel.norm().scl(deltaS));

          // Contact made with home ship.
          if (homeShip.getPos().diff(pos).magn() < HomeShip.getContactRad()) {
            homeShip.causeDamage(this);
          }
          break;

        case PULSED: // Being destroyed by a Pulse.
          // Move away from the home ship at max speed.
          pos.add(pos.diff(homeShip.getPos()).norm().scl(maxSpeed*delta/1000.0));
          break;

        case FOLLOW: // Following the parent ship or other torpedo.
          angle = deltaD.angle();
          deltaD.sub(deltaD.norm().scl(minDist)); // do not move closer than minDist from the target point
          double dist = deltaD.magn();
          double maxDist = maxSpeed * delta / 1000;
          if (dist > maxDist) // Too far away to catch up immediately: move at max speed
            pos.add(deltaD.norm().scl(maxDist));
          else if (dist > minDist) { // Close enough to move to optimal position within one tick
            pos.add(deltaD);
          }
          break;
      }
    }

    /*
     * Description: Draws the torpedo.
     * Parameters: bv: BattleView object,
     *             gg: Graphics object (to be casted as Graphics2D for drawing)
     *             c: Colour to draw TxTorpedo with
     *             exploded: Whether to draw the torpedo in its exploding phase.
     */
    void draw(BattleView bv, Graphics gg, Color c, boolean exploded) {
      Graphics2D g = (Graphics2D)gg;

      // Draw exploding animation
      if (exploded) {
        Pt2_i scrCoord = bv.toScrPt(pos);
        double s=10;
        Path2D l = G.mkExplodePath(s, 0);
        AffineTransform at = new AffineTransform();
        at.translate(scrCoord.x, scrCoord.y);// scrCoords.x, scrCoords.y);
        at.rotate(Math.PI - angle);
        AffineTransform xsave = g.getTransform();
        g.transform(at);
        g.setColor(new Color(200,200,64));
        g.setStroke(new BasicStroke(1.0f));
        g.draw(l);
        g.setTransform(xsave);
      }
      else // Draw the character for the torpedo.
      {
        Color prevClr = g.getColor();
        Font prevFont = g.getFont();

        FontMetrics metrics = g.getFontMetrics(font);

        Rectangle2D area = metrics.getStringBounds("" + ch, g);

        AffineTransform rotation = new AffineTransform();
        rotation.rotate(Math.PI - angle, area.getWidth() / 2, -area.getHeight() / 2);
        Font rotatedFont = font.deriveFont(rotation);
        g.setFont(rotatedFont);

        g.setColor(c);

        Pt2_i scrPos = bv.toScrPt(pos);
        g.drawString(""+ch, (int) (scrPos.x - area.getWidth() / 2), (int) (scrPos.y + area.getHeight() / 2));
        // center char vertically & horizontally
        if (G.DEBUG) {
          g.setFont(BattleView.dbgFont);
        }
        g.setFont(prevFont);
        g.setColor(prevClr);
      } // if (exploded) .. else ..
    } // draw()

  }

  class TorpedoGroup {
    String seq; // Engine sequence for the Torpedo Group.
    long startTime; // Time in ms of creation of the TorpedoGroup.
    List<TxTorpedo> torps; // Thread-safe list of TxTorpedoes in this group.
    int matchCt; // Number of pulses matching the engine sequence for this group.
    Pulse locked = null; // Pulse that is currently destroying this group.
    EnemyShip parentShip; // Refence to parent EnemyShip. This parent != null even when the group is detached.
    Game game;

    /*
     * Description: Creates a new TorpedoGroup and adds it to the global list.
     * Parameters: s: Engine sequence for this group.
     *             list: Thread-safe list of TxTorpedoes in the group.
     *             parent: Parent ship of this TorpedoGroup.
     */
    TorpedoGroup(String s, List<TxTorpedo> list, EnemyShip parent) {
      torps = Collections.synchronizedList(list);
      parentShip = parent;
      game = parent.game;
      matchCt = 0;
      seq = s;
      startTime = System.currentTimeMillis();

      List<TorpedoGroup> found = parent.game.tGroups.get(seq);
      // Add to the list of TorpedoGroups with the same engine sequence.
      // Create such a list if it does not already exist.
      if (found == null) {
        found = Collections.synchronizedList(new ArrayList<>());
        parent.game.tGroups.put(seq, found);
      }
      found.add(this);
    }

    /*
     * Description: Updates the (cosmetic) pulse match count for this TorpedoGroup according
     * to the pulse sequence provided.
     * Parameters: pulseStr: Active pulse sequence.
     * Return: Whether the last pulse sequence had an effect on this torpedo group,
     * ie, whether the match count increased.
     */
    boolean updateMatchCt(StringBuilder pulseStr) {
      matchCt = 0;
      // Loop through the current engine sequence
      for (int i = 0; i < seq.length(); i++) {
        // Pulse matches only to the end of the pulse sequence.
        if (pulseStr.toString().endsWith(seq.substring(0, i+1))) {
          matchCt = i+1;
        }
      }
      // Pulse may not target this ship on full match so clear the match count.
      // Last pulse had effect on this ship.
      if (matchCt == seq.length()) {
        matchCt = 0;
        return true;
      }
      return matchCt != 0;
    }

    /*
     * Description: Update the current torpedo group to be destroyed by the given pulse.
     * The TorpedoGroup will fly away from the home ship at max speed and be removed
     * when it is out of view.
     * Parameters: p: Pulse that made contact with this group
     *             closestTorp: Closest torpedo to the pulse center at the time.
     * Return: (none)
     */
    void lock(Pulse p, TxTorpedo closestTorp) {
      // Lock parent ship
      if (parentShip != null) parentShip.lock(p);
      locked = p;
      synchronized (torps) {
        // Loop through TxTorpedoes in this group and set them all to follow the closest one
        for (TxTorpedo t : torps) {
          t.state = TxTorpedo.FOLLOW;
          t.maxSpeed = TxTorpedo.spd_pulsed;
      }
    }
    // Set the closest torpedo to move away from pulse center at max speed
    if (closestTorp != null) {
      closestTorp.state = TxTorpedo.PULSED;
      // Because each torpedo follows the next lowest index one, we have to remap the first torp to the dragged one.
      torps.get(0).setTargetPt(closestTorp.getPos());
    }
  }

  /*
   * Description: Moves the torpedo group forward.
   * Parameters: bv: BattleView object.
   *             delta: Time, in ms, since the last update.
   * Return: Whether this torpedo group should be removed.
   */
  boolean moveFwd(BattleView bv, long delta) {

    boolean parentToRemove = false;
    // Move enemy ship forward.
    if (parentShip != null) {
      parentToRemove = parentShip.moveFwd(bv, delta);
    }
    // else if (torps.get(0).state == TxTorpedo.RELEASED) released = true;
    boolean anyInView = false;
    synchronized (torps) {
      Iterator<TxTorpedo> it = torps.iterator();
      // Loop through all torpedoes, move forward.
      while (it.hasNext()) {
        TxTorpedo t = it.next();
        t.moveFwd(bv, delta);
        // Check if any are in view.
        if (bv.inView(t.getPos())) anyInView = true;
      }
    }
    // delete if no torpedoes are in view and
    // the group has been destroyed
    return !anyInView && (locked != null || parentToRemove);
  }

  /*
   * Description: Draws the current TorpedoGroup.
   * Parameters: bv: BattleView object.
   *             g: Graphics object to draw onto.
   * Return: (none)
   */
  void draw(BattleView bv, Graphics g) {
    // Draw parent ship.
    if (parentShip != null) parentShip.draw(bv, g);
    synchronized (torps) {
      for (int i=0; i<torps.size(); i++) {
        Color clr = Color.GRAY;
        if (matchCt > i) clr = Color.ORANGE;
        // if (torps.get(i).state == TxTorpedo.PULSED) clr = Color.GREEN;
        torps.get(i).draw(bv, g, clr, locked!=null);
      }
    }
  }

  /*
   * Description: Returns the focused TorpedoGroup.
   * The focused group is the one that is closest to a full match,
   * and in case of a tie, the closest TorpedoGroup.
   * Parameters: tGroups: HashMap of all TorpedoGroups
   *             p: Center point to check distances from
   * Return: Closest TorpedoGroup, or null if none exist.
   */
  static TorpedoGroup getFocused(ConcurrentHashMap<String, List<TorpedoGroup>> tGroups, Pt2 p) {
    int bestMatchDiff = 9999;
    TorpedoGroup closestTG = null;
    double minDist = 9999;
    for (List<TorpedoGroup> tgs : tGroups.values()) {
      // Loop through all torpedo groups
      for (TorpedoGroup tg : tgs) {
        int matchDiff = tg.seq.length() - tg.matchCt;
        double distTo = tg.getFirstPos().diff(p).magn();
        // either found a closer match or the same match quality but this tg is closer to the target point
        if (matchDiff < bestMatchDiff || matchDiff == bestMatchDiff && distTo < minDist) {
          bestMatchDiff = matchDiff;
          closestTG = tg;
          minDist = distTo;
        }
      }
    }

    return closestTG;
  }

  /*
   * Description: Returns the position of the parent ship or the first torpedo.
   * Parameters: (none)
   * Return: Position of the first entity.
   */
  Pt2 getFirstPos() {
    if (parentShip != null) return parentShip.getPos();
    else if (torps.size() > 0) return torps.get(0).getPos();
    else return null;
  }

  static long lastTime = System.currentTimeMillis();
  /*
   * Description: Moves the current TorpedoGroup forward and removes groups if necessary.
   * Parameters: tGroups: List of all TorpedoGroups.
   *             bv: BattleView information.
   * Return: (none)
   */
  static void moveFwd(ConcurrentHashMap<String, List<TorpedoGroup>> tGroups, BattleView bv) {
    long currT = System.currentTimeMillis();
    long delta = currT - lastTime;
    lastTime = currT;
    synchronized (tGroups) {
      // Loop through all TorpedoGroups.
      Iterator<List<TorpedoGroup>> it = tGroups.values().iterator();
      while (it.hasNext()) {
        List<TorpedoGroup> tgs = it.next();
        // Move all forward and remove if necessary.
        for (int i=0; i<tgs.size(); i++) {
          if (tgs.get(i).moveFwd(bv, delta)) {
            tgs.remove(i);
            i--;
          }
        }
        // Remove the entire list if none remain.
        if (tgs.size() == 0) {
          it.remove();
        }
      }
    }
  }
}