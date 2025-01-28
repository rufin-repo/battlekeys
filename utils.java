// Name: Rufin Hsu
// Date: Jan 19, 2025
// Description: General utility class G and Pt2.

import java.awt.*;
import java.awt.geom.*;
import java.awt.Font;
import java.io.*;

class Pt2 {
  double x, y;
  /*
   * Description: Creates a new Pt2.
   * Parameters: xx, yy: Point coordinates.
   * Return: (none)
   */
  Pt2(double xx, double yy)
  {
    x = xx; y = yy;
  }

  /*
   * Description: Duplicates a Pt2.
   * Parameters: o: Pt2 to duplicate.
   * Return: (none)
   */
  Pt2 (Pt2 o) {
    x = o.x;
    y = o.y;
  }

  /*
   * Description: Interpolates between one Pt2 to another according to the fraction.
   * Parameters: from, to: Pt2s to interpolate between.
   *             fraction: Fraction of the way between from and to.
   * Return: Pt2
   */
  static Pt2 interp(Pt2 from, Pt2 to, double frac) {
    return new Pt2(from.x+(to.x-from.x)*frac, from.y+(to.y-from.y)*frac);
  }

  /*
   * Description: Creates a new Pt2 that is the difference between this and p2.
   * Parameters: p2: Pt2 to find the difference from.
   * Return: Difference Pt2.
   */
  Pt2 diff(Pt2 p2)
  {
    return new Pt2(x-p2.x, y-p2.y);
  }

  /*
   * Description: Returns the magnitude.
   * Parameters: (none)
   * Return: Magnitude of the Pt2.
   */
  double magn() {
    return Math.sqrt(x*x+y*y);
  }

  /*
   * Description: Returns the normalised vector of this.
   * Parameters: (none)
   * Return: Normalised vector.
   */
  Pt2 norm() {
    double m = magn();
    if (m != 0)
      return new Pt2(x/m, y/m);
    else return new Pt2(0, 0);
  }

  /*
   * Description: Returns this vector scaled by a factor of n.
   * Parameters: n: Scale factor.
   * Return: Scaled Pt2.
   */
  Pt2 scl(double n) {
    return new Pt2(x*n, y*n);
  }

  /*
   * Description: Adds p to self.
   * Parameters: p: Pt2 to add self to.
   * Return: (none)
   */
  void add(Pt2 p)
  {
    x += p.x; y += p.y;
  }

  /*
   * Description: Returns the sum of p and self.
   * Parameters: p: Pt2 to add.
   * Return: Result of the summation.
   */
  Pt2 sum(Pt2 p) {
    return new Pt2(x+p.x, y+p.y);
  }

  /*
   * Description: Subtracts p from self.
   * Parameters: p: Pt2 to subtract from self.
   * Return: (none)
   */
  void sub(Pt2 p) {
    x -= p.x; y -= p.y;
  }

  /**
   * Description: Sets own values to the given Pt2.
   * Parameters: p: Pt2 to set self to.
   * Return: (none)
   */
  void set(Pt2 p) {
    x = p.x;
    y = p.y;
  }


  /**
   * Description: Sets self to the given coordinates.
   * Parameters: xx, yy: New values for x and y.
   * Return: (none).
   */
  void set(int xx, int yy) {
    x = xx;
    y = yy;
  }

  /*
   * Description: Returns the angle of this Pt2.
   * Parameters: (none)
   * Return: Angle.
   */
  double angle() {
    return Math.atan2(y, x);
  }

  /*
   * Description: Returns a rotated vector.
   * Parameters: Rotation angle, radians
   * Return: Rotated vector.
   */
  Pt2 rotate(double rad) {
    double r = magn();
    double theta = angle();
    return new Pt2(r*Math.cos(theta+rad), r*Math.sin(theta+rad));
  }

  /*
   * Description: Returns a Pt2 rotated about a given point.
   * Parameters: rad: Rotation angle in radians
   *             base: Point to rotate about.
   * Return: Rotated vector.
   */
  Pt2 rotateAbout(double rad, Pt2 base) {
    return diff(base).rotate(rad).sum(base);
  }

  /*
   * Description: Returns this vector's coordinates for debugging.
   * Parameters: (none)
   * Return: String representation of this Pt2.
   */
  public String toString()
  {
    return String.format("  (%.2f, %.2f)", x, y);
  }
}

class Pt2_i {
  int x, y;
  /*
   * Description: Converts the given Pt2 into an integer Pt2.
   * Parameters: (None)
   * Return: (none)
   */
  Pt2_i(Pt2 p) {
    x = (int)p.x;
    y = (int)p.y;
  }

  /*
   * Description: Converts the given Pt2 into an integer Pt2.
   * Parameters: (none)
   * Return: (none)
   */
  Pt2_i(double xx, double yy) {
    x = (int)xx;
    y = (int)yy;
  }

  /*
   * Description: Returns whether the object is equal to this.
   * Parameters: o: Object to compare.
   * Return: Whether the objects are equal.
   */
  public boolean equals(Object o) {
    if (!(o instanceof Pt2_i)) return false;
    Pt2_i p = (Pt2_i) o;
    return p.x == x && p.y == y;
  }
}


class G {

  // Fonts for
  static Font btnFont, titleFont, comboFont, pulseFont, circFont, typoFont, LCDFont;
  static final boolean DEBUG = false;
  // static double[] lightVec = new double[3]; // Global light direction. (In normal right-hand 3D coordinates.)

  /*
   * Description: Inits the fonts.
   * Parameters: (none)
   * Return: (none)
   */
  static void init() {
    try {
      titleFont = Font.createFont( Font.TRUETYPE_FONT, new FileInputStream("Blox2.ttf") );
    } catch (FontFormatException ffe) {} catch (IOException ioe) {
      titleFont = new Font("Arial", Font.BOLD, 100);
    }
    try {
      btnFont = Font.createFont( Font.TRUETYPE_FONT, new FileInputStream("Crackman.otf") );
    } catch (FontFormatException ffe) {} catch (IOException ioe) {
      btnFont = new Font("Arial", Font.BOLD, 20);
    }

    try {
      LCDFont = Font.createFont( Font.TRUETYPE_FONT, new FileInputStream("LCD14.otf") );
    } catch (FontFormatException ffe) {} catch (IOException ioe) {
      LCDFont = new Font("Arial", Font.BOLD, 20);
    }
    try {
      comboFont = Font.createFont( Font.TRUETYPE_FONT, new FileInputStream("Betsy Flanagan.otf") );
    } catch (FontFormatException ffe) {} catch (IOException ioe) {
      comboFont = new Font("Arial", Font.PLAIN, 15);
    }
    try {
      pulseFont = Font.createFont( Font.TRUETYPE_FONT, new FileInputStream("High Fiber.ttf") );
    } catch (FontFormatException ffe) {} catch (IOException ioe) {
      pulseFont = new Font("Arial", Font.PLAIN, 15);
    }
    try {
      circFont = Font.createFont( Font.TRUETYPE_FONT, new FileInputStream("circulat.ttf") );
    } catch (FontFormatException ffe) {} catch (IOException ioe) {
      circFont = new Font("Arial", Font.PLAIN, 15);
    }
    try {
      typoFont = Font.createFont( Font.TRUETYPE_FONT, new FileInputStream("VTBULLET.ttf") );
    } catch (FontFormatException ffe) {} catch (IOException ioe) {
      typoFont = new Font("Arial", Font.PLAIN, 15);
    }
  }

  /*
   * Description: Returns the normalised angle.
   * Parameters: ang: Angle in radians.
   * Return: Normalised angle.
   */
  static double normalizeAngle(double ang) {
    while (ang>  Math.PI*2) ang-=Math.PI*2;
    while (ang< -Math.PI*2) ang+=Math.PI*2;
    if (ang>Math.PI)
      ang=ang -2*Math.PI;
    else if (ang<-Math.PI)
      ang=2*Math.PI + ang;
    return ang;
  }

  /*
   * Description: Returns the z-component of the cross product.
   * Parameters: x1, y1: x2, y2: Vectors to find the cross product for.
   * Return: z-component of the cross product.
   */
  static double crossProductZ(double x1, double y1, double x2, double y2) {
    return x1*y2 - x2*y1;
  }

  /*
   * Description: Returns the angle difference.
   * Parameters: targetAng, ang: Angles to find the difference between
   * Return: Difference between the angles.
   */
  static double deltaAngle(double targetAng, double ang) {
    double deltaAng = targetAng-ang;
    if (deltaAng>Math.PI) deltaAng=deltaAng - 2*Math.PI;
    else if (deltaAng<-Math.PI) deltaAng=2*Math.PI + deltaAng;
    return deltaAng;
  }

  /*
   * Description: Prints a formatted string.
   * Parameters: fmt, args: Arguments to pass to printf.
   * Return: (none)
   */
  static void sysprtf(String fmt, Object... args) {
    System.out.printf(fmt, args);
  }

  /*
   * Description: Returns the square of the value.
   */
  static double sq(double n) {
    return n * n;
  }

  /*
   * Description: Returns the magnitude of the Pt2.
   * Parameters: v: Pt2 to find the magnitude of.
   * Return: Magnitude.
   */
  static double magnitude(Pt2 v) {
    return Math.sqrt(v.x*v.x + v.y*v.y);
  }

  /*
   * Description: Makes a Path2D from a coordinate array, scaled to the scale factor.
   * Parameters: path: Path to add to
   *             scale: Scale factor.
   *             p: Coordinate array
   *             appendQ: Whether to append to the existing path.
   * Return: (none)
   */
  static void mkPath(Path2D path, double scale, double[][]p, boolean appendQ) {
    if (!appendQ) path.reset();
    path.moveTo(p[0][0]*scale,p[0][1]*scale);
    for (int i=1; i<p.length; i++) {
      path.lineTo(p[i][0]*scale, p[i][1]*scale);
    }
    path.closePath();
  }

  /*
   * Description: Makes the Path2D for the explosion animation.
   * Parameters: sz: Size of the explosion.
   *             phase: Reserved for future more elaborate animations.
   * Return: Path2D for the explosion animation.
   */
  static Path2D mkExplodePath(double sz, long phase)
  {
    Path2D l = new Path2D.Double();
    double[][] segs = new double[3+(int)(Math.random()*7)][2];
    double ox1 = sz*Math.random()/4;
    double oy1 = sz*Math.random()/4;
    double ox2 = sz*Math.random()/4;
    double oy2 = sz*Math.random()/4;
    for (int i=0; i<segs.length; i++) {
      double a=(Math.random()/3.0 + i)*2.0*Math.PI/segs.length;
      double l1 = sz * (0.3 + Math.random());
      double l2 = sz * (0.3 + Math.random());
      l.moveTo(l1*Math.cos(a)+ox1, l1*Math.sin(a)+oy1);
      l.lineTo(l2*Math.cos(a)+ox2, l2*Math.sin(a)+oy2);
    }
    return l;
  }

  /*
   * Description: Scales the rgb value.
   * Parameters: rgb: RGB value
   *             scale: Scale factor.
   * Return: New RGB value.
   */
  static int scaleRgb(int rgb, double scale) {
    int outc =
      (Math.min(255, (int)(scale*(rgb&0xff)+0.5))) +
      (Math.min(255, (int)(scale*((rgb>>8)&0xff)+0.5))<<8) +
      (Math.min(255, (int)(scale*((rgb>>16)&0xff)+0.5))<<16);
    return outc;
  }
}