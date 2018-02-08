import java.util.List;
import java.util.concurrent.Semaphore;

public class InputConsumerThread extends Thread {

	private volatile boolean isRunning;
	private volatile List<Integer> consoleInputQueue;
	private volatile List<ProcessImage> readyQueue;
	private volatile List<ProcessImage> blockedQueue;
	private volatile Semaphore mutexInput;
	private volatile Semaphore fullInput;
	private volatile Semaphore emptyInput;
	private volatile Semaphore mutex;
		
	public InputConsumerThread(List<Integer> consoleInputQueue, Semaphore mutex0, Semaphore mutex, Semaphore full, Semaphore empty, List<ProcessImage> blockedQueue, List<ProcessImage> readyQueue)
	{
		this.consoleInputQueue = consoleInputQueue;
		this.blockedQueue = blockedQueue;
		this.readyQueue = readyQueue;
		this.mutexInput = mutex;
		this.emptyInput = empty;
		this.fullInput = full;
		this.mutex = mutex0;
	}
	
	
	
	@Override
	public void run(){
		
		isRunning = true;	
		try 
		{
			
		
			while(isRunning)
			{
				fullInput.acquire();
				//System.out.println("bitti1");
				mutexInput.acquire();
				//System.out.println("bitti2");
				int i = consoleInputQueue.get(0);
				consoleInputQueue.remove(0);
				System.out.println("ConsoleConsumer: " + i + " is received from console input queue.");
				mutexInput.release();
				emptyInput.release();
			
				mutex.acquire();
				boolean isBlockedQueueEmpty = blockedQueue.isEmpty();
				mutex.release();
			
				if(!isBlockedQueueEmpty)
				{
					System.out.println("ConsoleConsumer: Blocked queue is not empty.");
					mutex.acquire();
					ProcessImage p = blockedQueue.get(0);
					blockedQueue.remove(0);
					p.V = i;
					readyQueue.add(p);
					System.out.println("ConsoleConsumer: " + p.processName + " is removed from blocked queue, consumed " + i + " and "
							+ "added to ready queue.");
					mutex.release();

				}
			
				else
				{	
					System.out.println("ConsoleConsumer: Blocked queue is empty. Sleeping.");
					sleep(2000);
				}
			}
		
		}
		catch (InterruptedException e) 
		{
			e.printStackTrace();
		}
		
	}
	
	
	public void stopThread() {
		isRunning = false;
	}
}
