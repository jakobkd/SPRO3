#include <Wire.h>

void setup() {
  Serial.begin(9600);
  Wire.begin(0x8); //slave address 0x8

  Wire.onReceive(receiveEvent);
}

void receiveEvent(int angle) {
  while(Wire.available()) {
    int i = Wire.read();
    if(i != 0) {
      Serial.println(i);
    }
  }
}

void loop() {
  // put your main code here, to run repeatedly:
  delay(100);
}
