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
#include "i2cmaster.h"
#include "lcd.h"

#define I2CADDRESS 0x62

int main(void)
{
    i2c_init();
	
	
	int distance(int, int, int);
	void configure(int, int);
	
	configure(0, I2CADDRESS);
	
	lcd_init();
	lcd_clear();
	lcd_gotoxy(1,1);
	printf("Distance: ");
    while (1) 
    {
		lcd_gotoxy(1, 12);
		printf("%d cm", distance(1, 1, I2CADDRESS));	
		_delay_ms(1000);
    }
}

void configure(int configuration, int LidarLiteI2cAddress){
	switch (configuration){
		case 0: //  Default configuration
		i2c_start(LidarLiteI2cAddress + I2C_WRITE);
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

int distance(int stablizePreampFlag, int takeReference, int LidarLiteI2cAddress){
	if(stablizePreampFlag){
		// Take acquisition & correlation processing with DC correction
		i2c_start(LidarLiteI2cAddress + I2C_WRITE);
		i2c_write(0x00);
		i2c_write(0x04);
		i2c_stop();
		}else{
		// Take acquisition & correlation processing without DC correction
		i2c_start(LidarLiteI2cAddress + I2C_WRITE);
		i2c_write(0x00);
		i2c_write(0x03);
		i2c_stop();
	}
	// Array to store high and low bytes of distance
	uint8_t distanceArray[2];
	// Read two bytes from register 0x8f. 
	//read(0x8f,2,distanceArray,true,LidarLiteI2cAddress);
	i2c_start(LidarLiteI2cAddress + I2C_WRITE);
	i2c_write(0x8f);
	_delay_us(10);
	i2c_start(LidarLiteI2cAddress + I2C_READ);
	distanceArray[0] = i2c_readAck(); //reads byte and request more data
	distanceArray[1] = i2c_readNak(); //reads byte with stop bit
	i2c_stop();
	// Shift high byte and add to low byte
	int distance = (uint16_t)((distanceArray[0] << 8) | distanceArray[1]);
	return distance;
}