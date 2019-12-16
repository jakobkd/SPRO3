#include <Pixy2.h>
#include <stdio.h>
#include <Wire.h>

#define RED_RED 200
#define RED_GREEN 50
#define RED_BLUE 50
#define GREEN_RED 50
#define GREEN_GREEN 200
#define GREEN_BLUE 50
#define BLUE_RED 50
#define BLUE_GREEN 50
#define BLUE_BLUE 200

#define RED_SPEED 3000
#define GREEN_SPEED 2000
#define BLUE_SPEED 4000

#define CMD_NORMAL 1
#define CMD_TURN_L 2
#define CMD_TURN_R 3

volatile int left_motor = 0, throttle_left_motor, throttle_counter_left_motor, throttle_left_motor_memory;
volatile int right_motor = 0, throttle_right_motor, throttle_counter_right_motor, throttle_right_motor_memory;

int error_num = 0;

byte left_motor_step_pin = 0b00000100;
byte left_motor_dir_pin = 0b00001000;
byte right_motor_step_pin = 0b00010000;
byte right_motor_dir_pin = 0b00100000;
byte microstep_select = 0b00010010;

int cmd = CMD_NORMAL;

Pixy2 pixy;

void setup()
{
  Serial.begin(115200);
  Serial.print("Starting...\n");

  Wire.begin(0x8); //slave address 0x8
  Wire.onReceive(receiveEvent);

  pixy.init();
  pixy.changeProg("video");
}

void loop()
{
  color_sense_speed_adjust();
}

void receiveEvent(int angle) {
  while(Wire.available()) {
    int i = Wire.read();
    if(i != 0) {
      switch(i) {
        case CMD_NORMAL:
          cmd = CMD_NORMAL;
          break;
        case CMD_TURN_L:
          cmd = CMD_TURN_L;
          break;
        case CMD_TURN_R:
          cmd = CMD_TURN_R;
          break;
      }
      
    }
  }
}

ISR(TIMER2_COMPA_vect){
  //Will execute this piece of code every 20us
  //Left motor pulse calculations
  throttle_counter_left_motor ++;                                           //Increase the throttle_counter_left_motor variable by 1 every time this routine is executed
  if(throttle_counter_left_motor > throttle_left_motor_memory){             //If the number of loops is larger then the throttle_left_motor_memory variable
    throttle_counter_left_motor = 0;                                        //Reset the throttle_counter_left_motor variable
    throttle_left_motor_memory = throttle_left_motor;                       //Load the next throttle_left_motor variable
    if(throttle_left_motor_memory < 0){                                     //If the throttle_left_motor_memory is negative
      PORTD &= ~left_motor_dir_pin;                                         //Set output 3 low to reverse the direction of the stepper controller
      throttle_left_motor_memory *= -1;                                     //Invert the throttle_left_motor_memory variable
    }
    else PORTD |= left_motor_dir_pin;                                       //Set output 3 high for a forward direction of the stepper motor
  }
  else if(throttle_counter_left_motor == 1)PORTD |= left_motor_step_pin;    //Set output 2 high to create a pulse for the stepper controller
  else if(throttle_counter_left_motor == 2)PORTD &= ~left_motor_step_pin;   //Set output 2 low because the pulse only has to last for 20us 

  //Right motor pulse calculations
  throttle_counter_right_motor ++;                                          //Increase the throttle_counter_right_motor variable by 1 every time the routine is executed
  if(throttle_counter_right_motor > throttle_right_motor_memory){           //If the number of loops is larger then the throttle_right_motor_memory variable
    throttle_counter_right_motor = 0;                                       //Reset the throttle_counter_right_motor variable
    throttle_right_motor_memory = throttle_right_motor;                     //Load the next throttle_right_motor variable
    if(throttle_right_motor_memory < 0){                                    //If the throttle_right_motor_memory is negative
      PORTD |= right_motor_dir_pin;                                         //Set output 5 low to reverse the direction of the stepper controller
      throttle_right_motor_memory *= -1;                                    //Invert the throttle_right_motor_memory variable
    }
    else PORTD &= ~right_motor_dir_pin;                                     //Set output 5 high for a forward direction of the stepper motor
  }
  else if(throttle_counter_right_motor == 1)PORTD |= right_motor_step_pin;  //Set output 4 high to create a pulse for the stepper controller
  else if(throttle_counter_right_motor == 2)PORTD &= ~right_motor_step_pin; //Set output 4 low because the pulse only has to last for 20us
}

void color_sense_speed_adjust()
  {
  uint8_t r, g, b; 

  pixy.video.getRGB(pixy.frameWidth/2, pixy.frameHeight/2, &r, &g, &b);

  if (r > RED_RED & g < RED_GREEN & b < RED_BLUE)
  {
    throttle_left_motor = RED_SPEED;
    throttle_right_motor = RED_SPEED;
  }
  else if (r < GREEN_RED & g > GREEN_GREEN & b < GREEN_BLUE)
  {
    throttle_left_motor = GREEN_SPEED;
    throttle_right_motor = GREEN_SPEED;    
  }
  else if (r < BLUE_RED & g < BLUE_GREEN & b > BLUE_BLUE)
  {
    throttle_left_motor = BLUE_SPEED;
    throttle_right_motor = BLUE_SPEED;
  }
  }
