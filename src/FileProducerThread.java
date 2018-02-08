import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Semaphore;

public class FileProducerThread extends Thread {
	
	public volatile boolean FP;
	private volatile List<ProcessImage> fileInputQueue;
	private volatile Semaphore mutexFile;
	private volatile Semaphore emptyFile;
	private volatile Semaphore fullFile;
	private volatile Semaphore deneme;
	
	
	public FileProducerThread(List<ProcessImage> fileInputQueue, Semaphore mutexFile, Semaphore emptyFile, Semaphore fullFile, boolean FP, Semaphore deneme)
	{
		this.fileInputQueue = fileInputQueue;
		this.mutexFile = mutexFile;
		this.fullFile = fullFile;
		this.emptyFile = emptyFile;
		this.FP = FP;
		this.deneme = deneme;
	}
	
	@Override
	public void run(){
		
		try
		{
			FileReader fileReader = new FileReader("inputSequence.txt");
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String line;
		
			while((line = bufferedReader.readLine()) != null)
			{
				String[] tokens = line.split(" ");
				String path = tokens[0];
				int indexOfDot = tokens[0].indexOf(".");
				String filename = tokens[0].substring(0,indexOfDot);
				Integer sleepTime = Integer.parseInt(tokens[1]);
				System.out.println("FileProducer: File " + filename + " is read from file having " + sleepTime + " sleep time.");
			
				emptyFile.acquire();
				mutexFile.acquire();
			
				Assembler a = new Assembler();
				String outFile = filename + ".bin";
				int instructionCount = a.createBinaryFile(path, outFile);
				//instructionCount /= 4;
				System.out.println("FileProducer: "+ outFile + " binary file created, this process has " + instructionCount/4 + " instructions.");
				ProcessImage p = new ProcessImage(outFile,0,instructionCount);
				fileInputQueue.add(p);
				System.out.println("FileProducer: " + outFile + " is added to file input queue.");	
				mutexFile.release();
				fullFile.release();
			
				sleep(sleepTime);
				System.out.println("FileProducer: Sleeping for " + sleepTime);
			}
			
			FP = false;
			deneme.release();
			fileReader.close();
		}
		catch (InterruptedException | IOException e) 
		{
			e.printStackTrace();
		}
	}
}


