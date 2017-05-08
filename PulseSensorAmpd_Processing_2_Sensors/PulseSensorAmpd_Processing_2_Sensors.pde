/*
DISPLAYS MULTIPLE PULSE SENSOR DATA STREAMS
THIS PROGRAM WORKS WITH PulseSensorAmped_n_Sensors ARDUINO CODE

PRESS 'S' OR 's' KEY TO SAVE A PICTURE OF THE SCREEN IN SKETCH FOLDER (.jpg)
PRESS 'R' OR 'r' KEY TO RESET THE DATA TRACES
MADE BY JOEL MURPHY WINTER 2016, MODIFIED WINTER 2017
UPDATED BY JOEL MURPHY SUMMER 2016 WITH SERIAL PORT LOCATOR TOOL
UPDATED BY JOEL MURPHY WINTER 2017 WITH IMPROVED SERIAL PORT SELECTOR TOOL

THIS CODE PROVIDED AS IS, WITH NO CLAIMS OF FUNCTIONALITY OR EVEN IF IT WILL WORK
      WYSIWYG
*/


import processing.serial.*;
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
color eggshell = color(255, 253, 248);
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


void setup() {
  size(900, 725);  // Stage size
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

void draw() {
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


void drawDataWindows(){
  noStroke();
  // DRAW OUT THE PULSE WINDOW AND BPM WINDOW RECTANGLES
  fill(eggshell);  // color for the window background
  for(int i=0; i<numSensors; i++){
    rect(PulseWindowX, PulseWindowY[i], PulseWindowWidth, PulseWindowHeight);
    rect(BPMWindowX, BPMWindowY[i], BPMWindowWidth, BPMWindowHeight);
  }
}

void drawPulseWaveform(){
  // DRAW THE PULSE WAVEFORM
  // prepare pulse data points
  for (int i=0; i<numSensors; i++) {
    RawPPG[i][PulseWindowWidth-1] = (1023 - Sensor[i]);   // place the new raw datapoint at the end of the array

    for (int j = 0; j < PulseWindowWidth-1; j++) {      // move the pulse waveform by
      RawPPG[i][j] = RawPPG[i][j+1];                         // shifting all raw datapoints one pixel left
      float dummy = RawPPG[i][j] * 0.625/numSensors;       // adjust the raw data to the selected scale
      offset = float(PulseWindowY[i]);                // calculate the offset needed at this window
      ScaledPPG[i][j] = int(dummy) + int(offset);   // transfer the raw data array to the scaled array
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

void drawBPMwaveform(){
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
    ScaledBPM[i][BPMWindowWidth-1] = int(dummy);       // set the rightmost pixel to the new data point value
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
void drawHeart(){
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



void listAvailablePorts(){
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

void autoScanPorts(){
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

void resetDataTraces(){
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

void printDataToScreen(){ // PRINT THE DATA AND VARIABLE VALUES
    fill(eggshell);                                       // get ready to print text
    text("Pulse Sensor Amped 2 Sensor Visualizer", 245, 30);     // tell them what you are
    for (int i=0; i<numSensors; i++) {
      text("Sensor # " + (i+1), 800, BPMWindowY[i] + 220);
      text(BPM[i] + " BPM", 800, BPMWindowY[i] +185);// 215          // print the Beats Per Minute
      text("IBI " + IBI[i] + "mS", 800, BPMWindowY[i] + 160);// 245   // print the time between heartbeats in mS

    }
}
