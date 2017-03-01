
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
    case SERIAL_PLOTTER1:  // open the Arduino Serial Plotter to visualize only Signal and BMP from PulseSensors
      for(int i=0; i<numPulseSensors; i++){
      	Serial.print(BPM[i]);
      	Serial.print(",");
//      	Serial.print(IBI[i]);
//      	Serial.print(",");
      	Serial.print(Signal[i]);
	      if(i!=numPulseSensors-1){ Serial.print(","); }
      }

       case SERIAL_PLOTTER2:  // open the Arduino Serial Plotter to visualize all Signal, BMP, and IBI from PulseSensors
      for(int i=0; i<numPulseSensors; i++){
        Serial.print(BPM[i]);
        Serial.print(",");
        Serial.print(IBI[i]);
        Serial.print(",");
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
      case PROCESSING_VISUALIZER:    //
	      c = 'A'+ char(p);
        sendDataToSerial(c,BPM[p]);   // send heart rate with a 'B' prefix
      	c = 'M'+ char(p);
        sendDataToSerial(c,IBI[p]);   // send time between beats with a 'Q' prefix
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



