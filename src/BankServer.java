import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.io.PrintWriter;
import java.io.IOException;

public class BankServer {
	private static final int BANKPORT = 8888;
	private BankAccount account;
	private Socket socket = new Socket();
	public static ExecutorService executor;

	// private ServerSocket serverSocket;

	public BankServer() {
		account = new BankAccount();
	}

	public void init() {
		executor = Executors.newWorkStealingPool(2);
		try {
			// try{
			// serverSocket = new ServerSocket(BANKPORT);
			try (ServerSocket serverSocket = new ServerSocket(BANKPORT)) {
				System.out.println("Socket created.");

				while (true) {
					System.out.println(
							"Listening for a connection on the local port " + serverSocket.getLocalPort() + "...");
					socket = serverSocket.accept();
					System.out.println("\nA connection established with the remote port " + socket.getPort() + " at "
							+ socket.getInetAddress().toString());

					executor.submit(new BankRunnable((socket)));

					// executeCommand(socket);
					// new Thread(new BankRunnable(socket)).start();

				}
			}
			// finally{
			// serverSocket.close();
			// }
		} catch (IOException exception) {
			exception.printStackTrace();
		}
	}

	private void executeCommand(Socket socket) {
		try {
			try {
				Scanner in = new Scanner(socket.getInputStream());
				PrintWriter out = new PrintWriter(socket.getOutputStream());
				System.out.println("I/O setup done");

				while (true) {
					if (in.hasNext()) {
						String command = in.next();
						if (command.equals("QUIT")) {
							System.out.println("QUIT: Connection being closed.");
							out.println("QUIT accepted. Connection being closed.");
							out.close();
							return;
						}
						accessAccount(command, in, out);
					}
				}
			} finally {
				socket.close();
				System.out.println("A connection is closed.");

			}
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	private void accessAccount(String command, Scanner in, PrintWriter out) throws InterruptedException {
		double amount;
		if (command.equals("DEPOSIT")) {
			amount = in.nextDouble();
			account.deposit(amount);
			System.out.println("DEPOSIT: Current balance: " + account.getBalance());
			out.println("DEPOSIT Done. Current balance: " + account.getBalance());
		} else if (command.equals("WITHDRAW")) {
			amount = in.nextDouble();
			account.withdraw(amount);
			System.out.println("WITHDRAW: Current balance: " + account.getBalance());
			out.println("WITHDRAW Done. Current balance: " + account.getBalance());
		} else if (command.equals("BALANCE")) {
			System.out.println("BALANCE: Current balance: " + account.getBalance());
			out.println("BALANCE accepted. Current balance: " + account.getBalance());
		} else if (command.equals("TERMINATE")) {
			System.out.println("TERMINATING");

			executor.shutdown();
			executor.awaitTermination(5, TimeUnit.SECONDS);

			if (!executor.isTerminated()) {

				System.err.println("Cancel non-finished tasks..");
			}
			executor.shutdownNow();
			System.out.println("\nShutdown finished");
		}

		else {
			System.out.println("Invalid Command");
			out.println("Invalid Command. Try another command.");
		}
		out.flush();
	}

	private class BankRunnable implements Runnable {
		BankServer bankServer = new BankServer();
		ThreadLocal<Date> threadLocal = new ThreadLocal<Date>();

		public BankRunnable(Socket socket1) {
			socket1 = socket;
			// bankServer.executeCommand(socket1);
		}

		@Override
		public void run() {
			Date date = new Date();
			threadLocal.set(new Timestamp(date.getTime()));

			bankServer.executeCommand(socket);
			System.out.println("---  LOGS  --- \n '" + Thread.currentThread().getName() + "' is getting executed!! \n"
					+ "The timestamp of this is: " + threadLocal.get());

		}

	}

	public static void main(String[] args) {
		BankServer server = new BankServer();
		server.init();
		try {
			server.socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
