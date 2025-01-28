// Name: Rufin Hsu
// Date: Jan 19, 2025
// Description: HomeShip class. The HomeShip class also handles, updates and draws its active pulses.

import java.util.*;
import java.util.List;
import java.awt.*;
import java.awt.geom.*;

class HomeShip {

  private Pt2 pos; // Position of the home ship, in BSU.
  private Pt2_i scaledSz; // Scaled size, in screen pixels, of the ship image.
  private List<Pulse> pulses; // All pulses.
  private double ang = Math.PI / 2; // Direction of the home ship orientation.
  private static double contactRad = 0.06; // Distance for TxTorpedoes to consider "contacted" with the HomeShip.
  private StringBuilder activePulseStr = new StringBuilder(); // Active pulse sequence.
  private Game game;

  public Pt2 getPos() {
    return pos;
  }

  public Pt2_i getScaledSz() {
    return scaledSz;
  }

  public List<Pulse> getPulses() {
    return pulses;
  }

  public double getAng() {
    return ang;
  }

  public static double getContactRad() {
    return contactRad;
  }

  public StringBuilder getActivePulseStr() {
    return activePulseStr;
  }

  public Game getGame() {
    return game;
  }

  private int  shipHealth=5; // Every TxTorpedo contact removes 1 health
  private long explodeTime = -1;  // time of explosion start
  private long damageTime  = -1;  // time of last torpedo hit
  private long kExplodeAnimDuration   = 1000;   // Explosion anim duration
  private long kGameOverFadeInDuration= 2000;   // Fade in duration of the "Game Over" message.
  private long kDamageShakeDuration   = 1000;   // How long should the ship shake on hit.
  long lastPulseTime = -1; // Last pulse time, in ms
  private Set<TxTorpedo> hitTorps=new HashSet<TxTorpedo>(); // Set of TxTorpedoes that have hit the HomeShip.

  /*
   * Description: Creates a new HomeShip.
   * Parameters: initialPos: Initial position, in BattleSpace, of the ship.
   *             g: Reference to the game object.
   * Return: (none)
   */
  HomeShip(Pt2 initialPos, Game g) {
    game = g;
    // baseImg = new ImageIcon("./hs.png").getImage();
    pos = new Pt2(initialPos);
    pulses = Collections.synchronizedList(new LinkedList<>());
  }

  /*
   * Description: Causes damage to the home ship.
   * Parameters: torp: TxTorpedo that causes damage.
   * Return: (none)
   */
  void causeDamage(TxTorpedo torp) {
    if (shipHealth<0)
      explode();  // Sustain the explosion effect a bit longer.
    else if (!hitTorps.contains(torp)) { // Update health and damage time.
      hitTorps.add(torp);
      shipHealth--;
      damageTime = System.currentTimeMillis();
      if (shipHealth<=0) {
        shipHealth=0;
        explode();
      }
    }
  }

  /*
   * Description: Returns the status of the ship.
   * Parameters: (none)
   * Return: Status of the ship.
   * >0 means the ship is still alive.
   * <0 means animating explosion.
   */
  int shipStatus() {
    if (shipHealth<=0 && explodeTime>0) {  // exploded/exploding/displaying "Game Over"
      long timeNow = System.currentTimeMillis();
      long timeAfterX = timeNow - explodeTime;
      if (timeAfterX>kExplodeAnimDuration + kGameOverFadeInDuration) { // Done animations.
        return 0;
      }
      else {
        return -1;       // exploding or displaying the GameOver Message.
      }
    }
    else
      return shipHealth; // >0 means the home ship is still alive.
  }

  /*
   * Description: Makes the ship start animating the explosion.
   * Parameters: (none)
   * Return: (none)
   */
  void explode() {
    game.aWave.canAdd = false;
    long timeNow = System.currentTimeMillis();

    if (explodeTime<0 // Must test! O.w. explodeTime will be pushed forward repeatedly.
    || (timeNow-explodeTime>500 && timeNow-explodeTime<kExplodeAnimDuration)
    // This weird test to allow the explosion to renew a bit if it has not completely died down.
    )
    {
      explodeTime = System.currentTimeMillis();
    }
  }


  /*
   * Description: Sends a new pulse out from the ship.
   * Parameters: ch: Character for the pulse.
   * Return: (none)
   */
  void pulse(char ch) {
    if (shipStatus() <= 0) return; // Exploded ship cannot send pulse.
    if (Character.isLetter(ch) || Character.isDigit(ch)
        || "!@#$%^&*()-=_+[]\\{}|;':\",./<>?".contains("" + ch)) { // Check valid character.
      lastPulseTime = System.currentTimeMillis();
      activePulseStr.append(ch);
      Pulse p = new Pulse(pos, game);
      pulses.add(p);
    }
  }

  /*
   * Description: Handles pulse dissipation.
   * Parameters: (none)
   * Return: (none)
   */
  void pulseExpiry() {
    if (activePulseStr.length() > 0) // Remove a letter from the pulse string.
      activePulseStr.deleteCharAt(0);
    updateAllMatchCts(false, null);
  }

  /*
   * Description: Updates (cosmetic) match counts for all TorpedoGroups.
   * Parameters: letterAdded: Whether a letter is being added (or removed)
   *             p: Pulse to update in some cases
   * Return: Whether any match counts increased.
   */
  boolean updateAllMatchCts(boolean letterAdded, Pulse p) {
    boolean fullMatch = letterAdded && p.checkFullMatch();
    boolean effect = false;
    boolean comboBroken = true;
    String word = "";
    synchronized (game.tGroups) {
      // Loop through all TorpedoGroups.
      for (List<TorpedoGroup> tgs : game.tGroups.values()) {
        for (TorpedoGroup tg : tgs) {
          int prevMatchCt = tg.matchCt;
          // Update match count.
          if (tg.updateMatchCt(activePulseStr)) {
            effect = true;
            word = tg.seq;
          }
          if (prevMatchCt < tg.matchCt && tg.matchCt != 1) comboBroken = false;
        }
      }
    }
    if (letterAdded) {
      if (!comboBroken || fullMatch) { // If the combo was not broken, increase points.
        if (fullMatch) { // If there is a full word match, add points and check if the phrase matches.
          game.stat.pts += word.length();

          String potentialMatch = game.stat.getComboStr() + word;
          // If the phrase matches, everything clears
          if (potentialMatch.endsWith(game.aWave.getPhrase())) {
            p.setType(Pulse.CLEARALL);
            game.aWave.canAdd = false;
          }
        }
        else game.stat.pts++;

        // If the active pulse sequence matches, everything clears as well.
        if (game.hShip.activePulseStr.toString().endsWith(game.aWave.getPhrase().replaceAll(" ", ""))) {
          p.setType(Pulse.CLEARALL);
            game.aWave.canAdd = false;
        }
      }
      else if (!effect) { // Pulses without effect (no matches) clear the combo.
        game.stat.clearCombo(game.waveCt);
      }
      else if (!game.stat.canStartWord) { // If the combo was broken and a new word was not started, clear the combo.
        game.stat.clearCombo(game.waveCt);
      }
      game.stat.canStartWord = fullMatch;
    }
    if (activePulseStr.length() == 0) { // If all pulses dissipate, clear the combo.
      game.stat.clearCombo(game.waveCt);
    }

    game.stat.updateDisplays();
    return effect;
  }

  /*
   * Description: Updates the display size of the HomeShip
   * Parameters: bv: BattleView for scaling.
   * Return: (none)
   */
  void onResize(BattleView bv) {
    scaledSz = bv.scale(new Pt2(0.05, 0.05));
    // shipImg = baseImg.getScaledInstance(scaledSz.x, scaledSz.y, Image.SCALE_SMOOTH);
  }

  // facets: Path2Ds for the ship's appearance.
  private Path2D[] facets = {new Path2D.Double(), new Path2D.Double(), new Path2D.Double(), new Path2D.Double(), new Path2D.Double(), new Path2D.Double()};
  // facetNs: 3D normal vectors of the facets (for illumination calculations)
  private double[][] facetNs = {  // Normalize them on facet initialization
    {0,-Math.sqrt(3)/2,.5},
    {0,0,1},
    {-Math.sqrt(3)/2*Math.sin(Math.PI/6), -Math.sqrt(3)/2, 0.5},
    { Math.sqrt(3)/2*Math.sin(Math.PI/6), -Math.sqrt(3)/2, 0.5},
    {-Math.sqrt(2)/2,0,Math.sqrt(2)/2},
    { Math.sqrt(2)/2,0,Math.sqrt(2)/2}
  };
  private boolean facetsReadyQ=false; // A flag for initializing the facet shapes.

  /*
   * Description: Draws the HomeShip.
   * Parameters: _g: Graphics object to draw onto
   *             bv: BattleView information.
   * Return: (none)
   */
  void draw(BattleView bv, Graphics _g) {
    if (!facetsReadyQ) {  // initialize the facet geometry
      double s=1.3;      // The intrinsic size of the geometry is ~2x2 ((-1,-0.7) to (1, 0.85))
      // Hand crafted geometry of the HomeShip.
      G.mkPath(facets[0], s, new double[][] {{0.25,-0.7}, {-0.25,-0.7}, {-0.25, -0.37}, {0.25, -0.37}}, false);
      G.mkPath(facets[0], s, new double[][] {{0.25,-0.25}, {-0.25,-0.25}, {-0.25, 0}, {0.25, 0}}, true);
      G.mkPath(facets[1], s, new double[][] {
        {0.35, -0.15}, {-0.35, -0.15}, {-1,0.35}, {-0.85,0.75}, {-0.25,0.6},
        {0.25,0.6}, {0.85,0.75}, {1,0.35}, {0.35, -0.15}}, false);
      G.mkPath(facets[2], s, new double[][] {{-0.25,-0.7}, {-0.35,-0.15}, {-0.25,0.6}, {-0.2,-0.1}}, false);
      G.mkPath(facets[3], s, new double[][] {{0.25,-0.7}, {0.35,-0.15}, {0.25,0.6}, {0.2,-0.1}}, false);
      G.mkPath(facets[4], s, new double[][] {{0,0.15}, {-0.1,0.15},{-0.1,0.85}, {0,0.85}}, false);
      G.mkPath(facets[5], s, new double[][] {{0.1,0.15}, {0,0.15}, {0,0.85}, {0.1,0.85}}, false);

      for (int i=0; i<facetNs.length; i++) {
        // Normalize the normal vectors of the facets.
        double[] n=facetNs[i];
        double nm = Math.sqrt(n[0]*n[0] + n[1]*n[1] + n[2]*n[2]);
        n[0]/=nm; n[1]/=nm; n[2]/=nm;
      }
      facetsReadyQ=true;  // Set this to avoid recreating the geometries again.
    } // if (!facetsReadyQ)

    Graphics2D g = (Graphics2D) _g;

    synchronized (pulses) {
      Iterator<Pulse> it = pulses.iterator();
      // Draw all pulses.
      while (it.hasNext())
        it.next().draw(bv, g);
    }

    Pt2_i scrCoords = bv.toScrPt(pos);
    AffineTransform at = new AffineTransform();

    long currTime = System.currentTimeMillis();

    long timeAfterX=-1;
    if (explodeTime>0) {
      // Render the exploded appearance of the home ship.
      timeAfterX = currTime - explodeTime;
      if (timeAfterX>=0) {
        if (timeAfterX<kExplodeAnimDuration) { // Animating ship exploding
          float fade = (1.0f - timeAfterX/(float)kExplodeAnimDuration);
          // Shrink the explosion and the thickness of the "sparks".
          Path2D xp = G.mkExplodePath(scaledSz.x*fade, timeAfterX);
          at.translate(scrCoords.x, scrCoords.y);
          AffineTransform xsave = g.getTransform();
          g.transform(at);
          float v=0.8f+(float)Math.random()*0.8f;
          g.setColor(new Color(G.scaleRgb(0x4169e1, v)));

          g.setStroke(new BasicStroke(3.0f*(0.1f + fade*0.9f), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
          g.draw(xp);
          g.setTransform(xsave);
        }
        else {
          int opacity = 255;
          // Animating game over screen
          if (timeAfterX<kExplodeAnimDuration + kGameOverFadeInDuration) {
            opacity = 255 - (int)((kExplodeAnimDuration+kGameOverFadeInDuration-timeAfterX)*255/kGameOverFadeInDuration);
          }
          Font waveInfoFont = G.titleFont !=null ? G.titleFont.deriveFont(80f) : new Font("Arial", Font.BOLD, 80);

          FontMetrics fm = g.getFontMetrics(waveInfoFont);
          String str = "GaMe OvEr";
          Rectangle2D bounds = fm.getStringBounds(str, g);
          if (opacity < 0) opacity = 0;
          if (opacity > 255) opacity = 255;
          g.setColor(new Color(0xff, 0xd7, 0, opacity));
          g.setFont(waveInfoFont);
          Pt2_i scrctr = bv.toScrPt(pos);
          g.drawString(str, (int)(scrctr.x - bounds.getWidth()/2), scrctr.y);
        }
      } // if (timeAfterX>=0)
    }
    else { // Draw the ship normally
      Font fnt = G.circFont.deriveFont(15f);
      // FontMetrics fm = g.getFontMetrics(fnt);
      String str = "12345678901234567890".substring(0, shipHealth);
      // Rectangle2D bounds = fm.getStringBounds(str, g);
      g.setColor(new Color(0xdd6050));
      g.setFont(fnt);
      g.drawString(str, 10,20);


      // Render the normal appearance of the home ship.
      AffineTransform xsave = g.getTransform(); // Back up the current transformation.

      int shakex=0, shakey=0;
      double shakea=0.0;
      boolean shaking=currTime - damageTime < kDamageShakeDuration;
      if (shaking) { // shake the ship a little.
        shakex = (int)(Math.random()*5);
        shakey = (int)(Math.random()*5);
        shakea = Math.random()*0.1 - 0.05;
      }

      at.translate(scrCoords.x+shakex, scrCoords.y+shakey);
      at.rotate(Math.PI / 2 - ang - shakea);
      at.scale(scaledSz.x, scaledSz.y);   // Ship facets are in its intrisic -1..1 coordinate system.
      g.transform(at);

      double a = 5*Math.PI/4 - ang - shakea;
      double r=0.5*Math.sqrt(3);
      double rcos= r * Math.cos(a);
      double rsin= -r * Math.sin(a);
      for (int i=0; i<facets.length; i++) {
        Path2D p=facets[i];
        double[] n=facetNs[i];
        // double dotp = n[0]*rcos + n[1]*rsin + n[2]/Math.sqrt(3); // dot product
        double dotp = n[0]*rcos + n[1]*rsin + n[2]*0.5; // dot product
        int lum = Math.max(0, Math.min(255, (int)Math.floor(125+dotp*120+0.5)));
        if (shaking)
          g.setColor(new Color(lum, lum/2, lum/3));
        else
          g.setColor(new Color(lum/2, lum/2, lum));
        g.fill(p);
      }

      g.setTransform(xsave);  // Restore the transformation


      if (G.DEBUG) bv.dbgPt(pos, g);

      if (G.DEBUG) {
        int scrRad = bv.scale(new Pt2(contactRad, 0)).x;
        g.drawArc(scrCoords.x-scrRad, scrCoords.y-scrRad, scrRad*2, scrRad*2, 0, 360);
      }
    }
  }

  private long lastTime = System.currentTimeMillis();
  /*
   * Description: Rotates the HomeShip towards the focused TorpedoGroup.
   * Parameters: bv: BattleView information.
   *             targetTG: focused TorpedoGroup.
   * Return: (none)
   */
  void moveFwd(BattleView bv, TorpedoGroup targetTG) {
    long currT = System.currentTimeMillis();
    long delta = currT - lastTime; //Math.min(1000, currT - lastTime);
    lastTime = currT;

    if (targetTG!=null) { // Found a torpedo group to rotate towards.
      Pt2 targetPt = targetTG.getFirstPos();
      // bv.dbgPt(targetPt, bv.getGraphics());
      double targetAngle = Math.atan2(targetPt.y-pos.y, targetPt.x-pos.x);
      double dA = G.deltaAngle(targetAngle, ang);
      double turn = (dA>0 ? 0.003 : -0.003)*delta;
      if (Math.abs(turn)>Math.abs(dA))
        ang=targetAngle;
      else
        ang+=turn;
      // double dA2 = G.deltaAngle(targetAngle, ang);
      // if (dA2*dA<0) ang=targetAngle;
      // double acc = dA2/1000;
      // angularVel += acc*delta;
      // if (Math.abs(angularVel)>Math.PI/4000) {
      //   angularVel = angularVel<0 ? -Math.PI/4000 : Math.PI/4000;
      // }
    }
    else { // Simply rotate around
      ang=G.normalizeAngle(ang+0.001*delta);
    }

    synchronized (pulses) {
      // Loop through all pulses and draw
      Iterator<Pulse> it = pulses.iterator();
      while (it.hasNext()) {
        Pulse p = it.next();
        boolean remove = p.moveFwd(bv, delta);
        if (remove) { // Remove pulses if they have expired
          pulseExpiry();
          it.remove();
        }
      }
    }
  }
}

