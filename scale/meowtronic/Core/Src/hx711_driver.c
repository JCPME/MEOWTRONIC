#include "hx711_driver.h"
#include "stdio.h"

void HX711_Init(void) {
    GPIO_InitTypeDef GPIO_InitStruct = {0};

    // Initialize CLOCK pin as output
    GPIO_InitStruct.Pin = HX711_CLK_Pin;
    GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
    GPIO_InitStruct.Pull = GPIO_NOPULL;
    GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
    HAL_GPIO_Init(HX711_CLK_GPIO_Port, &GPIO_InitStruct);

    // Initialize DATA pin as input with pull-up
    GPIO_InitStruct.Pin = HX711_DATA_Pin;
    GPIO_InitStruct.Mode = GPIO_MODE_INPUT;
    GPIO_InitStruct.Pull = GPIO_PULLUP;
    HAL_GPIO_Init(HX711_DATA_GPIO_Port, &GPIO_InitStruct);

    // Set CLOCK pin to LOW initially
    //HAL_GPIO_WritePin(HX711_CLK_GPIO_Port, HX711_CLK_Pin, GPIO_PIN_RESET);
}

uint8_t HX711_IsReady(void) {
	printf("HX711 is ready\n");
    return (HAL_GPIO_ReadPin(HX711_DATA_GPIO_Port, HX711_DATA_Pin) == GPIO_PIN_RESET);
}

int32_t HX711_Calibrate(void){
	return HX711_Read();
}

int32_t HX711_Read(void) {
    int32_t value = 0;

    // Wait for DATA line to go LOW
    while (!HX711_IsReady());
    printf("reading data\n");

    // Read 24 bits of data
    for (int i = 0; i < 24; i++) {
        HAL_GPIO_WritePin(HX711_CLK_GPIO_Port, HX711_CLK_Pin, GPIO_PIN_SET);

        value = (value << 1) | HAL_GPIO_ReadPin(HX711_DATA_GPIO_Port, HX711_DATA_Pin);
        HAL_GPIO_WritePin(HX711_CLK_GPIO_Port, HX711_CLK_Pin, GPIO_PIN_RESET);
    }

    // Pulse once more for gain/channel setting
    HAL_GPIO_WritePin(HX711_CLK_GPIO_Port, HX711_CLK_Pin, GPIO_PIN_SET);
    HAL_GPIO_WritePin(HX711_CLK_GPIO_Port, HX711_CLK_Pin, GPIO_PIN_RESET);

    // Convert 24-bit two's complement to 32-bit signed integer
    if (value & 0x800000) {
        value |= 0xFF000000; // Sign extend if negative
    }

    return value;
}

void HX711_SetGain(uint8_t gain) {
    // Send additional clock pulses to set gain
    for (int i = 0; i < gain; i++) {
        HAL_GPIO_WritePin(HX711_CLK_GPIO_Port, HX711_CLK_Pin, GPIO_PIN_SET);
        HAL_GPIO_WritePin(HX711_CLK_GPIO_Port, HX711_CLK_Pin, GPIO_PIN_RESET);
    }
}
