#include <Pixy2.h>

Pixy2 pixy;

#define period 4000
#define X_CENTER (pixy.frameWidth/2)

volatile int left_motor = 0, throttle_left_motor, throttle_counter_left_motor, throttle_left_motor_memory;
volatile int right_motor = 0, throttle_right_motor, throttle_counter_right_motor, throttle_right_motor_memory;

int error_num = 0;

byte left_motor_step_pin = 0b00000100;
byte left_motor_dir_pin = 0b00001000;
byte right_motor_step_pin = 0b00010000;
byte right_motor_dir_pin = 0b00100000;
byte microstep_select = 0b00010010;

int8_t res;

float previous_error = 0, error = 0, int_error = 0, output = 0;

//Parameters to tweak
float Kp = 2500.0, Ki = 800.0, Kd = 1000.0;
float param = 44.4444;
//Parameters to tweak

unsigned long loop_timer = 0;

void setup() {
  pixy.init();
  pixy_line_follow_initialization();
  pin_setup();
  interrupt_setup();

  Serial.begin(9600);
  
  loop_timer = micros() + period;
}

void loop() {

  res = pixy.line.getMainFeatures();
  
  /*
  if (res<=0)                                                               // If error or nothing detected, stop everything
  {
    error_num++;
    if (error_num > 10)
    {
      noInterrupts();
      while(1);  
    }
  
  }
  */
  
  if (res&LINE_VECTOR)                                                      // We found the vector
  {
    error_num = 0;
    // Calculate heading error with respect to m_x1, which is the far-end of the vector, the part of the vector we're heading toward.
    error = (float)pixy.line.vectors->m_x1 - (float)X_CENTER;               //if >0 vector endpoint is to the right --> left motor should turn faster
    pid_calculations();
    motor_pulse_calculations();
  }

  //Serial.println(output);
  Serial.println(throttle_right_motor);
  Serial.println();
  
  while(loop_timer > micros());
  loop_timer += period;
  
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

void pin_setup(){
  DDRD |= left_motor_step_pin;
  DDRD |= left_motor_dir_pin;
  DDRD |= right_motor_step_pin;
  DDRD |= right_motor_dir_pin;
  DDRB |= 0b00111111;
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

void pixy_line_follow_initialization(){
  pixy.changeProg("line");
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
  
  if(output > 0)      right_motor = int((2.0*50000.0/(output + param))),     left_motor = int((2.0*50000.0/(param)));
  else if(output < 0) left_motor = int((2.0*50000.0/(-output + param))),   right_motor = int((2.0*50000.0/(param)));

  throttle_left_motor = left_motor;                                         //Copy the pulse time to the throttle variables so the interrupt subroutine can use them
  throttle_right_motor = right_motor;
  }
