/*
 * Ultrasonic sensor.c
 *
 * Created: 22-02-2019 14:26:05
 * Author : jakob
 */ 

#define F_CPU 16E6

#include <stdio.h>
#include <avr/io.h>
#include <avr/interrupt.h>
#include <util/delay.h>
#include <stdio.h>
#include "usart.h"

volatile uint16_t Capt1, Capt2;
volatile uint8_t flag = 0;
float deltaTime;

int main(void)
{
	uart_init(); // open the communication to the microcontroller 
	io_redirect(); // redirect input and output to the uart

	void init_timer(void);
	void start_timer(void);
	uint16_t adc_read(uint8_t);
	void read_IR(void);
	void read_US(void);

	ADMUX |= (1 << REFS0); // select AVcc as ref
	ADCSRA |= (1 << ADPS0) | (1 << ADPS0) | (1 << ADPS0) | (1 << ADEN); //Set prescaler to 128 and turn on ADC
	
    DDRB = 0x02; //setting echo pin on D8 and Trigger pin on D9
	PORTB |= (1 << PORTB0); //pull-up input

	PORTB &= ~(1 << PORTB1);
	init_timer();
	start_timer();
    
	while (1) 
    {
		
		read_US();
		
		_delay_ms(1000);
    }
}

void read_US() {
	float distance = 0;
	
	PORTB |= (1 << PORTB1);
	_delay_us(10);
	PORTB &= ~(1 << PORTB1);

	while(flag < 2) {
		
	}
	if(flag >= 2) {
		deltaTime = (Capt2-Capt1)*0.5; //pulsewidth in microseconds
		flag = 0;
		TIMSK1 |= (1 << ICIE1) | (1 << TOIE1); // enable input capture and overflow interrupts
		TCNT1 = 0;

		distance = (deltaTime*0.0343)/2;
		printf("%.2f cm\n", distance);
	}
}

void read_IR() {
	uint16_t adc_read(uint8_t);
	
	uint16_t raw_input = adc_read(0);
	printf("%d\n", raw_input);
	_delay_ms(1000);
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

uint16_t adc_read(uint8_t adc_channel) {
	ADMUX &= 0xF0; //clear previous selected channel
	ADMUX |= adc_channel; //Select channel
	ADCSRA |= (1 << ADSC); // start conversion

	while( (ADCSRA & (1 << ADSC))); //Wait for conversion to be complete

	return ADC;
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



