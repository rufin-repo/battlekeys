// Name: Rufin Hsu
// Date: Jan 19 2025
// Description: EnemyShip class.
// The EnemyShip class moves along its FlightPath, launches and loads its TxTorpedoes.

import java.util.*;
import java.util.List;
import java.awt.*;
import java.awt.geom.*;

class EnemyShip {
  private FlightPath flightPath;
  private long cycleTime = 20000; // ms per path cycle
  private double maxSpeed = 0.5; // bsu / sec
  private double preferredSpeed = 0.4; // Will slow down to preferred speed when possible.
  private double accel = 0.8; // bsu / sec^2
  private Pt2 pos, vel;
  public Pt2 getPos() {
    return pos;
  }

  public Pt2 getVel() {
    return vel;
  }

  private Pt2_i scaledSz; // Scaled size of the enemy ship.
  private static Pt2 bsSz = new Pt2(0.05, 0.05);
  private static double minVel = 0.3;
  private Pt2 targetPos; // For steering.

  private String seq;
  private int matchCt = 0;

  private long startTime; // Creation time of the enemy ship.
  private long minTime = 5000; // Minimum time before launch of the enemy ship.
  private long explodeTime; // Time of explosion

  private boolean exploded = false;

  private Pulse locked = null; // Pulse that is destroying this enemy ship.
  private boolean hasTorps = true; // Whether it has torpedoes.

  Game game;

  private TorpedoGroup tGroup;
  // List<TxTorpedo> torpedoes;

  /*
   * Description: Calculates the path fraction given the time since start.
   * Parameters: timeSinceStart: time, in ms since the ship was created.
   * Return: Path fraction.
   */
  private double calcPathFrac(long timeSinceStart) {
    return (timeSinceStart<0) ? 0.0 : (((double)timeSinceStart/cycleTime)%1.0);
  }

  /*
   * Description: Creates a new enemy ship and TorpedoGroup.
   * Parameters: s: Engine sequence for the ship.
   *             g: Reference to the game.
   *             f: Flight path for this ship.
   *             startDelay_ms: Delay before the ship starts moving.
   * Return: (none)
   */
  EnemyShip(String s, Game g, FlightPath f, long startDelay_ms) {
    startTime = System.currentTimeMillis() + startDelay_ms;
    targetPos = new Pt2(0, 0);
    game = g;
    vel = new Pt2(0, 0);
    seq = s;
    flightPath = f;
    pos = new Pt2(flightPath.getTargetPos(0.0)); //pathFrac));

    cycleTime = (long) (flightPath.getTotalDist() / preferredSpeed * 1000);
    loadTorpedoes();
  }

  /*
   * Description: Loads torpedoes into the enemy ship.
   * Parameters: (none)
   * Return: (none)
   */
  void loadTorpedoes() {
    tGroup = new TorpedoGroup(seq, new ArrayList<>(), this);
    TxTorpedo lastT = null;
    // Add torpedoes to the group.
    for (int i = 0; i < seq.length(); i++) {
      TxTorpedo curr = new TxTorpedo(seq.charAt(i), pos, lastT == null ? pos : lastT.getPos(), i == 0, game.hShip, this);
      tGroup.torps.add(curr);
      lastT = curr;
    }
    if (matchCt == seq.length()) matchCt = 0;
  }

  /*
   * Description: Updates the scaled size when the window is resized.
   * Parameters: bv: BattleView information.
   * Return: (none)
   */
  void onResize(BattleView bv) {
    scaledSz = bv.scale(bsSz);
  }

  /*
   * Description: Updates parameters to lock onto a pulse.
   * Parameters: p: Target pulse.
   * Return: (none)
   */
  void lock(Pulse p) {
    locked = p;
    exploded = true;
    explodeTime = System.currentTimeMillis();
    maxSpeed = 0.5;
  }

  // For debug only
  private String dbgStatus = "";
  private Color dbgClr = null;

  private long prevTime = System.currentTimeMillis();

  /*
   * Description: Moves the EnemyShip forward.
   * Parameters: bv: BattleView information.
   *             _delta: Unused.
   * Return: Whether to remove the EnemyShip.
   */
  boolean moveFwd(BattleView bv, long _delta) {

    long currTime = System.currentTimeMillis();
    long delta = currTime - prevTime;
    prevTime = currTime;
    long timeSinceStart = currTime - startTime;
    dbgStatus = "";
    boolean toDelete = false;
    if (locked != null) {
      // move away from home ship at max speed if locked
      pos.add(pos.diff(game.hShip.getPos()).norm().scl(maxSpeed*delta/1000));
      if (!bv.inView(pos)) {
        toDelete = true;
      }
    }
    else if (timeSinceStart>0) { // Can start moving.
      double pathFrac = calcPathFrac(timeSinceStart);
      pos.add(vel.scl(delta / 1000.0));
      targetPos = flightPath.getTargetPos(pathFrac);
      switch (flightPath.action(pathFrac)) { // Check the current flight path action.
        case FPt.LAUNCH:
          double velnorm = G.magnitude(vel);
          Pt2    hsvec   = game.hShip.getPos().diff(pos);
          double hsvecnorm = G.magnitude(hsvec);
          double dotprod = (hsvec.x*vel.x + hsvec.y*vel.y)/velnorm/hsvecnorm;
          if (dotprod>0.95) { // vel is pointing almost directly towards the homeship => good to launch!
            flightPath.actionTaken(); // Consume the FPt.LAUNCH signal and stop flightPath.action() from returning it again.
            if (vel.magn() < minVel)  // Avoid launching clusters at low speed
              break;
            if (currTime - startTime < minTime) { // Must wait minimum time before launching
              break;
            }
            hasTorps = false;
            matchCt = 0;
            List<TxTorpedo> launchedTorps = Collections.synchronizedList(new ArrayList<>());
            // Loop through all torpedoes and launch.
            for (int i = 0; i < tGroup.torps.size(); i++) {
              TxTorpedo t = tGroup.torps.get(i);
              if (t.state == TxTorpedo.FOLLOW) {
                t.launch(hsvec.scl(velnorm)); //vel);
                launchedTorps.add(t);
              }
            }

            tGroup.parentShip = null;
            new TorpedoGroup("", Collections.synchronizedList(new ArrayList<>()), this);
          } // if (dotprod>..)
          break;

        case FPt.FILL: // Fill torpedoes with new words.
          if (hasTorps)
            break;
          seq = game.aWave.getWord();
          loadTorpedoes();
          flightPath.actionTaken(); // Stop subsequent calls to flightPath.action() from returning the same action again code.
          hasTorps = true;
          break;
      }
      Pt2 diff = targetPos.diff(pos);
      double maxDeltaV = accel * delta / 1000.0;
      double dist = diff.magn();
      double currSpeed = vel.magn();
      dbgClr = Color.WHITE;
      // decel to preferred speed = path movement speed and do not accelerate
      if (vel.magn() > preferredSpeed && dist < 0.1) {
        vel.sub(vel.norm().scl(maxDeltaV));
        dbgStatus = "DECEL-TOSPEED";
        dbgClr = Color.GREEN;
        if (preferredSpeed - vel.magn() > 0.01) {
          vel.set(diff.norm().scl(preferredSpeed));
        }
      } else if (dist > 0.1 && currSpeed * (currSpeed / accel) / 2 > dist * 0.8) {
        // @ max deceleration would leave <20% distance left, decel now
        vel.sub(vel.norm().scl(maxDeltaV));
        dbgStatus = "DECEL";
      }
      // accelerate to cover distance
      else if (dist > 0.1) {
        vel.add(diff.norm().scl(maxDeltaV));
        dbgStatus = "ACCEL";
      } else
        dbgStatus = "NONE";
      // Cap speed at max speed.
      if (vel.magn() > maxSpeed) {
        dbgStatus += "-MAXS";
        vel.set(vel.norm().scl(maxSpeed));
      }
    }
    return toDelete;
  }

  // shipTris: Path2Ds for the facets of the enemy ship's appearance.
  private Path2D[] shipTris = {new Path2D.Double(), new Path2D.Double(), new Path2D.Double()};
  // triNs: 3D normal vectors of the facets (for illumination calculations)
  // private double[][] triNs = {{-0.5,0,Math.sqrt(3)/2}, {0,-0.5,Math.sqrt(3)/2}, {0.5,0,Math.sqrt(3)/2}, };
  private double[][] triNs = {{-Math.sqrt(2)/2,0,Math.sqrt(2)/2}, {0,-0.5,Math.sqrt(3)/2}, {Math.sqrt(2)/2,0,Math.sqrt(2)/2}, };
  private boolean shipTrisReadyQ=false; // A flag for initializing the facet shapes.

  /*
   * Description: Draws the enemy ship.
   * Parameters: bv: BattleView information.
   *             g: Graphics object to draw onto.
   * Return: (none)
   */
  void draw(BattleView bv, Graphics _g) {
    if (!shipTrisReadyQ) {  // Do this only for the first time: define the shape of the ship facets.
      double s=0.7;
      Path2D
      p = shipTris[0];
      p.moveTo(-0.5*s,-0.75*s); p.lineTo(-s, s); p.lineTo(0, 0.75*s);
        p.lineTo(0,0);  // this last one is to create some overlapping to eliminate the gap.
      p.closePath();
      p=shipTris[1];
      p.moveTo(0.5*s, -0.75*s);  p.lineTo(-0.5*s, -0.75*s);  p.lineTo(0, 0.75*s);
        p.lineTo(0.5*s,0); // the last one is to create some overlapping to eliminate the gap.
      p.closePath();
      p=shipTris[2];
      p.moveTo(s,s); p.lineTo(0.5*s, -0.75*s); p.lineTo(0, 0.75*s);
      p.closePath();
      shipTrisReadyQ=true;
    }

    long timeNow = System.currentTimeMillis();
    if (startTime>=timeNow) return; // Still in the prelaunch period.

    Graphics2D g = (Graphics2D) _g;

    if (scaledSz==null) scaledSz = bv.scale(bsSz);

    if (G.DEBUG) // Debug flight path.
      flightPath.debugDraw(bv, g);

    Pt2 delta = targetPos.diff(pos);
    double ang = Math.atan2(delta.y, delta.x);

    Pt2 shiftVec = new Pt2(1, 0).rotate(ang).norm().scl(bsSz.x / 2);

    Pt2_i scrCoord = bv.toScrPt(pos.sum(shiftVec));

    long timeAfterX=-1;  // -1 means not yet exploded.
    if (exploded) {
      timeAfterX = timeNow - explodeTime;
      if (timeAfterX>0) // Animate explosion.
      {
        Path2D l = G.mkExplodePath(scaledSz.x, timeAfterX);
        AffineTransform at = new AffineTransform();
        at.translate(scrCoord.x, scrCoord.y);

        AffineTransform xsave = g.getTransform();
        g.transform(at);
        g.setColor(new Color(180,64,64));
        g.setStroke(new BasicStroke(1.0f));
        g.draw(l);
        g.setTransform(xsave);
      }
    }
    else { // Draw enemy ship normally.
      AffineTransform at = new AffineTransform();
      at.translate(scrCoord.x - scaledSz.x / 2, scrCoord.y - scaledSz.y / 2);// scrCoords.x, scrCoords.y);
      at.rotate(Math.PI / 2 - ang, scaledSz.x / 2, scaledSz.y / 2);
      at.scale(scaledSz.x, scaledSz.y);
      // Rotate the light vector by -ang (instead of rotation the normals of the ship's facets).
      double a = 5*Math.PI/4 - ang;
      double r=0.5*Math.sqrt(3);
      double rcos= r * Math.cos(a);
      double rsin= -r * Math.sin(a);
      AffineTransform xsave = g.getTransform();
      g.transform(at);
      // Draw the ship's facets
      for (int i=0; i<shipTris.length; i++) {
        Path2D p=shipTris[i];
        double[] n=triNs[i];
        // Illumination calculations
        double dotproduct = n[0]*rcos + n[1]*rsin + n[2]*0.5; // dot product
        int lum = Math.min(255, (int)Math.floor(80+dotproduct*180+0.5));
        g.setColor(new Color(lum, lum/2, lum/2));
        g.fill(p);
      }
      g.setTransform(xsave);

      if (G.DEBUG) { // Debug draw the current path fraction.
        double pathFrac = calcPathFrac(timeNow-startTime);
        bv.dbgStr(String.format("dist%.2f", flightPath.getTargetPos(pathFrac).diff(pos).magn()),
                  pos.sum(new Pt2(0, 0.03)), g, Color.WHITE);

        bv.dbgStr(dbgStatus + " " + matchCt, pos, g, dbgClr);
      }
    } // if (exploded) .. else ..
  }

}
