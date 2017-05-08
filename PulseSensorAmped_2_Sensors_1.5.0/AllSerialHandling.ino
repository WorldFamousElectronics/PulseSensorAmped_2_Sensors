
//////////
/////////  All Serial Handling Code,
/////////  It's Changeable with the 'outputType' variable
/////////  It's declared at start of code.
/////////

void serialOutput(){   // Decide How To Output Serial.
  switch(outputType){
    case PROCESSING_VISUALIZER:
      for(int i=0; i<numPulseSensors; i++){
        sendDataToSerial('a'+i, Signal[i]);
      }
      break;
    case SERIAL_PLOTTER:  // open the Arduino Serial Plotter to visualize these data
      for(int i=0; i<numPulseSensors; i++){
//      	Serial.print(BPM[i]);     //
//      	Serial.print(",");        //  UNCOMMENT THESE LINES TO SEE BPM AND IBI IN THE ARDUINO SERIAL PLOTTER
//      	Serial.print(IBI[i]);     //
//      	Serial.print(",");        //
      	Serial.print(Signal[i]);
	      if(i!=numPulseSensors-1){ Serial.print(","); }
      }
      Serial.println();
      break;
    default:
      break;
  }

}



void serialOutputWhenBeatHappens(int p){
	char c;
  switch(outputType){
      case PROCESSING_VISUALIZER:    
	      c = 'A'+ p;                   // send first sensor BPM with prefix 'A'
        sendDataToSerial(c,BPM[p]);   // send successive sensor BPM with prefix 'B', 'C', etc
      	c = 'M'+ p;                   // send first sensor IBI with prefix 'M'
        sendDataToSerial(c,IBI[p]);   // send successive sensor IBI with prefix 'N', 'O', etc 
      break;
    default:
      break;
  }
}


//  Sends Data to Pulse Sensor Processing App, Native Mac App, or Third-party Serial Readers.
void sendDataToSerial(char symbol, int data ){
    Serial.print(symbol);
    Serial.println(data);
  }



