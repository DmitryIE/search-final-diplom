import com.google.gson.Gson;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class Main {

    private static final int PORT = 8989;
    private static final String dirName = "pdfs";

    public static void main(String[] args) throws Exception {

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            BooleanSearchEngine engine = new BooleanSearchEngine(new File(dirName));
            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     BufferedReader reader = new BufferedReader
                             (new InputStreamReader(clientSocket.getInputStream()))) {
                    PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                    List<PageEntry> searchingResult = engine.search(reader.readLine());
                    writer.println(new Gson().toJson(searchingResult));
                }
            }
        } catch (IOException e) {
            System.out.println("Не могу стартовать сервер");
            e.printStackTrace();
        }
    }
}