package ru.avalon.javapp.j130.client;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class Client extends Thread {
    private final Socket socket;
    public static final int SERVER_PORT = 45678;
    private final ObjectInputStream inputStream;
    private final ObjectOutputStream outputStream;
    private static BufferedReader in;

    private String login;
    private String password;
    private String host;
    private String input;
    private static String command;
    private static String fileOrUser;
    private String user;
    private String file;
    private List<String> list;


    public Client() throws IOException {
        in = new BufferedReader(new InputStreamReader(System.in));
        check();
        socket = new Socket(host, SERVER_PORT);
        this.outputStream = new ObjectOutputStream(socket.getOutputStream());
        this.inputStream = new ObjectInputStream(socket.getInputStream());
        this.outputStream.writeUTF(login);
        this.outputStream.flush();
        this.outputStream.writeUTF(password);
        this.outputStream.flush();
        if (this.inputStream.readUTF().equals("No")) {
            in.close();
            socket.close();
            throw new IllegalArgumentException("Incorrect login or password or host");
        }
    }

    private void check() {
        try {
            do {
                System.out.println(">>> Enter your login:");
                login = in.readLine();
            } while (login == null || login.trim().isEmpty());

            do {
                System.out.println(">>> Enter your password:");
                password = in.readLine();
            } while (password == null || password.trim().isEmpty());

            do {
                System.out.println(">>> Enter the server host:");
                host = in.readLine();
            } while (host == null || host.trim().isEmpty());
        } catch (IOException e) {
            System.out.println("Error check()");
        }

    }

    /**
     * Functionality :
     * “put <file>”, “put *<file>” — загрузить файл на сервер и сделать его общедоступным
     *                              (вариант  без звёздочки),
     *                              либо недоступным другим пользователям (вариант со звёздочкой);
     * “get <file>” — скачать файл на локальный компьютер;
     * “del <file>” — удалить файл на сервере;
     * “list” — получить список своих файлов на сервере;
     * “users” — получить список зарегистрированных на сервере пользователей;
     * “list <user>” — получить список файлов заданного пользователя;
     * “get <user>/<file>” — скачать файл пользователя на локальный компьютер;
     */
    @Override
    public void run() {
        System.out.println("The connection is established!");
        while (true) {
            try {
                do {
                    System.out.println(">>> Waiting for input: ");
                    input = in.readLine();
                } while (input == null || input.trim().isEmpty());
                outputStream.writeUTF(input);
                outputStream.flush();

                if (input.contains("/")) {
                    parsing();
                    user = list.get(1);
                    file = list.get(2);
                    downloadFile(file);
                    continue;
                }
                if (input.contains(" ")) {
                    parsing();
                    fileOrUser = list.get(1);
                    try {
                        checkCommand();
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    }
                    continue;
                }
                if (input.equals("list")) {
                getMyFiles();
                    continue;
                }
                if (input.equals("users")) {
                getUsers();
                }
            } catch (IOException e) {
                System.out.println("Error 1" + e.getMessage());
            }
        }
    }

    private void parsing () {
        String[] array = input.split("[^A-Za-z0-9\\.]");
        list = Arrays.stream(array).filter(e -> !e.equals("")).toList();
        command = list.get(0);
    }
    private void checkCommand() throws IOException {
        switch (command) {
            case "put" -> uploadFileToServer();
            case "get" -> downloadFile(fileOrUser);
            case "del" -> deleteFile();
            case "list" -> getFilesOfUser();
            default -> System.out.println("You have entered incorrect data!");
        }
    }

    /**
     * Загрузить файл на сервер
     */
    private void uploadFileToServer() throws IOException {
        Path path = Path.of(fileOrUser);
        if(Files.exists(path)) {
            outputStream.writeLong(Files.size(path));
            outputStream.flush();
            outputStream.write(Files.readAllBytes(path));
            outputStream.flush();
            System.out.println(inputStream.readUTF());
        } else {
            System.out.println("File not found");
        }
    }

    /**
     * скачать файл на компьютер
     */
    private void downloadFile(String f) throws IOException {
        String s = inputStream.readUTF();
        if(s.equals("Ok")) {
            byte[] buffer = new byte[(int) inputStream.readLong()];
            inputStream.read(buffer);
            Path path = Path.of(f);
            Files.write(path, buffer);
            System.out.println("File " + file + " saved");
        } else {
            System.err.println(s);
        }
    }

    /**
     * удалить файл на сервере
     */
    private void deleteFile() throws IOException {
        System.out.println(inputStream.readUTF());
    }

    /**
     *  получить список своих файлов на сервере
     */
    private void getMyFiles() throws IOException {
        readInputStrings();
    }

    /**
     * получить список зарегистрированных на сервере пользователей
     */
    private void getUsers() throws IOException {
        System.out.println(inputStream.readUTF());
    }

    /**
     * получить список файлов заданного пользователя
     */
    private void getFilesOfUser() throws IOException {
        readInputStrings();
    }

    private void readInputStrings() throws IOException {
        int size = inputStream.readInt();
        for (int i = 0; i < size; i++)
            System.out.println(inputStream.readUTF());
    }
    public static void main(String[] args) throws IOException {
        Client client = new Client();
        client.start();
        try {
            client.join();
        } catch (InterruptedException e) {
            in.close();
            client.socket.close();
        }
        in.close();
        client.socket.close();
    }
}

