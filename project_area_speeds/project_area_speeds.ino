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

#define RED_SPEED 50
#define GREEN_SPEED 2000
#define BLUE_SPEED 200

#define CMD_NORMAL 1
#define CMD_TURN_L 2
#define CMD_TURN_R 3
#define CMD_MODE_COLOR 4
#define CMD_MODE_LINE 5

#define MODE_COLOR 1
#define MODE_LINE 2

#define X_CENTER (pixy.frameWidth/2)

volatile int left_motor = 0, throttle_left_motor, throttle_counter_left_motor, throttle_left_motor_memory;
volatile int right_motor = 0, throttle_right_motor, throttle_counter_right_motor, throttle_right_motor_memory;

int8_t res;

float previous_error = 0, error = 0, int_error = 0, output = 0;

//Parameters to tweak
float Kp = 2500.0, Ki = 800.0, Kd = 1000.0;
float param = 44.4444;
//Parameters to tweak

int error_num = 0;

byte left_motor_step_pin = 0b00000100;
byte left_motor_dir_pin = 0b00001000;
byte right_motor_step_pin = 0b00010000;
byte right_motor_dir_pin = 0b00100000;
byte microstep_select = 0b00000010; //original 0b00010010;

int cmd = CMD_NORMAL;
int MODE = MODE_LINE;

Pixy2 pixy;

void setup()
{
  Serial.begin(115200);
  Serial.print("Starting...\n");

  Wire.begin(0x8); //slave address 0x8
  Wire.onReceive(receiveEvent);

  pixy.init();
  pin_setup();
  interrupt_setup();
}

void loop()
{
  if(MODE == MODE_LINE) {
    pixy.changeProg("line");
    //res = pixy.line.getMainFeatures();
    //throttle_left_motor = RED_SPEED;
    //throttle_right_motor = RED_SPEED;
    while(MODE == MODE_LINE) {
      res = pixy.line.getMainFeatures();
      if (res&LINE_VECTOR){                                                      // We found the vector
        error_num = 0;
        // Calculate heading error with respect to m_x1, which is the far-end of the vector, the part of the vector we're heading toward.
        error = (float)pixy.line.vectors->m_x1 - (float)X_CENTER;               //if >0 vector endpoint is to the right --> left motor should turn faster
        pid_calculations();
        motor_pulse_calculations();
      }
    }
  } else if(MODE == MODE_COLOR) {
    pixy.changeProg("video");
    while(MODE == MODE_COLOR) {
      color_sense_speed_adjust();
    }
  }
}

void pin_setup(){
  DDRD |= left_motor_step_pin;
  DDRD |= left_motor_dir_pin;
  DDRD |= right_motor_step_pin;
  DDRD |= right_motor_dir_pin;
  DDRB |= 0b00000111;
  DDRC |= 0b00000111;
  PORTC |= microstep_select;
  PORTB |= microstep_select;
  }

void interrupt_setup(){
  //Setting up the timer
  TCCR2A = 0;                                                               //Make sure that the TCCR2A register is set to zero
  TCCR2B = 0;                                                               //Make sure that the TCCR2A register is set to zero
  TIMSK2 |= (1 << OCIE2A);                                                  //Set the interupt enable bit OCIE2A in the TIMSK2 register
  TCCR2B |= (1 << CS21);                                                    //Set the CS21 bit in the TCCRB register to set the prescaler to 8
  OCR2A = 39;                                                               //The compare register is set to 39 => 20us / (1s / (16.000.000MHz / 8)) - 1
  TCCR2A |= (1 << WGM21);                                                   //Set counter 2 to CTC (clear timer on compare) mode
  }

void receiveEvent(int angle) {
  while(Wire.available()) {
    int i = Wire.read();
    if(i != 0) {
      switch(i) {
        case CMD_NORMAL:
          cmd = CMD_NORMAL;
          throttle_left_motor = RED_SPEED;
          throttle_right_motor = RED_SPEED;
          break;
        case CMD_TURN_L:
          throttle_left_motor = BLUE_SPEED;
          throttle_right_motor = RED_SPEED;
          cmd = CMD_TURN_L;
          break;
        case CMD_TURN_R:
          cmd = CMD_TURN_R;
          throttle_left_motor = RED_SPEED;
          throttle_right_motor = BLUE_SPEED;
          break;
        case CMD_MODE_LINE:
          MODE = 2;
          break;
        case CMD_MODE_COLOR:
          MODE = 1;
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

void color_sense_speed_adjust(){
  uint8_t r, g, b; 

  pixy.video.getRGB(pixy.frameWidth/2, pixy.frameHeight/2, &r, &g, &b);

  if (r > RED_RED & g < RED_GREEN & b < RED_BLUE){
    throttle_left_motor = -RED_SPEED;
    throttle_right_motor = RED_SPEED;
  } else if (r < GREEN_RED & g > GREEN_GREEN & b < GREEN_BLUE){
    throttle_left_motor = GREEN_SPEED;
    throttle_right_motor = GREEN_SPEED;    
  } else if (r < BLUE_RED & g < BLUE_GREEN & b > BLUE_BLUE) {
    throttle_left_motor = BLUE_SPEED;
    throttle_right_motor = -BLUE_SPEED;
  }
}

void pid_calculations(){
  //Calculating the PID values to feed into the motors
  if(output > 10 || output < -10)error += output * 0.015;

  int_error += Ki * error;                                                  //Calculate the I-controller value and add it to the int_error variable
  if(int_error > 400)int_error = 400;                                       //Limit the I-controller to the maximum controller output
  else if(int_error < -400)int_error = -400;
  output = Kp * error + int_error + Kd * (error - previous_error);
  if(output > 400)output = 400;                                             //Limit the PI-controller to the maximum controller output
  else if(output < -400)output = -400;

  previous_error = error;                                                   //Store the error for the next loop
}

void motor_pulse_calculations(){
  //Working around the nonlinearity of the stepper motor control
  //FOR WOLFRAM ALPHA: 10 =0.45/(x3+x1/x2);100= 0.45/(x3+x1/(400+x2));55 = 0.45/(x3+x1/(200+x2));solve for x1, x2, x3
  
  if(output > 0)      left_motor = int((2.0*50000.0/(output + param))),     right_motor = int((2.0*50000.0/(param)));
  else if(output < 0) right_motor = int((2.0*50000.0/(-output + param))),   left_motor = int((2.0*50000.0/(param)));

  throttle_left_motor = -left_motor;                                         //Copy the pulse time to the throttle variables so the interrupt subroutine can use them
  throttle_right_motor = -right_motor;
}
