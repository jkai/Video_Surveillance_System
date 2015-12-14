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
// December 5, 2015
//
//*****************************************************************************

#ifndef _VC0706_IF_H_
#define _VC0706_IF_H_

#include "vc0706.h"

//*****************************************************************************
// If building with a C++ compiler, make all of the definitions in this header
// have a C binding.
//*****************************************************************************
#ifdef __cplusplus
extern "C"
{
#endif


//*****************************************************************************
// Function Prototypes
//*****************************************************************************
extern char CameraInit(unsigned char ucSerialNum, unsigned short usBaudRate,
                       unsigned char ucImageSize);
extern char *CameraSnapshot();


//*****************************************************************************
// Mark the end of the C bindings section for C++ compilers.
//*****************************************************************************
#ifdef __cplusplus
}
#endif

#endif /* _VC0706_IF_H_ */
