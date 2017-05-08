



void serialEvent(Serial port){
try{
   String inData = port.readStringUntil('\n');
   inData = trim(inData);                 // cut off white space (carriage return)

 for(int i=0; i<numSensors;i++){
   if (inData.charAt(0) == 'a'+i){           // leading 'a' for sensor data
     inData = inData.substring(1);           // cut off the leading 'a'
     Sensor[i] = int(inData);                // convert the string to usable int
   }
   if (inData.charAt(0) == 'A'+i){           // leading 'A' for BPM data
     inData = inData.substring(1);           // cut off the leading 'A'
     BPM[i] = int(inData);                   // convert the string to usable int
     beat[i] = true;                         // set beat flag to advance heart rate graph
     heart[i] = 20;                          // begin heart image 'swell' timer
   }
 if (inData.charAt(0) == 'M'+i){             // leading 'M' means IBI data
     inData = inData.substring(1);           // cut off the leading 'M'
     IBI[i] = int(inData);                   // convert the string to usable int
   }
 }
  } catch(Exception e) {
    // print("Serial Error: ");
    // println(e.toString());
  }

}
