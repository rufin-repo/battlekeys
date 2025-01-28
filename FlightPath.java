// Name: Rufin Hsu
// Date: Jan 19, 2025
// FlightPath and FPt class - determines where every EnemyShip should be at any given time.
// Also provides transformation and shift features from existing FlightPaths.
// The FPt class stores information and actions for points on the FlightPath.

import java.util.*;
import java.util.List;
import java.awt.*;

class FlightPath {
  private List<FPt> path;
  private int lastIdx; // Last stored index to determine
  private double totalDist; // Total length of the path.
  public double getTotalDist() {
    return totalDist;
  }

  private double dbg_lastFrac; // Last saved pathFrac. For debug only.
  private int actionPending = FPt.NONE; // Recommended action.
  // Colour of the path.
  private Color dbgClr = new Color((int)(Math.random()*255),(int)(Math.random()*255),(int)(Math.random()*255));

  /*
   * Description: Gets the target position in the flight path given the path fraction.
   * Parameters: pathFrac: Fraction of the path travelled.
   * Return: Target position, in BattleSpace.
   */
  Pt2 getTargetPos(double pathFrac)
  {
    dbg_lastFrac = pathFrac;
    double dist = pathFrac * totalDist;
    // Loop through path segments until we find one between the required distances
    for (int i=0; i<path.size()-1; i++) {
      FPt nextPt = path.get(i+1);
      if (nextPt.distTo > dist) { // found the right path segment
        FPt currPt = path.get(i);
        if (i != lastIdx) {                 // Moving onto a new track segment.
          lastIdx = i;
          actionPending = currPt.action;
          if (actionPending==FPt.LAUNCH) {  // Roll a die to see if we should launch or not in this launchable segment.
            if (Math.random()>0.7)
              actionPending=FPt.NONE;       // Sorry... Try again next time.
          }
        }
        double currSectionDist = nextPt.distTo - currPt.distTo;
        double fracBtw = (pathFrac - currPt.distTo/totalDist)/(currSectionDist/totalDist);
        return Pt2.interp(currPt.crd, nextPt.crd, fracBtw);
      }
    }
    return null;
  } // getTargetPos()



  /*
   * Description: Returns the action recommended at the current path fraction.
   * Parameters: pathFrac: Fraction of the path travelled.
   * Return: Action to take (see FPt constants)
   */
  int action(double pathFrac) {
    return actionPending;
  }

  /*
   * Description: Marks action as taken and resets the pending action.
   * Parameters: (none)
   * Return: (none)
   */
  void actionTaken() {
    actionPending=FPt.NONE; //true;
  }

  /*
   * Description: Creates a new FlightPath from a list of FlightPoints.
   * Parameters: pts: Flight Path points.
   * Return: (none)
   */
  FlightPath(List<FPt> pts)
  {
    path = processFPts(pts);
  }

  /*
   * Description: Adds metadata to all FlightPoints.
   * Parameters: pts: Raw FlightPoints.
   * Return: The same list.
   */
  List<FPt> processFPts(List<FPt> pts)
  {
    FPt firstPt = pts.get(0);
    FPt lastPt = new FPt(new Pt2(firstPt.crd), firstPt.action);
    pts.add(lastPt);

    totalDist = 0;
    // Set distTo for all FPts.
    for (int i=0; i<pts.size()-1; i++) {
      FPt curr = pts.get(i);
      curr.distTo = totalDist;
      FPt next = pts.get(i+1);
      double dist = next.crd.diff(curr.crd).magn();
      totalDist += dist;
    }
    lastPt.distTo = totalDist;
    return pts;
  }

  /*
   * Description: Derives a new FlightPath from a base path by rotating.
   * Parameters: base: Base FlightPath.
   *             theta: Angle to rotate by
   *             Pt2: Center of the flightPath for the rotation.
   * Return: new transformed FlightPath.
   */
  FlightPath(FlightPath base, double theta, Pt2 center)
  {
    List<FPt> newPts = Collections.synchronizedList(new ArrayList<>());
    // loop through all points and transform the points.
    for (int i=0; i<base.path.size(); i++) {
      FPt origPt = base.path.get(i);
      newPts.add(new FPt(origPt.crd.rotateAbout(theta, center), origPt.action));
    }
    path = processFPts(newPts);
  }

  /*
   * Description: Draws the FlightPath for debug.
   * Parameters: bv: BattleView info
   *             g: Graphics object to draw onto.
   * Return: (none)
   */
  void debugDraw(BattleView bv, Graphics g) {
    Color c = g.getColor();
    g.setColor(dbgClr);
    FPt lastPt = path.get(path.size()-1);
    // Loop through paths, draw every segment
    for (FPt p : path) {
      Pt2_i fromCrd = bv.toScrPt(lastPt.crd);
      Pt2_i toCrd = bv.toScrPt(p.crd);
      g.drawLine(fromCrd.x, fromCrd.y, toCrd.x, toCrd.y);
      // bv.dbgPt(p.crd, g, p == path.get(lastIdx) ? Color.GREEN : Color.WHITE);
      lastPt = p;
    }
    bv.dbgStr(String.format("%.2f, %s", dbg_lastFrac, getTargetPos(dbg_lastFrac)), getTargetPos(dbg_lastFrac), g, Color.WHITE);
    g.setColor(c);
  }
}

class FPt {
  static final int NONE = 0;
  static final int LAUNCH = 1;
  static final int FILL = 2;
  Pt2 crd; // Coordinate of the point.
  int action; // Action at point.
  double distTo; // Total distance to the point.

  /*
   * Description: Creates a new FlightPoint.
   * Parameters: c: Coordinate of point
   *             a: Action for point
   */
  FPt(Pt2 c, int a) {
    crd = c;
    action = a;
  }

  /*
   * Description: Creates a new FlightPoint.
   * Parameters:  x, y: Coordinate of point
   *              a: Action for point.
   */
  FPt(double x, double y, int a) {
    crd = new Pt2(x, y);
    action = a;
  }
}