#ifndef HX711_DRIVER_H
#define HX711_DRIVER_H
#include "main.h"

// Define GPIO Pins and Ports
#define HX711_CLK_Pin GPIO_PIN_6  // Replace with your pin number
#define HX711_CLK_GPIO_Port GPIOA // Replace with your GPIO port
#define HX711_DATA_Pin GPIO_PIN_7 // Replace with your pin number
#define HX711_DATA_GPIO_Port GPIOA // Replace with your GPIO port

// HX711 Constants
#define HX711_GAIN_128 25 // Gain 128, Channel A
#define HX711_GAIN_64  26 // Gain 64, Channel A
#define HX711_GAIN_32  27 // Gain 32, Channel B

// Function Prototypes
/**
 * @brief Initializes the GPIO pins for the HX711.
 */
void HX711_Init(void);

/**
 * @brief Reads a 24-bit signed value from the HX711.
 * @return The raw 24-bit value from the HX711.
 */
int32_t HX711_Read(void);

int32_t HX711_Calibrate(void);

/**
 * @brief Sets the gain and input channel for the HX711.
 * @param gain The desired gain and channel (HX711_GAIN_128, HX711_GAIN_64, HX711_GAIN_32).
 */
void HX711_SetGain(uint8_t gain);

/**
 * @brief Checks if the HX711 is ready to send data.
 * @return 1 if ready (DATA pin is LOW), 0 otherwise.
 */
uint8_t HX711_IsReady(void);

#endif // HX711_DRIVER_H
