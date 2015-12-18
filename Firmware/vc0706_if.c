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
// December 14, 2015
//
//*****************************************************************************

#include <stddef.h>
#include <stdlib.h>
#include <string.h>
#include "common.h"
#include "hw_types.h"
#include "rom_map.h"
#include "utils.h"
#include "uart_if.h"
#include "gpio_if.h"
#include "vc0706.h"

#include "vc0706_if.h"

tBoolean CameraInit(unsigned char ucSerialNum, unsigned short usBaudRate,
                unsigned char ucImageSize)
{
    VC0706InitDriver();

    // System reset not working... fix this later...
    /*if(!VC0706SystemReset())
    {
        GPIO_IF_LedOn(MCU_RED_LED_GPIO);
        return 0;
    }*/

    //MAP_UtilsDelay(10000);

    if(!VC0706SetSerialNum(ucSerialNum))
    {
        GPIO_IF_LedOn(MCU_ORANGE_LED_GPIO);
        return 0;
    }

    if(!VC0706SetBaudRate(usBaudRate))
    {
        GPIO_IF_LedOn(MCU_GREEN_LED_GPIO);
        return 0;
    }

    if(!VC0706SetImageSize(ucImageSize))
    {
        GPIO_IF_LedOn(MCU_RED_LED_GPIO);
        return 0;
    }

    return 1;
}

unsigned char *CameraSnapshot()
{
    unsigned int uiFrameLen;
    unsigned char *pucCameraBuf;
    unsigned char *pucImageBuf;
    unsigned char ucBytesToRead;
    unsigned int uiImageBufOffset = 0;

    // Stop updating frame
    if(!VC0706SetFrameControl(VC0706_CURRENT_FRAME_CONTROL_STOP))
    {
        return NULL;
    }

    // Get size of frame
    uiFrameLen = VC0706GetFrameLength();

    // Allocate memory for snapshot
    pucImageBuf = malloc(uiFrameLen);
    if(pucImageBuf == NULL)
    {
        //UART_PRINT("Can't Allocate Resources\r\n");
        LOOP_FOREVER();
    }

    // Get picture
    while(uiFrameLen > 0)
    {
        ucBytesToRead = uiFrameLen>64 ? 64 : uiFrameLen;

        pucCameraBuf = VC0706GetFrameBuffer(ucBytesToRead);

        memcpy(pucImageBuf+uiImageBufOffset, pucCameraBuf, ucBytesToRead);

        uiImageBufOffset+=ucBytesToRead;
        uiFrameLen-=ucBytesToRead;
    }

    // Resume updating frame
    if(!VC0706SetFrameControl(VC0706_CURRENT_FRAME_CONTROL_RESUME))
    {
        free(pucImageBuf);
        return NULL;
    }

    return pucImageBuf;
}
