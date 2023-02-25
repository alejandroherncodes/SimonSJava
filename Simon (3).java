import java.awt.Color;

import javalib.funworld.World;
import javalib.funworld.WorldScene;
import javalib.worldimages.AboveImage;
import javalib.worldimages.FontStyle;
import javalib.worldimages.OutlineMode;
import javalib.worldimages.OverlayOffsetImage;
import javalib.worldimages.PhantomImage;
import javalib.worldimages.Posn;
import javalib.worldimages.RectangleImage;
import javalib.worldimages.TextImage;
import javalib.worldimages.WorldImage;
import tester.Tester;

//class represents the world the game takes place in. 
class SimonWorld extends World {
  int playerScore; // Tracks # of successful clicks the user makes
  int matched;// tracks # of correct buttons user guesses in the current round
  ILoButton simonPattern; // ordered list of butons.
  ILoButton destructiveList; // used to flash buttons at every new round.
  // the buttons. mostly care about their color.
  Button yellowButton;
  Button redButton;
  Button blueButton;
  Button greenButton;
  int lastTimeFlashed; // tracks if the last flash was recent or not
  boolean lost; // true if the user loses.

  // base buttons.
  int sideLength = 100;
  Button baseYellowButton = new Button(Color.YELLOW, 5, 0, sideLength);
  Button baseRedButton = new Button(Color.RED, 0, 5, sideLength);
  Button baseBlueButton = new Button(Color.BLUE, 10, 5, sideLength);
  Button baseGreenButton = new Button(Color.GREEN, 5, 10, sideLength);

  // for initial game making
  SimonWorld() {
    super();
    this.playerScore = 0;
    this.matched = 0;

    ILoButton sp = new ConsLoButton(new ButtonGenerator().makeButton(), new MtLoButton());
    this.simonPattern = sp; // start with a list with one button.

    this.yellowButton = baseYellowButton;
    this.redButton = baseRedButton;
    this.blueButton = baseBlueButton;
    this.greenButton = baseGreenButton;

    this.destructiveList = this.simonPattern;

    this.lastTimeFlashed = 0;
    this.lost = false;
  }

  // Used to move the game to the next round.
  SimonWorld(int playerScore, int matched, ILoButton simonPattern) {
    super();
    this.simonPattern = simonPattern;
    this.playerScore = playerScore;
    this.matched = matched;
    this.destructiveList = simonPattern;

    this.yellowButton = baseYellowButton;
    this.redButton = baseRedButton;
    this.blueButton = baseBlueButton;
    this.greenButton = baseGreenButton;

    this.lastTimeFlashed = 0;
    this.lost = false;
  }

  // Used during the flashing period to flash buttons on and off.
  SimonWorld(int playerScore, int matched, ILoButton simonPattern, Button yb, Button rb, Button bb,
      Button gb, ILoButton destructive, int ltf) {
    super();
    this.simonPattern = simonPattern;
    this.playerScore = playerScore;
    this.matched = matched;

    // Mostly, we can use this to change the color of the buttons
    this.yellowButton = yb;
    this.redButton = rb;
    this.blueButton = bb;
    this.greenButton = gb;
    this.destructiveList = destructive;

    this.lastTimeFlashed = ltf;
    this.lost = false;
  }

  // This method draws the visual aspect of the game the user sees in a small
  // window. The method has two components
  // the main game screen shows the user the buttons to click, and compares their
  // answers with the simonPattern
  // the lost portion of the game recaps the points the user scored, and invitest
  // them to try again.
  public WorldScene makeScene() {
    int sceneSize = 500;
    WorldScene background = new WorldScene(sceneSize, sceneSize);
    int sideLength = 100;
    WorldImage pageHead = new TextImage("How long of a sequence can you remember?", Color.darkGray);

    // Creating the square grid.
    WorldImage centerSquares = new OverlayOffsetImage(this.redButton.drawButton(), 2 * sideLength,
        0, this.blueButton.drawButton());
    WorldImage bottomThreeSquares = new AboveImage(centerSquares, this.greenButton.drawButton());
    WorldImage allSquares = new AboveImage(this.yellowButton.drawButton(), bottomThreeSquares);
    WorldImage main = new AboveImage(pageHead, allSquares);

    if (lost) { // User has lost the game.
      // create end screen
      WorldImage endScreen = new AboveImage(new TextImage("Game Over", 50, Color.RED),
          new TextImage("You won " + (this.simonPattern.length() - 1) + " rounds", 30, Color.BLUE));
      WorldImage endScreenWRestart = new OverlayOffsetImage(endScreen, 0, 80,
          new TextImage("Press 'r' to restart", 40, Color.BLACK));
      return background.placeImageXY(endScreenWRestart, sceneSize / 2, sceneSize / 2);
    }
    else { // User is still playing the game.
      WorldImage score = new PhantomImage(
          new TextImage("Total Matched: " + this.playerScore, 20, FontStyle.BOLD, Color.BLACK));
      WorldImage round = new PhantomImage(
          new TextImage("Round: " + (this.simonPattern.length()), 20, FontStyle.BOLD, Color.BLACK));
      WorldImage scoreStack = new OverlayOffsetImage(score, 0, 20, round);

      WorldImage elseMain = new PhantomImage(new OverlayOffsetImage(scoreStack, 140, 120, main));

      return background.placeImageXY(elseMain, 250, 250);
    }
  }

  // This method takes in a button pressed by the user, and compares it to the
  // simonPattern.
  // If it matches, the player score is incremented, and the game continues on.
  // Otherwise, the player loses.
  public SimonWorld incrementForward(Button pressed) {

    if (this.simonPattern.buttonEqualsNthListItem(pressed, this.matched)) { // Correct button?
      if (this.matched >= simonPattern.length() - 1) { // Moves onto the next round
        return new SimonWorld((this.playerScore + 1), 0,
            new ConsLoButton(new ButtonGenerator().makeButton(), this.simonPattern));
      }
      else { // Correct button, but there is more to go within this round.
        this.playerScore = this.playerScore + 1;
        this.matched = this.matched + 1;
        return this;
      }
    }
    else { // Player has lost.
      this.lost = true;
      return this;
    }
  }

  // This method allows the player to restart the game by pressing "r". it returns
  // a new simonWorld state
  public SimonWorld onKeyEvent(String ke) { // restart game
    if (ke.equals("r")) {
      return new SimonWorld(); // Restart the game.
    }
    else { // If any other key is pressed, do nothing.
      return this;
    }
  }

  // This method overrides the default World's onTick, and is called every world
  // tick. It returns new
  // world states.
  public SimonWorld onTick() {
    // flash the next button!
    if ((this.destructiveList.length() > 0) && this.lastTimeFlashed == 0) {

      return this.flash(this.destructiveList);
    }

    // Either no buttons to clash, or we want to temporarily revert to the base to
    // give the flash time to cool down.
    else {
      this.lastTimeFlashed = 0; // reset the flash time.
      this.blueButton = baseBlueButton;
      this.redButton = baseRedButton;
      this.greenButton = baseGreenButton;
      this.yellowButton = baseYellowButton;
      return this;
    }
  }

  // Handles mouse clicks and is given the mouse location
  // return new SimonWorld, with the new userPattern.
  public SimonWorld onMousePressed(Posn pos) {
    // Constants
    int sideLength = 100;
    int topLeftYellowX = 200;
    int topLeftYellowY = 100; // around this.

    // Checks if the user clicked the top yellow button.
    if ((pos.x > topLeftYellowX) && (pos.x < (topLeftYellowX + sideLength))
        && (pos.y > topLeftYellowY) && (pos.y < (topLeftYellowY + sideLength))) {
      return incrementForward(this.yellowButton);// increments the top yellow button.
    }
    // Checks if the user clicked the red button.
    if ((pos.x > topLeftYellowX - sideLength) && (pos.x < (topLeftYellowX))
        && (pos.y > topLeftYellowY + sideLength) && (pos.y < (topLeftYellowY + 2 * sideLength))) {
      return incrementForward(this.redButton);
    }
    // Checks if the user clicked the red button.
    if ((pos.x > topLeftYellowX + sideLength) && (pos.x < (topLeftYellowX + 2 * sideLength))
        && (pos.y > topLeftYellowY + sideLength) && (pos.y < (topLeftYellowY + 2 * sideLength))) {
      return incrementForward(this.blueButton);
    }
    // Checks if the user clicked the red button.
    if ((pos.x > topLeftYellowX) && (pos.x < (topLeftYellowX + sideLength))
        && (pos.y > topLeftYellowY + 2 * sideLength)
        && (pos.y < (topLeftYellowY + 3 * sideLength))) {
      return incrementForward(this.greenButton);
    }
    else { // The user didn't click any buttons. Do nothing.
      return this;
    }
    
    /*
     * TEMPLATE FIELDS: 
     * ... this.simonPattern ... -- ILoButton
     * ... this.playerScore ... -- int
     * ... this.matched ... -- int
     * ... this.destructiveList... -- ILoButton
     * ... this.yellowButton ... -- Button
     * ... this.redButton  ... -- Button
     * ... this.blueButton ... -- Button
     * ... this.greenButton ... -- Button
     * ... this.lastTimeFlashed ... -- int
     * ... this.lost ... -- boolean
     * 
     * METHODS 
     * ... this.makeScene() ... --WorldScene
     * ... this.incrementForward(Button) ... --SimonWorld
     * ... this.onKeyEvent(String) ... --SimonWorld
     * ... this.onTick() ... --SimonWorld
     * ... this.onMousePressed ... --SimonWorld
     * 
     * METHODS ON FIELDS:
     * Button (this.yellowButton, this.redButton, 
     *          this.blueButton, this.greeenButton, 
     *          this.baseYellowButton, this.baseredButton, 
     *          this.baseBlueButton, this.baseGreenButton);
     * ... changeColor(Color) ... --Button
     * ... drawButton() ... --WorldImage
     * ... sameButton(Button) ... --boolean
     * 
     * ILoButton (this.simonPattern, this.destructiveList):
     * ... firstItemEquals(Button) ... --boolean
     * ... buttonEqualsNthListItem(Button, int) ... --boolean
     * ... getNthItem(int) ... --ILoButton
     * ... length() ... --int
     * ... shorten() ... --ILoButton
     * ... sameFirst(ILoButton) ... --boolean
     */
    
  }

  // Method for flashing the world state. Returns a new SimonWorld. Used heavily
  // by onTick().
  public SimonWorld flash(ILoButton dl) {

    this.destructiveList = this.destructiveList.shorten(); // go to the next item in the dl
    if (dl.firstItemEquals(baseYellowButton)) { // flash yellow
      return new SimonWorld(this.playerScore, this.matched, this.simonPattern,
          baseYellowButton.changeColor(new Color(100, 100, 0)), baseRedButton, baseBlueButton,
          baseGreenButton, this.destructiveList, 1);
    }
    if (dl.firstItemEquals(baseBlueButton)) { // flash blue
      return new SimonWorld(this.playerScore, this.matched, this.simonPattern, baseYellowButton,
          baseRedButton, baseBlueButton.changeColor(new Color(0, 0, 100)), baseGreenButton,
          this.destructiveList, 1);
    }
    if (dl.firstItemEquals(baseGreenButton)) { // flash green
      return new SimonWorld(this.playerScore, this.matched, this.simonPattern, baseYellowButton,
          baseRedButton, baseBlueButton, baseGreenButton.changeColor(new Color(0, 100, 0)),
          this.destructiveList, 1);
    }
    if (dl.firstItemEquals(baseRedButton)) { // flash red
      return new SimonWorld(this.playerScore, this.matched, this.simonPattern, baseYellowButton,
          baseRedButton.changeColor(new Color(100, 0, 0)), baseBlueButton, baseGreenButton,
          this.destructiveList, 1);
    }
    else { // return the world with the base button colors.
      return new SimonWorld(this.playerScore, this.matched, this.simonPattern, baseYellowButton,
          baseRedButton, baseBlueButton, baseGreenButton, this.destructiveList, 0);
    }
  }
}

//Represents a list of buttons
interface ILoButton {

  // Returns if the first item of this ILoButton equals the button provided
  boolean firstItemEquals(Button but);

  // Returns if the nth item in this ILoList is the same as the button provided.
  boolean buttonEqualsNthListItem(Button but, int inc);

  // returns the nth item in this list
  ILoButton getNthItem(int inc);

  // Returns the length of this list. An MtLoButton has a length of 0.
  int length();

  // Returns a new list without the first item. If called on an MtLoButton, it
  // returns a new MtLoButton.
  ILoButton shorten();

  // Returns if this list and the other ILoButton list share the same first item.
  // NOTE: not actually used as a method anywhere.
  boolean sameFirst(ILoButton but);
}

//Represents an empty list of buttons
class MtLoButton implements ILoButton {

  // Returns if the first item of this ILoButton equals the button provided
  public boolean firstItemEquals(Button but) { // unclear if this shoudl be true
    return true;
  }

  // Returns if the nth item in this ILoList is the same as the button provided.
  public boolean buttonEqualsNthListItem(Button but, int inc) {
    return false;
  }

  // returns the nth item in this list
  public ILoButton getNthItem(int inc) {
    return new MtLoButton();
  }

  // Returns the length of this list. An MtLoButton has a length of 0.
  public int length() {
    return 0;
  }

  // Returns a new list without the first item. If called on an MtLoButton, it
  // returns a new MtLoButton.
  public ILoButton shorten() {
    return new MtLoButton();
  }

  // Returns if this list and the other ILoButton list share the same first item.
  // NOTE: not actually used as a method anywhere.
  public boolean sameFirst(ILoButton but) {
    return false; // should this be false?
  }
  
  /*
   * TEMPLATE FIELDS: 
   * 
   * METHODS 
   * ... this.firstItemEquals(Button) ... --boolean
   * ... this.buttonEqualsNthListItem(Button, int) ... --boolean
   * ... this.getNthItem(int) ... --ILoButton
   * ... this.length() ... --int
   * ... this.shorten() ... --ILoButton
   * ... this.sameFirst(ILoButton) ... --boolean
   * METHODS ON FIELDS:
   * ... 
   */
  
}

//Represents a non-empty list of buttons
class ConsLoButton implements ILoButton {
  Button first;
  ILoButton rest;

  ConsLoButton(Button first, ILoButton rest) {
    this.first = first;
    this.rest = rest;
  }

  // Returns if the first item of this ILoButton equals the button provided
  public boolean firstItemEquals(Button but) {
    return this.first.sameButton(but);
  }

  // Returns if this list and the other ILoButton list share the same first item.
  // NOTE: not actually used as a method anywhere.
  public boolean sameFirst(ILoButton ilb) {
    return ilb.buttonEqualsNthListItem(this.first, 0);
  }

  // Returns if the nth item in this ILoList is the same as the button provided.
  public boolean buttonEqualsNthListItem(Button but, int inc) {
    return this.getNthItem(inc).firstItemEquals(but);
  }

  // returns the nth item in this list
  public ILoButton getNthItem(int inc) {
    if (inc == 0) {
      return this;
    }
    else {
      return this.rest.getNthItem(inc - 1);
    }
  }

  // Returns the length of this list. An MtLoButton has a length of 0.
  public int length() {
    return 1 + this.rest.length();
  }

  // Returns a new list without the first item. If called on an MtLoButton, it
  // returns a new MtLoButton.
  public ILoButton shorten() {
    return this.rest;// this.rest is an ILoButton

  }
  
  /*
   * TEMPLATE FIELDS: 
   * ... this.first ... -- Button
   * ... this.rest ... -- ILoButton
   * 
   * METHODS 
   * ... this.firstItemEquals(Button) ... --boolean
   * ... this.buttonEqualsNthListItem(Button, int) ... --boolean
   * ... this.getNthItem(int) ... --ILoButton
   * ... this.length() ... --int
   * ... this.shorten() ... --ILoButton
   * ... this.sameFirst(ILoButton) ... --boolean
   * 
   * METHODS ON FIELDS:
   * ... this.first.changeColor(Color) ... --Button
   * ... this.first.drawButton() ... --WorldImage
   * ... this.first.sameButton(Button) ... --boolean
   */
}

//Represents one of the four buttons you can click.
class Button {
  Color color;
  int x;
  int y;
  int sideLength;

  Button(Color color, int x, int y, int sideLength) {
    this.color = color;
    this.x = x;
    this.y = y;
    this.sideLength = sideLength;
  }

  // Returns a new button with the same charecteristics, but a color that matches
  // the given color.
  Button changeColor(Color color) {
    return new Button(color, this.x, this.y, this.sideLength);
  }
  /*
  // Draw this button dark
  WorldImage drawDark() {
    return this.drawButton(this.color.darker().darker());
  }

  // Draw this button lit
  WorldImage drawLit() {
    return this.drawButton(this.color.brighter().brighter());
  }

  // returns the image for an apple
  WorldImage drawButton(Color color) {
    return new RectangleImage(this.sideLength, this.sideLength, OutlineMode.SOLID, this.color);
  }
  */

  // Flashes the button if it's next in Simon's randomly generated list.
  //public WorldImage flash() {
  //  return this.drawLit();
  //}
  
  WorldImage drawButton() {
    return new RectangleImage(this.sideLength, this.sideLength, OutlineMode.SOLID, this.color);
  }

  // Returns if this button and the other button are the same button. Button's are
  // compared by their x,y location, not their color or sidelength.
  boolean sameButton(Button other) {
    return this.x == other.x && this.y == other.y;
  }

  
  /*
   * TEMPLATE FIELDS: 
   * ... this.color ... -- Color
   * ... this.x ... -- int
   * ... this.y ... -- int
   * ... this.sideLength ... -- int

   * 
   * METHODS 
   * ... this.changeColor(Color) ... --Button
   * ... this.drawButton() ... --WorldImage
   * ... this.sameButton(Button) ... --boolean
   * 
   * METHODS ON FIELDS:
   * ... Color has numerous methods available to it, such as Color.color
   * ... 
   */
  
}

//this class is a utility class meant to randomly generate a button with one of the 4 chosen colors.
class ButtonGenerator {

  // Method makeButton() returns one of 4 possible buttons, denoted primarily by
  // their color and relative location.
  Button makeButton() {
    double rand = Math.random();

    if (rand > 0.75) {
      return new Button(Color.YELLOW, 5, 0, 100);
    }
    if (rand > 0.5) {
      return new Button(Color.RED, 0, 5, 100);
    }
    if (rand > 0.25) {
      return new Button(Color.BLUE, 10, 5, 100);
    }
    else {
      return new Button(Color.GREEN, 5, 10, 100);
    }
  }
}

//Examples
class ExamplesSimon {
  // put all of your examples and tests here

  ILoButton buttonList = new ConsLoButton(new Button(Color.YELLOW, 5, 0, 100),
      new ConsLoButton(new Button(Color.GREEN, 5, 5, 100), new MtLoButton()));
  ILoButton emptyList = new MtLoButton();
  ILoButton shortenedList = emptyList.shorten();
  
  Button b3 = new Button(Color.YELLOW, 5, 0, 100);
  Button b1 = new Button(Color.YELLOW, 5, 0, 100);
  Button b2 = new Button(Color.BLUE, 0, 5, 100);

  boolean testOnKeyEvent(Tester t) {
    // Tests if the game restarts when the user presses "r".
    SimonWorld world = new SimonWorld();
    SimonWorld restartedWorld = world.onKeyEvent("r");
    return t.checkExpect(restartedWorld.playerScore, 0);
  }
  

  boolean testOnTick(Tester t) {
    // Tests if the world flashes when onTick is called.
    SimonWorld world = new SimonWorld();
    SimonWorld flashedWorld = world.onTick();
    return t.checkExpect(flashedWorld.yellowButton.color, new Color(255, 255, 0));
  }

  boolean testIncrementForward(Tester t) {
    // Tests if the user score increments when the user clicks the correct button.
    SimonWorld world = new SimonWorld();
    
    // Tests if the user score does not increment when the user clicks the wrong button.
    SimonWorld incrementedWorld2 = world.incrementForward(new Button(Color.BLUE, 0, 5, 100));
    
    return t.checkExpect(incrementedWorld2.matched, 0)
        && t.checkExpect(incrementedWorld2.playerScore, 0);
  }

  boolean testMakeScene(Tester t) {
    SimonWorld world = new SimonWorld();
    WorldScene scene = world.makeScene();
    return t.checkExpect(scene.width, 500) && t.checkExpect(scene.height, 500);
  }
  
  boolean testButtonEqualsNthListItem(Tester t) {
    return t.checkExpect(buttonList.buttonEqualsNthListItem(new Button(Color.GREEN, 5, 5, 100), 1), true);
  }

  boolean testFlash(Tester t) {
    SimonWorld world = new SimonWorld();
    SimonWorld flashedWorld = world.flash(world.destructiveList);
    return t.checkExpect(flashedWorld.yellowButton.color, new Color(255, 255, 0));
  }
  
  boolean testOnMousePressed(Tester t) {
    
    // Tests if the user score increments when the user clicks the correct button.
    SimonWorld world = new SimonWorld();
    SimonWorld incrementedWorld = world.onMousePressed(new Posn(210, 110));
    
    // Tests if the user score does not increment when the user clicks the wrong button.
    SimonWorld incrementedWorld2 = world.onMousePressed(new Posn(310, 110));
    
    return t.checkExpect(incrementedWorld2.playerScore, 0)
        && t.checkExpect(incrementedWorld2.playerScore, 0);
  }
  
  boolean testLength(Tester t) {
    return t.checkExpect(buttonList.length(), 2);
  }
  
  boolean testSameFirst(Tester t) {
    return t.checkExpect(buttonList.sameFirst(buttonList), true);
  }
  
  public boolean testButton(Tester t) {
    return t.checkExpect(b1.sameButton(b2), false) 
        && t.checkExpect(b1.sameButton(b1), true);
  }

  public boolean testChangeColor(Tester t) {
    Button b4 = b3.changeColor(Color.RED);
    
    return t.checkExpect(b3.color, Color.YELLOW)
        && t.checkExpect(b4.color, Color.RED);
  }

  boolean testButtonList(Tester t) {
    return t.checkExpect(buttonList.getNthItem(0), buttonList)
        && t.checkExpect(buttonList.getNthItem(5), new MtLoButton()) && t.checkExpect(
            buttonList.buttonEqualsNthListItem(new Button(Color.GREEN, 5, 5, 100), 1), true);
  }

  boolean testEquals(Tester t) {
    return t.checkExpect(buttonList.firstItemEquals(new Button(Color.YELLOW, 5, 0, 100)), true)
        && t.checkExpect(buttonList.firstItemEquals(new Button(Color.YELLOW, 5, 5, 100)), false);
  }
  
  boolean testShortenEmpty(Tester t) {
    return t.checkExpect(0, shortenedList.length());
  }
  
  boolean testLengthEmpty(Tester t) {
    return t.checkExpect(0, emptyList.length());
  }

  // runs the game by creating a world and calling bigBang
  boolean testSimonSays(Tester t) {
    SimonWorld starterWorld = new SimonWorld();
    int sceneSize = 500;
    return starterWorld.bigBang(sceneSize, sceneSize, .3);
  }
}

