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
// December 4, 2015
//
//*****************************************************************************

// Standard includes
#include <stdlib.h>

// Hardware includes
#include "hw_types.h"
#include "hw_ints.h"
#include "hw_memmap.h"

// Driverlib includes
#include "interrupt.h"
#include "prcm.h"
#include "rom.h"
#include "rom_map.h"

// SimpleLink includes
#include "simplelink.h"

// TFTP includes
#include "datatypes.h"
#include "tftp.h"

//Common includes
#include "network_if.h"
#include "uart_if.h"
#include "udma_if.h"
#include "common.h"

// Application includes
#include "pinmux.h"


// Vector table
extern void (* const g_pfnVectors[])(void);

static void BoardInit(void);
static void TFTPTransfer(void);

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

    // Start the SimpleLink Host
    lRetVal = VStartSimpleLinkSpawnTask(SPAWN_TASK_PRIORITY);
    if(lRetVal < 0)
    {
        ERR_PRINT(lRetVal);
        LOOP_FOREVER();
    }

    // Start TFTP transfer
    TFTPTransfer()

    // Hang at end of execution
    LOOP_FOREVER();
}

static void BoardInit(void)
{
	// Set base of vector table
    IntVTableBaseSet((unsigned long)&g_pfnVectors[0]);

    // Enable Processor
    MAP_IntMasterEnable();
    MAP_IntEnable(FAULT_SYSTICK);

    PRCMCC3200MCUInit();
}

static void TFTPTransfer(void)
{
	// TODO (Brandon): Test TFTP Transfer to and from a TFTP server
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
