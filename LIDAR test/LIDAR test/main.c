/*
 * LIDAR test.c
 *
 * Created: 23-09-2019 20:40:46
 * Author : jakob
 */ 

#define F_CPU 16E6

#include <avr/io.h>
#include <stdio.h>
#include <util/delay.h>
#include <avr/interrupt.h>
#include "i2cmaster.h"
#include "lcd.h"
#include "usart.h"

volatile uint16_t Capt1, Capt2;
volatile uint8_t flag = 0;
float deltaTime;

#define I2CADDRESS 0x62
#define LIDAR_WRITE_ADDRESS 0xC4
#define LIDAR_READ_ADDRESS 0xC5

int main(void)
{
    //i2c_init();
	uart_init();
	io_redirect();
	
	void init_timer(void);
	void start_timer(void);
	//int distance(int, int, unsigned char);
	void configure(int, unsigned char);
	
	DDRB = 0x02; //setting echo pin on D8 and Trigger pin on D9
	PORTB |= 0x02;
	
	init_timer();
	start_timer();
	
	
    while (1) 
    {
		
		float distance = 0;
		
		
		PORTB &= ~(1 << PORTB1);

		while(flag < 2) {
			
		}
		if(flag >= 2) {
			deltaTime = (Capt2-Capt1); //pulsewidth in microseconds
			flag = 0;
			TIMSK1 |= (1 << ICIE1) | (1 << TOIE1); // enable input capture and overflow interrupts
			TCNT1 = 0;
		
			distance = deltaTime/10;
			if(distance != 0){
			printf("distance: %.2f cm\n", distance);
			}
		}
		
		_delay_ms(1000);
    }
}

void configure(int configuration, unsigned char LidarLiteI2cAddress){
	switch (configuration){
		case 0: //  Default configuration
		i2c_start(LIDAR_WRITE_ADDRESS);//LidarLiteI2cAddress + I2C_WRITE);
		i2c_write(0x00);
		i2c_write(0x00);
		i2c_stop();
		break;
		case 1: //  Set acquisition count to 1/3 default value, faster reads, slightly
		//  noisier values
		i2c_start(LidarLiteI2cAddress + I2C_WRITE);
		i2c_write(0x04);
		i2c_write(0x00);
		i2c_stop();
		break;
		case 2: //  Low noise, low sensitivity: Pulls decision criteria higher
		//  above the noise, allows fewer false detections, reduces
		//  sensitivity
		i2c_start(LidarLiteI2cAddress + I2C_WRITE);
		i2c_write(0x1c);
		i2c_write(0x20);
		i2c_stop();
		break;
		case 3: //  High noise, high sensitivity: Pulls decision criteria into the
		//  noise, allows more false detections, increses sensitivity
		i2c_start(LidarLiteI2cAddress + I2C_WRITE);
		i2c_write(0x1c);
		i2c_write(0x60);
		i2c_stop();
		break;
	}
}

int distance(int stablizePreampFlag, int takeReference, unsigned char LidarLiteI2cAddress){
	if(stablizePreampFlag){
		// Take acquisition & correlation processing with DC correction
		i2c_start(LIDAR_WRITE_ADDRESS);//LidarLiteI2cAddress + I2C_WRITE);
		i2c_write(0x00);
		i2c_write(0x04);
		i2c_stop();
		}else{
		// Take acquisition & correlation processing without DC correction
		i2c_start(LIDAR_WRITE_ADDRESS);//LidarLiteI2cAddress + I2C_WRITE);
		i2c_write(0x00);
		i2c_write(0x03);
		i2c_stop();
	}
	// Array to store high and low bytes of distance
	uint8_t distanceArray[2];
	// Read two bytes from register 0x8f. 
	//read(0x8f,2,distanceArray,true,LidarLiteI2cAddress);
	i2c_start(LIDAR_WRITE_ADDRESS);//LidarLiteI2cAddress + I2C_WRITE);
	i2c_write(0xF);
	_delay_us(10);
	i2c_start(LIDAR_READ_ADDRESS);//LidarLiteI2cAddress + I2C_READ);
	distanceArray[0] = i2c_readAck(); //reads byte and request more data
	distanceArray[1] = i2c_readNak(); //reads byte with stop bit
	i2c_stop();
	// Shift high byte and add to low byte
	int distance = (uint16_t)((distanceArray[0] << 8) | distanceArray[1]);
	return distance;
}

void init_timer (void) {
	
	TCNT1 = 0; //initial timer value
	TCCR1B |= (1<<ICES1); //set first capture on rising edge
	TIMSK1 |= (1 << ICIE1) | (1 << TOIE1); //enable input capture and overflow interrupts
}

void start_timer (void) {
	TCCR1B |= (1 << CS11); // start timer with prescalar 8
	sei(); //enable global interrupts
}

ISR(TIMER1_CAPT_vect) {
	if(flag == 0) {
		Capt1 = ICR1; //Save first time stamp
		TCCR1B &= ~(1 << ICES1); //Change capture to falling edge
	}
	if(flag == 1) {
		Capt2 = ICR1; //Save second time stamp
		TCCR1B |= (1 << ICES1); //Change capture to rising edge
	}
	flag++;
}