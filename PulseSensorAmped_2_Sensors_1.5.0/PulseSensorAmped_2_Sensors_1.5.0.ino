
/*  Pulse Sensor Multi Sensor

     by
     Joel Murphy and Yury Gitman   http://www.pulsesensor.com *   Updated Winter 2017

     World Famous Electronics llc.  Public Domain. 2017


  ----------------------  Notes ----------------------  ----------------------
  This code:
  1) Blinks an LED to two user's Live Heartbeat   PIN 13 and PIN 12
  2) Fades an LED to two user's Live HeartBeat    PIN 5 and PIN 9
  3) Determines BPMs for both users
  4) Prints All of the Above to Arduino Serial Plotter or our Processing Visualizer

  Read Me:
  https://github.com/WorldFamousElectronics/PulseSensor_Amped_Arduino/blob/master/README.md
  ----------------------       ----------------------  ----------------------
*/

#define PROCESSING_VISUALIZER 1
#define SERIAL_PLOTTER  2

//  Variables
const int numPulseSensors = 2;
const int threshold = 530;
int pulsePin[numPulseSensors];                 // Pulse Sensor purple wire connected to analog pin 0
int blinkPin[numPulseSensors];                // pin to blink led at each beat
int fadePin[numPulseSensors];                  // pin to do fancy classy fading blink at each beat
int fadeRate[numPulseSensors];                 // used to fade LED on with PWM on fadePin




// Volatile Variables, used in the interrupt service routine!
volatile int BPM[numPulseSensors];                   // int that holds raw Analog in 0. updated every 2mS
volatile int Signal[numPulseSensors];                // holds the incoming raw data
volatile int IBI[numPulseSensors];             // int that holds the time interval between beats! Must be seeded!
volatile boolean Pulse[numPulseSensors];     // "True" when User's live heartbeat is detected. "False" when not a "live beat".
volatile boolean QS[numPulseSensors];        // becomes true when Arduoino finds a beat.

// Regards Serial OutPut  -- Set This Up to your needs
static boolean serialVisual = false;   // Set to 'false' by Default.  Re-set to 'true' to see Arduino Serial Monitor ASCII Visual Pulse

volatile int rate[numPulseSensors][10];                    // array to hold last ten IBI values
volatile unsigned long sampleCounter = 0;          // used to determine pulse timing
volatile unsigned long lastBeatTime[numPulseSensors];           // used to find IBI
volatile int P[numPulseSensors];                      // used to find peak in pulse wave, seeded
volatile int T[numPulseSensors];                     // used to find trough in pulse wave, seeded
volatile int thresh[numPulseSensors];                // used to find instant moment of heart beat, seeded
volatile int amp[numPulseSensors];                   // used to hold amplitude of pulse waveform, seeded
volatile boolean firstBeat[numPulseSensors];        // used to seed rate array so we startup with reasonable BPM
volatile boolean secondBeat[numPulseSensors];      // used to seed rate array so we startup with reasonable BPM

// SET THE SERIAL OUTPUT TYPE TO YOUR NEEDS
// PROCESSING_VISUALIZER works with Pulse Sensor Processing Visualizer
//      https://github.com/WorldFamousElectronics/PulseSensor_Amped_Processing_Visualizer
// SERIAL_PLOTTER outputs sensor data for viewing with the Arduino Serial Plotter
//      run the Arduino Serial Plotter at 115200 baud: Tools/Serial Plotter or Command+L
static int outputType = SERIAL_PLOTTER;

void setup() {

  setStuph();                       // initialize variables and pins

  Serial.begin(250000);             // we agree to talk fast!

  interruptSetup();                 // sets up to read Pulse Sensor signal every 2mS
}


void loop() {

  serialOutput() ;

  for (int i = 0; i < numPulseSensors; i++) {
    if (QS[i] == true) {
      fadeRate[i] = 0;         // Makes the LED Fade Effect Happen
      // Set 'fadeRate' Variable to 255 to fade LED with pulse
      serialOutputWhenBeatHappens(i);   // A Beat Happened, Output that to serial.
      QS[i] = false;
    }
  }

  ledFadeToBeat();                      // Makes the LED Fade Effect Happen
  delay(20);                            //  take a break
}




// FADE BOTH LEDS
void ledFadeToBeat() {
  for (int j = 0; j < numPulseSensors; j++) {
    fadeRate[j] += 15;
    fadeRate[j] = constrain(fadeRate[j], 0, 255); //  keep RGB LED fade value from going into negative numbers!
    analogWrite(fadePin[j], fadeRate[j]);         //  fade LED
  }

}


// INITIALIZE VARIABLES AND INPUT/OUTPUT PINS
void setStuph() {

  // aux power to Pin 8, for PulseSensor +5V Power
  pinMode(8, OUTPUT);
  digitalWrite(8, HIGH);
  pinMode(7, OUTPUT);
  digitalWrite(7, HIGH);

  //    Possible Power for 3'rd PulseSensor on Pin 7, un-comment
  //     pinMode(7,OUTPUT);
  //    digitalWrite(7, HIGH);


  for (int i = 0; i < numPulseSensors; i++) {
    lastBeatTime[i] = 0;
    P[i] = T[i] = 512;
    amp[i] = 0;
    thresh[i] = threshold;
    amp[i] = 0;               // used to hold amplitude of pulse waveform, seeded
    firstBeat[i] = true;      // used to seed rate array so we startup with reasonable BPM
    secondBeat[i] = false;
    IBI[i] = 600;             // int that holds the time interval between beats! Must be seeded!
    Pulse[i] = false;         // "True" when User's live heartbeat is detected. "False" when not a "live beat".
    QS[i] = false;
    switch (i) {
      case  0:
        pulsePin[i] = 0;    // pulse pin 0
        blinkPin[i] = 13;   // blink output for pulse 0
        fadePin[i] = 5;     // fade output for pulse 0
        break;
      case  1:
        pulsePin[i] = 1;    // pulse pin 1
        blinkPin[i] = 12;   // blink output for pulse 1
        fadePin[i] = 9;     // fade output for pulse 1
        break;
      // add more if you need to here
      default:
        break;
    }

    pinMode(blinkPin[i], OUTPUT);        // pin that will blink to your heartbeat!
    digitalWrite(blinkPin[i], LOW);
    pinMode(fadePin[i], OUTPUT);         // pin that will fade to your heartbeat!
    analogWrite(fadePin[i], 255);

  }
}
