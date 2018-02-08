import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class OS extends Thread {

	public volatile boolean FP;
	public volatile int fileInputQueueSize = 5;
	public volatile int consoleInputQueueSize = 5;
	private final int QUANTUM = 5;
	private volatile CPU cpu;
	private volatile Memory memory;
	private volatile List<ProcessImage> readyQueue;
	private volatile List<ProcessImage> blockedQueue;
	private volatile List<ProcessImage> fileInputQueue;
	private volatile List<Integer> consoleInputQueue;
	private volatile Semaphore mutex;	
	private FileProducerThread fileProducerThread;
	private FileConsumerThread fileConsumerThread;
	private InputProducerThread inputProducerThread;
	private InputConsumerThread inputConsumerThread;
	private volatile boolean [] bitmap;
	private volatile Semaphore mutexFile; 
	private volatile Semaphore emptyFile;
	private volatile Semaphore fullFile;
	private volatile Semaphore mutexInput;
	private volatile Semaphore emptyInput;
	private volatile Semaphore fullInput;
	private volatile Semaphore deneme;
	private volatile Semaphore deneme2;

	public OS(int size) {
		this.memory = new Memory(size);
		this.cpu = new CPU(memory);
		this.mutex=new Semaphore(1);
		this.mutexFile = new Semaphore(1);
		this.emptyFile = new Semaphore(fileInputQueueSize);
		this.fullFile = new Semaphore(0);
		this.mutexInput = new Semaphore(1);
		this.emptyInput = new Semaphore(consoleInputQueueSize);
		this.fullInput = new Semaphore(0);
		this.readyQueue = new ArrayList<ProcessImage>();
		this.blockedQueue = new ArrayList<ProcessImage>();
		this.fileInputQueue = new ArrayList<ProcessImage>();
		this.consoleInputQueue = new ArrayList<Integer>();
		this.bitmap = new boolean[size];//dolu olunca true
		this.FP = true;
		this.deneme = new Semaphore(0);
		this.deneme2 = new Semaphore(0);
		
		this.fileProducerThread = new FileProducerThread(fileInputQueue,mutexFile,emptyFile,fullFile, FP, deneme);
		this.fileConsumerThread = new FileConsumerThread(fileInputQueue, readyQueue, bitmap, memory, mutexFile,emptyFile,fullFile,mutex, deneme2);
		this.inputProducerThread = new InputProducerThread(consoleInputQueue,mutexInput,fullInput,emptyInput);
		this.inputConsumerThread = new InputConsumerThread(consoleInputQueue,mutex,mutexInput,fullInput,emptyInput, blockedQueue,readyQueue);
		fileProducerThread.start();
		fileConsumerThread.start();
		inputProducerThread.start();
		inputConsumerThread.start();
	}


	public void loadProcess(String processFile,Assembler assembler)
	{
		try {
			System.out.println( "Creating binary file for "+ processFile+"...") ;
			int instructionSize = assembler.createBinaryFile(processFile, "assemblyInput.bin");
			char[] process = assembler.readBinaryFile(instructionSize, "assemblyInput.bin");

			System.out.println("Loading process to memory...");

			mutex.acquire();

			readyQueue.add(new ProcessImage(processFile,memory.getEmptyIndex(),instructionSize)); // readyqueue ya ekledi.

			mutex.release();

			this.memory.addInstructions(process, instructionSize, memory.getEmptyIndex()); // memory e koydu.
			System.out.println("Process is loaded !");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	
	@Override
	public void run() {
		try {
			while (true) {

				mutex.acquire();
				boolean isBlockedQueueEmpty = blockedQueue.isEmpty();
				boolean isReadyQueueEmpty = readyQueue.isEmpty();
				//boolean isFileInputQueueEmpty = fileInputQueue.isEmpty();
				mutex.release();

				/*
				if(isBlockedQueueEmpty && isReadyQueueEmpty) { //????
					if(deneme.tryAcquire(1,TimeUnit.SECONDS))
						break;
				}
				*/
				if (!isReadyQueueEmpty) {
					System.out.println("Executing " + (readyQueue.get(0)).processName);
					cpu.transferFromImage(readyQueue.get(0));
					for (int i = 0; i < QUANTUM; i++) {
						if (cpu.getPC() < cpu.getLR()) {
							cpu.fetch(); 
							int returnCode = cpu.decodeExecute();

							if (returnCode == 0)  {
								System.out.println("Process " + readyQueue.get(0).processName + " made a system call for ");
								if (cpu.getV() == 0) {
									System.out.println( "Input, transfering to blocked queue and waiting for input...");
									ProcessImage p=new ProcessImage();
									this.cpu.transferToImage(p);
									
									mutex.acquire();
									readyQueue.remove(0);
									blockedQueue.add(p);
									mutex.release();
								} 
								else { //syscall for output
									System.out.print("Output Value: ");
									ProcessImage p=new ProcessImage();
									cpu.transferToImage(p);

									mutex.acquire();
									readyQueue.remove(0);
									System.out.println( p.V +"\n");
									readyQueue.add(p);
									mutex.release();
								}
								//Process blocked, need to end quantum prematurely
								break;
							}
						}
						else {
							System.out.println("Process " + readyQueue.get(0).processName +" has been finished! Removing from the queue...\n" );
							ProcessImage p = new ProcessImage();
							cpu.transferToImage(p);
							p.writeToDumpFile();
							//empty the memory//
							mutex.acquire();
							for(int g = cpu.getBR() ;g <= cpu.getLR(); g++)
								bitmap[g] = false;
							System.out.println("Memory held by " + readyQueue.get(0).processName + " has been freed.");
							mutex.release();
							///////////////////
							mutex.acquire();
							readyQueue.remove(0);
							mutex.release();
							break;
						}

						if (i == QUANTUM - 1) {
							//quantum finished put the process at the end of readyQ
							System.out.println ("Context Switch! Allocated quantum have been reached, switching to next process...\n");
							ProcessImage p = new ProcessImage();
							cpu.transferToImage(p);  

							mutex.acquire();
							readyQueue.remove(0);
							readyQueue.add(p);
							mutex.release();
						}
					}
				}
			}
			
			//System.out.println("bura");
			/*
			inputProducerThread.stopThread();
			inputConsumerThread.stopThread();
			fileConsumerThread.stopThread();*/
			//System.out.println("Execution of all processes has finished!");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
