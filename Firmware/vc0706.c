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
// December 13, 2015
//
//*****************************************************************************

#include "hw_memmap.h"
#include "hw_types.h"
#include "prcm.h"
#include "rom_map.h"
#include "uart.h"


#include "vc0706.h"

void VC0706InitDriver()
{
    MAP_UARTConfigSetExpClk(VC0706, MAP_PRCMPeripheralClockGet(VC0706_PERIPH),
                            VC0706_DEFAULT_BAUD_RATE,
                            (UART_CONFIG_WLEN_8 | UART_CONFIG_STOP_ONE |
                            UART_CONFIG_PAR_NONE));
}

char VC0706SystemReset()
{
    unsigned char ucArgs[] = {0x0};

    return _VC0706RunCommand(VC0706_COMMAND_SYSTEM_RESET, ucArgs, 1, 5, 0);
}

char VC0706SetSerialNum(unsigned char ucSerialNum)
{
    unsigned char ucArgs[] = {0x01, ucSerialNum};

    return _VC0706RunCommand(VC0706_COMMAND_SET_SERIAL_NUM, ucArgs,
                             sizeof(ucArgs), 5, 0);
}

char VC0706SetBaudRate(unsigned short usBaudRate)
{
    unsigned char ucArgs[] = {0x03, VC0706_INTERFACE_UART,
                              (usBaudRate >> 8) & 0xFF,
                              usBaudRate & 0xFF};

    return _VC0706RunCommand(VC0706_COMMAND_SET_PORT, ucArgs,
                             sizeof(ucArgs), 5, 0);
}

char VC0706SetImageSize(unsigned char ucImageSize)
{
    unsigned char ucArgs[] = {0x05, 0x04, 0x01, 0x00, 0x19, ucImageSize};

    return _VC0706RunCommand(VC0706_COMMAND_WRITE_DATA, ucArgs,
                             sizeof(ucArgs), 5, 0);
}

char VC0706SetFrameControl(unsigned char ucCtrlFlag)
{
    unsigned char ucArgs[] = {0x01, ucCtrlFlag};

    return _VC0706RunCommand(VC0706_COMMAND_FBUF_CTRL, ucArgs,
                             sizeof(ucArgs), 5, 0);
}

static char _VC0706RunCommand(unsigned char ucCmd, unsigned char *pucArgs,
                              unsigned char ucArgn, unsigned char ucRespLen,
                              unsigned char ucFlushFlag)
{
    if(ucFlushFlag)
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
        MAP_UARTCharPut(VC0706, ucCmd);
    }
}

static char _VC0706ReadResponse(unsigned char ucNumBytes,
                                unsigned char ucTimeout)
{
    // TODO (Brandon): Implement

    return 0;
}

static char _VC0706VerifyResponse(unsigned char ucCmd)
{
    if((_ucCameraBuf[0] != VC0706_PROTOCOL_SIGN_RETURN) ||
       (_ucCameraBuf[1] != _ucSerialNum) ||
       (_ucCameraBuf[2] != ucCmd) ||
       (_ucCameraBuf[3] != VC0706_STATUS_SUCCESS))
    {
        return 0;
    }

    return 1;
}
