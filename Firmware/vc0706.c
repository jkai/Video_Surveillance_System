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
// December 5, 2015
//
//*****************************************************************************

#include "vc0706.h"

char VC0706SystemReset()
{
    unsigned char ucArgs[] = {0x0};

    return _VC0706RunCommand(VC0706_COMMAND_SYSTEM_RESET, ucArgs, 1, 5);
}

char VC0706SetSerialNum(unsigned char ucSerialNum)
{
    unsigned char ucArgs[] = {0x01, ucSerialNum};

    return _VC0706RunCommand(VC0706_COMMAND_SET_SERIAL_NUM, ucArgs,
                             sizeof(ucArgs), 5);
}

char VC0706SetBaudRate(unsigned short usBaudRate)
{
    unsigned char ucArgs[] = {0x03, VC0706_INTERFACE_UART,
                              (usBaudRate >> 8) & 0xFF,
                              usBaudRate & 0xFF};

    return _VC0706RunCommand(VC0706_COMMAND_SET_PORT, ucArgs,
                             sizeof(ucArgs), 5);
}

static char _VC0706RunCommand(unsigned char ucCmd, unsigned char *pucArgs,
                              unsigned char ucArgn, unsigned char ucRespLen,
                              unsigned char ucFlushFlag)
{
    if(ucFlushFlag)
    {
        _VC0706ReadResponse(100, 10);
    }

    _VC0706SendCommand(_ucSerialNum, ucCmd, pucArgs, ucArgn);

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
    // TODO (Brandon): Implement
}

static char _VC0706ReadResponse(unsigned char ucNumBytes,
                                unsigned char ucTimeout)
{
    // TODO (Brandon): Implement
}

static char _VC0706VerifyResponse(unsigned char ucCmd,
                                  unsigned char *pucCameraBuf)
{
    if((_ucCameraBuf[0] != VC0706_PROTOCOL_SIGN_RETURN) ||
       (_ucCameraBuf[1] != _ucSerialNum) ||
       (_ucCameraBuf[2] != _ucCmd) ||
       (_ucCameraBuf[3] != VC0706_STATUS_SUCCESS))
    {
        return 0;
    }

    return 1;
}
