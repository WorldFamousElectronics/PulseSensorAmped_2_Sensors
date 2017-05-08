import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import processing.serial.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class PulseSensorAmpd_Processing_2_Sensors extends PApplet {

/*
DISPLAYS MULTIPLE PULSE SENSOR DATA STREAMS
THIS PROGRAM WORKS WITH PulseSensorAmped_n_Sensors ARDUINO CODE

PRESS 'S' OR 's' KEY TO SAVE A PICTURE OF THE SCREEN IN SKETCH FOLDER (.jpg)
MADE BY JOEL MURPHY WINTER 2016, MODIFIED WINTER 2017
UPDATED BY JOEL MURPHY SUMMER 2016 WITH SERIAL PORT LOCATOR TOOL
UPDATED BY JOEL MURPHY WINTER 2017 WITH IMPROVED SERIAL PORT SELECTOR TOOL

THIS CODE PROVIDED AS IS, WITH NO CLAIMS OF FUNCTIONALITY OR EVEN IF IT WILL WORK
      WYSIWYG
*/



PFont font;

Serial port;
int numSensors = 2;

int[] Sensor;      // HOLDS PULSE SENSOR DATA FROM ARDUINO
int[] IBI;         // HOLDS TIME BETWEN HEARTBEATS FROM ARDUINO
int[] BPM;         // HOLDS HEART RATE VALUE FROM ARDUINO
int[][] RawPPG;      // HOLDS HEARTBEAT WAVEFORM DATA BEFORE SCALING
int[][] ScaledPPG;   // USED TO POSITION SCALED HEARTBEAT WAVEFORM
int[][] ScaledBPM;      // USED TO POSITION BPM DATA WAVEFORM
float offset;    // USED WHEN SCALING PULSE WAVEFORM TO PULSE WINDOW
int eggshell = color(255, 253, 248);
int heart[];   // USED TO TIME THE HEART 'PULSE'

//  THESE VARIABLES DETERMINE THE SIZE OF THE DATA WINDOWS
int PulseWindowWidth; // = 490;
int PulseWindowHeight; // = 512;
int PulseWindowX;
int PulseWindowY[];
int BPMWindowWidth; // = 180;
int BPMWindowHeight; // = 340;
int BPMWindowX;
int BPMWindowY[];
int spacer = 10;
boolean beat[];    // set when a heart beat is detected, then cleared when the BPM graph is advanced

// SERIAL PORT STUFF TO HELP YOU FIND THE CORRECT SERIAL PORT
String serialPort;
String[] serialPorts = new String[Serial.list().length];
boolean serialPortFound = false;
Radio[] button = new Radio[Serial.list().length*2];
int numPorts = serialPorts.length;
boolean refreshPorts = false;


public void setup() {
    // Stage size
  frameRate(100);
  font = loadFont("Arial-BoldMT-24.vlw");
  textFont(font);
  textAlign(CENTER);
  rectMode(CORNER);
  ellipseMode(CENTER);
  // Display Window Setup
  PulseWindowWidth = 490;
  PulseWindowHeight = 640/numSensors;
  PulseWindowX = 10;
  PulseWindowY = new int [numSensors];
  for(int i=0; i<numSensors; i++){
    PulseWindowY[i] = 43 + (PulseWindowHeight * i);
    if(i > 0) PulseWindowY[i]+=spacer*i;
  }
  BPMWindowWidth = 180;
  BPMWindowHeight = PulseWindowHeight;
  BPMWindowX = PulseWindowX + PulseWindowWidth + 10;
  BPMWindowY = new int [numSensors];
  for(int i=0; i<numSensors; i++){
    BPMWindowY[i] = 43 + (BPMWindowHeight * i);
    if(i > 0) BPMWindowY[i]+=spacer*i;
  }
  heart = new int[numSensors];
  beat = new boolean[numSensors];
  // Data Variables Setup
  Sensor = new int[numSensors];      // HOLDS PULSE SENSOR DATA FROM ARDUINO
  IBI = new int[numSensors];         // HOLDS TIME BETWEN HEARTBEATS FROM ARDUINO
  BPM = new int[numSensors];         // HOLDS HEART RATE VALUE FROM ARDUINO
  RawPPG = new int[numSensors][PulseWindowWidth];          // initialize raw pulse waveform array
  ScaledPPG = new int[numSensors][PulseWindowWidth];       // initialize scaled pulse waveform array
  ScaledBPM = new int [numSensors][BPMWindowWidth];           // initialize BPM waveform array

  // set the visualizer lines to 0
  resetDataTraces();

 background(0);
 noStroke();
 // DRAW OUT THE PULSE WINDOW AND BPM WINDOW RECTANGLES
 drawDataWindows();
 drawHeart();

  // GO FIND THE ARDUINO
  fill(eggshell);
  text("Select Your Serial Port",245,30);
  listAvailablePorts();
}

public void draw() {
if(serialPortFound){
  // ONLY RUN THE VISUALIZER AFTER THE PORT IS CONNECTED
  background(0);
  drawDataWindows();
  drawPulseWaveform();
  drawBPMwaveform();
  drawHeart();
  printDataToScreen();

} else { // SCAN TO FIND THE SERIAL PORT
  autoScanPorts();

  if(refreshPorts){
    refreshPorts = false;
    drawDataWindows();
    drawHeart();
    listAvailablePorts();
  }

  for(int i=0; i<numPorts+1; i++){
    button[i].overRadio(mouseX,mouseY);
    button[i].displayRadio();
  }

}

}  //end of draw loop


public void drawDataWindows(){
  noStroke();
  // DRAW OUT THE PULSE WINDOW AND BPM WINDOW RECTANGLES
  fill(eggshell);  // color for the window background
  for(int i=0; i<numSensors; i++){
    rect(PulseWindowX, PulseWindowY[i], PulseWindowWidth, PulseWindowHeight);
    rect(BPMWindowX, BPMWindowY[i], BPMWindowWidth, BPMWindowHeight);
  }
}

public void drawPulseWaveform(){
  // DRAW THE PULSE WAVEFORM
  // prepare pulse data points
  for (int i=0; i<numSensors; i++) {
    RawPPG[i][PulseWindowWidth-1] = (1023 - Sensor[i]);   // place the new raw datapoint at the end of the array

    for (int j = 0; j < PulseWindowWidth-1; j++) {      // move the pulse waveform by
      RawPPG[i][j] = RawPPG[i][j+1];                         // shifting all raw datapoints one pixel left
      float dummy = RawPPG[i][j] * 0.625f/numSensors;       // adjust the raw data to the selected scale
      offset = PApplet.parseFloat(PulseWindowY[i]);                // calculate the offset needed at this window
      ScaledPPG[i][j] = PApplet.parseInt(dummy) + PApplet.parseInt(offset);   // transfer the raw data array to the scaled array
    }
    stroke(250, 0, 0);                               // red is a good color for the pulse waveform
    noFill();
    beginShape();                                  // using beginShape() renders fast
    for (int x = 1; x < PulseWindowWidth-1; x++) {
      vertex(x+10, ScaledPPG[i][x]);                    //draw a line connecting the data points
    }
    endShape();
  }

}

public void drawBPMwaveform(){
// DRAW THE BPM WAVE FORM
// first, shift the BPM waveform over to fit then next data point only when a beat is found
for (int i=0; i<numSensors; i++) {  // ONLY ADVANCE THE BPM WAVEFORM WHEN THERE IS A BEAT
if (beat[i] == true) {   // move the heart rate line over one pixel every time the heart beats
  beat[i] = false;      // clear beat flag (beat flag waset in serialEvent tab)

    for (int j=0; j<BPMWindowWidth-1; j++) {
      ScaledBPM[i][j] = ScaledBPM[i][j+1];                  // shift the bpm Y coordinates over one pixel to the left
    }
    // then limit and scale the BPM value
    BPM[i] = constrain(BPM[i], 0, 200);                     // limit the highest BPM value to 200
    float dummy = map(BPM[i], 0, 200, BPMWindowY[i]+BPMWindowHeight, BPMWindowY[i]);   // map it to the heart rate window Y
    ScaledBPM[i][BPMWindowWidth-1] = PApplet.parseInt(dummy);       // set the rightmost pixel to the new data point value
  }
}
// GRAPH THE HEART RATE WAVEFORM
stroke(250, 0, 0);                          // color of heart rate graph
strokeWeight(2);                          // thicker line is easier to read
noFill();

for (int i=0; i<numSensors; i++) {
  beginShape();
  for (int j=0; j < BPMWindowWidth; j++) {    // variable 'j' will take the place of pixel x position
    vertex(j+BPMWindowX, ScaledBPM[i][j]);                 // display history of heart rate datapoints
  }
  endShape();
}
}
public void drawHeart(){
  // DRAW THE HEART AND MAYBE MAKE IT BEAT
    fill(250,0,0);
    stroke(250,0,0);
  int bezierZero = 0;
  for(int i=0; i<numSensors; i++){
    // the 'heart' variable is set in serialEvent when arduino sees a beat happen
    heart[i]--;                    // heart is used to time how long the heart graphic swells when your heart beats
    heart[i] = max(heart[i], 0);       // don't let the heart variable go into negative numbers
    if (heart[i] > 0) {             // if a beat happened recently,
      strokeWeight(8);          // make the heart big
    }
    smooth();   // draw the heart with two bezier curves
    bezier(width-100, bezierZero+70, width-20, bezierZero, width, bezierZero+160, width-100, bezierZero+170);
    bezier(width-100, bezierZero+70, width-190, bezierZero, width-200, bezierZero+160, width-100, bezierZero+170);
    strokeWeight(1);          // reset the strokeWeight for next time
    bezierZero += BPMWindowHeight+spacer;
  }
}



public void listAvailablePorts(){
  println(Serial.list());    // print a list of available serial ports to the console
  serialPorts = Serial.list();
  fill(0);
  textFont(font,16);
  textAlign(LEFT);
  // set a counter to list the ports backwards
  int yPos = 0;

  for(int i=numPorts-1; i>=0; i--){
    button[i] = new Radio(35, 95+(yPos*20),12,color(180),color(80),color(255),i,button);
    text(serialPorts[i],50, 100+(yPos*20));
    yPos++;
  }
  int p = numPorts;
   fill(233,0,0);
  button[p] = new Radio(35, 95+(yPos*20),12,color(180),color(80),color(255),p,button);
    text("Refresh Serial Ports List",50, 100+(yPos*20));

  textFont(font);
  textAlign(CENTER);
}

public void autoScanPorts(){
  if(Serial.list().length != numPorts){
    if(Serial.list().length > numPorts){
      println("New Ports Opened!");
      int diff = Serial.list().length - numPorts;	// was serialPorts.length
      serialPorts = expand(serialPorts,diff);
      numPorts = Serial.list().length;
    }else if(Serial.list().length < numPorts){
      println("Some Ports Closed!");
      numPorts = Serial.list().length;
    }
    refreshPorts = true;
    return;
}
}

public void resetDataTraces(){
  for (int i=0; i<numSensors; i++) {
    BPM[i] = 0;
    for(int j=0; j<BPMWindowWidth; j++){
      ScaledBPM[i][j] = BPMWindowY[i] + BPMWindowHeight;
    }
  }
  for (int i=0; i<numSensors; i++) {
    Sensor[i] = 512;
    for (int j=0; j<PulseWindowWidth; j++) {
      RawPPG[i][j] = 1024 - Sensor[i]; // initialize the pulse window data line to V/2
    }
  }
}

public void printDataToScreen(){ // PRINT THE DATA AND VARIABLE VALUES
    fill(eggshell);                                       // get ready to print text
    text("Pulse Sensor Amped 2 Sensor Visualizer", 245, 30);     // tell them what you are
    for (int i=0; i<numSensors; i++) {
      text("Sensor # " + (i+1), 800, BPMWindowY[i] + 220);
      text(BPM[i] + " BPM", 800, BPMWindowY[i] +185);// 215          // print the Beats Per Minute
      text("IBI " + IBI[i] + "mS", 800, BPMWindowY[i] + 160);// 245   // print the time between heartbeats in mS

    }
}

public void mousePressed(){
  if(!serialPortFound){
    for(int i=0; i<=numPorts; i++){
      if(button[i].pressRadio(mouseX,mouseY)){
        if(i == numPorts){
          if(Serial.list().length > numPorts){
            println("New Ports Opened!");
            int diff = Serial.list().length - numPorts;	// was serialPorts.length
            serialPorts = expand(serialPorts,diff);
            //button = (Radio[]) expand(button,diff);
            numPorts = Serial.list().length;
          }else if(Serial.list().length < numPorts){
            println("Some Ports Closed!");
            numPorts = Serial.list().length;
          }else if(Serial.list().length == numPorts){
            return;
          }
          refreshPorts = true;
          return;
        }else

        try{
          port = new Serial(this, Serial.list()[i], 250000);  // make sure Arduino is talking serial at this baud rate
          delay(1000);
          println(port.read());
          port.clear();            // flush buffer
          port.bufferUntil('\n');  // set buffer full flag on receipt of carriage return
          serialPortFound = true;
        }
        catch(Exception e){
          println("Couldn't open port " + Serial.list()[i]);
          fill(255,0,0);
          textFont(font,16);
          textAlign(LEFT);
          text("Couldn't open port " + Serial.list()[i],60,70);
          textFont(font);
          textAlign(CENTER);
        }
      }
    }
  }
}

public void mouseReleased(){

}

public void keyPressed(){

 switch(key){
   case 's':    // pressing 's' or 'S' will take a jpg of the processing window
   case 'S':
     saveFrame("heartLight-####.jpg");    // take a shot of that!
     break;
   case 'r':
   case 'R':
     resetDataTraces();
     break;

   default:
     break;
 }
}


class Radio {
  int _x,_y;
  int size, dotSize;
  int baseColor, overColor, pressedColor;
  boolean over, pressed;
  int me;
  Radio[] radios;

  Radio(int xp, int yp, int s, int b, int o, int p, int m, Radio[] r) {
    _x = xp;
    _y = yp;
    size = s;
    dotSize = size - size/3;
    baseColor = b;
    overColor = o;
    pressedColor = p;
    radios = r;
    me = m;
  }

  public boolean pressRadio(float mx, float my){
    if (dist(_x, _y, mx, my) < size/2){
      pressed = true;
      for(int i=0; i<numPorts+1; i++){
        if(i != me){ radios[i].pressed = false; }
      }
      return true;
    } else {
      return false;
    }
  }

  public boolean overRadio(float mx, float my){
    if (dist(_x, _y, mx, my) < size/2){
      over = true;
      for(int i=0; i<numPorts+1; i++){
        if(i != me){ radios[i].over = false; }
      }
      return true;
    } else {
      over = false;
      return false;
    }
  }

  public void displayRadio(){
    noStroke();
    fill(baseColor);
    ellipse(_x,_y,size,size);
    if(over){
      fill(overColor);
      ellipse(_x,_y,dotSize,dotSize);
    }
    if(pressed){
      fill(pressedColor);
      ellipse(_x,_y,dotSize,dotSize);
    }
  }
}




public void serialEvent(Serial port){
try{
   String inData = port.readStringUntil('\n');
   inData = trim(inData);                 // cut off white space (carriage return)

 for(int i=0; i<numSensors;i++){
   if (inData.charAt(0) == 'a'+i){           // leading 'a' for sensor data
     inData = inData.substring(1);           // cut off the leading 'a'
     Sensor[i] = PApplet.parseInt(inData);                // convert the string to usable int
   }
   if (inData.charAt(0) == 'A'+i){           // leading 'A' for BPM data
     inData = inData.substring(1);           // cut off the leading 'A'
     BPM[i] = PApplet.parseInt(inData);                   // convert the string to usable int
     beat[i] = true;                         // set beat flag to advance heart rate graph
     heart[i] = 20;                          // begin heart image 'swell' timer
   }
 if (inData.charAt(0) == 'M'+i){             // leading 'M' means IBI data
     inData = inData.substring(1);           // cut off the leading 'M'
     IBI[i] = PApplet.parseInt(inData);                   // convert the string to usable int
   }
 }
  } catch(Exception e) {
    // print("Serial Error: ");
    // println(e.toString());
  }

}
  public void settings() {  size(900, 725); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "PulseSensorAmpd_Processing_2_Sensors" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
