import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class InputProducerThread extends Thread {

	private volatile boolean isRunning;
	private volatile List<Integer> consoleInputQueue;
	private volatile Semaphore mutexInput;
	private volatile Semaphore fullInput;
	private volatile Semaphore emptyInput;
	
	public InputProducerThread(List<Integer> consoleInputQueue, Semaphore mutex, Semaphore full, Semaphore empty)
	{
		this.consoleInputQueue = consoleInputQueue;
		this.mutexInput = mutex;
		this.emptyInput = empty;
		this.fullInput = full;
	}
	
	@Override
	public void run(){
		
		isRunning = true;	
		try
		{
			int num;
			
			Scanner in = new Scanner(System.in); 
			
			while(isRunning)
			{
				emptyInput.acquire();
				mutexInput.acquire();
				num = in.nextInt();
				consoleInputQueue.add(num);
				System.out.println("ConsoleProducer: " + num + " is added to console input queue.");
				mutexInput.release();
				//System.out.println("bittimi");
				fullInput.release();				
			}
			
			in.close();			
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












