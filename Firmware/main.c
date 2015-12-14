//*****************************************************************************
//
// main.c
//
// Firmware for CC3200-LAUNCHXL as part of a video surveillance system found
// at https://github.com/jkai/Video_Surveillance_System.
//
// This source file uses code snippets from Texas Instruments Incorporated's
// CC3200-LAUNCHXL sample projects. Copyright notice is moved to the end of
// this file.
//
// Created:
// December 4, 2015
//
// Modified:
// December 13, 2015
//
//*****************************************************************************


//*****************************************************************************
// Includes
//*****************************************************************************
// Standard includes
#include <stdlib.h>
#include <string.h>

// Hardware includes
#include "hw_types.h"
#include "hw_ints.h"
#include "hw_memmap.h"
#include "hw_common_reg.h"
#include "hw_uart.h"

// Driverlib includes
#include "interrupt.h"
#include "prcm.h"
#include "rom.h"
#include "rom_map.h"
#include "uart.h"
#include "udma.h"
#include "utils.h"

// SimpleLink includes
#include "simplelink.h"

// OS includes
#include "osi.h"

// Common includes
#include "network_if.h"
#include "uart_if.h"
#include "udma_if.h"
#include "common.h"

// TFTP includes
#include "datatypes.h"
#include "tftp.h"

// Application includes
#include "pin_mux_config.h"
#include "vc0706.h"
#include "vc0706_if.h"


//*****************************************************************************
// Defines
//*****************************************************************************
#define TFTP_IP         0xC0A8010E      // This is the host IP: 192.168.1.14
#define FILE_SIZE_MAX   (20*1024)       // Max File Size set to 20KB
#define SSID            "NETGEAR31"
#define SSID_KEY        "happystar329"
#define OSI_STACK_SIZE  2048


//*****************************************************************************
// Variables
//*****************************************************************************
// Vector table defined exterenally (in startup_css.c)
extern void (* const g_pfnVectors[])(void);


//*****************************************************************************
// Function Prototypes
//*****************************************************************************
static void BoardInit(void);
static void NetInit(void);
static void TFTPWrite(unsigned char *pucBuf, unsigned long ulBufSize);
static void MainTask(void);
static void UARTIntHandler();   // UART interrupt handler


//*****************************************************************************
// Main
//*****************************************************************************
void main() 
{
    long lRetVal = -1;

    // Board Initialization
    BoardInit();

    // Enable and configure DMA
    UDMAInit();

    // Pinmux for UART
    PinMuxConfig();

    // Configuring UART
    InitTerm();

    // Register interrupt handler for UARTA0
    MAP_UARTIntRegister(UARTA0_BASE, UARTIntHandler);

    // Enable DMA done interrupts for UARTA0
    //MAP_UARTIntEnable(UARTA0_BASE, UART_INT_DMARX);

    // Configure UART to work with the Terminal
    MAP_UARTConfigSetExpClk(CONSOLE, MAP_PRCMPeripheralClockGet(CONSOLE_PERIPH),
                            UART_BAUD_RATE,
                            (UART_CONFIG_WLEN_8 | UART_CONFIG_STOP_ONE |
                            UART_CONFIG_PAR_NONE));

    // Clear terminal
    ClearTerm();

    Report("Made it here...?\n\r");

    // Camera Initialzation
    if(!CameraInit(CAMERA_DEFAULT_SERIAL_NUM, CAMERA_DEFAULT_BAUD_RATE,
            CAMERA_DEFAULT_IMAGE_SIZE))
    {
        Report("Shit...\n\r");
        LOOP_FOREVER();
    }

    // Start the SimpleLink Host
    lRetVal = VStartSimpleLinkSpawnTask(SPAWN_TASK_PRIORITY);
    if(lRetVal < 0)
    {
        ERR_PRINT(lRetVal);
        LOOP_FOREVER();
    }

    // Display banner
    Report("\t\t *********************************************\n\r");
    Report("\t\t    Starting Video Streaming Application       \n\r");
    Report("\t\t *********************************************\n\r");

    // Start main task
    lRetVal = osi_TaskCreate(MainTask,
                    (const signed char *)"MainTask",
                    OSI_STACK_SIZE,
                    NULL,
                    1,
                    NULL );
    if(lRetVal < 0)
    {
        ERR_PRINT(lRetVal);
        LOOP_FOREVER();
    }

    // Start the task scheduler
    osi_start();
}


//*****************************************************************************
// Function Implementations
//*****************************************************************************
static void BoardInit(void)
{
    // Set base of vector table
    IntVTableBaseSet((unsigned long)&g_pfnVectors[0]);

    // Enable Processor
    MAP_IntMasterEnable();
    MAP_IntEnable(FAULT_SYSTICK);

    PRCMCC3200MCUInit();
}

static void NetInit(void)
{
    SlSecParams_t secParams;
    long lRetVal = -1;

    // Configuring security parameters for the AP
    secParams.Key = SSID_KEY;
    secParams.KeyLen = strlen(SSID_KEY);
    secParams.Type = SL_SEC_TYPE_WPA_WPA2;

    // Initialize network driver
    lRetVal = Network_IF_InitDriver(ROLE_STA);

    // Connecting to WLAN AP - Set with static parameters defined at the top
    // After this call we will be connected and have IP address
    lRetVal = Network_IF_ConnectAP(SSID, secParams);
}

static void TFTPWrite(unsigned char *pucBuf, unsigned long ulBufSize)
{
    unsigned long ulFileSize;

    char *FileWrite = "writeToServer.jpg";  // File to be written using TFTP

    long lRetVal = -1;
    unsigned short uiTftpErrCode;

    // Send to server
    lRetVal = sl_TftpSend(TFTP_IP, FileWrite, (char *)pucBuf,\
                        &ulBufSize, &uiTftpErrCode);
    if(lRetVal < 0)
    {
        ERR_PRINT(lRetVal);
        LOOP_FOREVER();
    }

    UART_PRINT("Snapshot sent.\r\n");
}

static void MainTask(void)
{
    //unsigned char pucBuf[] = "Testing TFTP Write!";
    unsigned char *pucBuf;

    // Network Driver Initialization
    NetInit();

    // Output IP to terminal
    UART_PRINT("Packet destination: %d.%d.%d.%d\n\r",\
                  SL_IPV4_BYTE(TFTP_IP, 3), SL_IPV4_BYTE(TFTP_IP, 2),
                  SL_IPV4_BYTE(TFTP_IP, 1), SL_IPV4_BYTE(TFTP_IP, 0));

    /*while (1)
    {
        // Get snapshot from camera

        // Send snapshot to server
        TFTPWrite(pucBuf, sizeof(pucBuf));
    }*/

    UART_PRINT("Taking snapshot...\n\r");
    pucBuf = CameraSnapshot();

    UART_PRINT("Sending snapshot over IP...\n\r");
    TFTPWrite(pucBuf, sizeof(pucBuf));

    UART_PRINT("Freeing memory...\n\r");
    free(pucBuf);
}

// Interrupt handler for UART
static void UARTIntHandler()
{
    // TODO (Brandon): Handle UART interrupts
    return;
}

//*****************************************************************************
//
// Copyright (C) 2014 Texas Instruments Incorporated - http://www.ti.com/
//
//
//  Redistribution and use in source and binary forms, with or without
//  modification, are permitted provided that the following conditions
//  are met:
//
//    Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
//
//    Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the
//    distribution.
//
//    Neither the name of Texas Instruments Incorporated nor the names of
//    its contributors may be used to endorse or promote products derived
//    from this software without specific prior written permission.
//
//  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
//  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
//  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
//  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
//  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
//  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
//  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
//  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
//  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
//  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
//  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//
//*****************************************************************************
