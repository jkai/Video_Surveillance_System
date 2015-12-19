//*****************************************************************************
//
// vc0706.c
//
// Driver for the VC0706 Serial Camera Module.
//
// Created:
// December 4, 2015
//
// Modified:
// December 18, 2015
//
//*****************************************************************************

#include "hw_memmap.h"
#include "hw_types.h"
#include "prcm.h"
#include "rom_map.h"
#include "uart.h"
#include "utils.h"
#include "uart_if.h"
#include "gpio_if.h"

#include "vc0706.h"

void VC0706InitDriver()
{
    _ucSerialNum = 0;
    _usCameraBufIndex = 0;

    MAP_UARTConfigSetExpClk(VC0706, MAP_PRCMPeripheralClockGet(VC0706_PERIPH),
                            VC0706_DEFAULT_BAUD_RATE,
                            (UART_CONFIG_WLEN_8 | UART_CONFIG_STOP_ONE |
                            UART_CONFIG_PAR_NONE));

    MAP_UARTEnable(VC0706);
}

tBoolean VC0706SystemReset()
{
    unsigned char ucArgs[] = {0x0};
    unsigned char ucRespLen = 5;

    return _VC0706RunCommand(VC0706_COMMAND_SYSTEM_RESET, ucArgs,
                             sizeof(ucArgs), ucRespLen, 0);
}

tBoolean VC0706SetSerialNum(unsigned char ucSerialNum)
{
    unsigned char ucArgs[] = {0x01, ucSerialNum};
    unsigned char ucRespLen = 5;

    return _VC0706RunCommand(VC0706_COMMAND_SET_SERIAL_NUM, ucArgs,
                             sizeof(ucArgs), ucRespLen, 0);
}

tBoolean VC0706SetBaudRate(unsigned short usBaudRate)
{
    unsigned char ucArgs[] = {0x03, VC0706_INTERFACE_UART,
                              (usBaudRate >> 8) & 0xFF,
                              usBaudRate & 0xFF};
    unsigned char ucRespLen = 5;

    return _VC0706RunCommand(VC0706_COMMAND_SET_PORT, ucArgs,
                             sizeof(ucArgs), ucRespLen, 0);
}

tBoolean VC0706SetImageSize(unsigned char ucImageSize)
{
    unsigned char ucArgs[] = {0x05, 0x04, 0x01, 0x00, 0x19, ucImageSize};
    unsigned char ucRespLen = 5;

    return _VC0706RunCommand(VC0706_COMMAND_WRITE_DATA, ucArgs,
                             sizeof(ucArgs), ucRespLen, 0);
}

tBoolean VC0706SetFrameControl(unsigned char ucCtrlFlag)
{
    unsigned char ucArgs[] = {0x01, ucCtrlFlag};
    unsigned char ucRespLen = 5;

    return _VC0706RunCommand(VC0706_COMMAND_FBUF_CTRL, ucArgs,
                             sizeof(ucArgs), ucRespLen, 0);
}

unsigned int VC0706GetFrameLength(void)
{
    unsigned int uiFrameLen;
    unsigned char ucArgs[] = {0x01, VC0706_CURRENT_FRAME};
    unsigned char ucRespLen = 9;

    if(!_VC0706RunCommand(VC0706_COMMAND_GET_FBUF_LEN, ucArgs,
                          sizeof(ucArgs), ucRespLen, 0))
    {
        return 0;
    }

    // Decode frame length
    uiFrameLen = _ucCameraBuf[5];
    uiFrameLen <<= 8;
    uiFrameLen |= _ucCameraBuf[6];
    uiFrameLen <<= 8;
    uiFrameLen |= _ucCameraBuf[7];
    uiFrameLen <<= 8;
    uiFrameLen |= _ucCameraBuf[8];

    return uiFrameLen;
}

unsigned char *VC0706GetFrameBuffer(unsigned char ucNumBytes)
{
    unsigned char ucArgs[] = {0x0C, VC0706_CURRENT_FRAME,
                              VC0706_CONTROL_MODE_MCU,
                              0x00, 0x00, (_usCameraBufIndex >> 8) & 0xFF,
                              _usCameraBufIndex & 0xFF, 0x00, 0x00, 0x00,
                              ucNumBytes, (_VC0706_CAMERA_DELAY >> 8) & 0xFF,
                              _VC0706_CAMERA_DELAY & 0xFF};
    unsigned char ucRespLen = 5;
    _usCameraBufIndex = 0;

    if(!_VC0706RunCommand(VC0706_COMMAND_READ_FBUF, ucArgs,
                          sizeof(ucArgs), ucRespLen, 0))
    {
        return 0;
    }

    // ucRespLen offset is to account for the end command bytes
    if(!_VC0706ReadResponse(ucNumBytes+ucRespLen, 200))
    {
        return 0;
    }

    _usCameraBufIndex += ucNumBytes;

    return _ucCameraBuf;
}

static tBoolean _VC0706RunCommand(unsigned char ucCmd, unsigned char *pucArgs,
                                  unsigned char ucArgn, unsigned char ucRespLen,
                                  tBoolean bFlush)
{
    if(bFlush)
    {
        _VC0706ReadResponse(100, 10);
    }

    _VC0706SendCommand(ucCmd, pucArgs, ucArgn);

    if(_VC0706ReadResponse(ucRespLen, 200) != ucRespLen)
    {
        return 0;
    }

    if(!_VC0706VerifyResponse(ucCmd))
    {
        return 0;
    }

    return 1;
}

static void _VC0706SendCommand(unsigned char ucCmd, unsigned char *pucArgs,
                               unsigned char ucArgn)
{
    int i;

    MAP_UARTCharPut(VC0706, VC0706_PROTOCOL_SIGN_RECEIVE);
    MAP_UARTCharPut(VC0706, _ucSerialNum);
    MAP_UARTCharPut(VC0706, ucCmd);

    for(i=0; i<ucArgn; i++)
    {
        MAP_UARTCharPut(VC0706, pucArgs[i]);
    }
}

static tBoolean _VC0706ReadResponse(unsigned char ucNumBytes,
                                    unsigned char ucTimeout)
{
    //unsigned char ucCounter = 0;
    _ucCameraBufLen = 0;
    //tBoolean bAvail;

    //while((ucCounter != ucTimeout) && (_ucCameraBufLen != ucNumBytes))
    while(_ucCameraBufLen != ucNumBytes)
    {
        /*bAvail = MAP_UARTCharsAvail(VC0706);

        if(!bAvail)
        {
            ucCounter++;
            MAP_UtilsDelay(100);
            continue;
        }
        ucCounter = 0;*/

        _ucCameraBuf[_ucCameraBufLen++] = MAP_UARTCharGet(VC0706);
    }

    return _ucCameraBufLen;
}

static tBoolean _VC0706VerifyResponse(unsigned char ucCmd)
{
    if((_ucCameraBuf[0] != VC0706_PROTOCOL_SIGN_RETURN) ||
       (_ucCameraBuf[1] != _ucSerialNum) ||
       (_ucCameraBuf[2] != ucCmd) ||
       (_ucCameraBuf[3] != VC0706_STATUS_SUCCESS))
    {
        GPIO_IF_LedOn(MCU_RED_LED_GPIO);
        return 0;
    }

    return 1;
}
