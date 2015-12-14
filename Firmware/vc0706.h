//*****************************************************************************
//
// vc0706.h
//
// Defines and Macros for the VC0706 Serial Camera Module.
//
// A partial port of Adafruit-VC0706-Serial-Camera-Library found here:
//  https://github.com/adafruit/Adafruit-VC0706-Serial-Camera-Library
//
// Created:
// December 4, 2015
//
// Modified:
// December 5, 2015
//
//*****************************************************************************

#ifndef _VC0706_H_
#define _VC0706_H_


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
#define VC0706                                  UARTA1_BASE
#define VC0706_PERIPH                           PRCM_UARTA1
#define VC0706_DEFAULT_BAUD_RATE                38400

#define VC0706_INTERFACE_UART                   0x01
#define VC0706_INTERFACE_HS_UART                0x02
#define VC0706_INTERFACE_SPI                    0x03

#define VC0706_INTERFACE_UART_BAUD_9600         0xAEC8
#define VC0706_INTERFACE_UART_BAUD_19200        0x56E4
#define VC0706_INTERFACE_UART_BAUD_38400        0x2AF2
#define VC0706_INTERFACE_UART_BAUD_57600        0x1C4C
#define VC0706_INTERFACE_UART_BAUD_115200       0x0DA6

#define VC0706_INTERFACE_HS_UART_BAUD_38400     0x002B03C8
#define VC0706_INTERFACE_HS_UART_BAUD_57600     0x001D0130
#define VC0706_INTERFACE_HS_UART_BAUD_115200    0x000E0298
#define VC0706_INTERFACE_HS_UART_BAUD_460800    0x000302A6
#define VC0706_INTERFACE_HS_UART_BAUD_921600    0x00010353

#define VC0706_PROTOCOL_SIGN_RECEIVE            0x56
#define VC0706_PROTOCOL_SIGN_RETURN             0x76

#define VC0706_STATUS_SUCCESS                   0x00
#define VC0706_STATUS_ERROR_COMMAND             0x01
#define VC0706_STATUS_ERROR_DATA_LENGTH         0x02
#define VC0706_STATUS_ERROR_DATA_FORMAT         0x03
#define VC0706_STATUS_ERROR_CANT_EXECUTE        0x04
#define VC0706_STATUS_ERROR_EXECUTION           0x05

#define VC0706_COMMAND_GEN_VERSION              0x11
#define VC0706_COMMAND_SET_SERIAL_NUM           0x21
#define VC0706_COMMAND_SET_PORT                 0x24
#define VC0706_COMMAND_SYSTEM_RESET             0x26
#define VC0706_COMMAND_READ_DATA                0x30
#define VC0706_COMMAND_WRITE_DATA               0x31
#define VC0706_COMMAND_READ_FBUF                0x32
#define VC0706_COMMAND_WRITE_FBUF               0x33
#define VC0706_COMMAND_GET_FBUF_LEN             0x34
#define VC0706_COMMAND_SET_FBUF_LEN             0x35
#define VC0706_COMMAND_FBUF_CTRL                0x36
#define VC0706_COMMAND_COMM_MOTION_CTRL         0x37
#define VC0706_COMMAND_COMM_MOTION_STATUS       0x38
#define VC0706_COMMAND_COMM_MOTION_DETECTED     0x39
#define VC0706_COMMAND_MIRROR_CTRL              0x3A
#define VC0706_COMMAND_MIRROR_STATUS            0x3B
#define VC0706_COMMAND_COLOR_CTRL               0x3C
#define VC0706_COMMAND_COLOR_STATUS             0x3D
#define VC0706_COMMAND_POWER_SAVE_CTRL          0x3E
#define VC0706_COMMAND_POWER_SAVE_STATUS        0x3F
#define VC0706_COMMAND_AE_CTRL                  0x40
#define VC0706_COMMAND_AE_STATUS                0x41
#define VC0706_COMMAND_MOTION_CTRL              0x42
#define VC0706_COMMAND_MOTION_STATUS            0x43
#define VC0706_COMMAND_TV_OUT_CTRL              0x44
#define VC0706_COMMAND_OSD_ADD_CHAR             0x45
#define VC0706_COMMAND_DOWNSIZE_CTRL            0x54
#define VC0706_COMMAND_DOWNSIZE_STATUS          0x55
#define VC0706_COMMAND_GET_FLASH_SIZE           0x60
#define VC0706_COMMAND_ERASE_FLASH_SECTOR       0x61
#define VC0706_COMMAND_ERASE_FLASH_ALL          0x62
#define VC0706_COMMAND_READ_LOGO                0x70
#define VC0706_COMMAND_SET_BITMAP               0x71
#define VC0706_COMMAND_BATCH_WRITE              0x80

#define VC0706_FRAME_CONTROL_STOP               0x00
#define VC0706_FRAME_CONTROL_RESUME             0x02

#define VC0706_IMAGE_SIZE_640_480               0x00
#define VC0706_IMAGE_SIZE_320_240               0x11
#define VC0706_IMAGE_SIZE_160_120               0x22

#define _VC0706_CAMERA_BUF_SIZE                 100
#define _VC0706_CAMERA_DELAY                    10


//*****************************************************************************
// Variables
//*****************************************************************************
static unsigned char _ucSerialNum;
static unsigned char _ucCameraBuf[_VC0706_CAMERA_BUF_SIZE+1];


//*****************************************************************************
// Function Prototypes
//*****************************************************************************
extern void VC0706InitDriver();
extern char VC0706SystemReset();
extern char VC0706SetSerialNum(unsigned char ucSerialNum);
extern char VC0706SetBaudRate(unsigned short usBaudRate);
extern char VC0706SetImageSize(unsigned char ucImageSize);
extern char VC0706SetFrameControl(unsigned char ucCtrlFlag);
static char _VC0706RunCommand(unsigned char ucCmd, unsigned char *pucArgs,
                              unsigned char ucArgn, unsigned char ucRespLen,
                              unsigned char ucFlushFlag);
static void _VC0706SendCommand(unsigned char ucCmd, unsigned char *pucArgs,
                               unsigned char ucArgn);
static char _VC0706ReadResponse(unsigned char ucNumBytes,
                                unsigned char ucTimeout);
static char _VC0706VerifyResponse(unsigned char ucCmd);


//*****************************************************************************
// Mark the end of the C bindings section for C++ compilers.
//*****************************************************************************
#ifdef __cplusplus
}
#endif

#endif /* _VC0706_H_ */
