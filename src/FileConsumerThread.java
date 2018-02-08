import java.util.List;
import java.util.concurrent.Semaphore;

public class FileConsumerThread extends Thread {
	
	private volatile boolean isRunning;
	private volatile List<ProcessImage> fileInputQueue;
	private volatile List<ProcessImage> readyQueue;
	private volatile boolean [] bitmap;
	private volatile Memory memory;
	private volatile Semaphore mutex;
	private volatile Semaphore mutexFile;
	private volatile Semaphore emptyFile;
	private volatile Semaphore fullFile;
	
	public FileConsumerThread(List<ProcessImage> fileInputQueue, List<ProcessImage> readyQueue, boolean [] bitmap, Memory memory, Semaphore mutexFile, Semaphore emptyFile, Semaphore fullFile, Semaphore mutex, Semaphore deneme2)
	{
		this.fileInputQueue = fileInputQueue;
		this.readyQueue = readyQueue;
		this.bitmap = bitmap;
		this.memory = memory;
		this.mutex = mutex;
		this.mutexFile = mutexFile;
		this.fullFile = fullFile;
		this.emptyFile = emptyFile;

	}
	
	public void firstFit(int instructionSize, int [] points)
	{
		int counter = 0;
		for(int i = 0; i<bitmap.length ; i++)
		{
			if(!bitmap[i])
			{
				counter++;
				//System.out.println(bitmap[i] + " " + "false" + counter);
			}
		
			else
			{
				counter = 0;
				//System.out.println(bitmap[i] + " " + "true" + counter);
			}
		
			if(counter == (instructionSize))
			{
				//points[1] = i;
				points[0] = i - (instructionSize)+1;
				//System.out.println(instructionSize + " " + memoryEnd + " "+ memoryStart);
				break;
			}
		
		}
	}
	
	public void fitFound(int instructionSize, String filename, int [] points)
	{
		//System.out.println(points[0] + " "+points[1]);
		Assembler a = new Assembler();
		char [] buffer = a.readBinaryFile(instructionSize, filename);
		memory.addInstructions(buffer, instructionSize, points[0]);
		for(int k = points[0] ; k <= instructionSize+points[0] ; k++) 
			bitmap[k] = true;
		//String pname = "Process " + (ProcessNumber++);
		ProcessImage p2 = new ProcessImage(filename,points[0],instructionSize);
		readyQueue.add(p2);
		System.out.println("File Consumer: There is enough space, indices "+ 
		points[0] + " to " +(instructionSize+points[0]-1) + ". Process " + filename + " is added to the ready queue. ");
		//points[0] = -1;
		//points[1] = -1;
	}
	
	@Override
	public void run(){
		
		isRunning = true;
		
		while(isRunning)
		{
			try
			{
				fullFile.acquire();
				mutexFile.acquire();
			
				ProcessImage p = fileInputQueue.get(0);
				fileInputQueue.remove(0);
				System.out.println("File Consumer: " + p.processName + " is removed from file input queue.");
				mutexFile.release();
				emptyFile.release();

				String filename = p.processName; //the binary file 
				int instructionSize = p.LR; //number of instructions

				int [] points = {-1,-1};

			
				mutex.acquire();
				//firstFit(instructionSize, memoryEnd, memoryStart);
				//System.out.println(memoryStart + " " + memoryEnd);
				
				firstFit(instructionSize, points);
				//System.out.println(points[0] + " " + points[1]);
				
				System.out.println("File Consumer: bitmap is traversed for available space.");
				
				if(points[0] != -1)
				{
					fitFound(instructionSize,filename,points);
					
				}
			
				mutex.release();
			
				while(points[0] == -1)
				{
					System.out.println("File Consumer: This process cannot fit to the memory. Sleeping.");
					firstFit(instructionSize, points);
					if(points[0] != -1)
					{
						//buldu
						fitFound(instructionSize,filename,points);
						break;
					}
					Thread.yield();
					sleep(2000);
				}
			}
			
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public void stopThread() {
		isRunning = false;
	}
	
}
