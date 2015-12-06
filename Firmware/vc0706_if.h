//*****************************************************************************
//
// vc0706_if.h
//
// API for using the VC0706 Serial Camera Module.
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
extern void CameraInit(unsigned short usSerialNum);


//*****************************************************************************
// Mark the end of the C bindings section for C++ compilers.
//*****************************************************************************
#ifdef __cplusplus
}
#endif

#endif /* _VC0706_IF_H_ */
