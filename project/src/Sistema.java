import java.util.*;
import java.util.concurrent.*;

public class Sistema {

	// -------------------------------------------------------------------------------------------------------
	// --------------------- H A R D W A R E - definicoes de HW
	// -----------------------------------------------------
	// -------------------------------------------------------------------------------------------------------
	// --------------------- M E M O R I A - definicoes de palavra de memoria,
	// memória ----------------------

	public class Word { // cada posicao da memoria tem uma instrucao (ou um dado)
		public Opcode opc; //
		public int r1; // indice do primeiro registrador da operacao (Rs ou Rd cfe opcode na tabela)
		public int r2; // indice do segundo registrador da operacao (Rc ou Rs cfe operacao)
		public int p; // parametro para instrucao (k ou A cfe operacao), ou o dado, se opcode = DADO

		public Word(Opcode _opc, int _r1, int _r2, int _p) { // vide definição da VM - colunas vermelhas da tabela
			opc = _opc;
			r1 = _r1;
			r2 = _r2;
			p = _p;
		}
	}

	public class Memory {
		private int tamMem;
		private int tamPag;
		private int numFrames;
		private boolean[] frameDisponivel; // Rastreia a disponibilidade de frames
		public Word[] m; // m representa a memória fisica: um array de posicoes de memoria (word)

		// Construtor agora aceita tamanho de memória e tamanho de página
		public Memory(int size, int tamPag) {
			this.tamMem = size;
			this.tamPag = tamPag;
			this.numFrames = size / tamPag;
			this.frameDisponivel = new boolean[numFrames];
			Arrays.fill(frameDisponivel, true); // Inicializa todos os frames como disponíveis
			m = new Word[tamMem];
			for (int i = 0; i < tamMem; i++) {
				m[i] = new Word(Opcode.___, -1, -1, -1);
			}
		}

		public int traduzEndereco(int enderecoLogico, int[] tabelaPaginas) {
			int numeroPagina = enderecoLogico / tamPag; // Calcula o número da página
			int deslocamento = enderecoLogico % tamPag; // Calcula o deslocamento dentro da página
			if (numeroPagina >= tabelaPaginas.length) {
				System.out.println("Erro: Página não encontrada");
				return -1;
			}
			int frame = tabelaPaginas[numeroPagina]; // Encontra o frame correspondente na tabela de páginas
			int enderecoFisico = frame * tamPag + deslocamento; // Calcula o endereço físico
			return enderecoFisico;
		}

		// Função para alocar memória para um número de páginas necessário
		public int[] aloca(int numPalavras) {
			int numFramesNecessarios = (numPalavras / tamPag) + 1;
			int[] tabelaPaginas = new int[numFramesNecessarios];
			int framesAlocados = 0;
			for (int i = 0; (i < numFrames) && (framesAlocados != numFramesNecessarios); i++) {
				if (frameDisponivel[i]) {
					tabelaPaginas[framesAlocados] = i;
					frameDisponivel[i] = false;
					framesAlocados++;
				}
			}
			if (framesAlocados != numFramesNecessarios) {
				System.out.println("Erro: Memória insuficiente");
				for (int frame : tabelaPaginas) {
					frameDisponivel[frame] = true;
				}
				return null;
			}
			return tabelaPaginas;
		}

		// Função para desalocar os frames alocados para um processo
		public void desaloca(int[] tabelaPaginas) {
			for (int frame : tabelaPaginas) {
				frameDisponivel[frame] = true;
			}
		}

		// Funções de dump de memória
		public void dump(Word w) {
			System.out.print("[ ");
			System.out.print(w.opc);
			System.out.print(", ");
			System.out.print(w.r1);
			System.out.print(", ");
			System.out.print(w.r2);
			System.out.print(", ");
			System.out.print(w.p);
			System.out.println("  ] ");
		}

		public void dump(int ini, int fim) {
			for (int i = ini; i < fim; i++) {
				System.out.print(i);
				System.out.print(":  ");
				dump(m[i]);
			}
		}
	}

	// -------------------------------------------------------------------------------------------------------
	// --------------------- P R O C E S S O S - definicoes de Processos
	// --------------------------------------
	// -------------------------------------------------------------------------------------------------------

	public class ProcessManager {
		private ProcessControlBlock[] pcb;
		private ProcessControlBlock state;
		private BlockingQueue<ProcessControlBlock> processosProntos;
		private Semaphore pcbSemaphore;
		private Memory mem;
		private int id;

		public ProcessManager(Memory mem, BlockingQueue<ProcessControlBlock> _processosProntos, Semaphore _pcbSemaphore) {
			id = 0;
			this.mem = mem;
			pcb = new ProcessControlBlock[mem.numFrames];
			processosProntos = _processosProntos;
			pcbSemaphore = _pcbSemaphore;
		}

		public ProcessControlBlock getPCB(int id) {
			if (id < 0 && id >= pcb.length)
				return null;

			try {
				pcbSemaphore.acquire();
				return pcb[id];
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				pcbSemaphore.release();
			}
			return null;
		}

		public ProcessControlBlock setState(ProcessControlBlock pcb) {
			try {
				pcbSemaphore.acquire();
				state = pcb;
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				pcbSemaphore.release();
			}
			return state;
		}

		public void setStateReady(boolean ready) {
			try {
				pcbSemaphore.acquire();
				state.ready = ready;
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				pcbSemaphore.release();
			}
		}

		public void resetIoParameter(int pcbId) {
			try {
				pcbSemaphore.acquire();
				pcb[pcbId].ioParameter = -1;
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				pcbSemaphore.release();
			}
		}

		public void setStateRunning(boolean running) {
			try {
				pcbSemaphore.acquire();
				state.running = running;
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				pcbSemaphore.release();
			}
		}

		public boolean criaProcesso(Word[] program) {
			int[] tabelaPaginas = mem.aloca(program.length);
			if (tabelaPaginas != null) {
				ProcessControlBlock newpcb = new ProcessControlBlock(id);
				newpcb.tabelaPaginas = tabelaPaginas;
				newpcb.processState = true;
				newpcb.running = false;
				try {
					pcbSemaphore.acquire();
					pcb[id] = newpcb;
					id++;

					for (int i = 0; i < program.length; i++) {
						int enderecoFisico = mem.traduzEndereco(i, tabelaPaginas);
						mem.m[enderecoFisico].opc = program[i].opc;
						mem.m[enderecoFisico].r1 = program[i].r1;
						mem.m[enderecoFisico].r2 = program[i].r2;
						mem.m[enderecoFisico].p = program[i].p;
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					pcbSemaphore.release();
				}
			} else {
				return false;
			}
			return true;
		}

		public boolean desalocaProcesso(int id) {
			ProcessControlBlock auxpcb = getPCB(id);
			if (auxpcb != null && auxpcb.running == false) {
				mem.desaloca(auxpcb.tabelaPaginas);
				try {
					pcbSemaphore.acquire();
					vm.cpu.processosBloqueados.remove(auxpcb);
					pcb[id] = null;
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					pcbSemaphore.release();
				}
				return true;
			}
			return false;
		}

		public boolean executaProcesso(int id) {
			ProcessControlBlock pcb = getPCB(id);
			if (pcb != null && pcb.ready && !pcb.running) {
				try {
					processosProntos.put(pcb);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				return true;
			}
			return false;
		}

		public ProcessControlBlock salvaEstadoProcesso(int id) {
			ProcessControlBlock pcb = getPCB(id);
			if (pcb != null) {
				pcb.pc = vm.cpu.pc;
				pcb.registradores = vm.cpu.reg;
				return pcb;
			}
			return null;
		}
	}

	public class ProcessControlBlock {
		public int[] tabelaPaginas;
		public int[] registradores;
		public boolean processState;
		public boolean running;
		public boolean ready;
		public int pc;
		public int id;
		public int ioParameter;

		public ProcessControlBlock(int id) {
			tabelaPaginas = null;
			ioParameter = -1;
			registradores = new int[10];
			ready = true;
			processState = false;
			running = false;
			pc = 0;
			this.id = id;
		}
	}

	// -------------------------------------------------------------------------------------------------------
	// --------------------- C P U - definicoes da CPU
	// -------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------

	public enum Opcode {
		DATA, ___, // se memoria nesta posicao tem um dado, usa DATA, se nao usada ee NULO ___
		JMP, JMPI, JMPIG, JMPIL, JMPIE, // desvios e parada
		JMPIM, JMPIGM, JMPILM, JMPIEM, STOP,
		JMPIGK, JMPILK, JMPIEK, JMPIGT,
		ADDI, SUBI, ADD, SUB, MULT, // matematicos
		LDI, LDD, STD, LDX, STX, MOVE, // movimentacao
		SYSCALL // chamada de sistema
	}

	public enum Interrupts { // possiveis interrupcoes que esta CPU gera
		noInterrupt, intEnderecoInvalido, intInstrucaoInvalida, intOverflow, intSTOP, intProcessTimeup, intIO;
	}

	public class CPU extends Thread {
		private int maxInt; // valores maximo e minimo para inteiros nesta cpu
		private int minInt;
		private boolean trace;
		private int cpuCicles;
		private BlockingQueue<ProcessControlBlock> processosProntos;
		private List<ProcessControlBlock> processosBloqueados;
		private int pc; // program counter
		private Word ir; // instruction register
		private int[] reg; // registradores da CPU
		private Interrupts irpt; // durante instrucao, interrupcao pode ser sinalizada
		private int base; // base e limite de acesso na memoria
		private int limite; // por enquanto toda memoria pode ser acessada pelo processo rodando
		private Memory mem; // mem tem funcoes de dump e o array m de memória 'fisica'
		private Word[] m; // CPU acessa MEMORIA, guarda referencia a 'm'. m nao muda. semre será um array
							// de palavras
		private boolean debug; // se true entao mostra cada instrucao em execucao
		private BlockingQueue<String> commandQueue;
		public ProcessManager pm;
		private Semaphore memSemaphore;
		private Semaphore ioSemaphore;
		private int output;
		private int outputId;

		public CPU(Memory _mem, boolean _debug, BlockingQueue<String> _commandQueue,
				Semaphore _semaphore) {
			cpuCicles = 0;
			processosProntos = new ArrayBlockingQueue<ProcessControlBlock>(100);
			processosBloqueados = new Vector<ProcessControlBlock>();
			maxInt = 32767; // capacidade de representacao modelada
			minInt = -32767; // se exceder deve gerar interrupcao de overflow
			mem = _mem; // usa mem para acessar funcoes auxiliares (dump)
			m = mem.m; // usa o atributo 'm' para acessar a memoria.
			reg = new int[10]; // aloca o espaço dos registradores - regs 8 e 9 usados somente para IO
			debug = _debug; // se true, print da instrucao em execucao
			trace = false;
			commandQueue = _commandQueue;
			memSemaphore = new Semaphore(1);
			pm = new ProcessManager(mem, processosProntos, _semaphore);
			ioSemaphore = new Semaphore(0);
		}

		private boolean legal(int e) {
			if (e >= 0 && e < mem.tamMem) {
				return true;
			} else {
				irpt = Interrupts.intEnderecoInvalido; // Set the interrupt type if illegal
				return false;
			}
		}

		private boolean testOverflow(int v) {
			if ((v < minInt) || (v > maxInt)) {
				irpt = Interrupts.intOverflow;
				return false;
			}
			return true;
		}

		public void allocateAll() {
			pm.criaProcesso(progs.fatorial);
			pm.criaProcesso(progs.soma);
			pm.criaProcesso(progs.subtrai);
			pm.criaProcesso(progs.progMinimo);
			pm.criaProcesso(progs.progLoopInfinito);
		}

		public boolean execAll() {
			for (ProcessControlBlock pcb : pm.pcb) {
				if (pcb != null) {
					try {
						processosProntos.put(pcb);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			return true;
		}

		public void setContext(int _base, int _limite, int _pc, int[] _reg) {
			base = _base;
			limite = _limite;
			reg = _reg;
			pc = _pc;
			irpt = Interrupts.noInterrupt;
		}

		public void interruptHandle() {
			switch (irpt) {
				case intEnderecoInvalido:
					System.out.println("Endereco Invalido");
					pm.setStateRunning(false);
					pm.desalocaProcesso(pm.state.id);
					break;
				case intInstrucaoInvalida:
					System.out.println("Instrucao Invalida");
					pm.setStateRunning(false);
					pm.desalocaProcesso(pm.state.id);
					break;
				case intOverflow:
					System.out.println("Overflow");
					pm.setStateRunning(false);
					pm.desalocaProcesso(pm.state.id);
					break;
				case intSTOP:
					System.out.println("Parada solicitada");
					pm.setStateRunning(false);
					pm.desalocaProcesso(pm.state.id);
					break;
				case intProcessTimeup:
					// System.out.println("Tempo de execucao esgotado");
					try {
						processosProntos.put(pm.salvaEstadoProcesso(pm.state.id));
						pm.setStateRunning(false);
						pm.setStateReady(true);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					break;
				case intIO:
					processosBloqueados.add(pm.salvaEstadoProcesso(pm.state.id));
					pm.setStateRunning(false);
					pm.setStateReady(false);
					break;
				default:
					break;
			}
		}

		private void processCommand(String command) {
			String[] commandParts = command.split(" ");
			try {
				memSemaphore.acquire();
				switch (commandParts[0].toLowerCase()) {
					case "executa":
						execAll();
						break;
					case "allocateall":
						allocateAll();
						break;
					case "trace1":
						trace = true;
						break;
					case "trace0":
						trace = false;
						break;
					case "new":
						createProcess(commandParts[1]);
						break;
					case "rm":
						removeProcess(commandParts[1]);
						break;
					case "ps":
						listProcesses();
						break;
					case "dump":
						dumpProcess(commandParts[1]);
						break;
					case "dumpmem":
						mem.dump(0, mem.tamMem);
						break;
					case "in":
						if (!handleIn(commandParts[1], commandParts[2])){
							System.out.println("Processo não encontrado ou não está esperando entrada");
						}
						break;
					case "exit":
						System.exit(0);
						break;
					default:
						System.out.println("Comando inválido");
						break;
				}
			} catch (Exception e) {
				System.out.println("Erro ao acessar a memória");
			} finally {
				memSemaphore.release();
			}
		}

		private void createProcess(String processType) {
			switch (processType) {
				case "fatorial":
					pm.criaProcesso(progs.fatorial);
					break;
				case "subtrai":
					pm.criaProcesso(progs.subtrai);
					break;
				case "soma":
					pm.criaProcesso(progs.soma);
					break;
				case "progminimo":
					pm.criaProcesso(progs.progMinimo);
					break;
				case "progloopinfinito":
					pm.criaProcesso(progs.progLoopInfinito);
					break;
				case "chamain":
					pm.criaProcesso(progs.chamain);
					break;
				case "chamaout":
					pm.criaProcesso(progs.chamaout);
					break;
				default:
					System.out.println("Processo inválido");
					break;
			}
		}

		private void removeProcess(String processIdStr) {
			try {
				int processId = Integer.parseInt(processIdStr);
				if (!pm.desalocaProcesso(processId)) {
					System.out.println("Processo não encontrado");
				}
			} catch (Exception e) {
				System.out.println("Processo inválido");
			}
		}

		private void listProcesses() {
			boolean existeProcessos = false;
			for (int i = 0; i < pm.pcb.length; i++) {
				String rodando = "Rodando";
				String status = "Bloqueado";
				ProcessControlBlock pcb = pm.getPCB(i);
				if (pcb != null) {
					existeProcessos = true;
					if (pcb.ready)
						status = "Pronto";
					if (!pcb.running)
						rodando = "Parado";
					System.out.println("Processo " + i + ": " + status + " - " + rodando);
				}
			}
			if (!existeProcessos) {
				System.out.println("Não existem processos.");
			}
		}

		private void dumpProcess(String processIdStr) {
			try {
				int processId = Integer.parseInt(processIdStr);
				ProcessControlBlock pcb = pm.getPCB(processId);
				if (pcb != null) {
					System.out.println("Processo " + processId + ": " + pcb.ready + " - " + pcb.running);
					for (int i = 0; i < pcb.tabelaPaginas.length; i++) {
						vm.mem.dump(pcb.tabelaPaginas[i] * vm.tamPag, (pcb.tabelaPaginas[i] + 1) * vm.tamPag);
					}
				} else {
					System.out.println("Processo não encontrado");
				}
			} catch (NumberFormatException e) {
				System.out.println("ID de processo inválido");
			}
		}

		private boolean handleIn(String id, String value) {
			for (ProcessControlBlock pcb : processosBloqueados) {
				if (pcb.id == Integer.parseInt(id)) {
					if (!processosBloqueados.contains(pcb) || pcb.ioParameter == -1)
						break;
					mem.m[mem.traduzEndereco(pcb.ioParameter, pcb.tabelaPaginas)].p = Integer.parseInt(value);
					pm.resetIoParameter(pcb.id);
					try {
						processosBloqueados.remove(pcb);
						processosProntos.put(pcb);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					return true;
				}
			}
			return false;
		}

		public void run() {
			Thread commandThread = new Thread(() -> {
				while (true) {
					try {
						String command = commandQueue.take();
						if (command != null) {
							processCommand(command);
						}
					} catch (Exception e) {
						System.out.println("Erro ao executar comando");
					}
				}
			});
			commandThread.start();

			Thread outThread = new Thread(() -> {
				while (true) {
					try {
						ioSemaphore.acquire();
						System.out.println("Out: " + output);
						for (ProcessControlBlock pcb : processosBloqueados) {
							if (pcb.id == outputId) {
								pcb.ioParameter = output;
								processosBloqueados.remove(pcb);
								processosProntos.put(pcb);
								break;
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			outThread.start();

			while (true) {
				try {
					ProcessControlBlock pcb = processosProntos.take();
					if (pcb != null) {
						pm.setState(pcb);
						pm.setStateRunning(true);
						setContext(0, mem.tamMem - 1, pm.state.pc, pm.state.registradores);
						memSemaphore.acquire();
						executeInstructions();
						memSemaphore.release();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		private void executeInstructions() throws InterruptedException {
			while (true) {
				// System.out.println("Processo: " + pm.state.id);
				if (legal(pc)) {
					cpuCicles++;
					ir = m[mem.traduzEndereco(pc, pm.state.tabelaPaginas)];
					if (trace) {
						System.out.println("pc: " + pc + " exec: " + ir.opc + " " + ir.r1 + " " + ir.r2 + " " + ir.p);
					}
					executeInstruction();
				}
				if (checkInterruption()) {
					break;
				}
			}
		}

		private void executeInstruction() {
			switch (ir.opc) {
				case LDI:
					reg[ir.r1] = ir.p;
					pc++;
					break;
				case LDD:
					if (legal(mem.traduzEndereco(ir.p, pm.state.tabelaPaginas))) {
						reg[ir.r1] = m[mem.traduzEndereco(ir.p, pm.state.tabelaPaginas)].p;
						pc++;
					}
					break;
				case LDX:
					if (legal(mem.traduzEndereco(reg[ir.r2], pm.state.tabelaPaginas))) {
						reg[ir.r1] = m[mem.traduzEndereco(reg[ir.r2], pm.state.tabelaPaginas)].p;
						pc++;
					}
					break;
				case STD:
					if (legal(mem.traduzEndereco(ir.p, pm.state.tabelaPaginas))) {
						m[mem.traduzEndereco(ir.p, pm.state.tabelaPaginas)].opc = Opcode.DATA;
						m[mem.traduzEndereco(ir.p, pm.state.tabelaPaginas)].p = reg[ir.r1];
						pc++;
					}
					break;
				case STX:
					if (legal(mem.traduzEndereco(reg[ir.r1], pm.state.tabelaPaginas))) {
						m[mem.traduzEndereco(reg[ir.r1], pm.state.tabelaPaginas)].opc = Opcode.DATA;
						m[mem.traduzEndereco(reg[ir.r1], pm.state.tabelaPaginas)].p = reg[ir.r2];
						pc++;
					}
					break;
				case MOVE:
					reg[ir.r1] = reg[ir.r2];
					pc++;
					break;
				case ADD:
					reg[ir.r1] = reg[ir.r1] + reg[ir.r2];
					testOverflow(reg[ir.r1]);
					pc++;
					break;
				case ADDI:
					reg[ir.r1] = reg[ir.r1] + ir.p;
					testOverflow(reg[ir.r1]);
					pc++;
					break;
				case SUB:
					reg[ir.r1] = reg[ir.r1] - reg[ir.r2];
					testOverflow(reg[ir.r1]);
					pc++;
					break;
				case SUBI:
					reg[ir.r1] = reg[ir.r1] - ir.p;
					testOverflow(reg[ir.r1]);
					pc++;
					break;
				case MULT:
					reg[ir.r1] = reg[ir.r1] * reg[ir.r2];
					testOverflow(reg[ir.r1]);
					pc++;
					break;
				case JMP:
					pc = ir.p;
					break;
				case JMPIG:
					if (reg[ir.r2] > 0) {
						pc = reg[ir.r1];
					} else {
						pc++;
					}
					break;
				case JMPIGK:
					if (reg[ir.r2] > 0) {
						pc = ir.p;
					} else {
						pc++;
					}
					break;
				case JMPILK:
					if (reg[ir.r2] < 0) {
						pc = ir.p;
					} else {
						pc++;
					}
					break;
				case JMPIEK:
					if (reg[ir.r2] == 0) {
						pc = ir.p;
					} else {
						pc++;
					}
					break;
				case JMPIL:
					if (reg[ir.r2] < 0) {
						pc = reg[ir.r1];
					} else {
						pc++;
					}
					break;
				case JMPIE:
					if (reg[ir.r2] == 0) {
						pc = reg[ir.r1];
					} else {
						pc++;
					}
					break;
				case JMPIM:
					pc = m[mem.traduzEndereco(ir.p, pm.state.tabelaPaginas)].p;
					break;
				case JMPIGM:
					if (reg[ir.r2] > 0) {
						pc = m[mem.traduzEndereco(ir.p, pm.state.tabelaPaginas)].p;
					} else {
						pc++;
					}
					break;
				case JMPILM:
					if (reg[ir.r2] < 0) {
						pc = m[mem.traduzEndereco(ir.p, pm.state.tabelaPaginas)].p;
					} else {
						pc++;
					}
					break;
				case JMPIEM:
					if (reg[ir.r2] == 0) {
						pc = m[mem.traduzEndereco(ir.p, pm.state.tabelaPaginas)].p;
					} else {
						pc++;
					}
					break;
				case JMPIGT:
					if (reg[ir.r1] > reg[ir.r2]) {
						pc = ir.p;
					} else {
						pc++;
					}
					break;
				case STOP:
					irpt = Interrupts.intSTOP;
					pm.desalocaProcesso(pm.state.id);
					break;
				case DATA:
					irpt = Interrupts.intInstrucaoInvalida;
					break;
				case SYSCALL:
					handleSyscall();
					irpt = Interrupts.intIO;
					pc++;
					break;
				default:
					irpt = Interrupts.intInstrucaoInvalida;
					break;
			}
		}

		private void handleSyscall() {
			int syscallCode = reg[8];
			int parameter = reg[9];
			switch (syscallCode) {
				case 1:
					System.out.println("IN " + pm.state.id);
					pm.state.ioParameter = parameter;
					break;
				case 2:
					output = vm.mem.m[mem.traduzEndereco(parameter, pm.state.tabelaPaginas)].p;
					outputId = pm.state.id;
					ioSemaphore.release();
					break;
				default:
					System.out.println("Unsupported system call: " + syscallCode);
					vm.cpu.irpt = Interrupts.intInstrucaoInvalida;
					break;
			}
		}

		private boolean checkInterruption() {
			if (irpt != Interrupts.noInterrupt) {
				interruptHandle();
				cpuCicles = 0;
				return true;
			} else if (cpuCicles == 3) {
				irpt = Interrupts.intProcessTimeup;
			}
			return false;
		}
	}

	// -------------------------------------------------------------------------------------------------------
	// --------------------- V M - constituida de CPU e MEMORIA
	// ---------------------------------------------
	// -------------------------------------------------------------------------------------------------------

	public class VM {
		public int tamMem;
		public int tamPag;
		public Word[] m;
		public Memory mem;
		public CPU cpu;

		public VM(BlockingQueue<String> commandQueue, Semaphore semaforo) {
			tamMem = 256;
			tamPag = 4;
			mem = new Memory(tamMem, tamPag);
			m = mem.m;
			cpu = new CPU(mem, true, commandQueue, semaforo); // true liga debug
		}
	}

	// -------------------------------------------------------------------------------------------------------
	// --------------------- S I S T E M A - definicoes do sistema
	// -------------------------------------------
	// -------------------------------------------------------------------------------------------------------

	public VM vm;
	public static Programas progs;
	private BlockingQueue<String> commandQueue;

	public Sistema() {
		commandQueue = new ArrayBlockingQueue<String>(100);
		Semaphore semaforo = new Semaphore(1);
		vm = new VM(commandQueue, semaforo);
		progs = new Programas();
	}

	public static void main(String args[]) {
		Sistema s = new Sistema();
		Scanner scanner = new Scanner(System.in);

		Thread shellThread = new Thread(() -> {
			System.out.println("Digite um comando:");
			System.out.println(
					"new - cria um novo processo (fatorial, subtrai, soma, progminimo, progloopinfinito)");
			System.out.println("rm - remove um processo");
			System.out.println("ps - lista os processos");
			System.out.println("dump [id] - mostra o conteúdo de um processo");
			System.out.println("dumpmem - mostra o conteúdo da memória");
			System.out.println("executa [id] - executa um processo");
			System.out.println("execall - executa todos os processos");
			System.out.println("allocateall - aloca todos os processos");
			System.out.println("trace1 - ativa o trace");
			System.out.println("trace0 - desativa o trace");
			System.out.println("exit - encerra o programa");

			while (true) {
				String opcao = scanner.nextLine();
				try {
					s.commandQueue.put(opcao);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		});
		shellThread.start();
		s.vm.cpu.start();
	}

	// -------------------------------------------------------------------------------------------------------
	// --------------------- P R O G R A M A S - definição dos programas
	// -------------------------------------
	// -------------------------------------------------------------------------------------------------------

	public class Programas {
		public Word[] fatorial = new Word[] {
				new Word(Opcode.LDI, 0, -1, 4),
				new Word(Opcode.LDI, 1, -1, 1),
				new Word(Opcode.LDI, 6, -1, 1),
				new Word(Opcode.LDI, 7, -1, 8),
				new Word(Opcode.JMPIE, 7, 0, 0),
				new Word(Opcode.MULT, 1, 0, -1),
				new Word(Opcode.SUB, 0, 6, -1),
				new Word(Opcode.JMP, -1, -1, 4),
				new Word(Opcode.STD, 1, -1, 10),
				new Word(Opcode.STOP, -1, -1, -1),
				new Word(Opcode.DATA, -1, -1, -1)
		};

		public Word[] progMinimo = new Word[] {
				new Word(Opcode.LDI, 0, -1, 999),
				new Word(Opcode.STD, 0, -1, 10),
				new Word(Opcode.STD, 0, -1, 11),
				new Word(Opcode.STD, 0, -1, 12),
				new Word(Opcode.STD, 0, -1, 13),
				new Word(Opcode.STD, 0, -1, 14),
				new Word(Opcode.STOP, -1, -1, -1),
				new Word(Opcode.DATA, -1, -1, -1),
				new Word(Opcode.DATA, -1, -1, -1),
				new Word(Opcode.DATA, -1, -1, -1),
				new Word(Opcode.DATA, -1, -1, -1),
				new Word(Opcode.DATA, -1, -1, -1),
				new Word(Opcode.DATA, -1, -1, -1),
				new Word(Opcode.DATA, -1, -1, -1),
				new Word(Opcode.DATA, -1, -1, -1)
		};

		public Word[] progLoopInfinito = new Word[] {
				new Word(Opcode.LDI, 0, -1, 1), // Carrega o valor 1 no registrador 0
				new Word(Opcode.STD, 0, -1, 11), // Armazena o valor do registrador 0 na posição de memória 10
				new Word(Opcode.LDI, 1, -1, 2), // Carrega o valor 2 no registrador 1
				new Word(Opcode.STD, 1, -1, 12), // Armazena o valor do registrador 1 na posição de memória 11
				new Word(Opcode.LDI, 2, -1, 3), // Carrega o valor 3 no registrador 2
				new Word(Opcode.STD, 2, -1, 13), // Armazena o valor do registrador 2 na posição de memória 12
				new Word(Opcode.LDI, 3, -1, 4), // Carrega o valor 4 no registrador 3
				new Word(Opcode.STD, 3, -1, 14), // Armazena o valor do registrador 3 na posição de memória 13
				new Word(Opcode.LDI, 4, -1, 5), // Carrega o valor 5 no registrador 4
				new Word(Opcode.STD, 4, -1, 15), // Armazena o valor do registrador 4 na posição de memória 14
				new Word(Opcode.JMP, -1, -1, 1), // Pula para a instrução na posição de memória 10 (loop infinito)
				new Word(Opcode.DATA, -1, -1, -1), // Espaço de dados
				new Word(Opcode.DATA, -1, -1, -1), // Espaço de dados
				new Word(Opcode.DATA, -1, -1, -1), // Espaço de dados
				new Word(Opcode.DATA, -1, -1, -1), // Espaço de dados
				new Word(Opcode.DATA, -1, -1, -1), // Espaço de dados
				new Word(Opcode.DATA, -1, -1, -1), // Espaço de dados
				new Word(Opcode.DATA, -1, -1, -1), // Espaço de dados
				new Word(Opcode.DATA, -1, -1, -1) // Espaço de dados
		};

		public Word[] soma = new Word[] {
				new Word(Opcode.LDI, 0, -1, 5),
				new Word(Opcode.ADDI, 0, -1, 5),
				new Word(Opcode.STD, 0, -1, 6),
				new Word(Opcode.STOP, -1, -1, -1)
		};

		public Word[] subtrai = new Word[] {
				new Word(Opcode.LDI, 0, -1, 1), // Carrega o valor 1 no registrador 0
				new Word(Opcode.STD, 0, -1, 11), // Armazena o valor do registrador 0 na posição de memória 11
				new Word(Opcode.LDI, 1, -1, 2), // Carrega o valor 2 no registrador 1
				new Word(Opcode.STD, 1, -1, 12), // Armazena o valor do registrador 1 na posição de memória 12
				new Word(Opcode.LDI, 8, -1, 1), // Carrega o código da syscall no registrador 8
				new Word(Opcode.LDI, 9, -1, 0), // Carrega o parâmetro da syscall no registrador 9
				new Word(Opcode.SYSCALL, -1, -1, -1), // Chamada de sistema para IO
				new Word(Opcode.JMP, -1, -1, 1), // Pula para a instrução na posição de memória 1 (loop infinito)
				new Word(Opcode.DATA, -1, -1, -1), // Espaço de dados
				new Word(Opcode.DATA, -1, -1, -1), // Espaço de dados
				new Word(Opcode.DATA, -1, -1, -1), // Espaço de dados
				new Word(Opcode.DATA, -1, -1, -1), // Espaço de dados
				new Word(Opcode.DATA, -1, -1, -1), // Espaço de dados
				new Word(Opcode.DATA, -1, -1, -1), // Espaço de dados
				new Word(Opcode.DATA, -1, -1, -1), // Espaço de dados
				new Word(Opcode.DATA, -1, -1, -1) // Espaço de dados
		};

		public Word[] chamain = new Word[] {
				new Word(Opcode.LDI, 0, -1, 5),
				new Word(Opcode.ADDI, 0, -1, 5),
				new Word(Opcode.STD, 0, -1, 7), // Armazena o valor do registrador 0 na posição de memória 6
				new Word(Opcode.LDI, 8, -1, 1), // Carrega o código da syscall no registrador 8
				new Word(Opcode.LDI, 9, -1, 8), // Carrega o parâmetro da syscall no registrador 9
				new Word(Opcode.SYSCALL, -1, -1, -1), // Chamada de sistema para IO
				new Word(Opcode.STOP, -1, -1, -1),
				new Word(Opcode.DATA, -1, -1, -1), // Espaço de dados
				new Word(Opcode.DATA, -1, -1, -1), // Espaço de dados
		};

		public Word[] chamaout = new Word[] {
				new Word(Opcode.LDI, 0, -1, 5),
				new Word(Opcode.ADDI, 0, -1, 5),
				new Word(Opcode.STD, 0, -1, 7), // Armazena o valor do registrador 0 na posição de memória 7
				new Word(Opcode.LDI, 8, -1, 2), // Carrega o código da syscall no registrador 8
				new Word(Opcode.LDI, 9, -1, 7), // Carrega o parâmetro da syscall no registrador 9
				new Word(Opcode.SYSCALL, -1, -1, -1), // Chamada de sistema para IO
				new Word(Opcode.STOP, -1, -1, -1),
				new Word(Opcode.DATA, -1, -1, -1), // Espaço de dados
				new Word(Opcode.DATA, -1, -1, -1), // Espaço de dados
		};
	}

	// -------------------------------------------------------------------------------------------------------
	// --------------------- U T I L I T A R I O S D O S I S T E M A
	// -----------------------------------------
	// -------------------------------------------------------------------------------------------------------

	public void loadProgram(Word[] p, Word[] m) {
		for (int i = 0; i < p.length; i++) {
			m[i].opc = p[i].opc;
			m[i].r1 = p[i].r1;
			m[i].r2 = p[i].r2;
			m[i].p = p[i].p;
		}
	}

	private void loadAndExec(Word[] p) {
		loadProgram(p, vm.m); // carga do programa na memoria
		System.out.println("---------------------------------- programa carregado na memoria");
		vm.mem.dump(0, p.length); // dump da memoria nestas posicoes
		vm.cpu.setContext(0, vm.tamMem - 1, 0, null); // seta estado da cpu
		System.out.println("---------------------------------- inicia execucao ");
		vm.cpu.run(); // cpu roda programa ate parar
		System.out.println("---------------------------------- memoria após execucao ");
		vm.mem.dump(0, p.length); // dump da memoria com resultado
	}
}
