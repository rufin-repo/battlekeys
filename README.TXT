===========================================================
BATTLE KEYS
- A space typing game designed and implemented by Rufin Hsu

===========================================================

<Introduction>

Space-date: ICS-202425-0123.
You are the captain of the flagship KBD-Destroyer,
deep in the enemy territory of the Alpha-battalion,
under siege. Swarms of formidable enemy fighters armed
with strings of T3xt-Torpedoes are circling your ship,
ready to attack. Your only defence is your sub-space
Resonance Pulse (RePulse). By firing a correct sequence
of pulses with frequencies identified by text
characters, the enemy T3xt-Torpedoes will resonate
violently and explode.

If you could figure out the master frequency series of
the current wave, then a correct sequence of RePulses
would wipe out the entire squadron in one go!

May your fingers be with you and your keys never get stuck.



<How to Play>

* Click/press the [Start] button in the Title screen to
  begin the game or [Exit] to exit.

* Your ship, the KBD-Destroyer, will be at the centre of
  the Battle window.  Enemy fighters carrying strings of
  text characters (T3xt-torpedoes) will start to orbit
  your ship, launching deadly strings of T3xt-torpedoes
  towards it. You have to generate a matching sequence
  of RePulse frequencies (i.e. type out a sequence of
  case sensitive characters) in order to destroy the
  T3xt-torpedo strings.
  If a Torpedo string is detonated before being released,
  the enemy fighters will get destroyed as well.

* Your ship will automatically turn around to point at the
  focused enemy fighter. Only one fighter/torpedo-string
  (the focused one) could be destroyed at one time. (Unless
  you’ve fired out the entire “master phrase” as one combo
  sequence, see below.)

* On the other hand, if your ship gets hit by released
  strings of T3xt-torpedoes, your ship will suffer damages.
  If your ship receives more than 5 hits, it will be
  destroyed and the game will end.

* Combo scores will be awarded if your pulse-sequence is
  uninterrupted by “typos”.

* Pay attention to the T3xt strings of the fighters in
  each wave and try to guess the “Master Phrase”, i.e. a
  (usually meaningful) sentence/phrase that uses all the
  words (could be more than once) carried by the fighters.
  If the tail of your current combo-sequence matches
  (case sensitively) that master phrase, the entire
  squadron will be wiped out in one go, and you could
  proceed to the next wave immediately.

* Click/press the [Quit] button to quit the game and return
  the title screen.

* Click/press the [Restart] button in the middle of a game
  to restart from the beginning.

* The current score and the top three longest combo
  sequences will be displayed.


<Remarks>

Intricate Design and Implementation Details:
--------------------------------------------
* The home-ship KBD-Destroyer and the enemy Alpha-battalion
  fighters are both modelled as 3D polygons rendered using
  Path2D, flat-shaded using a simple diffused illumination
  model based on a light source from infinity – appropriate
  for objects in space. (Check out HomeShip.draw() and
  EnemyShip.draw() for details.)

* The flight of the enemy fighters is driven by a Newtonian
  dynamics model. The flight paths are specified as simple
  rough polygons. The fighters will get pulled by an invisible
  attraction force proportional to the current fractional
  target position (based on real clock time) along the
  polygonal flight path. Therefore if an enemy fighter is
  lagging behind or is deviated from the intended path, it
  will automatically accelerate towards the moving target
  location, as if the fighters are being pulled along by a
  rubber band. The resulting motions of the fighters are
  therefore always second-order continuous and are
  dynamically convincing.

* The chain of T3xt-torpedoes is also being pulled along by
  the fighters but with a stiff invisible string. Therefore,
  they would tag behind one after another like a train.

* When a chain of T3xt-torpedoes is released, the torpedoes
  will simply follow their current linear velocity, which
  will usually be towards the KBD-Destroyer. Angular velocity
  of the torpedoes is ignored, so the released characters
  do not spin towards the target. This will cause their
  final journey to seem slightly less convincing (in terms
  of physics) but make the characters easier to read.

* The Resonance Pulses are animated in the style of a
  vibrating string. This is done by creating a Path2D with
  random but smoothly connected quadratic curve segments.
  The number of curve segments is determined by the total
  length of the “wavefront”, i.e. proportional to the radius
  of the pulse. (Check out Pulse.draw() for details.)


Use of HashMap:
---------------
* Lists of T3xt-torpedo chains (and the enemy fighters that
  carry them) are stored in a ConcurrentHashMap using the
  torpedo text string as key. This allows thread-safe, quick
  look ups of the particular torpedo chains that are being
  attacked by the current sequence of pulses.


Use of Collections:
-------------------
* Pulses are stored in a queue (a synchronized
  LinkedList<Pulse>); these are added when pulses are
  emitted and removed when they dissipate.

* Flight path points and T3xt-torpedo groups are stored in
  synchronized ArrayLists.

* In collecting the most frequently destroyed torpedo types,
  a HashSet<String, Integer> and a TreeSet<FreqData> are
  used to store and order the frequency data. (FreqData
  implements the Comparable interface.)

* In the collision testing of launched torpedoes with the
  home ship, a HashSet<TxTorpedo> is used to record the set
  of collided torpedoes that has already caused damage to
  the home ship. This is needed to prevent the still moving
  torpedoes from causing further damage.


Functionalities Missing:
------------------------
None.


Additional Features Introduced since Initial Design:
----------------------------------------------------
* Home ship damage accumulation (instead of instant
  destruction).

* Newtonian dynamics driven fighter movement.

* Shaded 3D graphics realised using Path2D for enemy fighters
  and the home ship.

* Animated vibrating string waves to represent the pulse
  wavefronts.

* Automatically turning home ship to provide a visual cue to
  the player for indicating which torpedo/enemy ship is
  currently being focused upon.

* Animated explosions and home ship damages.


Known bugs:
-----------
* Display of long combo sequences at the bottom of the
  BattleView could go outside of the screen and get
  truncated. Ideally some ellipses could be introduced
  to more gracefully handle such sequences. (Cosmetic)

* The text characters representing the T3xt Torpedoes are
  visually slightly off-centered because it is not easy to
  determine the tight bounding box of the actual glyph
  shapes of individual characters (as opposed to the font
  bounding box). (Cosmetic)


Credits, Copyright and Third Party Resources:
---------------------------------------------
Original game design, scenario, design by Rufin Hsu.
All source code (c) 2025 Rufin Hsu. All rights reserved.
All java code is original. No third party source code used.
All external TrueType fonts included are free for
non-commercial use.
