//*****************************************************************************
//
// vc0706_if.c
//
// Implementation of API for using the VC0706 Serial Camera Module.
//
// Created:
// December 5, 2015
//
// Modified:
// December 5, 2015
//
//*****************************************************************************

#include "vc0706_if.h"

char CameraInit(unsigned char ucSerialNum, unsigned short usBaudRate)
{
    if(!VC0706SystemReset())
    {
        return 0;
    }

    if(!VC0706SetSerialNum(ucSerialNum))
    {
        return 0;
    }

    if(!VC0706SetBaudRate(usBaudRate))
    {
        return 0;
    }

    return 1;
}