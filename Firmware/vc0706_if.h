//*****************************************************************************
//
// vc0706_if.h
//
// API for using the VC0706 Serial Camera Module.

// A partial port of Adafruit-VC0706-Serial-Camera-Library found here:
//  https://github.com/adafruit/Adafruit-VC0706-Serial-Camera-Library
//
// Created:
// December 5, 2015
//
// Modified:
// December 19, 2015
//
//*****************************************************************************

#ifndef _VC0706_IF_H_
#define _VC0706_IF_H_


//*****************************************************************************
// If building with a C++ compiler, make all of the definitions in this header
// have a C binding.
//*****************************************************************************
#ifdef __cplusplus
extern "C"
{
#endif


//*****************************************************************************
// Defines
//*****************************************************************************
#define CAMERA_DEFAULT_SERIAL_NUM           0
#define CAMERA_DEFAULT_BAUD_RATE            VC0706_INTERFACE_UART_BAUD_38400
#define CAMERA_DEFAULT_IMAGE_SIZE           VC0706_IMAGE_SIZE_160_120


//*****************************************************************************
// Function Prototypes
//*****************************************************************************
extern tBoolean CameraInit(unsigned char ucSerialNum, unsigned short usBaudRate,
                           unsigned char ucImageSize);
extern unsigned char *CameraSnapshot(unsigned int *uiFrameLen);


//*****************************************************************************
// Mark the end of the C bindings section for C++ compilers.
//*****************************************************************************
#ifdef __cplusplus
}
#endif

#endif /* _VC0706_IF_H_ */
