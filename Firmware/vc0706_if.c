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
// December 13, 2015
//
//*****************************************************************************

#include <stddef.h>

#include "vc0706_if.h"

char CameraInit(unsigned char ucSerialNum, unsigned short usBaudRate,
                unsigned char ucImageSize)
{
    VC0706InitDriver();

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

    if(!VC0706SetImageSize(ucImageSize))
    {
        return 0;
    }

    return 1;
}

char *CameraSnapshot()
{
    unsigned int uiFrameLen;

    // Stop updating frame
    if(VC0706SetFrameControl(VC0706_CURRENT_FRAME_CONTROL_STOP))
    {
        return NULL;
    }

    // Get size of frame
    uiFrameLen = VC0706GetFrameLength();

    // Get picture

    // Resume updating frame
    if(VC0706SetFrameControl(VC0706_CURRENT_FRAME_CONTROL_RESUME))
    {
        return NULL;
    }
}
