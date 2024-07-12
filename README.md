# Sistema - Virtual Machine Simulation

## Overview

This project simulates a virtual machine (VM) environment with basic CPU and memory management functionalities. It includes process management, memory allocation, and execution of predefined programs. The VM supports basic arithmetic operations, control flow, and system calls.

## Features

- **Memory Management**: Handles memory allocation and translation of logical to physical addresses.
- **Process Management**: Manages process creation, execution, and termination.
- **CPU Simulation**: Executes instructions, handles interrupts, and supports system calls.
- **Interactive Command Line Interface**: Allows users to create, remove, and manage processes through commands.

## Components

### Memory

The `Memory` class handles the memory allocation, translation, and management of the virtual machine.

- **Word**: Represents a unit of memory containing an opcode and parameters.
- **Memory Allocation**: Allocates memory for processes using a paging mechanism.
- **Memory Translation**: Translates logical addresses to physical addresses.
- **Dump Functions**: Provides methods to print memory content for debugging.

### CPU

The `CPU` class simulates the central processing unit of the virtual machine.

- **Registers**: Includes general-purpose registers and special registers for input/output.
- **Instruction Execution**: Executes instructions and handles various opcodes.
- **Interrupt Handling**: Manages different types of interrupts such as invalid address, overflow, and system calls.

### Process Management

The `ProcessManager` class handles the lifecycle of processes within the VM.

- **Process Control Block (PCB)**: Stores process-specific information such as registers, page table, and state.
- **Process Creation**: Allocates memory and initializes processes.
- **Process Execution**: Schedules and executes processes, handling context switches.

### Programs

The `Programas` class contains predefined programs to be executed by the VM.

- **Fatorial**: Computes the factorial of a number.
- **ProgMinimo**: A simple program to demonstrate basic memory storage.
- **ProgLoopInfinito**: Demonstrates a loop to test process scheduling.
- **Soma**: Adds numbers.
- **Subtrai**: Subtracts numbers.
- **Chamain/Chamaout**: Demonstrates system calls for input/output operations.

## Usage

### Starting the VM

To start the VM, execute the main class `Sistema`.

### Commands

The VM supports various commands to manage processes and memory:

- `new [program]: Creates a new process with the specified program (fatorial, subtrai, soma, progminimo, progloopinfinito, chamain, chamaout).`
- `rm [id]: Removes a process with the specified ID.`
- `ps: Lists all processes and their states.`
- `dump [id]: Shows the content of the specified process.`
- `dumpmem: Shows the content of the entire memory.`
- `executa [id]: Executes the specified process.`
- `execall: Executes all processes.`
- `allocateall: Allocates memory for all processes.`
- `trace1: Enables instruction tracing.`
- `trace0: Disables instruction tracing.`
- `exit: Exits the program.`
